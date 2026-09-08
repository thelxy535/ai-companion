package com.companion.cc.domain.character

import com.companion.cc.domain.engine.EmotionalEngine
import com.companion.cc.domain.memory.MemoryRetrievalService
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.commitment.CommitmentRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一角色回复组装器（V9PM 第 3 项）。
 *
 * 所有回复入口（正式聊天、图片消息、试聊、主动消息）共用同一套组装：
 * 角色人格 system prompt + 已确认长期记忆 + 自我叙事 + 时间感知 + Character Book 知识库 + 情绪/脾气指令，
 * 避免同一角色在不同入口说话方式分裂。
 */
@Singleton
class CharacterReplyAssembler @Inject constructor(
    private val memoryRetrievalService: MemoryRetrievalService,
    private val emotionalEngine: EmotionalEngine,
    private val characterRepository: CustomCharacterRepository,
    private val characterBookEngine: CharacterBookEngine,
    private val messageRepository: MessageRepository,
    private val commitmentRepository: CommitmentRepository,
    private val innerStateRepository: InnerStateRepository
) {

    /**
     * 组装完整 system prompt。
     *
     * @param baseSystem 角色人格 system prompt（正式聊天由 resolver 提供，试聊由草稿构建）
     * @param userId 用户 ID
     * @param companionId 角色 ID
     * @param query 用于召回长期记忆和触发知识库的当前消息（可为空）
     * @param includeTemperament 是否附加情绪/脾气指令（主动消息可关闭以降低侵入性）
     */
    suspend fun assemble(
        baseSystem: String,
        userId: String,
        companionId: String,
        query: String = "",
        includeTemperament: Boolean = true,
        onTrace: ((String) -> Unit)? = null,
        onRetrievalError: ((Throwable) -> Unit)? = null
    ): String {
        val scope = MemoryScopeKey.forCharacter(userId, companionId)
        val sb = StringBuilder(baseSystem)
        val customCharacter = runCatching {
            characterRepository.getCharacterByIdForUser(companionId, userId)
        }.getOrNull()
        val rhythm = customCharacter?.rhythm ?: defaultRhythmFor(companionId)
        val previousState = innerStateRepository.load(userId, companionId)
        val advanced = CompanionMindEngine.advance(
            previous = previousState,
            emotionalState = emotionalEngine.getEmotionalState(userId, companionId),
            rhythm = rhythm,
            nowHour = LocalDateTime.now().hour + LocalDateTime.now().minute / 60.0,
            elapsedMs = if (previousState.updatedAt > 0L) System.currentTimeMillis() - previousState.updatedAt else 0L
        )
        innerStateRepository.save(userId, companionId, advanced.state)

        // 1) 已确认的长期记忆（相关性召回）
        if (query.isNotBlank()) {
            runCatching {
                val result = memoryRetrievalService.retrieve(query, scope)
                onTrace?.invoke(result.traceId)
                if (result.memories.isNotEmpty()) {
                    val currentState = emotionalEngine.getEmotionalState(userId, companionId)
                    val temperament = customCharacter
                        ?.let { TemperamentProfile.fromPersonality(it.personality) }
                        ?: emotionalEngine.temperamentFor(companionId)
                    val resonance = MemoryResonanceEngine.resonate(
                        previous = advanced.state,
                        memory = result.memories.first().node.content,
                        relevance = 0.75f,
                        rhythm = rhythm
                    )
                    innerStateRepository.save(userId, companionId, resonance.state)
                    sb.append("\n\n【已确认的长期记忆】\n")
                    sb.append(result.memories.joinToString("\n") {
                        MemoryResonanceNarrative.build(it.node.content, currentState, rhythm)
                    })
                    sb.append("\n只在当前话题自然相关时轻轻带出这段记忆，不要为了证明记得而主动转题。")
                    sb.append("\n\n").append(MemoryRecallStyle.build(temperament, rhythm, currentState))
                }
            }.onFailure { error ->
                onRetrievalError?.invoke(error)
            }
        }

        // 2) 自我叙事：TA 对这段关系的当前理解
        runCatching {
            val narratives = memoryRetrievalService.relationshipNarratives(scope)
            if (narratives.isNotEmpty()) {
                sb.append("\n\n【我对这段关系的理解】\n")
                sb.append(narratives.joinToString("\n") { "- ${it.content}" })
            }
        }

        // 3) 时间感知（V9PM 深度③）：当前时间、距上次聊天、未完成的约定
        runCatching {
            val lastChatAt = messageRepository
                .observeLatestMessage(userId, companionId)
                .first()?.timestamp
            val openCommitment = commitmentRepository.forCharacter(userId, companionId)
                .filter { it.status == "ACTIVE" && it.dueAt > System.currentTimeMillis() }
                .minByOrNull { it.dueAt }
            TimeAwarenessBuilder.build(
                nowMs = System.currentTimeMillis(),
                lastChatAtMs = lastChatAt,
                commitment = openCommitment?.promise,
                commitmentDueAtMs = openCommitment?.dueAt
            )?.let { sb.append(it) }
        }

        // 4) 角色自己的连续状态：只注入有内容的部分，避免把角色变成任务清单。
        runCatching {
            val state = advanced.state
            val physical = state.effectiveSceneState()
            val lines = listOfNotNull(
                state.caringAbout.takeIf { it.isNotBlank() }?.let { "最近在意：$it" },
                state.unfinishedThought.takeIf { it.isNotBlank() }?.let { "还没说完：$it" },
                state.lookingForwardTo.takeIf { it.isNotBlank() }?.let { "最近期待：$it" },
                state.avoiding.takeIf { it.isNotBlank() }?.let { "暂时回避：$it" },
                state.scene.takeIf { it.isNotBlank() }?.let { "此刻场景：$it" },
                state.currentActivity.takeIf { it.isNotBlank() }?.let { "正在做的事：$it" },
                state.recentAction.takeIf { it.isNotBlank() }?.let { "上一条动作：$it（不要机械重复，可自然承接或省略）" },
                physical.location.takeIf { it.isNotBlank() }?.let { "所在地点：$it" },
                physical.room.takeIf { it.isNotBlank() }?.let { "所在房间：$it" },
                physical.posture.takeIf { it.isNotBlank() }?.let { "当前姿态：$it" },
                physical.clothing.takeIf { it.isNotBlank() }?.let { "当前穿着：$it" },
                physical.heldItem.takeIf { it.isNotBlank() }?.let { "手中物品：$it" },
                state.lifeThread.takeIf { it.isNotBlank() }?.let { "自己的生活线：$it" },
                state.emotionalAftertaste.takeIf { it.isNotBlank() }?.let { "上一段互动留下的感觉：$it" },
                state.currentNeed.takeIf { it.isNotBlank() }?.let { "此刻隐约想要：$it" },
                state.lastInteractionMeaning.takeIf { it.isNotBlank() }?.let { "刚才那段互动对我来说：$it" },
                advanced.cue.takeIf { it.isNotBlank() }?.let { "此刻的生活节奏：$it" },
                "关系阶段：${state.relationshipStage}"
            )
            if (lines.isNotEmpty()) {
                sb.append("\n\n【我最近带着的一点心事】\n")
                sb.append(lines.joinToString("\n") { "- $it" })
                sb.append("\n不要主动解释这是状态或记忆，只在自然相关时流露出来。不要为了展示这些内容而硬转话题。")
            }
        }

        // Physical continuity: avoid silently teleporting the character between replies.
        sb.append("""

【场景连续性】
动作是对白下方的小字旁白，不是对白的一部分。动作可省略；需要写时只写一个与当前情绪和正在做的事相连的小动作，不要每条都写完整舞台指示。
默认延续上一条消息中的身体状态、穿着、所在位置和正在做的事。几分钟内仍属于同一段聊天，不得因为时间感知就换衣服、出门、回家、睡醒或切换房间。只有用户明确要求，或你在回复中自然交代了经过、动作和转场，才改变场景。
若上一条没有说明，就不要主动补写具体穿着和地点。输出动作时优先使用单独一行的“[动作: ...]”，不要把动作写进对白句子中。
只有场景信息实际发生变化时，才在动作之后附加一行“[场景: 地点=...; 房间=...; 姿态=...; 穿着=...; 手持=...]”。未变化时不要输出场景行；未知字段留空，不得编造。
""".trimEnd())

        // 5) Character Book 知识库：常驻条目始终注入，关键词条目按当前消息触发
        runCatching {
            val book = characterRepository.getCharacterByIdForUser(companionId, userId)
                ?.characterBook
                .orEmpty()
            val injection = characterBookEngine.buildInjection(book, query)
            if (injection.block.isNotBlank()) {
                sb.append(injection.block)
            }
        }

        // 5) 情绪/脾气指令
        if (includeTemperament) {
            val state = emotionalEngine.getEmotionalState(userId, companionId)
            val character = characterRepository.getCharacterByIdForUser(companionId, userId)
            val temperament = character
                ?.let { TemperamentProfile.fromPersonality(it.personality) }
                ?: emotionalEngine.temperamentFor(companionId)
            val directive = TemperamentDirective.build(state, temperament)
            sb.append("\n\n").append(directive)
            sb.append(ActionStyleGuidance.build(temperament, innerStateRepository.load(userId, companionId)))
            sb.append("\n\n").append(
                NaturalConversationGuidance.build(
                    userMessage = query,
                    temperament = temperament,
                    emotionalState = state
                )
            )
            character?.let {
                sb.append("\n\n").append(
                    SpeechMannerGuidance.build(
                        personality = it.personality,
                        rules = it.behaviorRules ?: com.companion.cc.domain.model.BehaviorRules.default(),
                        examples = it.exampleDialogues
                    )
                )
            }
            RelationshipRepairContext.build(
                emotionalState = state,
                innerState = innerStateRepository.load(userId, companionId),
                temperament = temperament
            ).takeIf { it.isNotBlank() }?.let { sb.append("\n\n").append(it) }
        }

        ConversationContinuityGuard.forQuery(query).takeIf { it.isNotBlank() }?.let {
            sb.append("\n\n").append(it)
        }

        return sb.toString()
    }

    private fun defaultRhythmFor(companionId: String): CompanionRhythm = when {
        companionId.contains("muse", ignoreCase = true) -> CompanionRhythm.nightOwl()
        companionId.contains("xiaocan", ignoreCase = true) -> CompanionRhythm(
            socialBattery = 0.82f,
            memoryStickiness = 0.58f,
            recoverySpeed = 0.72f
        )
        else -> CompanionRhythm()
    }
}

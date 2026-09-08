package com.companion.cc.domain.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.character.CharacterPromptResolver
import com.companion.cc.domain.character.CharacterReplyAssembler
import com.companion.cc.domain.character.InnerStateRepository
import com.companion.cc.domain.character.CompanionRhythm
import com.companion.cc.domain.character.CompanionMindEngine
import com.companion.cc.domain.character.SceneContinuityPolicy
import com.companion.cc.domain.character.ActionContinuityPolicy
import com.companion.cc.domain.character.TemperamentProfile
import com.companion.cc.domain.engine.EmotionalEngine
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.usecase.SendMessageUseCase
import com.companion.cc.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/**
 * V9PM 主动消息 v1：TA 先开口
 *
 * 触发规则：时段窗口 9–11 / 20–22；静默 22:30–8:00（窗口外一律不打扰）；
 * 24h ≤2 条且间隔 ≥4h；总开关 SettingsManager.isNotificationPrefEnabled。
 * 消息以 ASSISTANT 角色入库（timestamp=now），用户打开 App 时已在对话流里；
 * 通知文案 = 消息内容本身（像微信）。
 */
@HiltWorker
class ProactiveMessageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val messageRepository: MessageRepository,
    private val characterPromptResolver: CharacterPromptResolver,
    private val characterReplyAssembler: CharacterReplyAssembler,
    private val sendMessageUseCase: SendMessageUseCase,
    private val emotionalEngine: EmotionalEngine,
    private val characterRepository: CustomCharacterRepository,
    private val innerStateRepository: InnerStateRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!SettingsManager.isNotificationPrefEnabled(context)) return Result.success()

        val now = LocalDateTime.now()
        val hourOfDay = now.hour + now.minute / 60.0

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = now.toLocalDate().toString()
        val sentToday = if (prefs.getString(KEY_DATE, null) == today) prefs.getInt(KEY_COUNT, 0) else 0
        if (sentToday >= MAX_PER_DAY) return Result.success()
        val lastAt = prefs.getLong(KEY_LAST_AT, 0L)
        if (System.currentTimeMillis() - lastAt < MIN_GAP_MS) return Result.success()

        val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            ?: return Result.success()

        // v1：发给最近对话过的那个 TA（多角色轮换策略留给 v2）
        val companionId = messageRepository.getLatestCompanionId(userId)
            ?: return Result.success()

        // V9PM 可靠性修复：脾气是内存态，进程重启后为默认值——主动决策前按角色人格重新注册
        runCatching {
            characterRepository.getCharacterByIdForUser(companionId, userId)?.let { custom ->
                emotionalEngine.setTemperament(
                    companionId,
                    TemperamentProfile.fromPersonality(custom.personality)
                )
            }
        }

        // 主动性决策：由脾气（粘人度）+ 心情/态度/压力 + 距离上次聊天时长共同决定
        val temperament = emotionalEngine.temperamentFor(companionId)
        val emotionalState = emotionalEngine.getEmotionalState(userId, companionId)
        val latestChatAt = messageRepository.observeLatestMessage(userId, companionId).first()?.timestamp ?: 0L
        val sinceLastMs = System.currentTimeMillis() - latestChatAt
        val innerState = innerStateRepository.load(userId, companionId)
        val rhythm = runCatching {
            characterRepository.getCharacterByIdForUser(companionId, userId)?.rhythm
        }.getOrNull() ?: when {
            companionId.contains("muse", ignoreCase = true) -> CompanionRhythm.nightOwl()
            companionId.contains("xiaocan", ignoreCase = true) -> CompanionRhythm(
                socialBattery = 0.82f,
                memoryStickiness = 0.58f,
                recoverySpeed = 0.72f
            )
            else -> CompanionRhythm()
        }
        val advanced = CompanionMindEngine.advance(
            previous = innerState,
            emotionalState = emotionalState,
            rhythm = rhythm,
            nowHour = hourOfDay,
            elapsedMs = sinceLastMs
        )
        innerStateRepository.save(userId, companionId, advanced.state)
        val decision = ProactiveEngine.decide(
            temperament = temperament,
            state = emotionalState,
            sinceLastChatMs = sinceLastMs.coerceAtLeast(0L),
            nowHour = hourOfDay,
            rhythm = rhythm,
            innerState = advanced.state
        )
        if (!decision.shouldReachOut) return Result.success()

        val used = prefs.getString(KEY_USED, "")
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        // 统一组装：主动消息也用角色配置 + 长期记忆 + 自我叙事（不附加情绪指令，降低打扰感）
        val content = if (ProactiveContentPolicy.shouldGenerateWithModel(advanced.state)) {
            generateProactiveContent(userId, companionId, now, decision.motivation, decision.interruptionCost)
        } else {
            null
        } ?: ProactiveMessagePool.pick(
            companionId,
            isMorning = now.hour < 12,
            recentUsed = used
        ).content

        val parsed = MessageContentParser.parse(content)
        val action = ActionContinuityPolicy.accept(parsed.action, advanced.state.recentAction)
        messageRepository.saveMessage(
            Message(
                id = "${System.currentTimeMillis()}_proactive",
                userId = userId,
                companionId = companionId,
                role = MessageRole.ASSISTANT,
                content = parsed.dialogue,
                timestamp = System.currentTimeMillis(),
                action = action,
                origin = "proactive",
            )
        )
        innerStateRepository.save(
            userId,
            companionId,
            SceneContinuityPolicy.update(
                advanced.state,
                parsed.dialogue,
                action,
                System.currentTimeMillis(),
                parsed.scene
            )
        )

        prefs.edit()
            .putLong(KEY_LAST_AT, System.currentTimeMillis())
            .putString(KEY_DATE, today)
            .putInt(KEY_COUNT, sentToday + 1)
            .putString(KEY_USED, (used + parsed.dialogue).takeLast(10).joinToString(SEP))
            .apply()

        NotificationHelper.sendProactiveMessageNotification(
            context,
            pickedCompanionName(companionId),
            parsed.dialogue,
            companionId
        )
        return Result.success()
    }

    /** 主动消息内容：优先角色化生成（带决策理由），失败回退本地池。 */
    private suspend fun generateProactiveContent(
        userId: String,
        companionId: String,
        now: LocalDateTime,
        motivation: String,
        interruptionCost: Float
    ): String? = runCatching {
        val resolved = characterPromptResolver.resolve(companionId)
        val baseSystem = resolved.config.prompts.system
        val prompt = characterReplyAssembler.assemble(
            baseSystem = baseSystem,
            userId = userId,
            companionId = companionId,
            query = "",
            includeTemperament = false
        )
        val state = innerStateRepository.load(userId, companionId)
        val continuity = listOfNotNull(
            state.unfinishedThought.takeIf { it.isNotBlank() }?.let { "你之前没说完的一点想法：$it" },
            state.caringAbout.takeIf { it.isNotBlank() }?.let { "你最近在意：$it" },
            state.lookingForwardTo.takeIf { it.isNotBlank() }?.let { "你最近期待：$it" }
            ,state.lifeThread.takeIf { it.isNotBlank() }?.let { "你自己的生活线：$it" },
            state.scene.takeIf { it.isNotBlank() }?.let { "你此刻还在：$it" },
            state.emotionalAftertaste.takeIf { it.isNotBlank() }?.let { "上一段互动留下的感觉：$it" },
            state.currentNeed.takeIf { it.isNotBlank() }?.let { "你此刻隐约想要：$it" },
            state.lastInteractionMeaning.takeIf { it.isNotBlank() }?.let { "刚才那段互动对你来说：$it" }
        ).joinToString("\n")
        val userLine = buildString {
            append("现在是${if (now.hour < 12) "早上" else "晚上"}。你心里想找用户说说话，动机是：$motivation。")
            if (continuity.isNotBlank()) {
                append("\n这是你和用户之间还带着的一点连续性：\n")
                append(continuity)
            }
            append("\n你有${if (interruptionCost >= 0.7f) "一点怕打扰对方的顾虑" else "足够的开口把握"}。自然地开口，两三句话，像平时说话一样。不要提到记忆、状态、系统或规则；如果没有自然的话，就宁愿只发一句具体的话。不要为了证明自己主动而硬找话题。")
            append("\n")
            append(ProactiveMessageGuidance.build(motivation, interruptionCost, continuity))
        }
        sendMessageUseCase(
            systemPrompt = prompt,
            conversationHistory = listOf(mapOf("role" to "user", "content" to userLine)),
            apiParams = ApiParameters(
                temperature = 0.8,
                topP = 0.9,
                maxTokens = 120,
                frequencyPenalty = 0.2,
                presencePenalty = 0.3
            )
        ).trim().takeIf { it.isNotBlank() && it.length <= 160 }
    }.getOrNull()

    private fun pickedCompanionName(companionId: String): String = when {
        companionId.contains("muse", ignoreCase = true) -> "缪斯"
        companionId.contains("xiaocan", ignoreCase = true) -> "小璨"
        else -> "TA"
    }

    private companion object {
        const val PREFS_NAME = "proactive_messages"
        const val KEY_LAST_AT = "last_proactive_at"
        const val KEY_DATE = "proactive_date"
        const val KEY_COUNT = "proactive_count_today"
        const val KEY_USED = "proactive_used_recent"
        const val SEP = "\u0001"
        const val MAX_PER_DAY = 2
        const val MIN_GAP_MS = 4L * 3_600_000L
    }
}

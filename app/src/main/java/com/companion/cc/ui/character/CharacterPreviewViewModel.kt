package com.companion.cc.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.engine.EmotionalEngine
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.character.TemperamentDirective
import com.companion.cc.domain.character.TemperamentProfile
import com.companion.cc.domain.character.HumanLikeCharacterGuidance
import com.companion.cc.domain.character.InnerState
import com.companion.cc.domain.character.SceneContinuityPolicy
import com.companion.cc.domain.character.ActionContinuityPolicy
import com.companion.cc.domain.message.MessageContentParser
import com.companion.cc.domain.usecase.StreamSendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 试聊（创建前测试对话）：
 * - 角色草稿不落库，直接构建专业 system prompt
 * - 复用脾气引擎（Big Five → 脾气五参 + 态度状态机），试聊即可感受 TA 的脾气
 * - 消息仅存内存；流式输出复用主对话用例
 */
@HiltViewModel
class CharacterPreviewViewModel @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val streamSendMessageUseCase: StreamSendMessageUseCase,
    private val emotionalEngine: EmotionalEngine,
    private val characterReplyAssembler: com.companion.cc.domain.character.CharacterReplyAssembler,
) : ViewModel() {

    data class PreviewMessage(
        val role: MessageRole,
        val content: String,
        val timestamp: Long,
        val isStreaming: Boolean = false,
        val action: String? = null,
    )

    private val _messages = MutableStateFlow<List<PreviewMessage>>(emptyList())
    val messages: StateFlow<List<PreviewMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    val attitude: StateFlow<Attitude> = emotionalEngine.currentState
        .map { it.attitude }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Attitude.NEUTRAL)

    val characterName: StateFlow<String> = CharacterPreviewStore.draft
        .map { it?.name ?: "角色" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "角色")

    private var systemPrompt: String = ""
    private var companionId: String = "preview"
    private var userId: String = "preview-user"
    private var temperament: TemperamentProfile = TemperamentProfile.DEFAULT
    private var previewSceneState: InnerState = InnerState()

    init {
        viewModelScope.launch {
            val draft = CharacterPreviewStore.draft.value ?: return@launch
            userId = runCatching { currentUserProvider.requireUserId() }.getOrDefault("preview-user")
            companionId = "preview_${draft.id}"

            // 脾气接线：Big Five → 脾气五参，注册进引擎（试聊即可感受态度状态机）
            temperament = TemperamentProfile.fromPersonality(draft.personality)
            emotionalEngine.setTemperament(companionId, temperament)
            emotionalEngine.resetEmotionalState(userId, companionId)
            emotionalEngine.setCurrentSession(userId, companionId)

            systemPrompt = buildPrompt(draft)
            _messages.value = listOf(
                PreviewMessage(
                    role = MessageRole.ASSISTANT,
                    content = draft.greetingMessage.ifBlank { "……（TA 看着你）" },
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _isSending.value || systemPrompt.isEmpty()) return
        _isSending.value = true

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _messages.value += PreviewMessage(MessageRole.USER, content, now)

            // 情绪与态度状态机随对话演化
            emotionalEngine.detectEmotion(content)
            val fullPrompt = characterReplyAssembler.assemble(
                baseSystem = systemPrompt,
                userId = userId,
                companionId = companionId,
                query = content,
                includeTemperament = true
            )

            val history = _messages.value
                .filter { !it.isStreaming }
                .takeLast(12)
                .map { mapOf("role" to if (it.role == MessageRole.ASSISTANT) "assistant" else "user", "content" to it.content) }

            val placeholder = PreviewMessage(MessageRole.ASSISTANT, "", System.currentTimeMillis(), isStreaming = true)
            _messages.value += placeholder

            runCatching {
                streamSendMessageUseCase(fullPrompt, history, defaultParams()).collect { chunk ->
                    val current = _messages.value.last()
                    _messages.value = _messages.value.dropLast(1) + current.copy(content = current.content + chunk)
                }
            }.onFailure { e ->
                val current = _messages.value.last()
                val note = current.content.ifBlank { "（试聊连接失败：${e.message ?: "网络异常"}。正式保存后不受影响。）" }
                _messages.value = _messages.value.dropLast(1) + current.copy(content = note, isStreaming = false)
            }

            val last = _messages.value.last()
            val parsed = if (last.role == MessageRole.ASSISTANT) MessageContentParser.parse(last.content) else null
            val acceptedAction = ActionContinuityPolicy.accept(parsed?.action, previewSceneState.recentAction)
            if (parsed != null) {
                previewSceneState = SceneContinuityPolicy.update(
                    previous = previewSceneState,
                    assistantText = parsed.dialogue,
                    action = acceptedAction,
                    now = System.currentTimeMillis(),
                    declaredScene = parsed.scene
                )
            }
            _messages.value = _messages.value.dropLast(1) + last.copy(
                content = parsed?.dialogue ?: last.content,
                action = acceptedAction,
                isStreaming = false
            )
            _isSending.value = false
        }
    }

    private fun defaultParams() = ApiParameters(
        temperature = 0.85,
        topP = 0.9,
        maxTokens = 600,
        frequencyPenalty = 0.3,
        presencePenalty = 0.3,
    )

    /** 专业 prompt：背景/Big Five 人格自然语言化/行为规则/示例对话/开场白 */
    private fun buildPrompt(draft: CustomCharacter): String {
        val p = draft.personality
        fun level(v: Float) = when {
            v >= 0.7f -> "高"
            v >= 0.35f -> "中等"
            else -> "低"
        }
        val rules = draft.behaviorRules ?: com.companion.cc.domain.model.BehaviorRules.default()
        val ruleLines = buildList {
            add("- 回复风格：${rules.responseStyle.name.replace('_', ' ').lowercase()}")
            add("- 表情符号频率：${rules.emojiFrequency.name.replace('_', ' ').lowercase()}")
            add("- 语气正式程度：${rules.formalityLevel.name.replace('_', ' ').lowercase()}")
            if (rules.topicPreferences.isNotEmpty()) add("- 喜欢主动聊：${rules.topicPreferences.joinToString("、")}")
            if (rules.avoidTopics.isNotEmpty()) add("- 避免聊：${rules.avoidTopics.joinToString("、")}")
        }
        val examples = draft.exampleDialogues.take(4).joinToString("\n") { "用户：${it.user}\n你：${it.assistant}" }

        return buildString {
            appendLine("你是「${draft.name}」。${draft.description}")
            if (draft.backstory.isNotBlank()) {
                appendLine()
                appendLine("## 你的背景故事")
                appendLine(draft.backstory)
            }
            appendLine()
            appendLine("## 你的人格（Big Five，内在地驱动你的说话方式，不要向用户罗列）")
            appendLine("- 开放性（${level(p.openness)}）：对新体验和想法的接受程度")
            appendLine("- 尽责性（${level(p.conscientiousness)}）：负责与自律程度")
            appendLine("- 外向性（${level(p.extraversion)}）：社交活跃度与表达热情")
            appendLine("- 宜人性（${level(p.agreeableness)}）：友好、合作与同理心")
            appendLine("- 神经质（${level(p.neuroticism)}）：情绪敏感与不稳定程度")
            if (p.customTraits.isNotEmpty()) {
                appendLine("- 附加特质：${p.customTraits.entries.joinToString("；") { "${it.key}：${it.value}" }}")
            }
            appendLine()
            appendLine("## 行为规则")
            appendLine(ruleLines.joinToString("\n"))
            if (examples.isNotBlank()) {
                appendLine()
                appendLine("## 示例对话（体会你的语气，不要逐字复述）")
                appendLine(examples)
            }
            appendLine()
            appendLine("## 开场白基调（你的第一句话应贴近这个感觉）")
            appendLine(draft.greetingMessage)
            appendLine()
            appendLine("## 硬性要求")
            appendLine("- 始终保持角色一致，口语化、有情绪温度；绝不跳出角色、绝不自称 AI 或语言模型。")
            appendLine("- 回复保持对话体，不写旁白说明，不解释你为什么这样说话。")
            appendLine("- 动作是对白下方的小字旁白，不是对白内容。动作可省略，不要每条都写；若写动作，用单独一行 [动作: ...]，不要混进对白。")
            appendLine("- 同一段聊天默认延续位置、穿着、正在做的事和身体状态。几分钟内不要自行换场，只有明确交代经过或转场时才改变场景。")
            appendLine("- 只有地点、房间、姿态、穿着或手持物确实变化时，才在动作后附加 [场景: 地点=...; 房间=...; 姿态=...; 穿着=...; 手持=...]；没变化就不要输出。")
            appendLine(HumanLikeCharacterGuidance.TEXT)
        }
    }
}

package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.data.remote.model.ChatRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 思考链UseCase
 * 让AI在回复前先进行内部思考
 * 参考：st-stepped-thinking
 */
class SteppedThinkingUseCase @Inject constructor(
    private val api: SiliconFlowApi,
    private val settingsManager: SettingsManager
) {
    /**
     * 执行思考步骤
     * @param companionId 角色ID
     * @param userMessage 用户消息
     * @param currentState 当前状态
     * @param recentMemories 最近的记忆
     * @return ThinkingResult 思考结果
     */
    suspend operator fun invoke(
        companionId: String,
        userMessage: String,
        currentState: Any,
        recentMemories: String
    ): ThinkingResult {
        val model = settingsManager.modelFlow.first()

        // 构建思考提示
        val thinkingPrompt = buildThinkingPrompt(
            companionId,
            userMessage,
            currentState,
            recentMemories
        )

        val messages = listOf(
            ChatMessage(role = "system", content = "你是一个内部思考助手，帮助角色分析情况并决定回复策略。"),
            ChatMessage(role = "user", content = thinkingPrompt)
        )

        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = 200,  // 思考不需要太长
            temperature = 0.7f
        )

        val response = api.chatCompletion(request)
        val thinkingContent = response.choices.firstOrNull()?.message?.content
            ?: "无法进行思考分析"

        return parseThinkingResult(thinkingContent)
    }

    private fun buildThinkingPrompt(
        companionId: String,
        userMessage: String,
        currentState: Any,
        recentMemories: String
    ): String {
        val characterName = if (companionId == "muse") "缪斯" else "小璨"

        return """
你是${characterName}的内部思考系统。用户刚发送了一条消息，你需要分析并制定回复策略。

【用户消息】
$userMessage

【当前状态】
$currentState

【最近记忆】
$recentMemories

【思考步骤】
1. **意图理解**：用户想表达什么？情绪如何？
2. **记忆召回**：有相关的历史对话或信息吗？
3. **状态检查**：当前的冷度/暖度如何？需要调整语气吗？
4. **回复策略**：应该用什么语气？说多长？要不要提供帮助？

请用简短的JSON格式输出：
{
  "intent": "用户意图",
  "emotion": "用户情绪",
  "relevantMemory": "是否有相关记忆",
  "responseStrategy": "回复策略（简短/详细/冷淡/温暖）",
  "tone": "语气（冷静/关心/敷衍）",
  "length": "回复长度（短/中/长）"
}
        """.trimIndent()
    }

    private fun parseThinkingResult(content: String): ThinkingResult {
        // 简单解析（实际可以用JSON解析）
        return ThinkingResult(
            intent = extractValue(content, "intent"),
            emotion = extractValue(content, "emotion"),
            relevantMemory = extractValue(content, "relevantMemory"),
            responseStrategy = extractValue(content, "responseStrategy"),
            tone = extractValue(content, "tone"),
            length = extractValue(content, "length"),
            rawThinking = content
        )
    }

    private fun extractValue(content: String, key: String): String {
        // 提取JSON中的值
        val pattern = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return pattern.find(content)?.groupValues?.get(1) ?: "未知"
    }
}

/**
 * 思考结果
 */
data class ThinkingResult(
    val intent: String,           // 用户意图
    val emotion: String,          // 用户情绪
    val relevantMemory: String,   // 相关记忆
    val responseStrategy: String, // 回复策略
    val tone: String,             // 语气
    val length: String,           // 长度
    val rawThinking: String       // 原始思考内容
)

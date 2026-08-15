package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.domain.model.MemoryType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 自动记忆提取UseCase
 * 从对话中提取关键信息
 */
class ExtractMemoryUseCase @Inject constructor(
    private val api: SiliconFlowApi,
    private val settingsManager: SettingsManager
) {
    /**
     * 从对话中提取记忆
     * @param conversation 对话内容
     * @return 提取的记忆列表
     */
    suspend operator fun invoke(conversation: String): List<ExtractedMemory> {
        val model = settingsManager.modelFlow.first()

        val extractionPrompt = """
你是一个记忆提取助手。分析以下对话，提取关键信息。

【对话内容】
$conversation

【提取规则】
1. 用户个人信息（姓名、年龄、职业、爱好等）
2. 重要事件（发生了什么）
3. 情感状态（用户的情绪变化）
4. 偏好/厌恶（喜欢什么，不喜欢什么）

【输出格式】
每条记忆一行，格式：[类型]内容
类型：USER_INFO、EVENT、EMOTION、PREFERENCE

示例：
[USER_INFO]用户是程序员
[EVENT]用户今天加班到很晚
[EMOTION]用户感到疲惫和沮丧
[PREFERENCE]用户喜欢喝咖啡

如果没有可提取的信息，输出：无
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "system", content = "你是记忆提取专家。"),
            ChatMessage(role = "user", content = extractionPrompt)
        )

        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = 300,
            temperature = 0.3f  // 较低的温度，更确定的提取
        )

        val response = api.chatCompletion(request)
        val content = response.choices.firstOrNull()?.message?.content ?: "无"

        return parseExtractedMemories(content)
    }

    /**
     * 解析提取的记忆
     */
    private fun parseExtractedMemories(content: String): List<ExtractedMemory> {
        if (content.contains("无") && content.length < 10) {
            return emptyList()
        }

        val memories = mutableListOf<ExtractedMemory>()
        val lines = content.split("\n").filter { it.isNotBlank() }

        for (line in lines) {
            // 匹配格式：[类型]内容
            val match = Regex("\\[([A-Z_]+)](.+)").find(line.trim())
            if (match != null) {
                val typeStr = match.groupValues[1]
                val memoryContent = match.groupValues[2].trim()

                val type = when (typeStr) {
                    "USER_INFO" -> MemoryType.USER_INFO
                    "EVENT" -> MemoryType.EVENT
                    "EMOTION" -> MemoryType.EMOTION
                    "PREFERENCE" -> MemoryType.PREFERENCE
                    else -> MemoryType.CONVERSATION
                }

                memories.add(ExtractedMemory(
                    content = memoryContent,
                    type = type
                ))
            }
        }

        return memories
    }
}

/**
 * 提取的记忆
 */
data class ExtractedMemory(
    val content: String,
    val type: MemoryType
)

package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.domain.manager.ApiParameters
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 发送消息UseCase - 简化版
 * 只负责API调用，System Prompt由外部传入
 */
class SendMessageUseCase @Inject constructor(
    private val api: SiliconFlowApi,
    private val settingsManager: SettingsManager
) {
    /**
     * 调用LLM API
     * @param systemPrompt 完整的System Prompt（由SystemPromptManager构建）
     * @param conversationHistory 对话历史
     * @param apiParams API参数（由CompanionStateManager提供）
     */
    suspend operator fun invoke(
        systemPrompt: String,
        conversationHistory: List<Map<String, String>>,
        apiParams: ApiParameters
    ): String {
        // 获取模型设置
        val model = settingsManager.modelFlow.first()

        // 构建完整消息列表
        val messages = mutableListOf<ChatMessage>()

        // 1. 添加System Prompt
        messages.add(ChatMessage(
            role = "system",
            content = systemPrompt
        ))

        // 2. 添加对话历史
        conversationHistory.forEach { msg ->
            messages.add(ChatMessage(
                role = msg["role"] ?: "user",
                content = msg["content"] ?: ""
            ))
        }

        // 3. 构建请求（使用动态API参数）
        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = apiParams.maxTokens,
            temperature = apiParams.temperature.toFloat(),
            topP = apiParams.topP.toFloat(),
            frequencyPenalty = apiParams.frequencyPenalty.toFloat(),
            presencePenalty = apiParams.presencePenalty.toFloat()
        )

        // 4. 调用API
        val response = api.chatCompletion(request)
        return response.choices.firstOrNull()?.message?.content
            ?: "抱歉，我没有收到回复"
    }
}

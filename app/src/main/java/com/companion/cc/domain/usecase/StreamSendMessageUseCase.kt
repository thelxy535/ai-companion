package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.api.StreamChatService
import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.data.remote.model.ChatRequest
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.model.StreamHttpException
import com.companion.cc.domain.model.StreamRetryPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import javax.inject.Inject

/**
 * 流式发送消息UseCase
 * 返回Flow<String>，逐字逐句emit AI回复
 */
class StreamSendMessageUseCase @Inject constructor(
    private val streamChatService: StreamChatService,
    private val settingsManager: SettingsManager
) {
    fun cancelActiveRequest() {
        streamChatService.cancelActiveCall()
    }

    /**
     * @param systemPrompt 完整的System Prompt
     * @param conversationHistory 对话历史
     * @param apiParams API参数
     * @return Flow<String> 每次emit一小段文本
     */
    suspend operator fun invoke(
        systemPrompt: String,
        conversationHistory: List<Map<String, String>>,
        apiParams: ApiParameters
    ): Flow<String> = invoke(
        systemPrompt = systemPrompt,
        conversationHistory = conversationHistory,
        apiParams = apiParams,
        userId = null,
        characterId = null
    )

    suspend operator fun invoke(
        systemPrompt: String,
        conversationHistory: List<Map<String, String>>,
        apiParams: ApiParameters,
        userId: String?,
        characterId: String?
    ): Flow<String> {
        // 获取设置
        val model = settingsManager.modelFlow.first()
        val baseUrl = settingsManager.baseUrlFlow.first()

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

        // 3. 构建请求
        val request = ChatRequest(
            model = model,
            messages = messages,
            maxTokens = apiParams.maxTokens,
            temperature = apiParams.temperature.toFloat(),
            topP = apiParams.topP.toFloat(),
            frequencyPenalty = apiParams.frequencyPenalty.toFloat(),
            presencePenalty = apiParams.presencePenalty.toFloat(),
            stream = true  // 启用流式响应
        )

        // 4. 返回流式响应
        var outputText = ""
        return streamChatService.streamChat(baseUrl, request)
            .retryWhen { cause, attempt ->
                val error = cause as? StreamHttpException
                if (error == null || !StreamRetryPolicy.shouldRetry(error, attempt.toInt())) {
                    false
                } else {
                    delay(StreamRetryPolicy.delayMillis(error))
                    true
                }
            }
            .onEach { fragment -> outputText += fragment }
            .onCompletion { cause ->
                if (userId != null && characterId != null) {
                    settingsManager.recordUsage(
                        com.companion.cc.domain.usage.UsageRecord(
                            userId = userId,
                            characterId = characterId,
                            feature = "chat",
                            model = model,
                            inputTokens = messages.sumOf { it.content.length.toLong() },
                            outputTokens = com.companion.cc.domain.usage.UsageCostCalculator
                                .estimatedTokens(outputText),
                            succeeded = com.companion.cc.domain.usage.UsageCostCalculator
                                .succeededForCompletion(cause)
                        )
                    )
                }
            }
    }
}

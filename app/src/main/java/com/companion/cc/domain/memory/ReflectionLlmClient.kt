package com.companion.cc.domain.memory

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.remote.api.SiliconFlowApi
import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.data.remote.model.ChatRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReflectionLlmClient @Inject constructor(
    private val api: SiliconFlowApi,
    private val settingsManager: SettingsManager
) {
    suspend fun reflect(source: String): String {
        val request = ChatRequest(
            model = settingsManager.modelFlow.first(),
            messages = listOf(
                ChatMessage("system", "你只输出合法 JSON。"),
                ChatMessage("user", ReflectionPromptBuilder.build(source))
            ),
            maxTokens = 1200,
            temperature = 0.3f,
            topP = 0.8f,
            frequencyPenalty = 0f,
            presencePenalty = 0f,
            stream = false
        )
        return api.chatCompletion(request).choices.firstOrNull()?.message?.content
            ?: error("反思模型没有返回内容")
    }

    /** 通用非流式补全（叙事自演化等低频后台任务使用）。 */
    suspend fun complete(systemPrompt: String, userPrompt: String, maxTokens: Int = 600): String {
        val request = ChatRequest(
            model = settingsManager.modelFlow.first(),
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            maxTokens = maxTokens,
            temperature = 0.4f,
            topP = 0.9f,
            frequencyPenalty = 0f,
            presencePenalty = 0f,
            stream = false
        )
        return api.chatCompletion(request).choices.firstOrNull()?.message?.content
            ?: error("模型没有返回内容")
    }
}

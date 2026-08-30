package com.companion.cc.domain.schedule

import com.companion.cc.data.remote.model.ChatMessage
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.usecase.SendMessageUseCase
import com.companion.cc.data.local.entity.ScheduleEntity
import javax.inject.Inject

/** Sends a scheduled prompt through the configured non-streaming provider. */
class ScheduleActionExecutor @Inject constructor(
    private val sendMessage: SendMessageUseCase
) {
    suspend fun execute(schedule: ScheduleEntity) {
        sendMessage(
            systemPrompt = "你是角色 ${schedule.characterId}。请完成用户的计划提醒。",
            conversationHistory = listOf(mapOf("role" to "user", "content" to schedule.prompt)),
            apiParams = ApiParameters(
                temperature = 0.7,
                topP = 0.9,
                maxTokens = 512,
                frequencyPenalty = 0.0,
                presencePenalty = 0.0
            )        )
    }
}

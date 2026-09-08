package com.companion.cc.domain.schedule

import com.companion.cc.data.local.entity.ScheduleEntity
import com.companion.cc.domain.character.CharacterPromptResolver
import com.companion.cc.domain.character.CharacterReplyAssembler
import com.companion.cc.domain.manager.ApiParameters
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.usecase.SendMessageUseCase
import java.util.UUID
import javax.inject.Inject

/** 到点执行：用角色配置 + 长期记忆 + 自我叙事生成有角色感的提醒，并作为 assistant 消息入库。 */
class ScheduleActionExecutor @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val characterPromptResolver: CharacterPromptResolver,
    private val characterReplyAssembler: CharacterReplyAssembler,
    private val messageRepository: MessageRepository
) {
    suspend fun execute(schedule: ScheduleEntity) {
        val characterId = schedule.characterId
        val userId = schedule.userId
        val promise = schedule.prompt.removePrefix(PROMPT_PREFIX)
        val userLine = "现在到了约定的时间，你之前答应过用户：$promise。请自然地提醒/兑现，像平时说话一样，两三句话。"
        val content = runCatching {
            val resolved = characterPromptResolver.resolve(characterId)
            val baseSystem = resolved.config.prompts.system
            val prompt = characterReplyAssembler.assemble(
                baseSystem = baseSystem,
                userId = userId,
                companionId = characterId,
                query = "",
                includeTemperament = false
            )
            sendMessage(
                systemPrompt = prompt,
                conversationHistory = listOf(mapOf("role" to "user", "content" to userLine)),
                apiParams = ApiParameters(
                    temperature = 0.7,
                    topP = 0.9,
                    maxTokens = 200,
                    frequencyPenalty = 0.0,
                    presencePenalty = 0.0
                )
            ).trim().takeIf { it.isNotBlank() && it.length <= 200 }
        }.getOrNull() ?: "到点啦，别忘了你答应我的事——$promise。"

        messageRepository.saveMessage(
            Message(
                id = "${System.currentTimeMillis()}_${UUID.randomUUID()}_commitment",
                userId = userId,
                companionId = characterId,
                role = MessageRole.ASSISTANT,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val PROMPT_PREFIX = "【承诺】"
    }
}

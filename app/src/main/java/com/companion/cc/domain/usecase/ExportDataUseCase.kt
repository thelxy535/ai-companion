package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val settingsManager: SettingsManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(): String {
        val userId = settingsManager.userIdFlow.first()
        val apiKey = settingsManager.apiKeyFlow.first() ?: ""
        val baseUrl = settingsManager.baseUrlFlow.first()
        val model = settingsManager.modelFlow.first()

        // 获取所有消息
        val allMessages = mutableListOf<Message>()

        // 获取小璨的消息
        messageRepository.getMessages(userId, "xiaocan", limit = 10000)
            .first()
            .let { allMessages.addAll(it) }

        // 获取缪斯的消息
        messageRepository.getMessages(userId, "muse", limit = 10000)
            .first()
            .let { allMessages.addAll(it) }

        val exportData = ExportData(
            version = "1.0",
            exportDate = Instant.now().toString(),
            userId = userId,
            messages = allMessages.map { it.toExportMessage() },
            settings = ExportSettings(
                apiKey = apiKey,
                baseURL = baseUrl,
                model = model
            )
        )

        return json.encodeToString(exportData)
    }

    private fun Message.toExportMessage() = ExportMessage(
        id = id,
        userId = userId,
        companionId = companionId,
        role = if (role == MessageRole.USER) "user" else "assistant",
        content = content,
        timestamp = timestamp,
        emotion = emotion,
        mentionedOther = mentionedOther,
        isDualConversation = isDualConversation,
        replyToId = replyToId
    )
}

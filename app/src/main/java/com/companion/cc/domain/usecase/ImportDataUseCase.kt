package com.companion.cc.domain.usecase

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.*
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val settingsManager: SettingsManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(jsonString: String): ImportResult {
        return try {
            val exportData = json.decodeFromString<ExportData>(jsonString)
            require(exportData.version == SUPPORTED_VERSION) {
                "不支持的导入版本: ${exportData.version}"
            }

            val currentUserId = settingsManager.userIdFlow.first()
            require(exportData.userId.trim() == currentUserId.trim()) {
                "导入数据属于其他用户"
            }

            // 导入设置
            if (exportData.settings.apiKey.isNotBlank()) {
                settingsManager.saveApiKey(exportData.settings.apiKey)
            }
            if (exportData.settings.baseURL.isNotBlank()) {
                settingsManager.saveBaseUrl(exportData.settings.baseURL)
            }
            if (exportData.settings.model.isNotBlank()) {
                settingsManager.saveModel(exportData.settings.model)
            }

            // 导入消息
            val messages = exportData.messages.map { exportMsg ->
                Message(
                    id = exportMsg.id,
                    userId = currentUserId, // 使用当前用户ID
                    companionId = exportMsg.companionId,
                    role = if (exportMsg.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
                    content = exportMsg.content,
                    timestamp = exportMsg.timestamp,
                    emotion = exportMsg.emotion,
                    mentionedOther = exportMsg.mentionedOther,
                    isDualConversation = exportMsg.isDualConversation,
                    replyToId = exportMsg.replyToId
                )
            }

            messageRepository.saveMessages(messages)

            ImportResult.Success(
                messagesImported = messages.size,
                settingsImported = true
            )

        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "导入失败")
        }
    }

    private companion object {
        const val SUPPORTED_VERSION = "1.0"
    }
}

sealed class ImportResult {
    data class Success(
        val messagesImported: Int,
        val settingsImported: Boolean
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}

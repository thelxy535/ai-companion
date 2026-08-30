package com.companion.cc.data.repository

import com.companion.cc.data.local.dao.MessageDao
import com.companion.cc.data.local.entity.MessageEntity
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getMessages(
        userId: String,
        companionId: String,
        limit: Int,
        offset: Int
    ): Flow<List<Message>> {
        return messageDao.getMessages(userId, companionId, limit, offset)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override fun getAllMessages(userId: String): Flow<List<Message>> =
        messageDao.getAllMessages(userId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun saveMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { it.toEntity() })
    }

    override suspend fun getMessageCount(userId: String): Int {
        return messageDao.getMessageCount(userId)
    }

    override fun observeMessageCount(userId: String): Flow<Int> =
        messageDao.observeMessageCount(userId)

    override suspend fun deleteAllMessages(userId: String) {
        messageDao.deleteAllMessages(userId)
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    override suspend fun deleteMessagesByCompanion(userId: String, companionId: String) {
        messageDao.deleteMessagesByCompanion(userId, companionId)
    }

    override suspend fun updateMessageImportance(messageId: String, importance: Int) {
        messageDao.updateMessageImportance(messageId, importance)
    }

    override fun searchMessages(
        userId: String,
        companionId: String?,
        query: String
    ): Flow<List<Message>> {
        return messageDao.searchMessages(userId, companionId, query)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    private fun MessageEntity.toDomain() = Message(
        id = id,
        userId = userId,
        companionId = companionId,
        role = if (role == "user") MessageRole.USER else MessageRole.ASSISTANT,
        content = content,
        timestamp = timestamp,
        emotion = emotion,
        mentionedOther = mentionedOther,
        isDualConversation = isDualConversation,
        replyToId = replyToId,
        createdAt = createdAt,
        importance = importance,
        action = action,
        imageUrl = imageUrl,
        imageAnalysis = imageAnalysis,
        isFavorited = isFavorited
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        userId = userId,
        companionId = companionId,
        role = if (role == MessageRole.USER) "user" else "assistant",
        content = content,
        timestamp = timestamp,
        emotion = emotion,
        mentionedOther = mentionedOther,
        isDualConversation = isDualConversation,
        replyToId = replyToId,
        createdAt = createdAt,
        importance = importance,
        action = action,
        imageUrl = imageUrl,
        imageAnalysis = imageAnalysis,
        isFavorited = isFavorited
    )

    override suspend fun toggleMessageFavorite(messageId: String, isFavorited: Boolean) {
        messageDao.updateMessageFavorite(messageId, isFavorited)
    }

    override fun getFavoritedMessages(userId: String): Flow<List<Message>> {
        return messageDao.getFavoritedMessages(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLatestMessage(userId: String, companionId: String): Flow<Message?> {
        return messageDao.observeLatestMessage(userId, companionId)
            .map { entity -> entity?.toDomain() }
    }
}

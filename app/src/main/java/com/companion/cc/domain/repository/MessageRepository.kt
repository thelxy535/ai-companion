package com.companion.cc.domain.repository

import com.companion.cc.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(
        userId: String,
        companionId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Flow<List<Message>>

    suspend fun saveMessage(message: Message)

    suspend fun saveMessages(messages: List<Message>)

    suspend fun getMessageCount(userId: String): Int

    suspend fun deleteAllMessages(userId: String)

    suspend fun deleteMessage(messageId: String)

    suspend fun updateMessageImportance(messageId: String, importance: Int)

    suspend fun toggleMessageFavorite(messageId: String, isFavorited: Boolean)

    fun getFavoritedMessages(userId: String): Flow<List<Message>>

    /**
     * 搜索消息
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID（可选）
     * @param query 搜索关键词
     */
    fun searchMessages(
        userId: String,
        companionId: String?,
        query: String
    ): Flow<List<Message>>
}

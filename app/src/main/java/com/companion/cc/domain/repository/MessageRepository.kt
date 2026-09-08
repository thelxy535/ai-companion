package com.companion.cc.domain.repository

import com.companion.cc.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface MessageRepository {
    fun getMessages(
        userId: String,
        companionId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Flow<List<Message>>

    /**
     * 取最新的 limit 条消息（时间升序返回）。
     * 聊天页必须用这个：旧的 getMessages 是"最旧的 N 条"，
     * 消息数超过 limit 后新消息会落在窗口外导致聊天不可见。
     */
    fun getLatestMessages(
        userId: String,
        companionId: String,
        limit: Int = 50
    ): Flow<List<Message>> = getMessages(userId, companionId, limit, 0)

    /** Reads a consistent snapshot for request construction, independent of the UI Flow. */
    suspend fun getLatestMessagesOnce(
        userId: String,
        companionId: String,
        limit: Int = 100
    ): List<Message> = getLatestMessages(userId, companionId, limit).first()

    suspend fun getProactiveMessagesOnce(
        userId: String,
        companionId: String
    ): List<Message> = emptyList()

    suspend fun getAllMessagesOnce(
        userId: String,
        companionId: String
    ): List<Message> = getMessages(userId, companionId, limit = Int.MAX_VALUE).first()

    suspend fun searchMessagesOnce(
        userId: String,
        companionId: String?,
        query: String
    ): List<Message> = searchMessages(userId, companionId, query).first()

    fun getAllMessages(userId: String): Flow<List<Message>>

    suspend fun saveMessage(message: Message)
    suspend fun saveMessages(messages: List<Message>)

    suspend fun getLatestCompanionId(userId: String): String?

    suspend fun getMessageCount(userId: String): Int

    fun observeMessageCount(userId: String): Flow<Int>

    suspend fun deleteAllMessages(userId: String)

    suspend fun deleteMessage(messageId: String)

    /**
     * 删除指定角色的所有消息
     *
     * @param userId 用户ID
     * @param companionId 角色ID
     */
    suspend fun deleteMessagesByCompanion(userId: String, companionId: String)

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

    /**
     * 观察特定角色的最新消息
     *
     * @param userId 用户ID
     * @param companionId 角色ID
     * @return 最新消息的 Flow，如果没有消息则为 null
     */
    fun observeLatestMessage(userId: String, companionId: String): Flow<Message?>
}

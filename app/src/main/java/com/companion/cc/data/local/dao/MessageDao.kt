package com.companion.cc.data.local.dao

import androidx.room.*
import com.companion.cc.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId AND companion_id = :companionId
        ORDER BY timestamp ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getMessages(
        userId: String,
        companionId: String,
        limit: Int,
        offset: Int
    ): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId
        ORDER BY timestamp ASC, id ASC
    """)
    fun getAllMessages(userId: String): Flow<List<MessageEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT COUNT(*) FROM messages WHERE user_id = :userId")
    suspend fun getMessageCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE user_id = :userId")
    fun observeMessageCount(userId: String): Flow<Int>

    @Query("DELETE FROM messages WHERE user_id = :userId")
    suspend fun deleteAllMessages(userId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE user_id = :userId AND companion_id = :companionId")
    suspend fun deleteMessagesByCompanion(userId: String, companionId: String)

    @Query("UPDATE messages SET importance = :importance WHERE id = :messageId")
    suspend fun updateMessageImportance(messageId: String, importance: Int)

    @Query("UPDATE messages SET is_favorited = :isFavorited WHERE id = :messageId")
    suspend fun updateMessageFavorite(messageId: String, isFavorited: Boolean)

    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId AND is_favorited = 1
        ORDER BY timestamp DESC
    """)
    fun getFavoritedMessages(userId: String): Flow<List<MessageEntity>>

    /**
     * 搜索消息
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID（可选，为空则搜索所有对话）
     * @param query 搜索关键词
     * @return 匹配的消息列表
     */
    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId
        AND (:companionId IS NULL OR companion_id = :companionId)
        AND content LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT 50
    """)
    fun searchMessages(
        userId: String,
        companionId: String?,
        query: String
    ): Flow<List<MessageEntity>>

    /**
     * 观察特定角色的最新消息
     *
     * @param userId 用户ID
     * @param companionId 角色ID
     * @return 最新消息的 Flow，如果没有消息则为 null
     */
    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId AND companion_id = :companionId
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    fun observeLatestMessage(userId: String, companionId: String): Flow<MessageEntity?>
}

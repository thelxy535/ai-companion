package com.companion.cc.data.local.dao

import androidx.room.*
import com.companion.cc.data.local.entity.MessageTagEntity
import com.companion.cc.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // 标签管理
    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY created_at DESC")
    fun getTags(userId: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM message_tags WHERE message_id IN (SELECT id FROM messages WHERE user_id = :userId AND companion_id = :companionId)")
    suspend fun deleteMessageTagsForCompanion(userId: String, companionId: String): Int

    // 消息-标签关联
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTagToMessage(messageTag: MessageTagEntity)

    @Query("DELETE FROM message_tags WHERE message_id = :messageId AND tag_id = :tagId")
    suspend fun removeTagFromMessage(messageId: String, tagId: String)

    @Query("SELECT * FROM tags WHERE id IN (SELECT tag_id FROM message_tags WHERE message_id = :messageId)")
    fun getMessageTags(messageId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT DISTINCT m.* FROM messages m
        INNER JOIN message_tags mt ON m.id = mt.message_id
        WHERE m.user_id = :userId AND mt.tag_id = :tagId
        ORDER BY m.timestamp DESC
    """)
    fun getMessagesByTag(userId: String, tagId: String): Flow<List<com.companion.cc.data.local.entity.MessageEntity>>

    @Query("SELECT COUNT(*) FROM message_tags WHERE tag_id = :tagId")
    suspend fun getTagUsageCount(tagId: String): Int
}

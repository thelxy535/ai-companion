package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.InteractionTimeEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 交互时间 DAO
 */
@Dao
interface InteractionTimeDao {

    /**
     * 插入交互时间记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interactionTime: InteractionTimeEntity): Long

    /**
     * 获取最近的一次交互时间
     */
    @Query("""
        SELECT * FROM interaction_times
        WHERE userId = :userId AND companionId = :companionId
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLastInteraction(userId: String, companionId: String): InteractionTimeEntity?

    /**
     * 获取今天的交互次数
     */
    @Query("""
        SELECT COUNT(*) FROM interaction_times
        WHERE userId = :userId
        AND companionId = :companionId
        AND timestamp >= :startOfDay
    """)
    suspend fun getTodayInteractionCount(
        userId: String,
        companionId: String,
        startOfDay: LocalDateTime
    ): Int

    /**
     * 获取本周的交互次数
     */
    @Query("""
        SELECT COUNT(*) FROM interaction_times
        WHERE userId = :userId
        AND companionId = :companionId
        AND timestamp >= :startOfWeek
    """)
    suspend fun getWeeklyInteractionCount(
        userId: String,
        companionId: String,
        startOfWeek: LocalDateTime
    ): Int

    /**
     * 获取最近N天的交互记录
     */
    @Query("""
        SELECT * FROM interaction_times
        WHERE userId = :userId
        AND companionId = :companionId
        AND timestamp >= :since
        ORDER BY timestamp DESC
    """)
    fun getRecentInteractions(
        userId: String,
        companionId: String,
        since: LocalDateTime
    ): Flow<List<InteractionTimeEntity>>

    /**
     * 清理过期的交互记录（超过N天）
     */
    @Query("""
        DELETE FROM interaction_times
        WHERE timestamp < :before
    """)
    suspend fun cleanupOldRecords(before: LocalDateTime): Int

    /**
     * 删除指定用户和伴侣的所有交互记录
     */
    @Query("""
        DELETE FROM interaction_times
        WHERE userId = :userId AND companionId = :companionId
    """)
    suspend fun deleteAllForUserAndCompanion(userId: String, companionId: String): Int
}

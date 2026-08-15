package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.UserEventEntity
import com.companion.cc.domain.model.EventType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 用户事件 DAO
 */
@Dao
interface UserEventDao {

    /**
     * 插入事件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: UserEventEntity): Long

    /**
     * 批量插入事件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<UserEventEntity>)

    /**
     * 获取最近的事件
     */
    @Query("""
        SELECT * FROM user_events
        WHERE userId = :userId AND companionId = :companionId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentEvents(
        userId: String,
        companionId: String,
        limit: Int
    ): List<UserEventEntity>

    /**
     * 获取指定时间范围内的事件
     */
    @Query("""
        SELECT * FROM user_events
        WHERE userId = :userId
        AND companionId = :companionId
        AND timestamp >= :since
        ORDER BY timestamp DESC
    """)
    suspend fun getEventsSince(
        userId: String,
        companionId: String,
        since: LocalDateTime
    ): List<UserEventEntity>

    /**
     * 获取指定类型的最近事件
     */
    @Query("""
        SELECT * FROM user_events
        WHERE userId = :userId
        AND companionId = :companionId
        AND eventType = :eventType
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentEventsByType(
        userId: String,
        companionId: String,
        eventType: EventType,
        limit: Int
    ): List<UserEventEntity>

    /**
     * 统计指定时间范围内的事件数量
     */
    @Query("""
        SELECT COUNT(*) FROM user_events
        WHERE userId = :userId
        AND companionId = :companionId
        AND eventType = :eventType
        AND timestamp >= :since
    """)
    suspend fun countEventsSince(
        userId: String,
        companionId: String,
        eventType: EventType,
        since: LocalDateTime
    ): Int

    /**
     * 获取最近的消息事件（用于节奏分析）
     */
    @Query("""
        SELECT * FROM user_events
        WHERE userId = :userId
        AND companionId = :companionId
        AND eventType IN ('MESSAGE_SENT', 'MESSAGE_WITH_IMAGE', 'MESSAGE_VOICE')
        AND timestamp >= :since
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentMessageEvents(
        userId: String,
        companionId: String,
        since: LocalDateTime
    ): List<UserEventEntity>

    /**
     * 清理过期的事件记录
     */
    @Query("""
        DELETE FROM user_events
        WHERE timestamp < :before
    """)
    suspend fun cleanupOldEvents(before: LocalDateTime): Int

    /**
     * 删除指定用户和伴侣的所有事件
     */
    @Query("""
        DELETE FROM user_events
        WHERE userId = :userId AND companionId = :companionId
    """)
    suspend fun deleteAllForUserAndCompanion(userId: String, companionId: String): Int

    /**
     * 观察最近的事件（Flow）
     */
    @Query("""
        SELECT * FROM user_events
        WHERE userId = :userId AND companionId = :companionId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun observeRecentEvents(
        userId: String,
        companionId: String,
        limit: Int
    ): Flow<List<UserEventEntity>>
}

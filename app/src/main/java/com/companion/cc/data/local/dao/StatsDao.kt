package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.MessageEntity

@Dao
interface StatsDao {

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE user_id = :userId
    """)
    suspend fun getTotalMessages(userId: String): Int

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE user_id = :userId AND role = 'user'
    """)
    suspend fun getUserMessages(userId: String): Int

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE user_id = :userId AND role = 'assistant'
    """)
    suspend fun getAssistantMessages(userId: String): Int

    @Query("""
        SELECT COUNT(DISTINCT DATE(timestamp/1000, 'unixepoch'))
        FROM messages
        WHERE user_id = :userId
    """)
    suspend fun getTotalDays(userId: String): Int

    @Query("""
        SELECT companion_id, COUNT(*) as count,
               MAX(timestamp) as last_time
        FROM messages
        WHERE user_id = :userId
        GROUP BY companion_id
    """)
    suspend fun getCompanionStats(userId: String): List<CompanionStatRow>

    @Query("""
        SELECT DATE(timestamp/1000, 'unixepoch') as date,
               COUNT(*) as count
        FROM messages
        WHERE user_id = :userId
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getMessageCountByDate(userId: String): List<DateCountRow>

    @Query("""
        SELECT * FROM messages
        WHERE user_id = :userId
          AND DATE(timestamp/1000, 'unixepoch') = :date
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesByDate(userId: String, date: String): List<MessageEntity>

    @Query("""
        DELETE FROM messages
        WHERE user_id = :userId
    """)
    suspend fun deleteAllMessages(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
}

data class CompanionStatRow(
    val companion_id: String,
    val count: Int,
    val last_time: Long
)

data class DateCountRow(
    val date: String,
    val count: Int
)

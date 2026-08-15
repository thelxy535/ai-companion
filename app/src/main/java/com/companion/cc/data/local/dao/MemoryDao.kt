package com.companion.cc.data.local.dao

import androidx.room.*
import com.companion.cc.data.local.entity.MemoryEntity
import com.companion.cc.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    // 插入记忆
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    // 获取最近N条记忆（短期记忆）
    @Query("SELECT * FROM memories WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(userId: String, limit: Int = 10): List<MemoryEntity>

    // 获取某天的记忆
    @Query("SELECT * FROM memories WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getMemoriesByDate(userId: String, date: String): List<MemoryEntity>

    // 获取最近N天的记忆
    @Query("""
        SELECT * FROM memories
        WHERE userId = :userId
        AND timestamp >= :sinceTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentDaysMemories(userId: String, sinceTimestamp: Long): List<MemoryEntity>

    // 搜索记忆（简单关键词匹配）
    @Query("""
        SELECT * FROM memories
        WHERE userId = :userId
        AND content LIKE '%' || :keyword || '%'
        ORDER BY importance DESC, timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchMemories(userId: String, keyword: String, limit: Int = 10): List<MemoryEntity>

    // 获取重要记忆
    @Query("""
        SELECT * FROM memories
        WHERE userId = :userId
        AND importance >= :minImportance
        ORDER BY importance DESC, timestamp DESC
        LIMIT :limit
    """)
    suspend fun getImportantMemories(userId: String, minImportance: Int = 70, limit: Int = 20): List<MemoryEntity>

    // 获取所有记忆数量
    @Query("SELECT COUNT(*) FROM memories WHERE userId = :userId")
    suspend fun getMemoryCount(userId: String): Int

    // 删除旧记忆（保留最近N条）
    @Query("""
        DELETE FROM memories
        WHERE userId = :userId
        AND id NOT IN (
            SELECT id FROM memories
            WHERE userId = :userId
            ORDER BY timestamp DESC
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldMemories(userId: String, keepCount: Int = 1000)
}

@Dao
interface UserProfileDao {

    // 插入或更新用户画像
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // 获取用户画像
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?

    // 获取用户画像（Flow）
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getProfileFlow(userId: String): Flow<UserProfileEntity?>

    // 删除用户画像
    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteProfile(userId: String)
}

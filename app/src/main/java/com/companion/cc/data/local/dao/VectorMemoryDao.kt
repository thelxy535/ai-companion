package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.VectorMemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 向量记忆DAO
 */
@Dao
interface VectorMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: VectorMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<VectorMemoryEntity>)

    @Query("SELECT * FROM vector_memories WHERE userId = :userId AND companionId = :companionId ORDER BY timestamp DESC")
    fun getMemoriesByUser(userId: String, companionId: String): Flow<List<VectorMemoryEntity>>

    @Query("SELECT * FROM vector_memories WHERE userId = :userId AND companionId = :companionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(userId: String, companionId: String, limit: Int): List<VectorMemoryEntity>

    @Query("SELECT * FROM vector_memories WHERE userId = :userId AND companionId = :companionId")
    suspend fun getAllMemories(userId: String, companionId: String): List<VectorMemoryEntity>

    @Query("SELECT * FROM vector_memories WHERE type = :type AND userId = :userId AND companionId = :companionId")
    suspend fun getMemoriesByType(type: String, userId: String, companionId: String): List<VectorMemoryEntity>

    @Query("DELETE FROM vector_memories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vector_memories WHERE userId = :userId AND companionId = :companionId")
    suspend fun deleteAll(userId: String, companionId: String)

    @Query("SELECT COUNT(*) FROM vector_memories WHERE userId = :userId AND companionId = :companionId")
    suspend fun getCount(userId: String, companionId: String): Int
}

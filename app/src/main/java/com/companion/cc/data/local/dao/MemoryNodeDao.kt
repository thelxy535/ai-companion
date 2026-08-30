package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.MemoryNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryNodeDao {
    @Query("SELECT COUNT(*) FROM memory_nodes WHERE scopeKey LIKE 'user:' || :userId || ':companion:%' AND status = 'active'")
    fun observeActiveCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(node: MemoryNodeEntity)

    @Query("DELETE FROM memory_nodes WHERE scopeKey = :scopeKey")
    suspend fun deleteByScope(scopeKey: String): Int

    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey ORDER BY updatedAt ASC, id ASC")
    suspend fun findAllInScope(scopeKey: String): List<MemoryNodeEntity>

    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey AND id = :id")
    suspend fun findByIdForScope(scopeKey: String, id: String): MemoryNodeEntity?

    @Query("SELECT * FROM memory_nodes WHERE id = :id")
    suspend fun findById(id: String): MemoryNodeEntity?

    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey AND status = 'active' AND validFrom <= :now AND (validUntil IS NULL OR validUntil > :now) AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecallCandidates(
        scopeKey: String,
        now: Long,
        query: String = "",
        limit: Int = 100
    ): Flow<List<MemoryNodeEntity>>


    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey AND status = :status AND (:kind = '' OR kind = :kind) AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC LIMIT :limit")
    fun observeFiltered(
        scopeKey: String,
        status: String = "active",
        kind: String = "",
        query: String = "",
        limit: Int = 100
    ): Flow<List<MemoryNodeEntity>>

    @Update
    suspend fun update(node: MemoryNodeEntity)
}

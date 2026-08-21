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
    @Query("SELECT COUNT(*) FROM memory_nodes WHERE status = 'active'")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(node: MemoryNodeEntity)

    @Query("SELECT * FROM memory_nodes WHERE id = :id")
    suspend fun findById(id: String): MemoryNodeEntity?

    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey AND status = :status AND (:kind = '' OR kind = :kind) AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun observeFiltered(scopeKey: String, status: String = "active", kind: String = "", query: String = ""): Flow<List<MemoryNodeEntity>>

    @Update
    suspend fun update(node: MemoryNodeEntity)
}

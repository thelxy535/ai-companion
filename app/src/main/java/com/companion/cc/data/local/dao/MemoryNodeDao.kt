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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(node: MemoryNodeEntity)

    @Query("SELECT * FROM memory_nodes WHERE id = :id")
    suspend fun findById(id: String): MemoryNodeEntity?

    @Query("SELECT * FROM memory_nodes WHERE scopeKey = :scopeKey AND status = 'active' ORDER BY updatedAt DESC")
    fun observeActive(scopeKey: String): Flow<List<MemoryNodeEntity>>

    @Update
    suspend fun update(node: MemoryNodeEntity)
}

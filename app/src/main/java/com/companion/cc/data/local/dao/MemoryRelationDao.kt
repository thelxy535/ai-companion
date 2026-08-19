package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.MemoryRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryRelationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(relation: MemoryRelationEntity)

    @Query("SELECT * FROM memory_relations WHERE scopeKey = :scopeKey AND status = 'active' ORDER BY updatedAt DESC")
    fun observeActive(scopeKey: String): Flow<List<MemoryRelationEntity>>

    @Query("SELECT * FROM memory_relations WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun findForNode(nodeId: String): List<MemoryRelationEntity>

    @Update
    suspend fun update(relation: MemoryRelationEntity)
}

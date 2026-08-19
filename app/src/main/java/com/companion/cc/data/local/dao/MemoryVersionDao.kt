package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.MemoryVersionEntity

@Dao
interface MemoryVersionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(version: MemoryVersionEntity)

    @Query("SELECT * FROM memory_versions WHERE nodeId = :nodeId ORDER BY version DESC")
    suspend fun findForNode(nodeId: String): List<MemoryVersionEntity>
}

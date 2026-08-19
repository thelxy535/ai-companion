package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.MemoryEvidenceEntity

@Dao
interface MemoryEvidenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(evidence: List<MemoryEvidenceEntity>)

    @Query("SELECT * FROM memory_evidence WHERE nodeId = :nodeId ORDER BY createdAt ASC")
    suspend fun findForNode(nodeId: String): List<MemoryEvidenceEntity>
}

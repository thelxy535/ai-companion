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

    @Query("DELETE FROM memory_evidence WHERE nodeId IN (SELECT id FROM memory_nodes WHERE scopeKey = :scopeKey)")
    suspend fun deleteByScope(scopeKey: String): Int

    @Query(
        "SELECT evidence.* FROM memory_evidence AS evidence " +
            "INNER JOIN memory_nodes AS node ON evidence.nodeId = node.id " +
            "INNER JOIN memory_sources AS source ON evidence.sourceId = source.id " +
            "WHERE node.scopeKey = :scopeKey AND source.scopeKey = :scopeKey " +
            "ORDER BY evidence.createdAt ASC, evidence.nodeId ASC, evidence.sourceId ASC"
    )
    suspend fun findAllInScope(scopeKey: String): List<MemoryEvidenceEntity>

    @Query("SELECT * FROM memory_evidence WHERE nodeId IN (SELECT id FROM memory_nodes WHERE scopeKey = :scopeKey AND id = :nodeId) ORDER BY createdAt ASC")
    suspend fun findForNodeInScope(scopeKey: String, nodeId: String): List<MemoryEvidenceEntity>

    @Query("SELECT * FROM memory_evidence WHERE nodeId = :nodeId ORDER BY createdAt ASC")
    suspend fun findForNode(nodeId: String): List<MemoryEvidenceEntity>
}

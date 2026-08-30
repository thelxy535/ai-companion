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

    @Query("DELETE FROM memory_versions WHERE nodeId IN (SELECT id FROM memory_nodes WHERE scopeKey = :scopeKey)")
    suspend fun deleteByScope(scopeKey: String): Int

    @Query(
        "SELECT version.* FROM memory_versions AS version " +
            "INNER JOIN memory_nodes AS node ON version.nodeId = node.id " +
            "WHERE node.scopeKey = :scopeKey " +
            "ORDER BY version.createdAt ASC, version.nodeId ASC, version.version ASC"
    )
    suspend fun findAllInScope(scopeKey: String): List<MemoryVersionEntity>

    @Query("SELECT * FROM memory_versions WHERE nodeId IN (SELECT id FROM memory_nodes WHERE scopeKey = :scopeKey AND id = :nodeId) ORDER BY version DESC")
    suspend fun findForNodeInScope(scopeKey: String, nodeId: String): List<MemoryVersionEntity>

    @Query("SELECT * FROM memory_versions WHERE nodeId = :nodeId ORDER BY version DESC")
    suspend fun findForNode(nodeId: String): List<MemoryVersionEntity>
}

package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.companion.cc.data.local.entity.MemoryScopeQuarantineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryScopeQuarantineDao {
    @Query(
        "SELECT * FROM memory_scope_quarantine " +
            "WHERE legacyScopeKey = :legacyScopeKey ORDER BY createdAt ASC"
    )
    fun observeForLegacyScope(legacyScopeKey: String): Flow<List<MemoryScopeQuarantineEntity>>

    @Query("DELETE FROM memory_scope_quarantine WHERE legacyScopeKey = :legacyScopeKey")
    suspend fun deleteForLegacyScope(legacyScopeKey: String): Int
}

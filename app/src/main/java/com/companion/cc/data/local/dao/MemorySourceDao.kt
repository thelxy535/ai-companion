package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.MemorySourceEntity

@Dao
interface MemorySourceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: MemorySourceEntity)

    @Query("DELETE FROM memory_sources WHERE scopeKey = :scopeKey")
    suspend fun deleteByScope(scopeKey: String): Int

    @Query("SELECT * FROM memory_sources WHERE scopeKey = :scopeKey ORDER BY occurredAt ASC, id ASC")
    suspend fun findAllInScope(scopeKey: String): List<MemorySourceEntity>

    @Query("SELECT * FROM memory_sources WHERE scopeKey = :scopeKey AND id = :id LIMIT 1")
    suspend fun findByIdForScope(scopeKey: String, id: String): MemorySourceEntity?

    @Query("SELECT * FROM memory_sources WHERE scopeKey = :scopeKey AND contentHash = :contentHash LIMIT 1")
    suspend fun findByContentHashInScope(scopeKey: String, contentHash: String): MemorySourceEntity?

    @Query("SELECT * FROM memory_sources WHERE id = :id")
    suspend fun findById(id: String): MemorySourceEntity?
}

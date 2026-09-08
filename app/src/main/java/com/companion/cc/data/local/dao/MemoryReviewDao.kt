package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.MemoryReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryReviewDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: MemoryReviewEntity)

    @Query("SELECT * FROM memory_reviews WHERE scopeKey = :scopeKey AND status IN ('pending', 'conflict') ORDER BY createdAt DESC")
    fun observePending(scopeKey: String): Flow<List<MemoryReviewEntity>>

    @Query("SELECT COUNT(*) FROM memory_reviews WHERE scopeKey = :scopeKey AND status IN ('pending', 'conflict')")
    suspend fun countPending(scopeKey: String): Int

    @Query("SELECT * FROM memory_reviews WHERE scopeKey = :scopeKey AND status = 'deferred' ORDER BY createdAt DESC")
    fun observeDeferred(scopeKey: String): Flow<List<MemoryReviewEntity>>

    @Query(
        "UPDATE memory_reviews SET status = 'pending', resolvedAt = NULL, " +
            "resolutionNote = 'auto-restored after cooldown' " +
            "WHERE scopeKey = :scopeKey AND status = 'deferred' AND createdAt <= :threshold"
    )
    suspend fun restoreDueDeferred(scopeKey: String, threshold: Long): Int

    @Query(
        "UPDATE memory_reviews SET status = 'pending', resolvedAt = NULL, " +
            "resolutionNote = 'restored by user' " +
            "WHERE scopeKey = :scopeKey AND id = :id AND status = 'deferred'"
    )
    suspend fun restoreById(scopeKey: String, id: String): Int

    @Query("DELETE FROM memory_reviews WHERE scopeKey = :scopeKey")
    suspend fun deleteByScope(scopeKey: String): Int

    @Query("SELECT * FROM memory_reviews WHERE scopeKey = :scopeKey ORDER BY createdAt ASC, id ASC")
    suspend fun findAllInScope(scopeKey: String): List<MemoryReviewEntity>

    @Query("SELECT * FROM memory_reviews WHERE scopeKey = :scopeKey AND proposalHash = :proposalHash LIMIT 1")
    suspend fun findByProposalHash(scopeKey: String, proposalHash: String): MemoryReviewEntity?

    @Query("SELECT * FROM memory_reviews WHERE scopeKey = :scopeKey AND id = :id LIMIT 1")
    suspend fun findByIdForScope(scopeKey: String, id: String): MemoryReviewEntity?

    @Query("SELECT * FROM memory_reviews WHERE id = :id")
    suspend fun findById(id: String): MemoryReviewEntity?

    @Update
    suspend fun update(review: MemoryReviewEntity)
}

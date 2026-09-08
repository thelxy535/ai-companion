package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.cc.data.local.entity.ReflectionJobEntity

@Dao
interface ReflectionJobDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(job: ReflectionJobEntity)

    @Query(
        "SELECT * FROM reflection_jobs " +
            "WHERE scopeKey = :scopeKey AND " +
            "((status IN ('PENDING', 'RETRY_WAIT') AND nextRunAt <= :now) " +
            "OR (status = 'RUNNING' AND leaseUntil IS NOT NULL AND leaseUntil <= :now)) " +
            "ORDER BY nextRunAt ASC, id ASC LIMIT :limit"
    )
    suspend fun findDue(scopeKey: String, now: Long, limit: Int): List<ReflectionJobEntity>

    @Query(
        "UPDATE reflection_jobs SET status = 'RUNNING', attempts = attempts + 1, " +
            "leaseOwner = :owner, leaseUntil = :leaseUntil, updatedAt = :now " +
            "WHERE id = :id AND scopeKey = :scopeKey AND " +
            "((status IN ('PENDING', 'RETRY_WAIT') AND nextRunAt <= :now) " +
            "OR (status = 'RUNNING' AND leaseUntil IS NOT NULL AND leaseUntil <= :now))"
    )
    suspend fun claim(
        id: String,
        scopeKey: String,
        owner: String,
        leaseUntil: Long,
        now: Long
    ): Int

    @Query(
        "UPDATE reflection_jobs SET status = 'COMPLETED', leaseOwner = NULL, leaseUntil = NULL, " +
            "lastError = NULL, updatedAt = :now " +
            "WHERE id = :id AND scopeKey = :scopeKey AND status = 'RUNNING' AND leaseOwner = :owner"
    )
    suspend fun markCompleted(id: String, scopeKey: String, owner: String, now: Long): Int

    @Query(
        "UPDATE reflection_jobs SET status = 'RETRY_WAIT', nextRunAt = :nextRunAt, " +
            "leaseOwner = NULL, leaseUntil = NULL, lastError = :error, updatedAt = :now " +
            "WHERE id = :id AND scopeKey = :scopeKey AND status = 'RUNNING' AND leaseOwner = :owner"
    )
    suspend fun requeue(
        id: String,
        scopeKey: String,
        owner: String,
        nextRunAt: Long,
        error: String?,
        now: Long
    ): Int

    @Query(
        "UPDATE reflection_jobs SET status = 'FAILED', leaseOwner = NULL, leaseUntil = NULL, " +
            "lastError = :error, updatedAt = :now " +
            "WHERE id = :id AND scopeKey = :scopeKey AND status = 'RUNNING' AND leaseOwner = :owner"
    )
    suspend fun markFailed(id: String, scopeKey: String, owner: String, error: String?, now: Long): Int
}

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

    @Query("SELECT * FROM memory_reviews WHERE id = :id")
    suspend fun findById(id: String): MemoryReviewEntity?

    @Update
    suspend fun update(review: MemoryReviewEntity)
}

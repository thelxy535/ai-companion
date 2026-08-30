package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity

@Dao
interface CharacterCleanupTaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: CharacterCleanupTaskEntity)

    @Query("SELECT * FROM character_cleanup_tasks WHERE userId = :userId AND status = 'pending' ORDER BY updatedAt ASC")
    suspend fun findPending(userId: String): List<CharacterCleanupTaskEntity>

    @Query("SELECT * FROM character_cleanup_tasks WHERE userId = :userId AND id = :id")
    suspend fun findById(userId: String, id: String): CharacterCleanupTaskEntity?

    @Update
    suspend fun update(task: CharacterCleanupTaskEntity)
}

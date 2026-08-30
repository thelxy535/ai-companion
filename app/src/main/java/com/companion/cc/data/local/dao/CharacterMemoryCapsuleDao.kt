package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterMemoryCapsuleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(capsule: CharacterMemoryCapsuleEntity)

    @Query("SELECT * FROM character_memory_capsules WHERE userId = :userId AND id = :id")
    suspend fun findById(userId: String, id: String): CharacterMemoryCapsuleEntity?

    @Query("SELECT * FROM character_memory_capsules WHERE userId = :userId AND revivalTokenHash = :tokenHash")
    suspend fun findByTokenHash(userId: String, tokenHash: String): CharacterMemoryCapsuleEntity?

    @Query("SELECT * FROM character_memory_capsules WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<CharacterMemoryCapsuleEntity>>

    @Query("UPDATE character_memory_capsules SET status = 'restored', restoredAt = :restoredAt, restoredCharacterId = :restoredCharacterId WHERE userId = :userId AND id = :id AND status = 'available'")
    suspend fun markRestoredIfAvailable(
        userId: String,
        id: String,
        restoredCharacterId: String,
        restoredAt: Long
    ): Int

    @Update
    suspend fun update(capsule: CharacterMemoryCapsuleEntity)
}

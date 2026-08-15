package com.companion.cc.data.local.dao

import androidx.room.*
import com.companion.cc.data.local.entity.CustomCharacterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 自定义角色 DAO
 */
@Dao
interface CustomCharacterDao {

    @Query("SELECT * FROM custom_characters WHERE userId = :userId ORDER BY createdAt DESC")
    fun getCharactersByUser(userId: String): Flow<List<CustomCharacterEntity>>

    @Query("SELECT * FROM custom_characters WHERE id = :characterId LIMIT 1")
    suspend fun getCharacterById(characterId: String): CustomCharacterEntity?

    @Query("SELECT * FROM custom_characters WHERE id = :characterId LIMIT 1")
    fun getCharacterByIdFlow(characterId: String): Flow<CustomCharacterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CustomCharacterEntity)

    @Update
    suspend fun update(character: CustomCharacterEntity)

    @Query("DELETE FROM custom_characters WHERE id = :characterId")
    suspend fun delete(characterId: String)

    @Query("DELETE FROM custom_characters WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT COUNT(*) FROM custom_characters WHERE userId = :userId")
    suspend fun getCharacterCount(userId: String): Int
}

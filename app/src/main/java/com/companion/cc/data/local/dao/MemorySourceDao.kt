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

    @Query("SELECT * FROM memory_sources WHERE id = :id")
    suspend fun findById(id: String): MemorySourceEntity?
}

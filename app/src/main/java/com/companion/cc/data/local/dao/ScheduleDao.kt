package com.companion.cc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.companion.cc.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: ScheduleEntity)

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE userId = :userId AND characterId = :characterId AND status != 'DELETED' ORDER BY nextRunAt ASC")
    fun observe(userId: String, characterId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE userId = :userId AND characterId = :characterId AND id = :id")
    suspend fun find(userId: String, characterId: String, id: String): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE userId = :userId AND characterId = :characterId AND status = 'ACTIVE' AND nextRunAt <= :now ORDER BY nextRunAt ASC")
    suspend fun due(userId: String, characterId: String, now: Long): List<ScheduleEntity>
}

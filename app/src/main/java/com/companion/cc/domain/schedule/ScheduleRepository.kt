package com.companion.cc.domain.schedule

import com.companion.cc.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    suspend fun create(schedule: ScheduleEntity)
    fun observe(userId: String, characterId: String): Flow<List<ScheduleEntity>>
    suspend fun pause(userId: String, characterId: String, id: String)
    suspend fun resume(userId: String, characterId: String, id: String)
    suspend fun delete(userId: String, characterId: String, id: String)
    suspend fun due(userId: String, characterId: String, now: Long): List<ScheduleEntity>
    suspend fun update(schedule: ScheduleEntity)}

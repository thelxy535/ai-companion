package com.companion.cc.data.local.repository

import com.companion.cc.data.local.dao.ScheduleDao
import com.companion.cc.data.local.entity.ScheduleEntity
import com.companion.cc.domain.schedule.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomScheduleRepository @Inject constructor(
    private val dao: ScheduleDao
) : ScheduleRepository {
    override suspend fun create(schedule: ScheduleEntity) = dao.insert(schedule)

    override fun observe(userId: String, characterId: String): Flow<List<ScheduleEntity>> =
        dao.observe(userId, characterId)

    override suspend fun pause(userId: String, characterId: String, id: String) = updateStatus(userId, characterId, id, "PAUSED")

    override suspend fun resume(userId: String, characterId: String, id: String) = updateStatus(userId, characterId, id, "ACTIVE")

    override suspend fun delete(userId: String, characterId: String, id: String) = updateStatus(userId, characterId, id, "DELETED")

    override suspend fun due(userId: String, characterId: String, now: Long): List<ScheduleEntity> =
        dao.due(userId, characterId, now)

    override suspend fun update(schedule: ScheduleEntity) = dao.update(schedule)

    private suspend fun updateStatus(userId: String, characterId: String, id: String, status: String) {
        val current = requireNotNull(dao.find(userId, characterId, id)) { "计划不存在" }
        dao.update(current.copy(status = status, updatedAt = System.currentTimeMillis()))
    }
}

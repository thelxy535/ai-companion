package com.companion.cc.domain.commitment

import com.companion.cc.data.local.entity.ScheduleEntity
import com.companion.cc.domain.schedule.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentRepositoryTest {
    private val scopeUserId = "user-1"
    private val scopeCharacter = "char-1"

    private class FakeScheduleRepository : ScheduleRepository {
        val store = MutableStateFlow<List<ScheduleEntity>>(emptyList())

        override suspend fun create(schedule: ScheduleEntity) {
            store.value = store.value + schedule
        }

        override fun observe(userId: String, characterId: String) =
            store

        override suspend fun pause(userId: String, characterId: String, id: String) {
            store.value = store.value.map { if (it.id == id) it.copy(status = "PAUSED") else it }
        }

        override suspend fun resume(userId: String, characterId: String, id: String) {
            store.value = store.value.map { if (it.id == id) it.copy(status = "ACTIVE") else it }
        }

        override suspend fun delete(userId: String, characterId: String, id: String) {
            store.value = store.value.filterNot { it.id == id }
        }

        override suspend fun due(userId: String, characterId: String, now: Long) =
            store.value.filter { it.status == "ACTIVE" && it.nextRunAt <= now }

        override suspend fun update(schedule: ScheduleEntity) {
            store.value = store.value.map { if (it.id == schedule.id) schedule else it }
        }
    }

    @Test
    fun `create stores commitment with prefix and due in future`() = runTest {
        val repo = CommitmentRepository(FakeScheduleRepository())
        val now = 1_000_000L
        val id = repo.create(scopeUserId, scopeCharacter, "明天提醒你吃药", now + 3_600_000L, now = now)

        val all = repo.forCharacter(scopeUserId, scopeCharacter)
        assertEquals(1, all.size)
        assertEquals("明天提醒你吃药", all.single().promise)
        assertEquals("ACTIVE", all.single().status)
        assertTrue(id.startsWith("commitment:"))
    }

    @Test
    fun `due returns only active commitments at or before now`() = runTest {
        val repo = CommitmentRepository(FakeScheduleRepository())
        val now = 1_000_000L
        repo.create(scopeUserId, scopeCharacter, "早上的提醒", now + 3_600_000L, now = now)
        repo.create(scopeUserId, scopeCharacter, "到点的承诺", now + 100L, now = now)

        // 模拟时间流逝：now 推进到超过第二个承诺的 dueAt
        val later = now + 200L
        val due = repo.due(scopeUserId, scopeCharacter, later)
        assertEquals(1, due.size)
        assertEquals("到点的承诺", due.single().promise)
    }

    @Test
    fun `markFulfilled updates status`() = runTest {
        val repo = CommitmentRepository(FakeScheduleRepository())
        val now = 1_000_000L
        val id = repo.create(scopeUserId, scopeCharacter, "今晚陪你", now + 100L, now = now)

        repo.markFulfilled(scopeUserId, scopeCharacter, id, now = now + 200L)

        val item = repo.forCharacter(scopeUserId, scopeCharacter).single()
        assertEquals("FULFILLED", item.status)
    }
}

package com.companion.cc.data.local.repository

import com.companion.cc.data.local.dao.ReflectionJobDao
import com.companion.cc.data.local.entity.ReflectionJobEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReflectionJobSchedulerTest {
    @Test
    fun `same scope cursor and policy produce same job identity`() = runTest {
        val inserted = mutableListOf<ReflectionJobEntity>()
        val scheduler = ReflectionJobScheduler(recordingDao(inserted))

        val first = scheduler.enqueue("user:u1:companion:c1", "message-a", now = 100L, delayMs = 0L)
        val second = scheduler.enqueue("user:u1:companion:c1", "message-a", now = 200L, delayMs = 0L)

        assertEquals(first.id, second.id)
        assertEquals(first.idempotencyKey, second.idempotencyKey)
        assertEquals(2, inserted.size)
    }

    @Test
    fun `different cursor or scope produces different job identity`() = runTest {
        val scheduler = ReflectionJobScheduler(recordingDao(mutableListOf()))

        val first = scheduler.enqueue("user:u1:companion:c1", "message-a", now = 100L, delayMs = 0L)
        val otherCursor = scheduler.enqueue("user:u1:companion:c1", "message-b", now = 100L, delayMs = 0L)
        val otherScope = scheduler.enqueue("user:u1:companion:c2", "message-a", now = 100L, delayMs = 0L)

        assertNotEquals(first.id, otherCursor.id)
        assertNotEquals(first.id, otherScope.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank cursor is rejected`() = runTest {
        ReflectionJobScheduler(recordingDao(mutableListOf()))
            .enqueue("user:u1:companion:c1", " ")
    }

    private fun recordingDao(inserted: MutableList<ReflectionJobEntity>) = object : ReflectionJobDao {
        override suspend fun insert(job: ReflectionJobEntity) {
            inserted += job
        }
        override suspend fun findDue(scopeKey: String, now: Long, limit: Int) = emptyList<ReflectionJobEntity>()
        override suspend fun claim(id: String, scopeKey: String, owner: String, leaseUntil: Long, now: Long) = 0
        override suspend fun markCompleted(id: String, scopeKey: String, owner: String, now: Long) = 0
        override suspend fun requeue(id: String, scopeKey: String, owner: String, nextRunAt: Long, error: String?, now: Long) = 0
        override suspend fun markFailed(id: String, scopeKey: String, owner: String, error: String?, now: Long) = 0
    }
}

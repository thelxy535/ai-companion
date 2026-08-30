package com.companion.cc.domain.schedule

import com.companion.cc.data.local.entity.ScheduleEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ScheduleWorkRunnerTest {
    @Test
    fun `advances recurring schedule instead of leaving it immediately due`() = runBlocking {
        val due = ScheduleEntity("s1", "u1", "c1", "hello", "DAILY", 1L, createdAt = 1L, updatedAt = 1L)
        var updated: ScheduleEntity? = null
        val runner = ScheduleWorkRunner(
            loadDue = { listOf(due) },
            execute = {},
            markCompleted = { updated = it }
        )

        assertEquals(ScheduleWorkResult.SUCCESS, runner.run(now = 2L))
        assertEquals(86_400_002L, updated?.nextRunAt)
        assertEquals("ACTIVE", updated?.status)
    }
    @Test
    fun `propagates cancellation instead of converting it to retry`() = runBlocking {
        val due = ScheduleEntity("s1", "u1", "c1", "hello", "ONCE", 1L, createdAt = 1L, updatedAt = 1L)
        var marked = false
        val runner = ScheduleWorkRunner(
            loadDue = { listOf(due) },
            execute = { throw CancellationException("worker stopped") },
            markCompleted = { marked = true }
        )

        try {
            runner.run(now = 2L)
            fail("CancellationException should be propagated")
        } catch (error: CancellationException) {
            assertEquals("worker stopped", error.message)
        }
        assertEquals(false, marked)
    }
}

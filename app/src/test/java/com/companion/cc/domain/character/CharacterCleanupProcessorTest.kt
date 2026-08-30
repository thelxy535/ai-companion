package com.companion.cc.domain.character

import com.companion.cc.data.local.dao.CharacterCleanupTaskDao
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CharacterCleanupProcessorTest {
    private lateinit var dao: CharacterCleanupTaskDao
    private lateinit var cleaner: CharacterExternalCleanup
    private lateinit var processor: CharacterCleanupProcessor

    @Before
    fun setUp() {
        dao = mock()
        cleaner = mock()
        processor = CharacterCleanupProcessor(dao, cleaner)
    }

    @Test
    fun successfulCleanupMarksTaskCompleted() = runTest {
        val task = task()
        whenever(dao.findPending("user-1")).thenReturn(listOf(task))

        processor.processPending("user-1", now = 20L)

        verify(cleaner).cleanup(task)
        val updated = argumentCaptor<CharacterCleanupTaskEntity>()
        verify(dao).update(updated.capture())
        assertEquals("completed", updated.firstValue.status)
        assertEquals(1, updated.firstValue.attempts)
        assertEquals(20L, updated.firstValue.updatedAt)
    }

    @Test
    fun failedCleanupRemainsPendingWithRetryMetadata() = runTest {
        val task = task()
        whenever(dao.findPending("user-1")).thenReturn(listOf(task))
        whenever(cleaner.cleanup(task)).thenThrow(IllegalStateException("avatar unavailable"))

        processor.processPending("user-1", now = 30L)

        val updated = argumentCaptor<CharacterCleanupTaskEntity>()
        verify(dao).update(updated.capture())
        assertEquals("pending", updated.firstValue.status)
        assertEquals(1, updated.firstValue.attempts)
        assertEquals("avatar unavailable", updated.firstValue.lastError)
        assertEquals(30L, updated.firstValue.updatedAt)
    }

    @Test
    fun processPendingReportsWhetherRetryWorkRemains() = runTest {
        val successful = task().copy(id = "success")
        val failed = task().copy(id = "failed")
        whenever(dao.findPending("user-1")).thenReturn(listOf(successful, failed))
        whenever(cleaner.cleanup(failed)).thenThrow(IllegalStateException("still unavailable"))

        val result = processor.processPending("user-1", now = 40L)

        assertEquals(2, result.processedCount)
        assertEquals(1, result.failedCount)
        assertFalse(result.isComplete)
    }

    private fun task() = CharacterCleanupTaskEntity(
        id = "task-1", userId = "user-1", characterId = "character-1",
        createdAt = 1L, updatedAt = 1L
    )
}

package com.companion.cc.data.local.dao

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemorySourceEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MemorySourceWindowTest {
    private lateinit var database: AppDatabase

    private val scope = "user:u1:companion:c1"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seed(id: String, occurredAt: Long, scopeKey: String = scope) {
        database.memorySourceDao().insert(
            MemorySourceEntity(
                id = id,
                scopeKey = scopeKey,
                messageId = "m-$id",
                contentSnapshot = "snapshot-$id",
                sourceType = "message",
                occurredAt = occurredAt,
                contentHash = "hash-$id",
                createdAt = occurredAt
            )
        )
    }

    @Test
    fun `window returns sources at or before cursor newest first`() = runTest {
        seed("s1", 100L)
        seed("s2", 200L)
        seed("s3", 300L)
        seed("s4", 400L)

        val window = database.memorySourceDao().findRecentBefore(scope, occurredAt = 300L, limit = 10)

        assertEquals(listOf("s3", "s2", "s1"), window.map { it.id })
    }

    @Test
    fun `window respects limit`() = runTest {
        seed("s1", 100L)
        seed("s2", 200L)
        seed("s3", 300L)

        val window = database.memorySourceDao().findRecentBefore(scope, occurredAt = 300L, limit = 2)

        assertEquals(listOf("s3", "s2"), window.map { it.id })
    }

    @Test
    fun `window is scope isolated`() = runTest {
        seed("s1", 100L)
        seed("other", 150L, scopeKey = "user:u2:companion:c9")

        val window = database.memorySourceDao().findRecentBefore(scope, occurredAt = 500L, limit = 10)

        assertEquals(listOf("s1"), window.map { it.id })
    }
}

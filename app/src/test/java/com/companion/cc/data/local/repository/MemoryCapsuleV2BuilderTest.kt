package com.companion.cc.data.local.repository

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MemoryCapsuleV2BuilderTest {
    private val scope = "user:user-1:companion:character-1"
    private lateinit var database: AppDatabase

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

    @Test
    fun buildsEveryMemoryCapsuleRecordForOnlyTheRequestedScope() = runTest {
        database.memorySourceDao().insert(
            MemorySourceEntity("source-1", scope, "message-1", "Likes tea", "message", 1L, "hash-1", 1L)
        )
        database.memorySourceDao().insert(
            MemorySourceEntity("other-source", "user:user-2:companion:character-1", null, "Other", "message", 1L, "hash-2", 1L)
        )
        database.memoryNodeDao().insert(
            MemoryNodeEntity("node-1", scope, "preference", "user", "user-1", "Tea", "Likes tea", 50, 0.8, 1L, null, "active", 1L, 1L, 1)
        )
        database.memoryReviewDao().insert(
            MemoryReviewEntity("review-1", scope, "preference", "Tea", "Likes tea", 0.8, "pending", "[\"source-1\"]", "proposal-1", 1L)
        )
        database.memoryEvidenceDao().insertAll(
            listOf(MemoryEvidenceEntity("node-1", "source-1", "support", 0.8, "Tea evidence", 1L))
        )
        database.memoryVersionDao().insert(
            MemoryVersionEntity("node-1", 1, "preference", "Tea", "Likes tea", 50, 0.8, 1L, null, "accepted", "user", 1L)
        )
        database.memoryNodeDao().insert(
            MemoryNodeEntity("node-2", scope, "context", "user", "user-1", "Morning", "Tea in the morning", 40, 0.6, 1L, null, "active", 1L, 1L, 1)
        )
        database.memoryRelationDao().insert(
            MemoryRelationEntity("node-1", "node-2", "supports", scope, 0.7, "proposed", "[]", 1L, 1L)
        )
        database.memoryRetrievalDao().insertTrace(
            MemoryRetrievalTraceEntity("trace-1", scope, "tea", "[\"node-1\"]", "{}", 1L, 4L)
        )
        database.memoryRetrievalDao().insertFeedback(
            MemoryRetrievalFeedbackEntity("trace-1", "node-1", "positive", "useful", 1L)
        )

        val result = MemoryCapsuleV2Builder(database).build(scope)

        assertTrue(result.isSuccess)
        val capsule = result.getOrThrow()
        assertEquals(scope, capsule.scopeKey)
        assertEquals(1, capsule.sources.size)
        assertEquals(2, capsule.nodes.size)
        assertEquals(1, capsule.evidence.size)
        assertEquals(1, capsule.reviews.size)
        assertEquals(1, capsule.versions.size)
        assertEquals(1, capsule.relations.size)
        assertEquals(1, capsule.retrievalTraces.size)
        assertEquals(1, capsule.retrievalFeedback.size)
        assertEquals("source-1", capsule.reviews.single().sourceIds.single())
        assertEquals("node-1", capsule.retrievalFeedback.single().nodeId)
    }

    @Test
    fun rejectsMalformedStoredReviewSourceIdsInsteadOfExportingAnInvalidCapsule() = runTest {
        database.memoryReviewDao().insert(
            MemoryReviewEntity("review-1", scope, "preference", "Tea", "Likes tea", 0.8, "pending", "not-json", "proposal-1", 1L)
        )

        val result = MemoryCapsuleV2Builder(database).build(scope)

        assertTrue(result.isFailure)
    }
}

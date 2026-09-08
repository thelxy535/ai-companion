package com.companion.cc.data.local.repository

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.domain.memory.NarrativeKinds
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NarrativeSupersedeTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MemoryRepository

    private val scope = "user:u1:companion:c1"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = MemoryRepository(
            database = database,
            sourceDao = database.memorySourceDao(),
            reviewDao = database.memoryReviewDao(),
            nodeDao = database.memoryNodeDao(),
            evidenceDao = database.memoryEvidenceDao(),
            versionDao = database.memoryVersionDao(),
            relationDao = database.memoryRelationDao(),
            retrievalDao = database.memoryRetrievalDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedSource(id: String) {
        database.memorySourceDao().insert(
            MemorySourceEntity(
                id = "message:s$id",
                scopeKey = scope,
                messageId = "m-$id",
                contentSnapshot = "snapshot-$id",
                sourceType = "message",
                occurredAt = 1L,
                contentHash = "hash-src-$id",
                createdAt = 1L
            )
        )
    }

    private suspend fun seedReview(id: String, kind: String, content: String) {
        database.memoryReviewDao().insert(
            MemoryReviewEntity(
                id = "review-$id",
                scopeKey = scope,
                kind = kind,
                title = "叙事-$id",
                content = content,
                confidence = 0.8,
                sourceIdsJson = "[\"message:s$id\"]",
                proposalHash = "hash-review-$id",
                createdAt = 1L
            )
        )
    }

    @Test
    fun `accepting relationship narrative supersedes previous active narrative`() = runTest {
        seedSource("1")
        seedSource("2")
        seedReview("1", NarrativeKinds.RELATIONSHIP, "我们刚认识不久。")
        seedReview("2", NarrativeKinds.RELATIONSHIP, "我们变得更亲密了。")

        val first = repository.acceptReview(scope, "review-1", "relationship", "pair", now = 100L)
        val second = repository.acceptReview(scope, "review-2", "relationship", "pair", now = 200L)

        assertEquals(true, first.isSuccess)
        assertEquals(true, second.isSuccess)

        val nodeDao = database.memoryNodeDao()
        val all = nodeDao.findAllInScope(scope)
        assertEquals(2, all.size)
        assertEquals(1, all.count { it.status == "active" && it.content == "我们变得更亲密了。" })
        assertEquals(1, all.count { it.status == "superseded" && it.validUntil == 200L })
    }

    @Test
    fun `non narrative kinds never supersede`() = runTest {
        seedSource("1")
        seedSource("2")
        seedReview("1", "fact", "用户在上海工作。")
        seedReview("2", "fact", "用户喜欢猫。")

        repository.acceptReview(scope, "review-1", "user", "user", now = 100L)
        repository.acceptReview(scope, "review-2", "user", "user", now = 200L)

        val all = database.memoryNodeDao().findAllInScope(scope)
        assertEquals(2, all.count { it.status == "active" })
    }
}

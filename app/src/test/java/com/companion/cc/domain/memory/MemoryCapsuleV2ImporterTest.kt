package com.companion.cc.domain.memory

import androidx.room.Room
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.repository.MemoryCapsuleV2Importer
import kotlinx.coroutines.flow.first
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
class MemoryCapsuleV2ImporterTest {
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
    fun importsAllValidatedRecordsIntoTheDeclaredScope() = runTest {
        val capsule = capsuleWithSourceAndNode()

        val result = MemoryCapsuleV2Importer(database).importCapsule(capsule, scope)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().insertedSources)
        assertEquals(1, result.getOrThrow().insertedNodes)
        assertEquals(capsule.nodes.first().id, database.memoryNodeDao().findByIdForScope(scope, "node-1")?.id)
        assertEquals(capsule.sources.first().id, database.memorySourceDao().findByIdForScope(scope, "source-1")?.id)
    }

    @Test
    fun rejectsADifferentTargetScopeWithoutWritingAnything() = runTest {
        val capsule = capsuleWithSourceAndNode()

        val result = MemoryCapsuleV2Importer(database).importCapsule(
            capsule,
            "user:user-2:companion:character-1"
        )

        assertTrue(result.isFailure)
        assertEquals(0, database.memoryNodeDao().observeFiltered(scope).first().size)
        assertEquals(0, database.memorySourceDao().findByIdForScope(scope, "source-1")?.let { 1 } ?: 0)
    }

    @Test
    fun aRepeatedIdenticalImportIsIdempotent() = runTest {
        val capsule = capsuleWithSourceAndNode()
        val importer = MemoryCapsuleV2Importer(database)

        importer.importCapsule(capsule, scope).getOrThrow()
        val second = importer.importCapsule(capsule, scope).getOrThrow()

        assertEquals(0, second.insertedSources)
        assertEquals(0, second.insertedNodes)
        assertTrue(second.skippedRecords > 0)
    }

    @Test
    fun conflictingSourceContentHashRollsBackTheEntireImport() = runTest {
        database.memorySourceDao().insert(
            com.companion.cc.data.local.entity.MemorySourceEntity(
                id = "source-1",
                scopeKey = scope,
                messageId = null,
                contentSnapshot = "Different content",
                sourceType = "message",
                occurredAt = 1L,
                contentHash = "hash-1",
                createdAt = 1L
            )
        )

        val result = MemoryCapsuleV2Importer(database).importCapsule(capsuleWithSourceAndNode(), scope)

        assertTrue(result.isFailure)
        assertEquals(null, database.memoryNodeDao().findByIdForScope(scope, "node-1"))
    }
    @Test
    fun duplicateSourceContentHashWithAnotherIdRejectsImport() = runTest {
        database.memorySourceDao().insert(
            com.companion.cc.data.local.entity.MemorySourceEntity(
                id = "existing-source",
                scopeKey = scope,
                messageId = null,
                contentSnapshot = "Existing content",
                sourceType = "message",
                occurredAt = 1L,
                contentHash = "hash-1",
                createdAt = 1L
            )
        )

        val result = MemoryCapsuleV2Importer(database).importCapsule(capsuleWithSourceAndNode(), scope)

        assertTrue(result.isFailure)
        assertEquals(null, database.memoryNodeDao().findByIdForScope(scope, "node-1"))
    }


    @Test
    fun importsEvidenceReviewsVersionsRelationsTracesAndFeedbackTogether() = runTest {
        val capsule = capsuleWithAllRecords()

        val report = MemoryCapsuleV2Importer(database).importCapsule(capsule, scope).getOrThrow()

        assertEquals(1, report.insertedEvidence)
        assertEquals(1, report.insertedReviews)
        assertEquals(1, report.insertedVersions)
        assertEquals(1, report.insertedRelations)
        assertEquals(1, report.insertedTraces)
        assertEquals(1, report.insertedFeedback)
        assertEquals(1, database.memoryEvidenceDao().findForNodeInScope(scope, "node-1").size)
        assertEquals(1, database.memoryVersionDao().findForNodeInScope(scope, "node-1").size)
        assertEquals(1, database.memoryRelationDao().findForNodeInScopeIncludingArchived(scope, "node-1").size)
        assertEquals("trace-1", database.memoryRetrievalDao().findTraceById("trace-1")?.id)
        assertEquals(1, database.memoryRetrievalDao().findFeedbackForNodes(scope, listOf("node-1")).size)
    }

    @Test
    fun scopedTraceLookupNeverReturnsARecordFromAnotherScope() = runTest {
        database.memoryRetrievalDao().insertTrace(
            MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = "user:user-2:companion:character-2",
                query = "other",
                selectedNodeIdsJson = "[]",
                explanationJson = "{}",
                createdAt = 1L,
                durationMs = 1L
            )
        )

        assertEquals(
            null,
            database.memoryRetrievalDao().findTraceByIdInScope(scope, "trace-1")
        )
    }

    @Test
    fun rollsBackEarlierRecordsWhenALateRelationConflicts() = runTest {
        database.memoryNodeDao().insert(
            MemoryNodeEntity("node-1", scope, "", "", "", "", "", 50, 0.5, 0L, null, "active", 0L, 0L, 1)
        )
        database.memoryNodeDao().insert(
            MemoryNodeEntity("node-2", scope, "", "", "", "", "", 50, 0.5, 0L, null, "active", 0L, 0L, 1)
        )
        database.memoryRelationDao().insert(
            MemoryRelationEntity("node-1", "node-2", "supports", scope, 0.1, "proposed", "[]", 1L, 1L)
        )
        val capsule = capsuleWithSourceAndNode().copy(
            nodes = listOf(
                MemoryCapsuleNode(id = "node-1", scopeKey = scope),
                MemoryCapsuleNode(id = "node-2", scopeKey = scope)
            ),
            relations = listOf(
                MemoryCapsuleRelation(
                    fromNodeId = "node-1",
                    toNodeId = "node-2",
                    relationType = "supports",
                    scopeKey = scope,
                    weight = 0.9,
                    status = "proposed",
                    evidenceJson = "[]",
                    createdAt = 1L,
                    updatedAt = 1L
                )
            )
        )

        val result = MemoryCapsuleV2Importer(database).importCapsule(capsule, scope)

        assertTrue(result.isFailure)
        assertEquals(null, database.memorySourceDao().findByIdForScope(scope, "source-1"))
        assertEquals(2, database.memoryNodeDao().observeFiltered(scope, limit = 10).first().size)
    }

    private fun capsuleWithAllRecords() = MemoryCapsuleV2(
        scopeKey = scope,
        sources = listOf(
            MemoryCapsuleSource(
                id = "source-1",
                scopeKey = scope,
                messageId = null,
                contentSnapshot = "Likes tea",
                sourceType = "message",
                occurredAt = 1L,
                contentHash = "hash-1",
                createdAt = 1L
            )
        ),
        nodes = listOf(
            MemoryCapsuleNode(id = "node-1", scopeKey = scope),
            MemoryCapsuleNode(id = "node-2", scopeKey = scope)
        ),
        evidence = listOf(MemoryCapsuleEvidence("node-1", "source-1", createdAt = 1L)),
        reviews = listOf(
            MemoryCapsuleReview(
                id = "review-1",
                scopeKey = scope,
                kind = "preference",
                title = "Tea",
                content = "Likes tea",
                confidence = 0.8,
                sourceIds = listOf("source-1"),
                proposalHash = "proposal-1",
                createdAt = 1L
            )
        ),
        versions = listOf(
            MemoryCapsuleVersion(
                nodeId = "node-1",
                version = 1,
                kind = "preference",
                title = "Tea",
                content = "Likes tea",
                importance = 50,
                confidence = 0.8,
                validFrom = 1L,
                validUntil = null,
                changeReason = "accepted",
                actor = "user",
                createdAt = 1L
            )
        ),
        relations = listOf(
            MemoryCapsuleRelation(
                fromNodeId = "node-1",
                toNodeId = "node-2",
                relationType = "supports",
                scopeKey = scope,
                createdAt = 1L,
                updatedAt = 1L
            )
        ),
        retrievalTraces = listOf(
            MemoryCapsuleRetrievalTrace(
                id = "trace-1",
                scopeKey = scope,
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "{}",
                createdAt = 1L,
                durationMs = 2L
            )
        ),
        retrievalFeedback = listOf(
            MemoryCapsuleRetrievalFeedback(
                traceId = "trace-1",
                nodeId = "node-1",
                feedback = "positive",
                createdAt = 1L
            )
        )
    )

    private fun capsuleWithSourceAndNode() = MemoryCapsuleV2(
        scopeKey = scope,
        sources = listOf(
            MemoryCapsuleSource(
                id = "source-1",
                scopeKey = scope,
                messageId = null,
                contentSnapshot = "Likes tea",
                sourceType = "message",
                occurredAt = 1L,
                contentHash = "hash-1",
                createdAt = 1L
            )
        ),
        nodes = listOf(
            MemoryCapsuleNode(
                id = "node-1",
                scopeKey = scope,
                kind = "preference",
                title = "Tea",
                content = "Likes tea",
                validFrom = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    )
}

package com.companion.cc.data.local.repository

import androidx.room.Room
import com.companion.cc.data.local.dao.MemoryEvidenceDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.MemoryRelationDao
import com.companion.cc.data.local.dao.MemoryRetrievalDao
import com.companion.cc.data.local.dao.MemoryReviewDao
import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.dao.MemoryVersionDao
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MemoryRepositoryTest {
    @Test
    fun proposalStatusDefaultsToPending() {
        val review = MemoryReviewDraft(
            id = "review-1",
            scopeKey = "companion:a",
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            proposalHash = "hash-1"
        )
        assertEquals("pending", review.status)
    }

    @Test
    fun sourceIdsJsonParsesEvidenceSourceIds() {
        assertEquals(
            listOf("source-1", "source-2"),
            MemoryRepository.parseSourceIds("[\"source-1\",\"source-2\"]")
        )
    }

    @Test
    fun malformedSourceIdsJsonIsRejected() {
        val result = runCatching { MemoryRepository.parseSourceIds("not-json") }
        assertEquals(true, result.isFailure)
    }

    @Test
    fun duplicateProposalHashDoesNotInsertAnotherReview() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val existing = MemoryReviewEntity(
            id = "review-existing",
            scopeKey = "user:user-1:companion:character-1",
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            proposalHash = "hash-1",
            createdAt = 1L
        )
        whenever(reviewDao.findByProposalHash(existing.scopeKey, existing.proposalHash))
            .thenReturn(existing)
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = repository.createReview(
            MemoryReviewDraft(
                id = "review-new",
                scopeKey = existing.scopeKey,
                kind = existing.kind,
                title = existing.title,
                content = existing.content,
                confidence = existing.confidence,
                proposalHash = existing.proposalHash
            )
        )

        assertEquals("review-existing", result)
        verify(reviewDao, never()).insert(any())
    }

    @Test
    fun editNodeRejectsDifferentScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(
            nodeDao.findByIdForScope(
                "user:user-1:companion:character-1",
                "node-a"
            )
        ).thenReturn(null)

        val result = repository.editNode(
            scopeKey = "user:user-1:companion:character-1",
            nodeId = "node-a",
            title = "修改后",
            content = "不应写入",
            importance = 80
        )

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).update(any())
        verify(versionDao, never()).insert(any())
    }

    @Test
    fun restoreVersionRejectsNodeOutsideScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(
            nodeDao.findByIdForScope(
                "user:user-1:companion:character-1",
                "node-a"
            )
        ).thenReturn(null)

        val result = repository.restoreVersion(
            scopeKey = "user:user-1:companion:character-1",
            nodeId = "node-a",
            version = 1
        )

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).update(any())
        verify(versionDao, never()).insert(any())
    }

    @Test
    fun acceptReviewRejectsReviewOutsideScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(
            reviewDao.findByIdForScope(
                "user:user-1:companion:character-1",
                "review-from-user-2"
            )
        ).thenReturn(null)

        val result = repository.acceptReview(
            scopeKey = "user:user-1:companion:character-1",
            reviewId = "review-from-user-2",
            subjectRole = "user",
            subjectKey = "user"
        )

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).insert(any())
        verify(evidenceDao, never()).insertAll(any())
        verify(versionDao, never()).insert(any())
        verify(reviewDao, never()).update(any())
    }

    @Test
    fun acceptReviewRejectsEvidenceSourceOutsideScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val scope = "user:user-1:companion:character-1"
        val review = MemoryReviewEntity(
            id = "review-1",
            scopeKey = scope,
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            sourceIdsJson = "[\"source-1\"]",
            proposalHash = "hash-1",
            createdAt = 1L
        )
        whenever(reviewDao.findByIdForScope(scope, review.id)).thenReturn(review)
        whenever(sourceDao.findByIdForScope(scope, "source-1")).thenReturn(null)
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = repository.acceptReview(
            scopeKey = scope,
            reviewId = review.id,
            subjectRole = "user",
            subjectKey = "user"
        )

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).insert(any())
        verify(evidenceDao, never()).insertAll(any())
        verify(versionDao, never()).insert(any())
        verify(reviewDao, never()).update(any())
    }

    @Test
    fun acceptingReviewCreatesScopedEvidenceAndInitialVersion() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val scope = "user:user-1:companion:character-1"
        val source = MemorySourceEntity(
            id = "source-1",
            scopeKey = scope,
            messageId = "message-1",
            contentSnapshot = "用户说喜欢茶",
            sourceType = "message",
            occurredAt = 1L,
            contentHash = "source-hash-1",
            createdAt = 1L
        )
        val review = MemoryReviewEntity(
            id = "review-1",
            scopeKey = scope,
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            sourceIdsJson = "[\"${source.id}\"]",
            proposalHash = "proposal-hash-1",
            createdAt = 1L
        )
        database.memorySourceDao().insert(source)
        database.memoryReviewDao().insert(review)
        val repository = MemoryRepository(
            database,
            database.memorySourceDao(),
            database.memoryReviewDao(),
            database.memoryNodeDao(),
            database.memoryEvidenceDao(),
            database.memoryVersionDao(),
            database.memoryRelationDao(),
            database.memoryRetrievalDao()
        )

        val result = repository.acceptReview(
            scopeKey = scope,
            reviewId = review.id,
            subjectRole = "user",
            subjectKey = "user-1",
            now = 2L
        )

        val nodeId = result.getOrThrow()
        assertEquals(
            listOf(source.id),
            repository.getEvidence(scope, nodeId).map { it.sourceId }
        )
        assertEquals(
            "用户喜欢茶",
            repository.getEvidence(scope, nodeId).single().summarySnapshot
        )
        assertEquals(
            listOf(1),
            repository.getVersions(scope, nodeId).map { it.version }
        )
        assertEquals(
            "accepted",
            database.memoryReviewDao().findByIdForScope(scope, review.id)?.status
        )
        database.close()
    }

    @Test
    fun acceptingAnAlreadyAcceptedReviewIsIdempotent() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val scope = "user:user-1:companion:character-1"
        val source = MemorySourceEntity(
            id = "source-idempotent",
            scopeKey = scope,
            messageId = "message-idempotent",
            contentSnapshot = "用户说喜欢茶",
            sourceType = "message",
            occurredAt = 1L,
            contentHash = "source-hash-idempotent",
            createdAt = 1L
        )
        val review = MemoryReviewEntity(
            id = "review-idempotent",
            scopeKey = scope,
            kind = "preference",
            title = "喜欢茶",
            content = "用户喜欢茶",
            confidence = 0.8,
            sourceIdsJson = "[\"${source.id}\"]",
            proposalHash = "proposal-hash-idempotent",
            createdAt = 1L
        )
        database.memorySourceDao().insert(source)
        database.memoryReviewDao().insert(review)
        val repository = MemoryRepository(
            database,
            database.memorySourceDao(),
            database.memoryReviewDao(),
            database.memoryNodeDao(),
            database.memoryEvidenceDao(),
            database.memoryVersionDao(),
            database.memoryRelationDao(),
            database.memoryRetrievalDao()
        )

        val first = repository.acceptReview(scope, review.id, "user", "user-1", now = 2L)
        val second = repository.acceptReview(scope, review.id, "user", "user-1", now = 3L)

        assertEquals(first.getOrThrow(), second.getOrThrow())
        assertEquals(1, database.memoryNodeDao().findAllInScope(scope).size)
        assertEquals(1, database.memoryEvidenceDao().findAllInScope(scope).size)
        assertEquals(1, database.memoryVersionDao().findAllInScope(scope).size)
        database.close()
    }

    @Test
    fun acceptedNodesWithSameProposalHashRemainScoped() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val sourceDao = database.memorySourceDao()
        val reviewDao = database.memoryReviewDao()
        val nodeDao = database.memoryNodeDao()
        val evidenceDao = database.memoryEvidenceDao()
        val versionDao = database.memoryVersionDao()
        val relationDao = database.memoryRelationDao()
        val retrievalDao = database.memoryRetrievalDao()
        val scopeOne = "user:user-1:companion:character-1"
        val scopeTwo = "user:user-2:companion:character-1"
        val reviewOne = MemoryReviewEntity(
            id = "review-1",
            scopeKey = scopeOne,
            kind = "preference",
            title = "用户一的偏好",
            content = "用户一喜欢茶",
            confidence = 0.8,
            proposalHash = "same-hash",
            createdAt = 1L
        )
        val reviewTwo = reviewOne.copy(
            id = "review-2",
            scopeKey = scopeTwo,
            title = "用户二的偏好",
            content = "用户二喜欢咖啡"
        )
        reviewDao.insert(reviewOne)
        reviewDao.insert(reviewTwo)
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        repository.acceptReview(scopeOne, reviewOne.id, "user", "user-1")
        repository.acceptReview(scopeTwo, reviewTwo.id, "user", "user-2")

        val nodes = nodeDao.observeFiltered(scopeOne).first() + nodeDao.observeFiltered(scopeTwo).first()
        assertEquals(2, nodes.map { it.id }.distinct().size)
        assertEquals(scopeOne, nodes.first { it.scopeKey == scopeOne }.scopeKey)
        assertEquals(scopeTwo, nodes.first { it.scopeKey == scopeTwo }.scopeKey)
        database.close()
    }
    @Test
    fun resolveReviewRejectsReviewOutsideScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(
            reviewDao.findByIdForScope(
                "user:user-1:companion:character-1",
                "review-from-user-2"
            )
        ).thenReturn(null)

        val result = repository.resolveReview(
            scopeKey = "user:user-1:companion:character-1",
            reviewId = "review-from-user-2",
            status = "rejected"
        )

        assertEquals(true, result.isFailure)
        verify(reviewDao, never()).update(any())
    }

    @Test
    fun unscopedReviewResolutionIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(reviewDao.findById("review-1")).thenReturn(
            MemoryReviewEntity(
                id = "review-1",
                scopeKey = "user:user-2:companion:character-1",
                kind = "preference",
                title = "跨用户审核",
                content = "不应修改",
                confidence = 0.8,
                proposalHash = "hash-1",
                createdAt = 1L
            )
        )

        val result = repository.resolveReview("review-1", "rejected")

        assertEquals(true, result.isFailure)
        verify(reviewDao, never()).update(any())
    }

    @Test
    fun unscopedRelationResolutionIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        val relation = MemoryRelationEntity(
            fromNodeId = "node-a",
            toNodeId = "node-b",
            relationType = "supports",
            scopeKey = "user:user-2:companion:character-1",
            createdAt = 1L,
            updatedAt = 1L
        )

        val result = runCatching {
            repository.resolveRelation(relation, accepted = true)
        }

        assertEquals(true, result.isFailure)
        verify(relationDao, never()).update(any())
    }

    @Test
    fun unscopedNodeLookupIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = runCatching { repository.findNode("node-from-other-user") }

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).findById(any())
    }

    @Test
    fun unscopedEvidenceLookupIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = runCatching { repository.getEvidence("node-from-other-user") }

        assertEquals(true, result.isFailure)
        verify(evidenceDao, never()).findForNode(any())
    }

    @Test
    fun unscopedVersionLookupIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = runCatching { repository.getVersions("node-from-other-user") }

        assertEquals(true, result.isFailure)
        verify(versionDao, never()).findForNode(any())
    }

    @Test
    fun unscopedRelationLookupIsRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = runCatching { repository.getRelations("node-from-other-user") }

        assertEquals(true, result.isFailure)
        verify(relationDao, never()).findForNode(any())
    }

    @Test
    fun unscopedNodeMutationsAreRejected() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        whenever(nodeDao.findById("node-from-other-user")).thenReturn(
            MemoryNodeEntity(
                id = "node-from-other-user",
                scopeKey = "user:user-2:companion:character-1",
                kind = "preference",
                subjectRole = "user",
                subjectKey = "user",
                title = "原标题",
                content = "原内容",
                validFrom = 1L,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val edit = repository.editNode("node-from-other-user", "新标题", "新内容", 50)
        val deleted = repository.softDeleteNode("node-from-other-user")
        val restored = repository.restoreNode("node-from-other-user")

        assertEquals(true, edit.isFailure)
        assertEquals(true, deleted.isFailure)
        assertEquals(true, restored.isFailure)
        verify(nodeDao, never()).update(any())
        verify(versionDao, never()).insert(any())
    }

    @Test
    fun suppressNodeRejectsNodeOutsideScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        val scope = "user:user-1:companion:character-1"
        whenever(nodeDao.findByIdForScope(scope, "node-a")).thenReturn(null)

        val result = repository.suppressNode(scope, "node-a", now = 2L)

        assertEquals(true, result.isFailure)
        verify(nodeDao, never()).update(any())
        verify(versionDao, never()).insert(any())
    }

    @Test
    fun suppressNodeMarksItDoNotRecallAndAppendsVersion() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val scope = "user:user-1:companion:character-1"
        val node = MemoryNodeEntity(
            id = "node-a",
            scopeKey = scope,
            kind = "preference",
            subjectRole = "user",
            subjectKey = "user",
            title = "喜欢茶",
            content = "用户喜欢茶",
            validFrom = 1L,
            createdAt = 1L,
            updatedAt = 1L
        )
        database.memoryNodeDao().insert(node)
        val repository = MemoryRepository(
            database,
            database.memorySourceDao(),
            database.memoryReviewDao(),
            database.memoryNodeDao(),
            database.memoryEvidenceDao(),
            database.memoryVersionDao(),
            database.memoryRelationDao(),
            database.memoryRetrievalDao()
        )

        val result = repository.suppressNode(scope, node.id, now = 2L)

        assertEquals(true, result.isSuccess)
        assertEquals(
            "do_not_recall",
            database.memoryNodeDao().findByIdForScope(scope, node.id)?.status
        )
        assertEquals(
            "marked do not recall",
            database.memoryVersionDao().findForNodeInScope(scope, node.id).first().changeReason
        )
        database.close()
    }

    @Test
    fun scopedRelationLookupIncludesProposedRelationsForAudit() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val scope = "user:user-1:companion:character-1"
        val relation = MemoryRelationEntity(
            fromNodeId = "node-a",
            toNodeId = "node-b",
            relationType = "supports",
            scopeKey = scope,
            status = "proposed",
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(relationDao.findForNodeInScopeIncludingArchived(scope, "node-a"))
            .thenReturn(listOf(relation))
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = repository.getRelations(scope, "node-a")

        assertEquals(listOf(relation), result)
        verify(relationDao).findForNodeInScopeIncludingArchived(scope, "node-a")
    }

    @Test
    fun relationResolutionUpdatesOnlyAcceptedRelationInScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        val relation = MemoryRelationEntity(
            fromNodeId = "node-a",
            toNodeId = "node-b",
            relationType = "supports",
            scopeKey = "user:user-1:companion:character-1",
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(
            relationDao.findForKeyInScope(
                relation.scopeKey,
                relation.fromNodeId,
                relation.toNodeId,
                relation.relationType
            )
        ).thenReturn(relation)

        val result = repository.resolveRelation(
            scopeKey = relation.scopeKey,
            relation = relation,
            accepted = true,
            now = 2L
        )

        assertEquals(true, result.isSuccess)
        verify(relationDao).update(
            relation.copy(status = "active", updatedAt = 2L)
        )
    }

    @Test
    fun relationResolutionRejectsMissingStoredRelation() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        val relation = MemoryRelationEntity(
            fromNodeId = "node-a",
            toNodeId = "node-b",
            relationType = "supports",
            scopeKey = "user:user-1:companion:character-1",
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(
            relationDao.findForKeyInScope(
                relation.scopeKey,
                relation.fromNodeId,
                relation.toNodeId,
                relation.relationType
            )
        ).thenReturn(null)

        val result = repository.resolveRelation(
            scopeKey = relation.scopeKey,
            relation = relation,
            accepted = true
        )

        assertEquals(true, result.isFailure)
        verify(relationDao, never()).update(any())
    }

    @Test
    fun retrievalTraceReadRejectsTraceOutsideRequestedScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(retrievalDao.findTraceById("trace-1")).thenReturn(
            com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = "user:user-2:companion:character-1",
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"title=2.0\"]",
                createdAt = 1L,
                durationMs = 2L
            )
        )

        assertEquals(
            null,
            repository.getRetrievalTrace("user:user-1:companion:character-1", "trace-1")
        )
    }

    @Test
    fun feedbackRejectsUnsupportedValueWithoutWriting() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )

        val result = repository.recordFeedback(
            scopeKey = "user:user-1:companion:character-1",
            feedback = com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                traceId = "trace-1",
                nodeId = "node-1",
                feedback = "unknown",
                createdAt = 1L
            )
        )

        assertEquals(true, result.isFailure)
        verify(retrievalDao, never()).insertFeedback(any())
    }

    @Test
    fun feedbackRejectsNodeNotSelectedByTrace() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(retrievalDao.findTraceById("trace-1")).thenReturn(
            com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = "user:user-1:companion:character-1",
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"title=2.0\"]",
                createdAt = 1L,
                durationMs = 2L
            )
        )

        val result = repository.recordFeedback(
            scopeKey = "user:user-1:companion:character-1",
            feedback = com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                traceId = "trace-1",
                nodeId = "node-2",
                feedback = "negative",
                createdAt = 2L
            )
        )

        assertEquals(true, result.isFailure)
        verify(retrievalDao, never()).insertFeedback(any())
    }

    @Test
    fun feedbackRejectsTraceOutsideRequestedScope() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(retrievalDao.findTraceById("trace-1")).thenReturn(
            com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = "user:user-2:companion:character-1",
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"title=2.0\"]",
                createdAt = 1L,
                durationMs = 2L
            )
        )

        val result = repository.recordFeedback(
            scopeKey = "user:user-1:companion:character-1",
            feedback = com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                traceId = "trace-1",
                nodeId = "node-1",
                feedback = "negative",
                createdAt = 2L
            )
        )

        assertEquals(true, result.isFailure)
        verify(retrievalDao, never()).insertFeedback(any())
    }

    @Test
    fun feedbackAcceptsPositiveOrNegativeForSelectedNode() = runTest {
        val database = mock<AppDatabase>()
        val sourceDao = mock<MemorySourceDao>()
        val reviewDao = mock<MemoryReviewDao>()
        val nodeDao = mock<MemoryNodeDao>()
        val evidenceDao = mock<MemoryEvidenceDao>()
        val versionDao = mock<MemoryVersionDao>()
        val relationDao = mock<MemoryRelationDao>()
        val retrievalDao = mock<MemoryRetrievalDao>()
        val repository = MemoryRepository(
            database, sourceDao, reviewDao, nodeDao, evidenceDao,
            versionDao, relationDao, retrievalDao
        )
        whenever(retrievalDao.findTraceById("trace-1")).thenReturn(
            com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = "user:user-1:companion:character-1",
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"title=2.0\"]",
                createdAt = 1L,
                durationMs = 2L
            )
        )
        val feedback = com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
            traceId = "trace-1",
            nodeId = "node-1",
            feedback = "positive",
            createdAt = 2L
        )

        val result = repository.recordFeedback(
            scopeKey = "user:user-1:companion:character-1",
            feedback = feedback
        )

        assertEquals(true, result.isSuccess)
        verify(retrievalDao).insertFeedback(feedback)
    }

    @Test
    fun creatingSourceAndReviewAtomicallyRollsBackSourceWhenReviewInsertFails() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val scope = "user:user-1:companion:character-1"
        val existingReview = MemoryReviewEntity(
            id = "review-collision",
            scopeKey = scope,
            kind = "preference",
            title = "已有",
            content = "已有内容",
            confidence = 0.8,
            proposalHash = "existing-hash",
            createdAt = 1L
        )
        database.memoryReviewDao().insert(existingReview)
        val source = MemorySourceEntity(
            id = "source-rollback",
            scopeKey = scope,
            messageId = "message-rollback",
            contentSnapshot = "新来源",
            sourceType = "message",
            occurredAt = 2L,
            contentHash = "source-hash-rollback",
            createdAt = 2L
        )
        val repository = MemoryRepository(
            database,
            database.memorySourceDao(),
            database.memoryReviewDao(),
            database.memoryNodeDao(),
            database.memoryEvidenceDao(),
            database.memoryVersionDao(),
            database.memoryRelationDao(),
            database.memoryRetrievalDao()
        )

        val result = runCatching {
            repository.createSourceAndReviewAtomically(
                source,
                MemoryReviewDraft(
                    id = existingReview.id,
                    scopeKey = scope,
                    kind = "preference",
                    title = "新提议",
                    content = "新内容",
                    confidence = 0.9,
                    proposalHash = "new-hash",
                    sourceIdsJson = "[\"${source.id}\"]"
                )
            )
        }

        assertEquals(true, result.isFailure)
        assertEquals(null, database.memorySourceDao().findByIdForScope(scope, source.id))
        database.close()
    }
}

package com.companion.cc.data.local.repository

import androidx.room.withTransaction
import com.companion.cc.data.local.dao.MemoryEvidenceDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.MemoryReviewDao
import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.dao.MemoryVersionDao
import com.companion.cc.data.local.dao.MemoryRelationDao
import com.companion.cc.data.local.dao.MemoryRetrievalDao
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import kotlinx.coroutines.flow.Flow

data class MemoryReviewDraft(
    val id: String,
    val scopeKey: String,
    val kind: String,
    val title: String,
    val content: String,
    val confidence: Double,
    val proposalHash: String,
    val status: String = "pending",
    val sourceIdsJson: String = "[]"
)

class MemoryRepository(
    private val database: AppDatabase,
    private val sourceDao: MemorySourceDao,
    private val reviewDao: MemoryReviewDao,
    private val nodeDao: MemoryNodeDao,
    private val evidenceDao: MemoryEvidenceDao,
    private val versionDao: MemoryVersionDao,
    private val relationDao: MemoryRelationDao,
    private val retrievalDao: MemoryRetrievalDao
) {
    fun observePendingReviews(scopeKey: String): Flow<List<MemoryReviewEntity>> = reviewDao.observePending(scopeKey)
    fun observeNodes(scopeKey: String, kind: String = "", query: String = ""): Flow<List<MemoryNodeEntity>> = nodeDao.observeFiltered(scopeKey, kind = kind, query = query)

    suspend fun findNode(nodeId: String): MemoryNodeEntity? = nodeDao.findById(nodeId)

    suspend fun getEvidence(nodeId: String): List<MemoryEvidenceEntity> = evidenceDao.findForNode(nodeId)
    suspend fun getVersions(nodeId: String): List<MemoryVersionEntity> = versionDao.findForNode(nodeId)
    suspend fun getRelations(nodeId: String): List<MemoryRelationEntity> = relationDao.findForNode(nodeId)

    suspend fun createReview(draft: MemoryReviewDraft, createdAt: Long = System.currentTimeMillis()) {
        reviewDao.insert(
            MemoryReviewEntity(
                id = draft.id,
                scopeKey = draft.scopeKey,
                kind = draft.kind,
                title = draft.title,
                content = draft.content,
                confidence = draft.confidence,
                status = draft.status,
                sourceIdsJson = draft.sourceIdsJson,
                proposalHash = draft.proposalHash,
                createdAt = createdAt
            )
        )
    }

    suspend fun acceptReview(reviewId: String, subjectRole: String, subjectKey: String, now: Long = System.currentTimeMillis()): Result<String> = runCatching {
        database.withTransaction {
            val review = requireNotNull(reviewDao.findById(reviewId)) { "Memory review not found: $reviewId" }
            require(review.status == "pending" || review.status == "conflict") { "Memory review is already resolved" }
            val nodeId = "node:${review.proposalHash}"
            val node = MemoryNodeEntity(
                id = nodeId, scopeKey = review.scopeKey, kind = review.kind,
                subjectRole = subjectRole, subjectKey = subjectKey, title = review.title,
                content = review.content, confidence = review.confidence,
                validFrom = now, createdAt = now, updatedAt = now
            )
            nodeDao.insert(node)
            versionDao.insert(MemoryVersionEntity(
                nodeId = nodeId, version = 1, kind = node.kind, title = node.title,
                content = node.content, importance = node.importance, confidence = node.confidence,
                validFrom = node.validFrom, validUntil = node.validUntil,
                changeReason = "review accepted", actor = "user", createdAt = now
            ))
            reviewDao.update(review.copy(status = "accepted", resolvedAt = now, resolutionNote = "accepted"))
            nodeId
        }
    }

    suspend fun resolveReview(reviewId: String, status: String, note: String = "", now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        require(status in setOf("rejected", "deferred", "conflict"))
        val review = requireNotNull(reviewDao.findById(reviewId)) { "Memory review not found: $reviewId" }
        require(review.status == "pending" || review.status == "conflict") { "Memory review is already resolved" }
        reviewDao.update(review.copy(status = status, resolvedAt = if (status == "deferred") null else now, resolutionNote = note))
    }

    suspend fun addSource(source: MemorySourceEntity) = sourceDao.insert(source)

    suspend fun addEvidence(evidence: List<MemoryEvidenceEntity>) = evidenceDao.insertAll(evidence)

    suspend fun editNode(nodeId: String, title: String, content: String, importance: Int, now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        database.withTransaction {
            val old = requireNotNull(nodeDao.findById(nodeId)) { "Memory node not found: $nodeId" }
            val updated = old.copy(title = title, content = content, importance = importance.coerceIn(0, 100), updatedAt = now, currentVersion = old.currentVersion + 1)
            nodeDao.update(updated)
            versionDao.insert(MemoryVersionEntity(nodeId, updated.currentVersion, updated.kind, updated.title, updated.content, updated.importance, updated.confidence, updated.validFrom, updated.validUntil, "user edited", "user", now))
        }
    }

    suspend fun softDeleteNode(nodeId: String, now: Long = System.currentTimeMillis()): Result<Unit> = setNodeStatus(nodeId, "deleted", "soft deleted", now)
    suspend fun restoreNode(nodeId: String, now: Long = System.currentTimeMillis()): Result<Unit> = setNodeStatus(nodeId, "active", "restored", now)

    suspend fun proposeRelation(relation: MemoryRelationEntity) = relationDao.insert(relation.copy(status = "proposed"))

    suspend fun resolveRelation(relation: MemoryRelationEntity, accepted: Boolean, now: Long = System.currentTimeMillis()) = relationDao.update(
        relation.copy(status = if (accepted) "active" else "rejected", updatedAt = now)
    )

    suspend fun recordRetrieval(trace: MemoryRetrievalTraceEntity) = retrievalDao.insertTrace(trace)
    suspend fun recordFeedback(feedback: MemoryRetrievalFeedbackEntity) = retrievalDao.insertFeedback(feedback)

    private suspend fun setNodeStatus(nodeId: String, status: String, reason: String, now: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val old = requireNotNull(nodeDao.findById(nodeId)) { "Memory node not found: $nodeId" }
            val updated = old.copy(status = status, updatedAt = now, currentVersion = old.currentVersion + 1)
            nodeDao.update(updated)
            versionDao.insert(MemoryVersionEntity(nodeId, updated.currentVersion, updated.kind, updated.title, updated.content, updated.importance, updated.confidence, updated.validFrom, updated.validUntil, reason, "user", now))
        }
    }

    companion object {
        fun isConflictStatus(status: String): Boolean = status == "conflict"
    }
}

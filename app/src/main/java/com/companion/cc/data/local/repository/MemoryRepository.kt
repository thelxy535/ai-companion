package com.companion.cc.data.local.repository

import androidx.room.withTransaction
import com.companion.cc.data.local.dao.MemoryEvidenceDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.MemoryReviewDao
import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.dao.MemoryVersionDao
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
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
    private val versionDao: MemoryVersionDao
) {
    fun observePendingReviews(scopeKey: String): Flow<List<MemoryReviewEntity>> = reviewDao.observePending(scopeKey)
    fun observeActiveNodes(scopeKey: String): Flow<List<MemoryNodeEntity>> = nodeDao.observeActive(scopeKey)

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
}

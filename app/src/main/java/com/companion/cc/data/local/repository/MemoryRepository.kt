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
import com.google.gson.Gson
import java.security.MessageDigest

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

    fun observeDeferredReviews(scopeKey: String): Flow<List<MemoryReviewEntity>> = reviewDao.observeDeferred(scopeKey)

    /** 手动恢复：仅允许 deferred -> pending，用于用户在审核页找回延后的候选。 */
    suspend fun restoreReview(
        scopeKey: String,
        reviewId: String
    ): Result<Unit> = runCatching {
        val review = requireNotNull(
            reviewDao.findByIdForScope(scopeKey, reviewId)
        ) { "Memory review not found in scope: $reviewId" }
        require(review.status == "deferred") { "Only deferred reviews can be restored" }
        reviewDao.update(
            review.copy(status = "pending", resolvedAt = null, resolutionNote = "restored by user")
        )
    }

    /** 到期自动恢复：延后超过 cooldown 的候选在打开审核页时回归待审核队列。 */
    suspend fun restoreDueDeferredReviews(
        scopeKey: String,
        cooldownMs: Long = 24L * 3_600_000L,
        now: Long = System.currentTimeMillis()
    ): Int = reviewDao.restoreDueDeferred(scopeKey, now - cooldownMs)

    /**
     * 叙事定期自演化（V9PM 深度②）：直接写入新的关系叙事并取代旧叙事。
     * 与审核接受路径同级的审计保障：版本记录 + 证据绑定，旧叙事可追溯。
     */
    suspend fun evolveNarrative(
        scopeKey: String,
        content: String,
        confidence: Double,
        sourceIds: List<String>,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        database.withTransaction {
            require(content.isNotBlank()) { "narrative content must not be blank" }
            // 取代旧的 active 关系叙事
            nodeDao.findActiveByKindAndSubject(
                scopeKey,
                com.companion.cc.domain.memory.NarrativeKinds.RELATIONSHIP,
                "relationship",
                "pair"
            ).forEach { previous ->
                nodeDao.update(
                    previous.copy(status = "superseded", validUntil = now, updatedAt = now)
                )
            }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(("$scopeKey|narrative|$now|$content").toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
            val nodeId = "node:$digest"
            val node = MemoryNodeEntity(
                id = nodeId,
                scopeKey = scopeKey,
                kind = com.companion.cc.domain.memory.NarrativeKinds.RELATIONSHIP,
                subjectRole = "relationship",
                subjectKey = "pair",
                title = "关系理解",
                content = content,
                confidence = confidence,
                validFrom = now,
                createdAt = now,
                updatedAt = now
            )
            nodeDao.insert(node)
            sourceIds.take(5).forEach { sourceId ->
                sourceDao.findByIdForScope(scopeKey, sourceId)?.let {
                    evidenceDao.insertAll(listOf(
                        com.companion.cc.data.local.entity.MemoryEvidenceEntity(
                            nodeId = nodeId,
                            sourceId = sourceId,
                            summarySnapshot = content,
                            createdAt = now
                        )
                    ))
                }
            }
            versionDao.insert(
                com.companion.cc.data.local.entity.MemoryVersionEntity(
                    nodeId = nodeId,
                    version = 1,
                    kind = node.kind,
                    title = node.title,
                    content = node.content,
                    importance = node.importance,
                    confidence = node.confidence,
                    validFrom = node.validFrom,
                    validUntil = node.validUntil,
                    changeReason = "periodic auto evolution",
                    actor = "system-evolution",
                    createdAt = now
                )
            )
        }
    }

    /** 读取当前生效的指定类型节点（供 Prompt 注入等一次性读取）。 */
    suspend fun getActiveNodesByKind(
        scopeKey: String,
        kind: String,
        limit: Int = 3,
        now: Long = System.currentTimeMillis()
    ): List<MemoryNodeEntity> = nodeDao.findActiveByKind(scopeKey, kind, now, limit)
    fun observeNodes(
        scopeKey: String,
        kind: String = "",
        query: String = "",
        status: String = "active",
        limit: Int = 100
    ): Flow<List<MemoryNodeEntity>> =
        nodeDao.observeFiltered(
            scopeKey = scopeKey,
            status = status,
            kind = kind,
            query = query,
            limit = limit
        )

    fun observeRecallCandidates(
        scopeKey: String,
        now: Long,
        query: String = "",
        limit: Int = 100
    ): Flow<List<MemoryNodeEntity>> =
        nodeDao.observeRecallCandidates(scopeKey, now = now, query = query, limit = limit)

    suspend fun findNode(scopeKey: String, nodeId: String): MemoryNodeEntity? =
        nodeDao.findByIdForScope(scopeKey, nodeId)

    suspend fun getRetrievalTrace(
        scopeKey: String,
        traceId: String
    ): MemoryRetrievalTraceEntity? = retrievalDao.findTraceById(traceId)
        ?.takeIf { it.scopeKey == scopeKey }

    @Deprecated("Memory reads require an explicit user and character scope")
    suspend fun findNode(nodeId: String): MemoryNodeEntity? {
        error("Memory node scope is required for node=$nodeId")
    }

    suspend fun getEvidence(scopeKey: String, nodeId: String): List<MemoryEvidenceEntity> =
        evidenceDao.findForNodeInScope(scopeKey, nodeId)

    @Deprecated("Memory reads require an explicit user and character scope")
    suspend fun getEvidence(nodeId: String): List<MemoryEvidenceEntity> {
        error("Memory evidence scope is required for node=$nodeId")
    }

    suspend fun getVersions(scopeKey: String, nodeId: String): List<MemoryVersionEntity> =
        versionDao.findForNodeInScope(scopeKey, nodeId)

    @Deprecated("Memory reads require an explicit user and character scope")
    suspend fun getVersions(nodeId: String): List<MemoryVersionEntity> {
        error("Memory version scope is required for node=$nodeId")
    }

    suspend fun getRelations(scopeKey: String, nodeId: String): List<MemoryRelationEntity> =
        relationDao.findForNodeInScopeIncludingArchived(scopeKey, nodeId)

    @Deprecated("Memory reads require an explicit user and character scope")
    suspend fun getRelations(nodeId: String): List<MemoryRelationEntity> {
        error("Memory relation scope is required for node=$nodeId")
    }

    suspend fun createReview(draft: MemoryReviewDraft, createdAt: Long = System.currentTimeMillis()): String {
        val existing = reviewDao.findByProposalHash(draft.scopeKey, draft.proposalHash)
        if (existing != null) return existing.id
        val existingId = reviewDao.findByIdForScope(draft.scopeKey, draft.id)
        require(existingId == null) {
            "Memory review ID already exists in scope: ${draft.id}"
        }
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
        return draft.id
    }

    suspend fun createSourceAndReviewAtomically(
        source: MemorySourceEntity,
        review: MemoryReviewDraft,
        createdAt: Long = System.currentTimeMillis()
    ): String = database.withTransaction {
        require(source.scopeKey == review.scopeKey) {
            "Memory source and review must belong to the same scope"
        }
        sourceDao.insert(source)
        createReview(review, createdAt)
    }

    suspend fun acceptReview(
        scopeKey: String,
        reviewId: String,
        subjectRole: String,
        subjectKey: String,
        now: Long = System.currentTimeMillis()
    ): Result<String> = runCatching {
        database.withTransaction {
            val review = requireNotNull(
                reviewDao.findByIdForScope(scopeKey, reviewId)
            ) { "Memory review not found in scope: $reviewId" }
            val nodeId = nodeIdFor(scopeKey, review.proposalHash)
            if (review.status == "accepted") {
                requireNotNull(nodeDao.findByIdForScope(scopeKey, nodeId)) {
                    "Accepted memory node is missing: $nodeId"
                }
                return@withTransaction nodeId
            }
            require(review.status == "pending" || review.status == "conflict") {
                "Memory review is already resolved"
            }
            val sourceIds = parseSourceIds(review.sourceIdsJson)
            sourceIds.forEach { sourceId ->
                requireNotNull(sourceDao.findByIdForScope(scopeKey, sourceId)) {
                    "Memory source not found in scope: $sourceId"
                }
            }
            // 自我叙事：同一主题的新叙事接受时取代旧叙事（旧节点关闭有效期，保留版本历史）
            if (review.kind == com.companion.cc.domain.memory.NarrativeKinds.RELATIONSHIP) {
                nodeDao.findActiveByKindAndSubject(scopeKey, review.kind, subjectRole, subjectKey)
                    .forEach { previous ->
                        nodeDao.update(
                            previous.copy(status = "superseded", validUntil = now, updatedAt = now)
                        )
                    }
            }
            val node = MemoryNodeEntity(
                id = nodeId, scopeKey = scopeKey, kind = review.kind,
                subjectRole = subjectRole, subjectKey = subjectKey, title = review.title,
                content = review.content, confidence = review.confidence,
                validFrom = now, createdAt = now, updatedAt = now
            )
            nodeDao.insert(node)
            evidenceDao.insertAll(sourceIds.map { sourceId ->
                MemoryEvidenceEntity(
                    nodeId = nodeId,
                    sourceId = sourceId,
                    summarySnapshot = review.content,
                    createdAt = now
                )
            })
            versionDao.insert(
                MemoryVersionEntity(
                    nodeId = nodeId, version = 1, kind = node.kind, title = node.title,
                    content = node.content, importance = node.importance, confidence = node.confidence,
                    validFrom = node.validFrom, validUntil = node.validUntil,
                    changeReason = "review accepted", actor = "user", createdAt = now
                )
            )
            reviewDao.update(review.copy(status = "accepted", resolvedAt = now, resolutionNote = "accepted"))
            nodeId
        }
    }

    @Deprecated("Review mutations require an explicit user and character scope")
    suspend fun acceptReview(
        reviewId: String,
        subjectRole: String,
        subjectKey: String,
        now: Long = System.currentTimeMillis()
    ): Result<String> = runCatching {
        error(
            "Memory review scope is required for review=$reviewId, " +
                "subjectRole=$subjectRole, subjectKey=$subjectKey, now=$now"
        )
    }

    suspend fun resolveReview(
        scopeKey: String,
        reviewId: String,
        status: String,
        note: String = "",
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        require(status in setOf("rejected", "deferred", "conflict"))
        val review = requireNotNull(
            reviewDao.findByIdForScope(scopeKey, reviewId)
        ) { "Memory review not found in scope: $reviewId" }
        require(review.status == "pending" || review.status == "conflict") {
            "Memory review is already resolved"
        }
        reviewDao.update(
            review.copy(
                status = status,
                resolvedAt = if (status == "deferred") null else now,
                resolutionNote = note
            )
        )
    }

    @Deprecated("Review mutations require an explicit user and character scope")
    suspend fun resolveReview(
        reviewId: String,
        status: String,
        note: String = "",
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        error(
            "Memory review scope is required for review=$reviewId, " +
                "status=$status, note=$note, now=$now"
        )
    }

    suspend fun addSource(source: MemorySourceEntity) = sourceDao.insert(source)

    suspend fun addEvidence(evidence: List<MemoryEvidenceEntity>) = evidenceDao.insertAll(evidence)

    suspend fun editNode(
        scopeKey: String,
        nodeId: String,
        title: String,
        content: String,
        importance: Int,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val old = requireNotNull(nodeDao.findByIdForScope(scopeKey, nodeId)) {
            "Memory node not found in scope: $nodeId"
        }
        database.withTransaction {
            val updated = old.copy(
                title = title,
                content = content,
                importance = importance.coerceIn(0, 100),
                updatedAt = now,
                currentVersion = old.currentVersion + 1
            )
            nodeDao.update(updated)
            versionDao.insert(
                MemoryVersionEntity(
                    nodeId,
                    updated.currentVersion,
                    updated.kind,
                    updated.title,
                    updated.content,
                    updated.importance,
                    updated.confidence,
                    updated.validFrom,
                    updated.validUntil,
                    "user edited",
                    "user",
                    now
                )
            )
        }
    }

    @Deprecated("Node mutations require an explicit user and character scope")
    suspend fun editNode(
        nodeId: String,
        title: String,
        content: String,
        importance: Int,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        error(
            "Memory node scope is required for node=$nodeId, " +
                "title=$title, contentLength=${content.length}, importance=$importance, now=$now"
        )
    }

    suspend fun softDeleteNode(
        scopeKey: String,
        nodeId: String,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = setNodeStatus(scopeKey, nodeId, "deleted", "soft deleted", now)

    suspend fun suppressNode(
        scopeKey: String,
        nodeId: String,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = setNodeStatus(scopeKey, nodeId, "do_not_recall", "marked do not recall", now)

    suspend fun restoreNode(
        scopeKey: String,
        nodeId: String,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = setNodeStatus(scopeKey, nodeId, "active", "restored", now)

    @Deprecated("Node mutations require an explicit user and character scope")
    suspend fun softDeleteNode(nodeId: String, now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        error("Memory node scope is required for node=$nodeId, status=deleted, now=$now")
    }

    @Deprecated("Node mutations require an explicit user and character scope")
    suspend fun restoreNode(nodeId: String, now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        error("Memory node scope is required for node=$nodeId, status=active, now=$now")
    }

    suspend fun restoreVersion(
        scopeKey: String,
        nodeId: String,
        version: Int,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val old = requireNotNull(nodeDao.findByIdForScope(scopeKey, nodeId)) {
            "Memory node not found in scope: $nodeId"
        }
        val target = versionDao.findForNodeInScope(scopeKey, nodeId)
            .firstOrNull { it.version == version }
            ?: error("Memory version not found in scope: $nodeId@$version")

        database.withTransaction {
            val updated = old.copy(
                kind = target.kind,
                title = target.title,
                content = target.content,
                importance = target.importance,
                confidence = target.confidence,
                validFrom = target.validFrom,
                validUntil = target.validUntil,
                updatedAt = now,
                currentVersion = old.currentVersion + 1
            )
            nodeDao.update(updated)
            versionDao.insert(
                target.copy(
                    version = updated.currentVersion,
                    changeReason = "restored version $version",
                    actor = "user",
                    createdAt = now
                )
            )
        }
    }


    suspend fun resolveRelation(
        scopeKey: String,
        relation: MemoryRelationEntity,
        accepted: Boolean,
        now: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        require(relation.scopeKey == scopeKey) {
            "Memory relation does not belong to scope: ${relation.scopeKey}"
        }
        val stored = requireNotNull(
            relationDao.findForKeyInScope(
                scopeKey = scopeKey,
                fromNodeId = relation.fromNodeId,
                toNodeId = relation.toNodeId,
                relationType = relation.relationType
            )
        ) {
            "Memory relation not found in scope"
        }
        require(stored.status == "proposed" || stored.status == "conflict") {
            "Memory relation is already resolved"
        }
        relationDao.update(
            stored.copy(
                status = if (accepted) "active" else "rejected",
                updatedAt = now
            )
        )
    }

    @Deprecated("Relation mutations require an explicit user and character scope")
    suspend fun resolveRelation(
        relation: MemoryRelationEntity,
        accepted: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        error(
            "Memory relation scope is required for ${relation.fromNodeId}->" +
                "${relation.toNodeId}/${relation.relationType}, accepted=$accepted, now=$now"
        )
    }

    suspend fun getRetrievalFeedback(
        scopeKey: String,
        nodeIds: List<String>
    ): List<MemoryRetrievalFeedbackEntity> =
        if (nodeIds.isEmpty()) emptyList()
        else retrievalDao.findFeedbackForNodes(scopeKey, nodeIds)

    suspend fun recordRetrieval(trace: MemoryRetrievalTraceEntity) = retrievalDao.insertTrace(trace)

    suspend fun recordFeedback(
        scopeKey: String,
        feedback: MemoryRetrievalFeedbackEntity
    ): Result<Unit> = runCatching {
        val normalizedFeedback = feedback.feedback.trim().lowercase()
        require(normalizedFeedback == "positive" || normalizedFeedback == "negative") {
            "Retrieval feedback must be positive or negative"
        }
        val trace = requireNotNull(retrievalDao.findTraceById(feedback.traceId)) {
            "Retrieval trace not found: ${feedback.traceId}"
        }
        require(trace.scopeKey == scopeKey) {
            "Retrieval trace does not belong to scope: ${trace.scopeKey}"
        }
        val selectedNodeIds = Gson().fromJson(
            trace.selectedNodeIdsJson,
            Array<String>::class.java
        )?.toSet().orEmpty()
        require(feedback.nodeId in selectedNodeIds) {
            "Retrieval feedback node was not selected by trace: ${feedback.nodeId}"
        }
        retrievalDao.insertFeedback(feedback.copy(feedback = normalizedFeedback))
    }

    private suspend fun setNodeStatus(
        scopeKey: String,
        nodeId: String,
        status: String,
        reason: String,
        now: Long
    ): Result<Unit> = runCatching {
        val old = requireNotNull(nodeDao.findByIdForScope(scopeKey, nodeId)) {
            "Memory node not found in scope: $nodeId"
        }
        database.withTransaction {
            val updated = old.copy(
                status = status,
                updatedAt = now,
                currentVersion = old.currentVersion + 1
            )
            nodeDao.update(updated)
            versionDao.insert(
                MemoryVersionEntity(
                    nodeId,
                    updated.currentVersion,
                    updated.kind,
                    updated.title,
                    updated.content,
                    updated.importance,
                    updated.confidence,
                    updated.validFrom,
                    updated.validUntil,
                    reason,
                    "user",
                    now
                )
            )
        }
    }

    companion object {
        fun nodeIdFor(scopeKey: String, proposalHash: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$scopeKey $proposalHash".toByteArray(Charsets.UTF_8))
            return "node:" + digest.joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
        }

        fun parseSourceIds(sourceIdsJson: String): List<String> {
            val sourceIds = Gson().fromJson(sourceIdsJson, Array<String>::class.java)
                ?: throw IllegalArgumentException("Memory source IDs must be a JSON array")
            return sourceIds.map { sourceId ->
                sourceId.trim().also {
                    require(it.isNotBlank()) { "Memory source ID must not be blank" }
                }
            }.distinct()
        }

        fun isConflictStatus(status: String): Boolean = status == "conflict"
    }
}

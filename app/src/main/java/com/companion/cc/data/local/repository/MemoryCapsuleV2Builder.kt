package com.companion.cc.data.local.repository

import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.domain.memory.MemoryCapsuleEvidence
import com.companion.cc.domain.memory.MemoryCapsuleNode
import com.companion.cc.domain.memory.MemoryCapsuleRelation
import com.companion.cc.domain.memory.MemoryCapsuleRetrievalFeedback
import com.companion.cc.domain.memory.MemoryCapsuleRetrievalTrace
import com.companion.cc.domain.memory.MemoryCapsuleReview
import com.companion.cc.domain.memory.MemoryCapsuleSource
import com.companion.cc.domain.memory.MemoryCapsuleVersion
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.memory.MemoryCapsuleV2Validator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MemoryCapsuleV2Builder(
    private val database: AppDatabase,
    private val gson: Gson = Gson()
) {
    suspend fun build(scopeKey: String): Result<MemoryCapsuleV2> = runCatching {
        val capsule = MemoryCapsuleV2(
            scopeKey = scopeKey,
            sources = database.memorySourceDao().findAllInScope(scopeKey).map { it.toCapsule() },
            nodes = database.memoryNodeDao().findAllInScope(scopeKey).map { it.toCapsule() },
            evidence = database.memoryEvidenceDao().findAllInScope(scopeKey).map { it.toCapsule() },
            reviews = database.memoryReviewDao().findAllInScope(scopeKey).map { it.toCapsule(gson) },
            versions = database.memoryVersionDao().findAllInScope(scopeKey).map { it.toCapsule() },
            relations = database.memoryRelationDao().findAllInScope(scopeKey).map { it.toCapsule() },
            retrievalTraces = database.memoryRetrievalDao().findAllTracesInScope(scopeKey)
                .map { it.toCapsule() },
            retrievalFeedback = database.memoryRetrievalDao().findAllFeedbackInScope(scopeKey)
                .map { it.toCapsule() }
        )
        MemoryCapsuleV2Validator.validate(capsule).getOrThrow()
        capsule
    }
}

private fun MemorySourceEntity.toCapsule() = MemoryCapsuleSource(
    id = id,
    scopeKey = scopeKey,
    messageId = messageId,
    contentSnapshot = contentSnapshot,
    sourceType = sourceType,
    occurredAt = occurredAt,
    contentHash = contentHash,
    createdAt = createdAt
)

private fun MemoryNodeEntity.toCapsule() = MemoryCapsuleNode(
    id = id,
    scopeKey = scopeKey,
    kind = kind,
    subjectRole = subjectRole,
    subjectKey = subjectKey,
    title = title,
    content = content,
    importance = importance,
    confidence = confidence,
    validFrom = validFrom,
    validUntil = validUntil,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    currentVersion = currentVersion
)

private fun MemoryEvidenceEntity.toCapsule() = MemoryCapsuleEvidence(
    nodeId = nodeId,
    sourceId = sourceId,
    evidenceRole = evidenceRole,
    confidence = confidence,
    summarySnapshot = summarySnapshot,
    createdAt = createdAt
)

private fun MemoryReviewEntity.toCapsule(gson: Gson): MemoryCapsuleReview {
    val sourceIds = gson.fromJson<List<String>>(
        sourceIdsJson,
        object : TypeToken<List<String>>() {}.type
    ) ?: emptyList()
    return MemoryCapsuleReview(
        id = id,
        scopeKey = scopeKey,
        kind = kind,
        title = title,
        content = content,
        confidence = confidence,
        status = status,
        sourceIds = sourceIds,
        proposalHash = proposalHash,
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        resolutionNote = resolutionNote
    )
}

private fun MemoryVersionEntity.toCapsule() = MemoryCapsuleVersion(
    nodeId = nodeId,
    version = version,
    kind = kind,
    title = title,
    content = content,
    importance = importance,
    confidence = confidence,
    validFrom = validFrom,
    validUntil = validUntil,
    changeReason = changeReason,
    actor = actor,
    createdAt = createdAt
)

private fun MemoryRelationEntity.toCapsule() = MemoryCapsuleRelation(
    fromNodeId = fromNodeId,
    toNodeId = toNodeId,
    relationType = relationType,
    scopeKey = scopeKey,
    weight = weight,
    status = status,
    evidenceJson = evidenceJson,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MemoryRetrievalTraceEntity.toCapsule() = MemoryCapsuleRetrievalTrace(
    id = id,
    scopeKey = scopeKey,
    query = query,
    selectedNodeIdsJson = selectedNodeIdsJson,
    explanationJson = explanationJson,
    createdAt = createdAt,
    durationMs = durationMs
)

private fun MemoryRetrievalFeedbackEntity.toCapsule() = MemoryCapsuleRetrievalFeedback(
    traceId = traceId,
    nodeId = nodeId,
    feedback = feedback,
    note = note,
    createdAt = createdAt
)

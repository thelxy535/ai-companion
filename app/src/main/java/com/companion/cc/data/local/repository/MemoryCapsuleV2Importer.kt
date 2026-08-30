package com.companion.cc.data.local.repository

import androidx.room.withTransaction
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
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.memory.MemoryCapsuleVersion
import com.companion.cc.domain.memory.MemoryCapsuleV2Validator

class MemoryCapsuleV2Importer(
    private val database: AppDatabase
) {
    data class Report(
        val insertedSources: Int = 0,
        val insertedNodes: Int = 0,
        val insertedEvidence: Int = 0,
        val insertedReviews: Int = 0,
        val insertedVersions: Int = 0,
        val insertedRelations: Int = 0,
        val insertedTraces: Int = 0,
        val insertedFeedback: Int = 0,
        val skippedRecords: Int = 0
    )

    suspend fun importCapsule(capsule: MemoryCapsuleV2, targetScopeKey: String): Result<Report> = runCatching {
        MemoryCapsuleV2Validator.validate(capsule).getOrThrow()
        require(capsule.scopeKey == targetScopeKey) {
            "Memory capsule scope does not match import target"
        }

        database.withTransaction {
            var insertedSources = 0
            var insertedNodes = 0
            var insertedEvidence = 0
            var insertedReviews = 0
            var insertedVersions = 0
            var insertedRelations = 0
            var insertedTraces = 0
            var insertedFeedback = 0
            var skippedRecords = 0

            capsule.sources.forEach { source ->
                val existing = database.memorySourceDao().findById(source.id)
                if (existing != null) {
                    require(existing.scopeKey == targetScopeKey) {
                        "Memory source belongs to another scope: ${source.id}"
                    }
                    require(existing == source.toEntity()) {
                        "Memory source conflicts with existing record: ${source.id}"
                    }
                    skippedRecords++
                } else {
                    val hashConflict = database.memorySourceDao()
                        .findByContentHashInScope(targetScopeKey, source.contentHash)
                    require(hashConflict == null) {
                        "Memory source content hash conflicts with existing record: ${source.contentHash}"
                    }
                    database.memorySourceDao().insert(source.toEntity())
                    insertedSources++
                }
            }

            capsule.nodes.forEach { node ->
                val existing = database.memoryNodeDao().findById(node.id)
                if (existing != null) {
                    require(existing.scopeKey == targetScopeKey) {
                        "Memory node belongs to another scope: ${node.id}"
                    }
                    require(existing == node.toEntity()) {
                        "Memory node conflicts with existing record: ${node.id}"
                    }
                    skippedRecords++
                } else {
                    database.memoryNodeDao().insert(node.toEntity())
                    insertedNodes++
                }
            }

            capsule.reviews.forEach { review ->
                val existing = database.memoryReviewDao().findById(review.id)
                if (existing != null) {
                    require(existing.scopeKey == targetScopeKey) {
                        "Memory review belongs to another scope: ${review.id}"
                    }
                    require(existing == review.toEntity()) {
                        "Memory review conflicts with existing record: ${review.id}"
                    }
                    skippedRecords++
                } else {
                    database.memoryReviewDao().insert(review.toEntity())
                    insertedReviews++
                }
            }

            capsule.evidence.forEach { evidence ->
                val existing = database.memoryEvidenceDao()
                    .findForNodeInScope(targetScopeKey, evidence.nodeId)
                    .firstOrNull { it.sourceId == evidence.sourceId }
                if (existing != null) {
                    require(existing == evidence.toEntity()) {
                        "Memory evidence conflicts with existing record: ${evidence.nodeId}/${evidence.sourceId}"
                    }
                    skippedRecords++
                } else {
                    database.memoryEvidenceDao().insertAll(listOf(evidence.toEntity()))
                    insertedEvidence++
                }
            }

            capsule.versions.forEach { version ->
                val existing = database.memoryVersionDao()
                    .findForNodeInScope(targetScopeKey, version.nodeId)
                    .firstOrNull { it.version == version.version }
                if (existing != null) {
                    require(existing == version.toEntity()) {
                        "Memory version conflicts with existing record: ${version.nodeId}@${version.version}"
                    }
                    skippedRecords++
                } else {
                    database.memoryVersionDao().insert(version.toEntity())
                    insertedVersions++
                }
            }

            capsule.relations.forEach { relation ->
                val existing = database.memoryRelationDao().findForKeyInScope(
                    targetScopeKey,
                    relation.fromNodeId,
                    relation.toNodeId,
                    relation.relationType
                )
                if (existing != null) {
                    require(existing == relation.toEntity()) {
                        "Memory relation conflicts with existing record"
                    }
                    skippedRecords++
                } else {
                    database.memoryRelationDao().insert(relation.toEntity())
                    insertedRelations++
                }
            }

            capsule.retrievalTraces.forEach { trace ->
                val existing = database.memoryRetrievalDao()
                    .findTraceByIdInScope(targetScopeKey, trace.id)
                if (existing != null) {
                    require(existing == trace.toEntity()) {
                        "Memory retrieval trace conflicts with existing record: ${trace.id}"
                    }
                    skippedRecords++
                } else {
                    database.memoryRetrievalDao().insertTrace(trace.toEntity())
                    insertedTraces++
                }
            }

            capsule.retrievalFeedback.forEach { feedback ->
                val existing = database.memoryRetrievalDao()
                    .findFeedbackForNodes(targetScopeKey, listOf(feedback.nodeId))
                    .firstOrNull { it.traceId == feedback.traceId }
                if (existing != null) {
                    require(existing == feedback.toEntity()) {
                        "Memory retrieval feedback conflicts with existing record"
                    }
                    skippedRecords++
                } else {
                    database.memoryRetrievalDao().insertFeedback(feedback.toEntity())
                    insertedFeedback++
                }
            }

            Report(
                insertedSources,
                insertedNodes,
                insertedEvidence,
                insertedReviews,
                insertedVersions,
                insertedRelations,
                insertedTraces,
                insertedFeedback,
                skippedRecords
            )
        }
    }
}

private fun MemoryCapsuleSource.toEntity() = MemorySourceEntity(
    id, scopeKey, messageId, contentSnapshot, sourceType, occurredAt, contentHash, createdAt
)

private fun MemoryCapsuleNode.toEntity() = MemoryNodeEntity(
    id, scopeKey, kind, subjectRole, subjectKey, title, content, importance, confidence,
    validFrom, validUntil, status, createdAt, updatedAt, currentVersion
)

private fun MemoryCapsuleEvidence.toEntity() = MemoryEvidenceEntity(
    nodeId, sourceId, evidenceRole, confidence, summarySnapshot, createdAt
)

private fun MemoryCapsuleReview.toEntity() = MemoryReviewEntity(
    id, scopeKey, kind, title, content, confidence, status,
    com.google.gson.Gson().toJson(sourceIds), proposalHash, createdAt, resolvedAt, resolutionNote
)

private fun MemoryCapsuleVersion.toEntity() = MemoryVersionEntity(
    nodeId, version, kind, title, content, importance, confidence, validFrom,
    validUntil, changeReason, actor, createdAt
)

private fun MemoryCapsuleRelation.toEntity() = MemoryRelationEntity(
    fromNodeId, toNodeId, relationType, scopeKey, weight, status, evidenceJson, createdAt, updatedAt
)

private fun MemoryCapsuleRetrievalTrace.toEntity() = MemoryRetrievalTraceEntity(
    id, scopeKey, query, selectedNodeIdsJson, explanationJson, createdAt, durationMs
)

private fun MemoryCapsuleRetrievalFeedback.toEntity() = MemoryRetrievalFeedbackEntity(
    traceId, nodeId, feedback, note, createdAt
)

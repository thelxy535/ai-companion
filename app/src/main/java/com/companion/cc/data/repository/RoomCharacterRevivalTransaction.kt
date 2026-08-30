package com.companion.cc.data.repository

import androidx.room.withTransaction
import com.companion.cc.data.local.database.AppDatabase
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.entity.MemorySourceEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.data.mapper.CustomCharacterMapper
import com.companion.cc.domain.character.CharacterRevivalTransaction
import com.companion.cc.domain.memory.MemoryCapsuleV2
import com.companion.cc.domain.memory.MemoryCapsuleV2Validator
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.model.CustomCharacter
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCharacterRevivalTransaction @Inject constructor(
    private val database: AppDatabase
) : CharacterRevivalTransaction {
    private val gson = Gson()

    override suspend fun reviveAtomically(
        capsule: CharacterMemoryCapsuleEntity,
        character: CustomCharacter,
        restoredAt: Long,
        memorySnapshot: MemoryCapsuleV2?
    ): Boolean = runCatching {
        database.withTransaction {
            database.customCharacterDao().insert(CustomCharacterMapper.toEntity(character))
            memorySnapshot?.let { copyMemory(it, capsule, character) }
            check(
                database.characterMemoryCapsuleDao().markRestoredIfAvailable(
                    userId = capsule.userId,
                    id = capsule.id,
                    restoredCharacterId = character.id,
                    restoredAt = restoredAt
                ) == 1
            ) { "Capsule has already been restored" }
            true
        }
    }.getOrElse { false }

    private suspend fun copyMemory(
        snapshot: MemoryCapsuleV2,
        capsule: CharacterMemoryCapsuleEntity,
        character: CustomCharacter
    ) {
        val userId = capsule.userId
        MemoryCapsuleV2Validator.validate(snapshot).getOrThrow()
        val oldScope = snapshot.scopeKey
        val newScope = MemoryScopeKey.forCharacter(userId, character.id)
        require(oldScope == MemoryScopeKey.forCharacter(userId, capsule.sourceCharacterId)) {
            "Memory capsule does not belong to source character"
        }

        val sourceIds = snapshot.sources.associate { it.id to UUID.randomUUID().toString() }
        val nodeIds = snapshot.nodes.associate { it.id to UUID.randomUUID().toString() }
        val reviewIds = snapshot.reviews.associate { it.id to UUID.randomUUID().toString() }
        val traceIds = snapshot.retrievalTraces.associate { it.id to UUID.randomUUID().toString() }
        val references = sourceIds + nodeIds + reviewIds + traceIds

        snapshot.sources.forEach { source ->
            database.memorySourceDao().insert(
                MemorySourceEntity(
                    id = sourceIds.getValue(source.id),
                    scopeKey = newScope,
                    messageId = null,
                    contentSnapshot = source.contentSnapshot,
                    sourceType = source.sourceType,
                    occurredAt = source.occurredAt,
                    contentHash = source.contentHash,
                    createdAt = source.createdAt
                )
            )
        }
        snapshot.nodes.forEach { node ->
            database.memoryNodeDao().insert(
                MemoryNodeEntity(
                    id = nodeIds.getValue(node.id),
                    scopeKey = newScope,
                    kind = node.kind,
                    subjectRole = node.subjectRole,
                    subjectKey = node.subjectKey,
                    title = node.title,
                    content = node.content,
                    importance = node.importance,
                    confidence = node.confidence,
                    validFrom = node.validFrom,
                    validUntil = node.validUntil,
                    status = node.status,
                    createdAt = node.createdAt,
                    updatedAt = node.updatedAt,
                    currentVersion = node.currentVersion
                )
            )
        }
        snapshot.reviews.forEach { review ->
            database.memoryReviewDao().insert(
                MemoryReviewEntity(
                    id = reviewIds.getValue(review.id),
                    scopeKey = newScope,
                    kind = review.kind,
                    title = review.title,
                    content = review.content,
                    confidence = review.confidence,
                    status = review.status,
                    sourceIdsJson = gson.toJson(review.sourceIds.map { sourceIds.getValue(it) }),
                    proposalHash = review.proposalHash,
                    createdAt = review.createdAt,
                    resolvedAt = review.resolvedAt,
                    resolutionNote = review.resolutionNote
                )
            )
        }
        snapshot.versions.forEach { version ->
            database.memoryVersionDao().insert(
                MemoryVersionEntity(
                    nodeId = nodeIds.getValue(version.nodeId),
                    version = version.version,
                    kind = version.kind,
                    title = version.title,
                    content = version.content,
                    importance = version.importance,
                    confidence = version.confidence,
                    validFrom = version.validFrom,
                    validUntil = version.validUntil,
                    changeReason = version.changeReason,
                    actor = version.actor,
                    createdAt = version.createdAt
                )
            )
        }
        database.memoryEvidenceDao().insertAll(
            snapshot.evidence.map { evidence ->
                MemoryEvidenceEntity(
                    nodeId = nodeIds.getValue(evidence.nodeId),
                    sourceId = sourceIds.getValue(evidence.sourceId),
                    evidenceRole = evidence.evidenceRole,
                    confidence = evidence.confidence,
                    summarySnapshot = evidence.summarySnapshot,
                    createdAt = evidence.createdAt
                )
            }
        )
        snapshot.relations.forEach { relation ->
            database.memoryRelationDao().insert(
                MemoryRelationEntity(
                    fromNodeId = nodeIds.getValue(relation.fromNodeId),
                    toNodeId = nodeIds.getValue(relation.toNodeId),
                    relationType = relation.relationType,
                    scopeKey = newScope,
                    weight = relation.weight,
                    status = relation.status,
                    evidenceJson = rewriteJson(relation.evidenceJson, sourceIds),
                    createdAt = relation.createdAt,
                    updatedAt = relation.updatedAt
                )
            )
        }
        snapshot.retrievalTraces.forEach { trace ->
            database.memoryRetrievalDao().insertTrace(
                MemoryRetrievalTraceEntity(
                    id = traceIds.getValue(trace.id),
                    scopeKey = newScope,
                    query = trace.query,
                    selectedNodeIdsJson = rewriteJson(trace.selectedNodeIdsJson, nodeIds),
                    explanationJson = rewriteJson(trace.explanationJson, references),
                    createdAt = trace.createdAt,
                    durationMs = trace.durationMs
                )
            )
        }
        snapshot.retrievalFeedback.forEach { feedback ->
            database.memoryRetrievalDao().insertFeedback(
                MemoryRetrievalFeedbackEntity(
                    traceId = traceIds.getValue(feedback.traceId),
                    nodeId = nodeIds.getValue(feedback.nodeId),
                    feedback = feedback.feedback,
                    note = feedback.note,
                    createdAt = feedback.createdAt
                )
            )
        }
    }

    private fun rewriteJson(json: String, references: Map<String, String>): String {
        val root = rewriteJsonElement(JsonParser().parse(json), references)
        return gson.toJson(root)
    }

    private fun rewriteJsonElement(element: JsonElement, references: Map<String, String>): JsonElement = when {
        element.isJsonArray -> {
            element.asJsonArray.map { rewriteJsonElement(it, references) }
                .let { gson.toJsonTree(it) }
        }
        element.isJsonObject -> {
            element.asJsonObject.entrySet().associate { entry ->
                entry.key to rewriteJsonElement(entry.value, references)
            }.let { gson.toJsonTree(it) }
        }
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
            references[element.asString]?.let(::JsonPrimitive) ?: element
        }
        else -> element
    }
}

package com.companion.cc.domain.memory

import com.google.gson.JsonParser

const val MEMORY_CAPSULE_V2_FORMAT = "cc-switch.memory-capsule"
const val MEMORY_CAPSULE_V2_SCHEMA_VERSION = 2

data class MemoryCapsuleV2(
    val format: String = MEMORY_CAPSULE_V2_FORMAT,
    val schemaVersion: Int = MEMORY_CAPSULE_V2_SCHEMA_VERSION,
    val scopeKey: String,
    val sources: List<MemoryCapsuleSource> = emptyList(),
    val nodes: List<MemoryCapsuleNode> = emptyList(),
    val evidence: List<MemoryCapsuleEvidence> = emptyList(),
    val reviews: List<MemoryCapsuleReview> = emptyList(),
    val versions: List<MemoryCapsuleVersion> = emptyList(),
    val relations: List<MemoryCapsuleRelation> = emptyList(),
    val retrievalTraces: List<MemoryCapsuleRetrievalTrace> = emptyList(),
    val retrievalFeedback: List<MemoryCapsuleRetrievalFeedback> = emptyList()
)

data class MemoryCapsuleSource(
    val id: String,
    val scopeKey: String,
    val messageId: String?,
    val contentSnapshot: String,
    val sourceType: String,
    val occurredAt: Long,
    val contentHash: String,
    val createdAt: Long
)

data class MemoryCapsuleNode(
    val id: String,
    val scopeKey: String,
    val kind: String = "",
    val subjectRole: String = "",
    val subjectKey: String = "",
    val title: String = "",
    val content: String = "",
    val importance: Int = 50,
    val confidence: Double = 0.5,
    val validFrom: Long = 0L,
    val validUntil: Long? = null,
    val status: String = "active",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val currentVersion: Int = 1
)

data class MemoryCapsuleEvidence(
    val nodeId: String,
    val sourceId: String,
    val evidenceRole: String = "support",
    val confidence: Double = 0.5,
    val summarySnapshot: String = "",
    val createdAt: Long
)

data class MemoryCapsuleReview(
    val id: String,
    val scopeKey: String,
    val kind: String,
    val title: String,
    val content: String,
    val confidence: Double,
    val status: String = "pending",
    val sourceIds: List<String> = emptyList(),
    val proposalHash: String,
    val createdAt: Long,
    val resolvedAt: Long? = null,
    val resolutionNote: String = ""
)

data class MemoryCapsuleVersion(
    val nodeId: String,
    val version: Int,
    val kind: String,
    val title: String,
    val content: String,
    val importance: Int,
    val confidence: Double,
    val validFrom: Long,
    val validUntil: Long?,
    val changeReason: String,
    val actor: String,
    val createdAt: Long
)

data class MemoryCapsuleRelation(
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: String,
    val scopeKey: String,
    val weight: Double = 0.5,
    val status: String = "proposed",
    val evidenceJson: String = "[]",
    val createdAt: Long,
    val updatedAt: Long
)

data class MemoryCapsuleRetrievalTrace(
    val id: String,
    val scopeKey: String,
    val query: String,
    val selectedNodeIdsJson: String,
    val explanationJson: String,
    val createdAt: Long,
    val durationMs: Long
)

data class MemoryCapsuleRetrievalFeedback(
    val traceId: String,
    val nodeId: String,
    val feedback: String,
    val note: String = "",
    val createdAt: Long
)

object MemoryCapsuleV2Validator {
    fun validate(capsule: MemoryCapsuleV2): Result<Unit> = runCatching {
        require(capsule.format == MEMORY_CAPSULE_V2_FORMAT) {
            "Unsupported memory capsule format"
        }
        require(capsule.schemaVersion == MEMORY_CAPSULE_V2_SCHEMA_VERSION) {
            "Unsupported memory capsule schema version"
        }
        require(capsule.scopeKey.matches(Regex("^user:[^:]+:companion:[^:]+$"))) {
            "Memory capsule scope must identify a user and companion"
        }
        capsule.sources.forEach { source ->
            require(source.id.isNotBlank()) { "Memory capsule source ID is required" }
            require(source.scopeKey == capsule.scopeKey) {
                "Memory capsule source does not belong to declared scope"
            }
        }
        capsule.nodes.forEach { node ->
            require(node.id.isNotBlank()) { "Memory capsule node ID is required" }
            require(node.status in setOf("active", "do_not_recall", "deleted")) {
                "Memory capsule node has an invalid status"
            }
            require(node.scopeKey == capsule.scopeKey) {
                "Memory capsule node does not belong to declared scope"
            }
        }
        capsule.reviews.forEach { review ->
            require(review.id.isNotBlank()) { "Memory capsule review ID is required" }
            require(review.proposalHash.isNotBlank()) { "Memory capsule review proposal hash is required" }
            require(review.scopeKey == capsule.scopeKey) {
                "Memory capsule review does not belong to declared scope"
            }
        }
        capsule.evidence.forEach { evidence ->
            require(evidence.nodeId.isNotBlank()) { "Memory capsule evidence node ID is required" }
            require(evidence.sourceId.isNotBlank()) { "Memory capsule evidence source ID is required" }
        }
        val sourceIds = capsule.sources.map { it.id }
        require(sourceIds.size == sourceIds.toSet().size) {
            "Memory capsule contains duplicate source IDs"
        }
        val nodeIds = capsule.nodes.map { it.id }
        require(nodeIds.size == nodeIds.toSet().size) {
            "Memory capsule contains duplicate node IDs"
        }
        val reviewIds = capsule.reviews.map { it.id }
        require(reviewIds.size == reviewIds.toSet().size) {
            "Memory capsule contains duplicate review IDs"
        }
        val proposalHashes = capsule.reviews.map { it.proposalHash }
        require(proposalHashes.size == proposalHashes.toSet().size) {
            "Memory capsule contains duplicate review proposal hashes"
        }
        val traceIds = capsule.retrievalTraces.map { it.id }
        require(traceIds.size == traceIds.toSet().size) {
            "Memory capsule contains duplicate retrieval trace IDs"
        }
        val entityIds = sourceIds + nodeIds + reviewIds + traceIds
        require(entityIds.size == entityIds.toSet().size) {
            "Memory capsule contains IDs shared across entity types"
        }
        val feedbackKeys = capsule.retrievalFeedback.map { "${it.traceId}|${it.nodeId}" }
        require(feedbackKeys.size == feedbackKeys.toSet().size) {
            "Memory capsule contains duplicate retrieval feedback"
        }
        val evidenceKeys = capsule.evidence.map { "${it.nodeId}|${it.sourceId}" }
        require(evidenceKeys.size == evidenceKeys.toSet().size) {
            "Memory capsule contains duplicate evidence"
        }
        val sourceIdSet = sourceIds.toSet()
        val nodeIdSet = nodeIds.toSet()
        capsule.reviews.forEach { review ->
            review.sourceIds.forEach { sourceId ->
                require(sourceId in sourceIdSet) {
                    "Memory capsule review references a missing source"
                }
            }
        }
        val versionKeys = capsule.versions.map { "${it.nodeId}|${it.version}" }
        require(versionKeys.size == versionKeys.toSet().size) {
            "Memory capsule contains duplicate versions"
        }
        capsule.versions.forEach { version ->
            require(version.nodeId.isNotBlank()) { "Memory capsule version node ID is required" }
            require(version.version > 0) { "Memory capsule version must be positive" }
            require(version.nodeId in nodeIdSet) {
                "Memory capsule version references a missing node"
            }
        }
        capsule.evidence.forEach { evidence ->
            require(evidence.nodeId in nodeIdSet) {
                "Memory capsule evidence references a missing node"
            }
            require(evidence.sourceId in sourceIdSet) {
                "Memory capsule evidence references a missing source"
            }
        }
        val relationKeys = capsule.relations.map { relation ->
            require(relation.fromNodeId.isNotBlank() && relation.toNodeId.isNotBlank()) {
                "Memory capsule relation node IDs are required"
            }
            require(relation.relationType.isNotBlank()) {
                "Memory capsule relation type is required"
            }
            require(relation.scopeKey == capsule.scopeKey) {
                "Memory capsule relation does not belong to declared scope"
            }
            require(relation.fromNodeId in nodeIdSet && relation.toNodeId in nodeIdSet) {
                "Memory capsule relation references a missing node"
            }
            val relationEvidence = JsonParser().parse(relation.evidenceJson)
            require(relationEvidence.isJsonArray) {
                "Memory capsule relation evidence must be an array"
            }
            relationEvidence.asJsonArray.forEach { sourceId ->
                require(sourceId.isJsonPrimitive && sourceId.asJsonPrimitive.isString) {
                    "Memory capsule relation evidence IDs must be strings"
                }
                require(sourceId.asString in sourceIdSet) {
                    "Memory capsule relation evidence references a missing source"
                }
            }
            "${relation.fromNodeId}|${relation.toNodeId}|${relation.relationType}"
        }
        require(relationKeys.size == relationKeys.toSet().size) {
            "Memory capsule contains duplicate relations"
        }
        capsule.retrievalTraces.forEach { trace ->
            require(trace.id.isNotBlank()) { "Memory capsule retrieval trace ID is required" }
            require(trace.scopeKey == capsule.scopeKey) {
                "Memory capsule retrieval trace does not belong to declared scope"
            }
            val selected = JsonParser().parse(trace.selectedNodeIdsJson).asJsonArray
            selected.forEach { selectedNode ->
                require(selectedNode.isJsonPrimitive && selectedNode.asJsonPrimitive.isString) {
                    "Memory capsule trace selected node IDs must be strings"
                }
                require(selectedNode.asString in nodeIdSet) {
                    "Memory capsule trace references a missing node"
                }
            }
            JsonParser().parse(trace.explanationJson)
        }
        val traceById = capsule.retrievalTraces.associateBy { it.id }
        capsule.retrievalFeedback.forEach { feedback ->
            require(feedback.traceId.isNotBlank()) { "Memory capsule feedback trace ID is required" }
            require(feedback.nodeId.isNotBlank()) { "Memory capsule feedback node ID is required" }
            require(feedback.feedback == "positive" || feedback.feedback == "negative") {
                "Memory capsule feedback must be positive or negative"
            }
            require(feedback.traceId in traceIds.toSet()) {
                "Memory capsule feedback references a missing trace"
            }
            require(feedback.nodeId in nodeIdSet) {
                "Memory capsule feedback references a missing node"
            }
            val selected = JsonParser().parse(traceById.getValue(feedback.traceId).selectedNodeIdsJson).asJsonArray
            require(selected.any { it.asString == feedback.nodeId }) {
                "Memory capsule feedback node was not selected by its trace"
            }
        }
    }
}

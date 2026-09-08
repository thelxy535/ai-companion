package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity

/** Immutable query for the bounded Memory 2.0 graph projection. */
data class GraphQuery(
    val scopeKey: String,
    val rootNodeId: String? = null,
    val hop: Int = 2,
    val maxNodes: Int = 120,
    val maxEdges: Int = 240,
    val asOf: Long = System.currentTimeMillis(),
    val statuses: Set<String> = setOf("active"),
    val relationTypes: Set<String> = emptySet(),
    val text: String = "",
    val includeExpired: Boolean = false,
    val includeSuppressed: Boolean = false,
) {
    init {
        require(scopeKey.isNotBlank()) { "scopeKey must not be blank" }
        require(hop in 0..8) { "hop must be between 0 and 8" }
        require(maxNodes in 1..2_000) { "maxNodes must be between 1 and 2000" }
        require(maxEdges in 0..4_000) { "maxEdges must be between 0 and 4000" }
    }
}

data class GraphSnapshot(
    val nodes: List<MemoryNodeEntity>,
    val edges: List<MemoryRelationEntity>,
    val roots: List<String>,
    val orphans: List<String>,
    val truncated: Boolean,
    val frontierCount: Int,
    val query: GraphQuery,
    val generatedAt: Long,
    val revision: Long = generatedAt,
)

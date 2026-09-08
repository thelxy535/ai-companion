package com.companion.cc.domain.memory

import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.MemoryRelationDao
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import javax.inject.Inject

interface MemoryGraphRepository {
    suspend fun project(query: GraphQuery): GraphSnapshot
}

/** Builds a bounded graph in memory from one node query and one relation query. */
class RoomMemoryGraphRepository @Inject constructor(
    private val nodeDao: MemoryNodeDao,
    private val relationDao: MemoryRelationDao,
) : MemoryGraphRepository {
    override suspend fun project(query: GraphQuery): GraphSnapshot {
        val allNodes = nodeDao.findAllInScope(query.scopeKey)
        val eligibleNodes = allNodes
            .asSequence()
            .filter { node -> node.status in query.statuses }
            .filter { node -> query.includeExpired || isValidAt(node, query.asOf) }
            .filter { node -> query.includeSuppressed || node.status != "suppressed" }
            .filter { node -> query.text.isBlank() || node.title.contains(query.text, true) || node.content.contains(query.text, true) }
            .sortedWith(nodeComparator())
            .toList()
        val eligibleIds = eligibleNodes.asSequence().map { it.id }.toHashSet()

        val allRelations = relationDao.findAllInScope(query.scopeKey)
        val eligibleRelations = allRelations
            .asSequence()
            .filter { relation -> relation.status in query.statuses || relation.status == "active" }
            .filter { relation -> query.relationTypes.isEmpty() || relation.relationType in query.relationTypes }
            .filter { relation -> relation.fromNodeId in eligibleIds && relation.toNodeId in eligibleIds }
            .sortedWith(relationComparator())
            .toList()

        val selectedIds = selectNodes(query, eligibleNodes, eligibleRelations)
        val selectedNodes = eligibleNodes
            .filter { it.id in selectedIds }
            .take(query.maxNodes)
            .map { node ->
                node.copy(
                    title = MojibakeRepair.repair(node.title),
                    content = MojibakeRepair.repair(node.content),
                )
            }
        val selectedNodeIds = selectedNodes.mapTo(HashSet()) { it.id }
        val selectedEdges = eligibleRelations
            .filter { it.fromNodeId in selectedNodeIds && it.toNodeId in selectedNodeIds }
            .take(query.maxEdges)

        val truncated = selectedNodes.size < eligibleNodes.size || selectedEdges.size < eligibleRelations.count {
            it.fromNodeId in selectedNodeIds && it.toNodeId in selectedNodeIds
        }
        val roots = selectedNodes.filter { node -> selectedEdges.none { it.toNodeId == node.id } }.map { it.id }
        val connected = selectedEdges.flatMap { listOf(it.fromNodeId, it.toNodeId) }.toSet()
        val orphans = selectedNodes.filter { it.id !in connected }.map { it.id }

        return GraphSnapshot(
            nodes = selectedNodes,
            edges = selectedEdges,
            roots = roots,
            orphans = orphans,
            truncated = truncated,
            frontierCount = (eligibleNodes.size - selectedNodes.size).coerceAtLeast(0),
            query = query,
            generatedAt = query.asOf,
            revision = stableRevision(selectedNodes, selectedEdges),
        )
    }

    private fun selectNodes(
        query: GraphQuery,
        nodes: List<MemoryNodeEntity>,
        relations: List<MemoryRelationEntity>,
    ): Set<String> {
        if (query.rootNodeId == null) return nodes.take(query.maxNodes).mapTo(HashSet()) { it.id }
        if (query.rootNodeId !in nodes.map { it.id }) return emptySet()

        val adjacency = HashMap<String, MutableList<String>>()
        relations.forEach { relation ->
            adjacency.getOrPut(relation.fromNodeId) { mutableListOf() }.add(relation.toNodeId)
            adjacency.getOrPut(relation.toNodeId) { mutableListOf() }.add(relation.fromNodeId)
        }
        val selected = linkedSetOf(query.rootNodeId)
        var frontier = setOf(query.rootNodeId)
        repeat(query.hop) {
            frontier = frontier.flatMap { adjacency[it].orEmpty() }
                .distinct()
                .filter { selected.add(it) }
                .toSet()
            if (frontier.isEmpty()) return@repeat
        }
        return selected
    }

    private fun isValidAt(node: MemoryNodeEntity, asOf: Long): Boolean =
        node.validFrom <= asOf && (node.validUntil == null || node.validUntil > asOf)

    private fun nodeComparator() = compareByDescending<MemoryNodeEntity> {
        it.importance * it.confidence
    }.thenByDescending { it.updatedAt }.thenBy { it.id }

    private fun relationComparator() = compareByDescending<MemoryRelationEntity> {
        it.weight
    }.thenByDescending { it.updatedAt }
        .thenBy { it.fromNodeId }.thenBy { it.toNodeId }.thenBy { it.relationType }

    private fun stableRevision(nodes: List<MemoryNodeEntity>, edges: List<MemoryRelationEntity>): Long =
        (nodes.joinToString("|") { "${it.id}:${it.updatedAt}" } + ":" +
            edges.joinToString("|") { "${it.fromNodeId}:${it.toNodeId}:${it.updatedAt}" }).hashCode().toLong()
}

package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class RetrievedMemory(val node: MemoryNodeEntity, val score: Double, val explanation: String)
data class RetrievalResult(val traceId: String, val memories: List<RetrievedMemory>)

class MemoryRetrievalService @Inject constructor(private val repository: MemoryRepository) {
    suspend fun retrieve(query: String, scopeKey: String, limit: Int = 8): RetrievalResult = withContext(Dispatchers.IO) {
        val candidates = repository.observeNodes(scopeKey, query = "").first()
        val ranked = rank(query, candidates, System.currentTimeMillis()).take(limit)
        val traceId = "trace:${UUID.randomUUID()}"
        repository.recordRetrieval(MemoryRetrievalTraceEntity(
            id = traceId,
            scopeKey = scopeKey,
            query = query,
            selectedNodeIdsJson = ranked.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]") { it.node.id },
            explanationJson = ranked.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]") { it.explanation },
            createdAt = System.currentTimeMillis(),
            durationMs = 0
        ))
        RetrievalResult(traceId, ranked)
    }

    companion object {
        fun rank(query: String, nodes: List<MemoryNodeEntity>, now: Long): List<RetrievedMemory> {
            val normalized = query.trim().lowercase()
            return nodes.filter { it.status == "active" }.map { node ->
                val titleMatch = if (normalized.isNotEmpty() && node.title.lowercase().contains(normalized)) 2.0 else 0.0
                val contentMatch = if (normalized.isNotEmpty() && node.content.lowercase().contains(normalized)) 1.0 else 0.0
                val score = titleMatch + contentMatch + node.importance / 100.0 + node.confidence
                RetrievedMemory(node, score, "title=$titleMatch content=$contentMatch importance=${node.importance} confidence=${node.confidence}")
            }.sortedWith(compareByDescending<RetrievedMemory> { it.score }.thenByDescending { it.node.updatedAt })
        }
    }
}

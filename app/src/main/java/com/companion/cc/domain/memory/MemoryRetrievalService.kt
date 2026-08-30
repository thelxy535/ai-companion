package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

data class RetrievedMemory(val node: MemoryNodeEntity, val score: Double, val explanation: String)
data class RetrievalResult(val traceId: String, val memories: List<RetrievedMemory>)
data class RetrievalFeedbackSummary(
    val positiveCount: Int = 0,
    val negativeCount: Int = 0
)

class MemoryRetrievalService(
    private val repository: MemoryRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    suspend fun retrieve(query: String, scopeKey: String, limit: Int = 8): RetrievalResult = withContext(Dispatchers.IO) {
        val startedAt = elapsedMillis()
        val asOf = now()
        val candidateLimit = candidateLimitFor(limit)
        val candidates = repository.observeRecallCandidates(
            scopeKey,
            now = asOf,
            query = query,
            limit = candidateLimit
        ).first()
        val feedback = repository.getRetrievalFeedback(
            scopeKey,
            candidates.map { it.id }
        ).orEmpty().groupBy { it.nodeId }.mapValues { (_, entries) ->
            RetrievalFeedbackSummary(
                positiveCount = entries.count { it.feedback.equals("positive", ignoreCase = true) },
                negativeCount = entries.count { it.feedback.equals("negative", ignoreCase = true) }
            )
        }
        val ranked = rank(query, candidates, asOf, feedback).take(limit)
        val traceId = "trace:${UUID.randomUUID()}"
        repository.recordRetrieval(MemoryRetrievalTraceEntity(
            id = traceId,
            scopeKey = scopeKey,
            query = query,
            selectedNodeIdsJson = Gson().toJson(ranked.map { it.node.id }),
            explanationJson = Gson().toJson(ranked.map { it.explanation }),
            createdAt = now(),
            durationMs = (elapsedMillis() - startedAt).coerceAtLeast(0L)
        ))
        RetrievalResult(traceId, ranked)
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L
        private const val RECENCY_WEIGHT = 0.5
        private const val POSITIVE_FEEDBACK_WEIGHT = 0.25
        private const val NEGATIVE_FEEDBACK_WEIGHT = 1.0

        fun candidateLimitFor(limit: Int): Int = limit.coerceIn(1, 100)

        fun rank(
            query: String,
            nodes: List<MemoryNodeEntity>,
            now: Long,
            feedback: Map<String, RetrievalFeedbackSummary> = emptyMap()
        ): List<RetrievedMemory> {
            val normalized = query.trim().lowercase()
            return nodes.filter { node ->
                node.status == "active" &&
                    node.validFrom <= now &&
                    (node.validUntil == null || node.validUntil > now)
            }.map { node ->
                val titleMatch = if (normalized.isNotEmpty() && node.title.lowercase().contains(normalized)) 2.0 else 0.0
                val contentMatch = if (normalized.isNotEmpty() && node.content.lowercase().contains(normalized)) 1.0 else 0.0
                val ageMs = (now - node.updatedAt).coerceAtLeast(0L)
                val recency = 1.0 / (1.0 + ageMs.toDouble() / DAY_MILLIS)
                val nodeFeedback = feedback[node.id] ?: RetrievalFeedbackSummary()
                val feedbackAdjustment =
                    nodeFeedback.positiveCount * POSITIVE_FEEDBACK_WEIGHT -
                        nodeFeedback.negativeCount * NEGATIVE_FEEDBACK_WEIGHT
                val score = titleMatch + contentMatch + node.importance / 100.0 +
                    node.confidence + recency * RECENCY_WEIGHT + feedbackAdjustment
                RetrievedMemory(
                    node,
                    score,
                    "title=$titleMatch content=$contentMatch importance=${node.importance} " +
                        "confidence=${node.confidence} recency=$recency " +
                        "positive=${nodeFeedback.positiveCount} negative=${nodeFeedback.negativeCount}"
                )
            }.sortedWith(
                compareByDescending<RetrievedMemory> { it.score }
                    .thenByDescending { it.node.updatedAt }
                    .thenBy { it.node.id }
            )
        }
    }
}

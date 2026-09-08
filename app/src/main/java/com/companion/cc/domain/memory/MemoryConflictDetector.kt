package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryReviewEntity

sealed interface MemoryConflictResult {
    data object None : MemoryConflictResult
    data class Conflict(val existing: MemoryNodeEntity, val reason: String) : MemoryConflictResult
}

/** Conservative conflict check: only flag shared keywords with opposite preference polarity. */
object MemoryConflictDetector {
    private val negations = setOf("不喜欢", "不再", "讨厌", "不想", "不愿意", "不爱")
    private val positives = setOf("喜欢", "爱吃", "想吃", "偏好", "习惯")

    fun detect(candidate: MemoryReviewEntity, existing: List<MemoryNodeEntity>): MemoryConflictResult {
        val candidateText = candidate.title + candidate.content
        val candidateNegative = negations.any(candidateText::contains)
        val candidatePositive = positives.any(candidateText::contains)
        if (!candidateNegative && !candidatePositive) return MemoryConflictResult.None
        return existing.firstOrNull { node ->
            if (node.kind != candidate.kind) return@firstOrNull false
            val existingText = node.title + node.content
            val topicOverlap = topicTokens(candidateText).any { it in existingText }
            val existingNegative = negations.any(existingText::contains)
            val existingPositive = positives.any(existingText::contains)
            topicOverlap && ((candidateNegative && existingPositive) || (candidatePositive && existingNegative))
        }?.let { existingNode ->
            val differentScene = hasSceneMarker(candidateText) || hasSceneMarker(existingNode.content + existingNode.title)
            MemoryConflictResult.Conflict(
                existingNode,
                if (differentScene) "同一主题在不同场景出现了相反倾向，可能需要分别保留"
                else "同一主题出现了相反倾向"
            )
        } ?: MemoryConflictResult.None
    }

    private fun topicTokens(text: String): Set<String> {
        val normalized = text
            .replace(Regex("不喜欢|不再|讨厌|不想|不愿意|不爱|喜欢|爱吃|想吃|偏好|习惯"), "")
            .replace(Regex("[^一-龥A-Za-z0-9]"), "")
        return buildSet {
            addAll(normalized.split(Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])")))
            normalized.windowed(2).forEach(::add)
        }.filter { it.length > 1 }.toSet()
    }

    private fun hasSceneMarker(text: String): Boolean =
        text.containsAny("在工作时", "在家时", "周末", "平时", "有时候", "最近", "以前", "现在")

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }
}

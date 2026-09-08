package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryReviewEntity

data class ReviewInboxGroup(
    val key: String,
    val items: List<MemoryReviewEntity>
) {
    val representative: MemoryReviewEntity get() = items.maxByOrNull { it.confidence } ?: items.first()
    val confidence: Double get() = items.map { it.confidence }.average().coerceIn(0.0, 1.0)
}

/** Groups noisy reflection candidates before they reach the user-facing inbox. */
object ReviewInboxPolicy {
    private val sensitiveMarkers = setOf("地址", "电话", "身份证", "密码", "健康", "疾病", "收入", "账号")

    fun group(reviews: List<MemoryReviewEntity>): List<ReviewInboxGroup> {
        val groups = linkedMapOf<String, MutableList<MemoryReviewEntity>>()
        reviews.sortedWith(compareByDescending<MemoryReviewEntity> { it.confidence }.thenByDescending { it.createdAt })
            .forEach { review ->
                val key = keyFor(review)
                groups.getOrPut(key) { mutableListOf() }.add(review)
            }
        return groups.values.mapIndexed { index, items ->
            ReviewInboxGroup("$index:${keyFor(items.first())}", items)
        }
    }

    fun isSensitive(review: MemoryReviewEntity): Boolean =
        review.kind == "commitment" ||
            sensitiveMarkers.any { review.title.contains(it) || review.content.contains(it) }

    /** These items need a deliberate, visible decision even when ordinary items are batched. */
    fun isBatchable(review: MemoryReviewEntity): Boolean =
        review.status != "conflict" && !isSensitive(review) && review.kind != NarrativeKinds.RELATIONSHIP

    private fun keyFor(review: MemoryReviewEntity): String {
        if (isSensitive(review)) return "sensitive:${review.kind}:${review.id}"
        val text = "${review.title} ${review.content}"
        val topic = when {
            text.containsAny("火锅", "吃", "饭", "喝", "食物") -> "food"
            text.containsAny("书", "画", "音乐", "电影", "游戏") -> "interest"
            text.containsAny("工作", "学校", "学习", "项目") -> "work-study"
            text.containsAny("喜欢", "偏好", "习惯") -> "preference"
            review.kind == NarrativeKinds.RELATIONSHIP -> "relationship"
            else -> review.kind
        }
        return "$topic:${review.kind}"
    }

    private fun String.containsAny(vararg markers: String): Boolean = markers.any { contains(it) }
}

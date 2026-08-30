package com.companion.cc.domain.usage

data class UsageRecord(
    val userId: String,
    val characterId: String,
    val feature: String,
    val model: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val inputCostPerThousand: Double = 0.0,
    val outputCostPerThousand: Double = 0.0,
    val succeeded: Boolean = true
)

data class UsageSummary(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val estimatedCost: Double = 0.0
)

object UsageCostCalculator {
    fun cost(record: UsageRecord): Double =
        record.inputTokens / 1000.0 * record.inputCostPerThousand +
            record.outputTokens / 1000.0 * record.outputCostPerThousand

    fun succeededForCompletion(cause: Throwable?): Boolean = cause == null

    /** Conservative local estimate used when the provider omits token usage. */
    fun estimatedTokens(text: String): Long =
        if (text.isEmpty()) 0L else (text.length + 3L) / 4L
}

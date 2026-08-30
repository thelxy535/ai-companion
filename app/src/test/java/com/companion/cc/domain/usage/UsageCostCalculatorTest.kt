package com.companion.cc.domain.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageCostCalculatorTest {
    @Test
    fun `calculates cost from input and output tokens`() {
        val record = UsageRecord(
            userId = "u1",
            characterId = "c1",
            feature = "chat",
            model = "model-a",
            inputTokens = 1_000,
            outputTokens = 500,
            inputCostPerThousand = 0.002,
            outputCostPerThousand = 0.004
        )

        assertEquals(0.004, UsageCostCalculator.cost(record), 0.000001)
    }

    @Test
    fun `completion outcome is not successful when stream fails`() {
        val failed = UsageCostCalculator.succeededForCompletion(IllegalStateException("network"))
        val cancelled = UsageCostCalculator.succeededForCompletion(kotlinx.coroutines.CancellationException())

        assertEquals(false, failed)
        assertEquals(false, cancelled)
    }
}

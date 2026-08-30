package com.companion.cc.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class AdaptiveMetricGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun metricItem_exposesItsLabelAndValueAsOneAccessibleSummary() {
        composeRule.setContent {
            MaterialTheme {
                AdaptiveMetricGrid(
                    items = listOf(
                        MetricGridItem(
                            label = "数据库",
                            value = "12 MB",
                            icon = Icons.Default.Storage
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithContentDescription("数据库，12 MB").assertIsDisplayed()
    }
}

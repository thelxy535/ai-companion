package com.companion.cc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScenePrimitivesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topBar_exposesTitleAndInvokesItsOwnActions() {
        var backClicks = 0
        var refreshClicks = 0

        composeRule.setContent {
            MaterialTheme {
                SceneTopBar(
                    title = {
                        Text("缪斯")
                        Text("等待开始对话")
                    },
                    onNavigateBack = { backClicks++ },
                    actions = listOf(
                        SceneTopBarAction(
                            icon = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            onClick = { refreshClicks++ }
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithText("缪斯").assertIsDisplayed()
        composeRule.onNodeWithText("等待开始对话").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithContentDescription("刷新").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, refreshClicks)
    }

    @Test
    fun action_respectsEnabledStateAndKeepsDestructiveLabelExplicit() {
        var enabledClicks = 0
        var disabledClicks = 0

        composeRule.setContent {
            MaterialTheme {
                Column {
                    SceneAction(
                        title = "清除全部对话",
                        icon = Icons.Default.Delete,
                        tone = SceneActionTone.Destructive,
                        onClick = { enabledClicks++ }
                    )
                    SceneAction(
                        title = "导出数据备份",
                        icon = Icons.Default.Storage,
                        enabled = false,
                        onClick = { disabledClicks++ }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("scene_action_清除全部对话").assertIsDisplayed().performTouchInput { click() }
        composeRule.onNodeWithTag("scene_action_导出数据备份").assertIsNotEnabled()

        assertEquals(1, enabledClicks)
        assertEquals(0, disabledClicks)
    }
}

package com.companion.cc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlassAlertDialogComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun glassAlertDialog_exposesDismissAndConfirmActions() {
        var confirms = 0
        var dismisses = 0
        composeRule.setContent {
            MaterialTheme {
                GlassAlertDialog(
                    onDismissRequest = { dismisses++ },
                    title = { Text("确认操作") },
                    text = { Text("继续将执行操作") },
                    confirmButton = { TextButton(onClick = { confirms++ }) { Text("确认") } },
                    dismissButton = { TextButton(onClick = { dismisses++ }) { Text("取消") } }
                )
            }
        }

        composeRule.onNodeWithText("确认操作").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("确认").performClick()

        assertEquals(1, dismisses)
        assertEquals(1, confirms)
    }
}

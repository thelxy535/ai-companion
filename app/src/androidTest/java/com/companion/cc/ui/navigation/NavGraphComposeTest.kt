package com.companion.cc.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavGraphComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun invalidRouteScreen_backButtonInvokesCallback() {
        var navigatedBack = false
        composeRule.setContent {
            MaterialTheme {
                InvalidRouteScreen(
                    argumentName = "companionId",
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }

        composeRule.onNodeWithText("返回").performClick()

        assertTrue(navigatedBack)
    }
}

package com.companion.cc.ui.character

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CharacterCustomizationScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stepIndicator_announcesCurrentStepAndTotal() {
        composeRule.setContent {
            MaterialTheme {
                StepIndicator(
                    steps = listOf("基本信息", "人格设定", "行为风格", "示例对话"),
                    currentStep = 2
                )
            }
        }

        composeRule.onNodeWithContentDescription("行为风格，第 3 步，共 4 步，当前步骤")
            .assertIsDisplayed()
    }

    @Test
    fun stepIndicator_allowsReturningToVisitedStepOnly() {
        val selected = mutableListOf<Int>()
        composeRule.setContent {
            MaterialTheme {
                StepIndicator(
                    steps = listOf("基本信息", "人格设定", "行为风格", "示例对话"),
                    currentStep = 2,
                    onStepClick = { selected += it }
                )
            }
        }

        composeRule.onNodeWithText("人格设定").performClick()
        composeRule.onNodeWithText("示例对话").performClick()

        assertEquals(listOf(1), selected)
    }
}

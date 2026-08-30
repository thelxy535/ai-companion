package com.companion.cc.ui.favorites

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesContentComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun content_removeFavoriteForwardsTheMessageId() {
        var removedId: String? = null
        val message = Message(
            id = "favorite-1",
            userId = "user-1",
            companionId = "companion-1",
            role = MessageRole.ASSISTANT,
            content = "值得珍藏的消息",
            timestamp = 1L,
            isFavorited = true
        )

        composeRule.setContent {
            MaterialTheme {
                FavoritesContent(
                    state = FavoritesUiState.Content(listOf(message)),
                    onRemoveFavorite = { removedId = it }
                )
            }
        }

        composeRule.onNodeWithText("移出珍藏").assertIsDisplayed().performClick()
        assertEquals("favorite-1", removedId)
    }
}

package com.companion.cc.ui.character

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.companion.cc.domain.model.ChatCharacter
import org.junit.Rule
import org.junit.Test

class CharacterAvatarComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun characterRow_rendersPersistedImageAvatarThroughSharedAvatarComponent() {
        composeRule.setContent {
            MaterialTheme {
                CharacterRow(
                    character = ChatCharacter.Custom(
                        id = "custom-1",
                        name = "Moss",
                        avatar = "content://avatar/1",
                        description = "quiet",
                        personality = "calm",
                        userId = "user-1"
                    ),
                    onStartChat = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("头像").assertIsDisplayed()
    }
}

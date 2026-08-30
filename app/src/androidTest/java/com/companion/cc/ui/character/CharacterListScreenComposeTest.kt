package com.companion.cc.ui.character

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.companion.cc.domain.model.ChatCharacter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CharacterListScreenComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun characterRow_editAndDeleteAreIndependentFromChat() {
        var chats = 0
        var edits = 0
        var deletes = 0
        composeRule.setContent {
            MaterialTheme {
                CharacterRow(
                    character = ChatCharacter.Custom(
                        id = "custom-1",
                        name = "Moss",
                        avatar = null,
                        description = "quiet",
                        personality = "calm",
                        userId = "user-1"
                    ),
                    onStartChat = { chats++ },
                    onEdit = { edits++ },
                    onDelete = { deletes++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("编辑").performClick()
        composeRule.onNodeWithContentDescription("删除").performClick()
        composeRule.onNodeWithText("取消").performClick()

        assertEquals(0, chats)
        assertEquals(1, edits)
        assertEquals(0, deletes)
    }

    @Test
    fun characterRow_deleteConfirmation_invokesDeleteOnce() {
        var deletes = 0
        composeRule.setContent {
            MaterialTheme {
                CharacterRow(
                    character = ChatCharacter.Custom(
                        id = "custom-1",
                        name = "Moss",
                        avatar = null,
                        description = "quiet",
                        personality = "calm",
                        userId = "user-1"
                    ),
                    onStartChat = {},
                    onEdit = {},
                    onDelete = { deletes++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("删除").performClick()
        composeRule.onNodeWithText("删除").performClick()

        assertEquals(1, deletes)
    }

    @Test
    fun characterRow_exposesExplicitStartChatAction() {
        var chats = 0
        composeRule.setContent {
            MaterialTheme {
                CharacterRow(
                    character = ChatCharacter.Custom(
                        id = "custom-1",
                        name = "Moss",
                        avatar = null,
                        description = "quiet",
                        personality = "calm",
                        userId = "user-1"
                    ),
                    onStartChat = { chats++ },
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText("开始对话").assertIsDisplayed().performClick()
        assertEquals(1, chats)
    }

    @Test
    fun emptyState_createButton_isVisibleAndActivatesCallback() {
        var creates = 0
        composeRule.setContent {
            MaterialTheme {
                EmptyState(onCreateCharacter = { creates++ })
            }
        }

        composeRule.onNodeWithText("创建角色").assertIsDisplayed().performClick()
        assertEquals(1, creates)
    }
}

package com.companion.cc.ui.companion

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.MessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun requestedCharacterUsesCurrentUserAndExactMessageScope() = runTest(main.dispatcher) {
        val catalog = mock<CharacterCatalog>()
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        val settings = mock<SettingsManager>()
        val character = ChatCharacter.BuiltIn("character-1", "小灿", "🌸", "描述")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(catalog.getCharacter("character-1")).thenReturn(character)
        whenever(repository.getMessages("user-1", "character-1", Int.MAX_VALUE, 0)).thenReturn(
            flowOf(listOf(message("message-1", "character-1", 1)))
        )
        whenever(settings.getCompanionAvatarFlow("character-1")).thenReturn(flowOf("content://avatar"))
        val viewModel = CompanionDetailViewModel(catalog, users, repository, settings)

        viewModel.setCompanion("character-1")
        advanceUntilIdle()

        verify(repository).getMessages("user-1", "character-1", Int.MAX_VALUE, 0)
        val state = viewModel.uiState.value
        assertTrue(state is CompanionDetailUiState.Content)
        state as CompanionDetailUiState.Content
        assertEquals(character, state.character)
        assertEquals("content://avatar", state.avatar)
        assertEquals(1, state.messageCount)
        assertEquals(1L, state.firstMetTimestamp)
    }

    @Test
    fun unknownCharacterEmitsMissingWithoutReadingMessages() = runTest(main.dispatcher) {
        val catalog = mock<CharacterCatalog>()
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        val settings = mock<SettingsManager>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(catalog.getCharacter("deleted-character")).thenReturn(null)
        val viewModel = CompanionDetailViewModel(catalog, users, repository, settings)

        viewModel.setCompanion("deleted-character")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CompanionDetailUiState.Missing)
        verify(repository, org.mockito.kotlin.never()).getMessages(any(), any(), any(), any())
    }

    @Test
    fun saveAvatarTargetsTheSelectedCharacter() = runTest(main.dispatcher) {
        val catalog = mock<CharacterCatalog>()
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        val settings = mock<SettingsManager>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(catalog.getCharacter("character-1")).thenReturn(
            ChatCharacter.BuiltIn("character-1", "小灿", "🌸", "描述")
        )
        whenever(repository.getMessages("user-1", "character-1", Int.MAX_VALUE, 0)).thenReturn(flowOf(emptyList()))
        whenever(settings.getCompanionAvatarFlow("character-1")).thenReturn(flowOf(null))
        val viewModel = CompanionDetailViewModel(catalog, users, repository, settings)
        viewModel.setCompanion("character-1")
        advanceUntilIdle()

        viewModel.saveAvatar("content://new-avatar")
        advanceUntilIdle()

        verify(settings).saveCompanionAvatar("character-1", "content://new-avatar")
    }

    private fun message(id: String, companionId: String, timestamp: Long) = Message(
        id = id,
        userId = "user-1",
        companionId = companionId,
        role = MessageRole.USER,
        content = "hello",
        timestamp = timestamp
    )
}

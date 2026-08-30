package com.companion.cc.ui.favorites

import com.companion.cc.MainDispatcherRule
import com.companion.cc.domain.identity.CurrentUserProvider
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun filtersCurrentUsersFavoritesToSelectedCharacter() = runTest(main.dispatcher) {
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getFavoritedMessages("user-1")).thenReturn(flowOf(
            listOf(message("wanted", "character-1"), message("other", "character-2"))
        ))
        val viewModel = FavoritesViewModel(users, repository)

        viewModel.setCompanion("character-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FavoritesUiState.Content)
        assertEquals(listOf("wanted"), (state as FavoritesUiState.Content).messages.map { it.id })
    }

    @Test
    fun emptyFavoritesEmitEmptyStateAfterLoading() = runTest(main.dispatcher) {
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getFavoritedMessages("user-1")).thenReturn(flowOf(emptyList()))
        val viewModel = FavoritesViewModel(users, repository)

        viewModel.setCompanion("character-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is FavoritesUiState.Empty)
    }

    @Test
    fun removeFavoriteDelegatesToRepository() = runTest(main.dispatcher) {
        val users = mock<CurrentUserProvider>()
        val repository = mock<MessageRepository>()
        val viewModel = FavoritesViewModel(users, repository)

        viewModel.removeFavorite("message-1")
        advanceUntilIdle()

        verify(repository).toggleMessageFavorite("message-1", false)
    }

    private fun message(id: String, companionId: String) = Message(
        id = id,
        userId = "user-1",
        companionId = companionId,
        role = MessageRole.USER,
        content = id,
        timestamp = 1L,
        isFavorited = true
    )
}

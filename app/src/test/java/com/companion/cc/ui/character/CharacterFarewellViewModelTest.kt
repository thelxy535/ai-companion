package com.companion.cc.ui.character

import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.MainDispatcherRule
import com.companion.cc.domain.character.CharacterDeletionResult
import com.companion.cc.domain.character.CharacterDeletionService
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.ChatCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterFarewellViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var deletionService: CharacterDeletionService
    private lateinit var catalog: CharacterCatalog
    private lateinit var currentUserProvider: CurrentUserProvider
    private lateinit var viewModel: CharacterFarewellViewModel

    @Before
    fun setUp() {
        deletionService = mock()
        catalog = mock()
        currentUserProvider = mock()
        viewModel = CharacterFarewellViewModel(
            deletionService = deletionService,
            characterCatalog = catalog,
            currentUserProvider = currentUserProvider
        )
    }

    @Test
    fun loadingAnOwnedCustomCharacterExposesReadyState() = runTest {
        val character = customCharacter()
        whenever(catalog.getCharacter("character-1")).thenReturn(character)

        viewModel.load("character-1")
        advanceUntilIdle()

        assertEquals(CharacterFarewellState.Ready(character), viewModel.state.value)
    }

    @Test
    fun confirmingDeletionCallsServiceForCurrentUserAndExposesDeletedState() = runTest {
        val character = customCharacter()
        whenever(catalog.getCharacter("character-1")).thenReturn(character)
        whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
        val result = CharacterDeletionResult("capsule-1", "token-1")
        whenever(deletionService.delete("user-1", "character-1"))
            .thenReturn(Result.success(result))

        viewModel.load("character-1")
        advanceUntilIdle()
        viewModel.confirmDeletion()
        advanceUntilIdle()

        verify(deletionService).delete(eq("user-1"), eq("character-1"))
        assertEquals(CharacterFarewellState.Deleted(result), viewModel.state.value)
    }

    @Test
    fun builtInOrMissingCharacterCannotBeDeleted() = runTest {
        whenever(catalog.getCharacter("built-in")).thenReturn(
            ChatCharacter.BuiltIn("built-in", "Muse", null, "built-in")
        )

        viewModel.load("built-in")
        advanceUntilIdle()
        viewModel.confirmDeletion()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is CharacterFarewellState.Failure)
        verify(deletionService, never()).delete(any(), any())
    }

    private fun customCharacter() = ChatCharacter.Custom(
        id = "character-1",
        name = "Moss",
        avatar = null,
        description = "quiet",
        personality = "calm",
        userId = "user-1"
    )
}

package com.companion.cc.ui.character

import com.companion.cc.MainDispatcherRule
import com.companion.cc.domain.character.CharacterRevivalMode
import com.companion.cc.domain.character.CharacterRevivalService
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterRevivalViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun successfulRevivalUsesCurrentUserAndSelectedMode() = runTest(main.dispatcher) {
        val service = mock<CharacterRevivalService>()
        val users = mock<CurrentUserProvider>()
        val character = character("new-character")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(service.revive("user-1", "token-1", CharacterRevivalMode.WITH_MEMORIES))
            .thenReturn(Result.success(character))
        val viewModel = CharacterRevivalViewModel(service, users)

        viewModel.revive("token-1", CharacterRevivalMode.WITH_MEMORIES)
        advanceUntilIdle()

        verify(service).revive("user-1", "token-1", CharacterRevivalMode.WITH_MEMORIES)
        assertEquals(CharacterRevivalState.Success(character), viewModel.state.value)
    }

    @Test
    fun invalidTokenIsExposedAndDoesNotReportSuccess() = runTest(main.dispatcher) {
        val service = mock<CharacterRevivalService>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(service.revive("user-1", "bad-token", CharacterRevivalMode.REINTRODUCTION))
            .thenReturn(Result.failure(IllegalArgumentException("Capsule token is invalid")))
        val viewModel = CharacterRevivalViewModel(service, users)

        viewModel.revive("bad-token", CharacterRevivalMode.REINTRODUCTION)
        advanceUntilIdle()

        assertEquals(
            CharacterRevivalState.Failure("恢复失败: Capsule token is invalid"),
            viewModel.state.value
        )
    }

    private fun character(id: String) = CustomCharacter(
        id = id,
        userId = "user-1",
        name = "Moss",
        avatar = null,
        description = "quiet",
        personality = PersonalityTraits.default(),
        backstory = "story",
        greetingMessage = "hello",
        exampleDialogues = emptyList(),
        voiceConfig = null,
        behaviorRules = null
    )
}

package com.companion.cc.ui.home

import com.companion.cc.MainDispatcherRule
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.manager.OnlineStatusManager
import com.companion.cc.domain.model.ChatCharacter
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.PersonalityTraits
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `home emits built in and custom rows sorted by latest message`() = runTest(main.dispatcher) {
        val catalog = mock<CharacterCatalog>()
        val users = mock<CurrentUserProvider>()
        val messages = mock<MessageRepository>()
        val onlineStatus = mock<OnlineStatusManager>()

        whenever(users.userId).thenReturn(flowOf("user-real"))
        whenever(onlineStatus.onlineCompanions).thenReturn(MutableStateFlow(emptySet()))
        whenever(catalog.observeCharacters()).thenReturn(
            flowOf(
                listOf(
                    TestCharacters.customChat("custom-1"),
                    TestCharacters.muse()
                )
            )
        )
        whenever(messages.observeLatestMessage("user-real", "muse"))
            .thenReturn(flowOf(TestMessages.message("muse", timestamp = 10L)))
        whenever(messages.observeLatestMessage("user-real", "custom-1"))
            .thenReturn(flowOf(TestMessages.message("custom-1", timestamp = 20L)))

        val settings = mock<com.companion.cc.data.local.SettingsManager>()
        whenever(settings.conversationReadAtFlow("user-real", "muse")).thenReturn(flowOf(0L))
        whenever(settings.conversationReadAtFlow("user-real", "custom-1")).thenReturn(flowOf(0L))
        val vm = HomeViewModel(messages, users, onlineStatus, catalog, settings)

        // uiState 使用 WhileSubscribed，需要有订阅者才会开始收集
        val job = vm.uiState.launchIn(this)
        advanceUntilIdle()
        job.cancel()

        val state = vm.uiState.value
        assert(state is HomeUiState.Content) { "Expected Content but got ${state::class.simpleName}" }
        val items = (state as HomeUiState.Content).items
        assertEquals(listOf("custom-1", "muse"), items.map { it.character.id })
    }

    @Test
    fun `home moves a character to the top when its latest message changes`() = runTest(main.dispatcher) {
        val catalog = mock<CharacterCatalog>()
        val users = mock<CurrentUserProvider>()
        val messages = mock<MessageRepository>()
        val onlineStatus = mock<OnlineStatusManager>()
        val museMessages = MutableStateFlow<Message?>(TestMessages.message("muse", timestamp = 10L))
        val customMessages = MutableStateFlow<Message?>(TestMessages.message("custom-1", timestamp = 20L))

        whenever(users.userId).thenReturn(flowOf("user-real"))
        whenever(onlineStatus.onlineCompanions).thenReturn(MutableStateFlow(emptySet()))
        whenever(catalog.observeCharacters()).thenReturn(
            flowOf(listOf(TestCharacters.muse(), TestCharacters.customChat("custom-1")))
        )
        whenever(messages.observeLatestMessage("user-real", "muse")).thenReturn(museMessages)
        whenever(messages.observeLatestMessage("user-real", "custom-1")).thenReturn(customMessages)

        val settings = mock<com.companion.cc.data.local.SettingsManager>()
        whenever(settings.conversationReadAtFlow("user-real", "muse")).thenReturn(flowOf(0L))
        whenever(settings.conversationReadAtFlow("user-real", "custom-1")).thenReturn(flowOf(0L))
        val vm = HomeViewModel(messages, users, onlineStatus, catalog, settings)
        val job = vm.uiState.launchIn(this)
        advanceUntilIdle()

        assertEquals(
            listOf("custom-1", "muse"),
            (vm.uiState.value as HomeUiState.Content).items.map { it.character.id }
        )

        museMessages.value = TestMessages.message("muse", timestamp = 30L)
        advanceUntilIdle()

        assertEquals(
            listOf("muse", "custom-1"),
            (vm.uiState.value as HomeUiState.Content).items.map { it.character.id }
        )
        job.cancel()
    }

    object TestCharacters {
        fun muse() = ChatCharacter.BuiltIn(
            id = "muse",
            name = "Muse",
            avatar = "🎨",
            description = "Creative companion"
        )

        fun customChat(id: String) = ChatCharacter.Custom(
            id = id,
            name = "Custom $id",
            avatar = null,
            description = "Test character",
            personality = "friendly, helpful",
            userId = "user-real"
        )
    }

    object TestMessages {
        fun message(companionId: String, timestamp: Long) = Message(
            id = "msg-$companionId",
            userId = "user-real",
            companionId = companionId,
            role = MessageRole.ASSISTANT,
            content = "Test message",
            timestamp = timestamp
        )
    }
}

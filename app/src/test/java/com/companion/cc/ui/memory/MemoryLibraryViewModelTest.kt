package com.companion.cc.ui.memory

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryLibraryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun emptyFilterUsesAllMemoryKinds() {
        assertEquals("", MemoryLibraryViewModel.normalizeKind("全部"))
    }

    @Test
    fun archiveFilterReadsDoNotRecallNodesOnlyInCurrentCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        val scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.observeNodes(scopeKey, status = "active")).thenReturn(flowOf(emptyList()))
        whenever(repository.observeNodes(scopeKey, status = "do_not_recall")).thenReturn(flowOf(emptyList()))

        val viewModel = MemoryLibraryViewModel(repository, users)
        viewModel.setCompanion("character-1")
        advanceUntilIdle()
        viewModel.setStatus("已禁止召回")
        advanceUntilIdle()

        verify(repository).observeNodes(scopeKey, status = "do_not_recall")
    }
}

package com.companion.cc.ui.memory

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.entity.MemoryRetrievalTraceEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryRetrievalTraceViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadUsesCurrentUserAndCharacterScopeAndParsesSelectedMemories() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        val scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getRetrievalTrace(scopeKey, "trace-1")).thenReturn(
            MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = scopeKey,
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\",\"node-2\"]",
                explanationJson = "[\"lexical=2.0\",\"recency=1.0\"]",
                createdAt = 1L,
                durationMs = 12L
            )
        )

        val viewModel = MemoryRetrievalTraceViewModel(repository, users)
        viewModel.load("character-1", "trace-1")
        advanceUntilIdle()

        verify(repository).getRetrievalTrace(scopeKey, "trace-1")
        assertEquals("tea", viewModel.state.query)
        assertEquals(
            listOf(
                MemoryRetrievalTraceSelection("node-1", "lexical=2.0"),
                MemoryRetrievalTraceSelection("node-2", "recency=1.0")
            ),
            viewModel.state.selections
        )
    }

    @Test
    fun submitFeedbackUsesLoadedTraceAndCurrentScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        val scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getRetrievalTrace(scopeKey, "trace-1")).thenReturn(
            MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = scopeKey,
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"lexical=2.0\"]",
                createdAt = 1L,
                durationMs = 12L
            )
        )
        whenever(
            repository.recordFeedback(
                scopeKey,
                com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                    traceId = "trace-1",
                    nodeId = "node-1",
                    feedback = "positive",
                    createdAt = 2L
                )
            )
        ).thenReturn(Result.success(Unit))

        val viewModel = MemoryRetrievalTraceViewModel(repository, users)
        viewModel.load("character-1", "trace-1")
        advanceUntilIdle()
        viewModel.submitFeedback("node-1", "positive", now = 2L)
        advanceUntilIdle()

        verify(repository).recordFeedback(
            scopeKey,
            com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                traceId = "trace-1",
                nodeId = "node-1",
                feedback = "positive",
                createdAt = 2L
            )
        )
    }

    @Test
    fun failedFeedbackKeepsTraceVisibleAndExposesActionError() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        val scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getRetrievalTrace(scopeKey, "trace-1")).thenReturn(
            MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = scopeKey,
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"lexical=2.0\"]",
                createdAt = 1L,
                durationMs = 12L
            )
        )
        whenever(
            repository.recordFeedback(
                scopeKey,
                com.companion.cc.data.local.entity.MemoryRetrievalFeedbackEntity(
                    traceId = "trace-1",
                    nodeId = "node-1",
                    feedback = "negative",
                    createdAt = 2L
                )
            )
        ).thenReturn(Result.failure(IllegalStateException("write failed")))

        val viewModel = MemoryRetrievalTraceViewModel(repository, users)
        viewModel.load("character-1", "trace-1")
        advanceUntilIdle()
        viewModel.submitFeedback("node-1", "negative", now = 2L)
        advanceUntilIdle()

        assertEquals("write failed", viewModel.actionError)
        assertEquals("trace-1", viewModel.state.traceId)
    }

    @Test
    fun submitFeedbackRejectsNodeMissingFromLoadedTrace() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        val scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1")
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.getRetrievalTrace(scopeKey, "trace-1")).thenReturn(
            MemoryRetrievalTraceEntity(
                id = "trace-1",
                scopeKey = scopeKey,
                query = "tea",
                selectedNodeIdsJson = "[\"node-1\"]",
                explanationJson = "[\"lexical=2.0\"]",
                createdAt = 1L,
                durationMs = 12L
            )
        )
        whenever(repository.recordFeedback(any(), any())).thenReturn(Result.success(Unit))

        val viewModel = MemoryRetrievalTraceViewModel(repository, users)
        viewModel.load("character-1", "trace-1")
        advanceUntilIdle()
        viewModel.submitFeedback("node-2", "negative", now = 2L)
        advanceUntilIdle()

        verify(repository, never()).recordFeedback(any(), any())
    }
}

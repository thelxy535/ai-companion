package com.companion.cc.ui.memory

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadUsesCurrentUserAndCharacterScopeBeforeReadingNode() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.findNode(MemoryScopeKey.forCharacter("user-1", "character-1"), "node-1"))
            .thenReturn(null)

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()

        verify(repository).findNode(
            MemoryScopeKey.forCharacter("user-1", "character-1"),
            "node-1"
        )
        assertNull(viewModel.state.node)
    }

    @Test
    fun suppressNodeUsesCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.suppressNode(node.scopeKey, node.id, 2L)).thenReturn(Result.success(Unit))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()
        viewModel.suppressNode(now = 2L)
        advanceUntilIdle()

        verify(repository).suppressNode(node.scopeKey, node.id, 2L)
    }

    @Test
    fun restoreNodeUsesCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node(status = "do_not_recall")
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.restoreNode(node.scopeKey, node.id, 3L)).thenReturn(Result.success(Unit))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()
        viewModel.restoreNode(now = 3L)
        advanceUntilIdle()

        verify(repository).restoreNode(node.scopeKey, node.id, 3L)
    }

    @Test
    fun failedNodeActionExposesRetryableErrorWithoutClearingDetail() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.suppressNode(node.scopeKey, node.id, 4L))
            .thenReturn(Result.failure(IllegalStateException("write failed")))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()
        viewModel.suppressNode(now = 4L)
        advanceUntilIdle()

        assertEquals("write failed", viewModel.actionError)
        assertEquals(node, viewModel.state.node)
    }

    @Test
    fun loadIncludesRelationsOnlyFromCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        val relation = MemoryRelationEntity(
            fromNodeId = node.id,
            toNodeId = "node-2",
            relationType = "supports",
            scopeKey = node.scopeKey,
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getRelations(node.scopeKey, node.id)).thenReturn(listOf(relation))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()

        verify(repository).getRelations(node.scopeKey, node.id)
        assertEquals(listOf(relation), viewModel.state.relations)
    }

    @Test
    fun resolveRelationUsesCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        val relation = MemoryRelationEntity(
            fromNodeId = node.id,
            toNodeId = "node-2",
            relationType = "supports",
            scopeKey = node.scopeKey,
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getRelations(node.scopeKey, node.id)).thenReturn(listOf(relation))
        whenever(repository.resolveRelation(node.scopeKey, relation, accepted = true, now = 5L))
            .thenReturn(Result.success(Unit))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()
        viewModel.resolveRelation(relation, accepted = true, now = 5L)
        advanceUntilIdle()

        verify(repository, times(1)).resolveRelation(node.scopeKey, relation, accepted = true, now = 5L)
    }

    @Test
    fun failedRelationResolutionPreservesScopedDetailWithoutReloading() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        val relation = MemoryRelationEntity(
            fromNodeId = node.id,
            toNodeId = "node-2",
            relationType = "supports",
            scopeKey = node.scopeKey,
            status = "proposed",
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getRelations(node.scopeKey, node.id)).thenReturn(listOf(relation))
        whenever(repository.resolveRelation(node.scopeKey, relation, accepted = true, now = 6L))
            .thenReturn(Result.failure(IllegalStateException("relation write failed")))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", node.id)
        advanceUntilIdle()
        viewModel.resolveRelation(relation, accepted = true, now = 6L)
        advanceUntilIdle()

        assertEquals("relation write failed", viewModel.actionError)
        assertEquals(node, viewModel.state.node)
        assertEquals(listOf(relation), viewModel.state.relations)
        verify(repository).resolveRelation(node.scopeKey, relation, accepted = true, now = 6L)
        verify(repository, times(1)).findNode(node.scopeKey, node.id)
    }

    @Test
    fun restoreVersionUsesCurrentUserAndCharacterScope() = runTest(main.dispatcher) {
        val repository = mock<MemoryRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        val node = node()
        whenever(repository.findNode(node.scopeKey, node.id)).thenReturn(node)
        whenever(repository.getEvidence(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.getVersions(node.scopeKey, node.id)).thenReturn(emptyList())
        whenever(repository.restoreVersion(node.scopeKey, node.id, 1, 4L)).thenReturn(Result.success(Unit))

        val viewModel = MemoryDetailViewModel(repository, users)
        viewModel.load("character-1", "node-1")
        advanceUntilIdle()
        viewModel.restoreVersion(1, now = 4L)
        advanceUntilIdle()

        verify(repository).restoreVersion(node.scopeKey, node.id, 1, 4L)
    }

    private fun node(status: String = "active") = MemoryNodeEntity(
        id = "node-1",
        scopeKey = MemoryScopeKey.forCharacter("user-1", "character-1"),
        kind = "preference",
        subjectRole = "user",
        subjectKey = "user-1",
        title = "Tea",
        content = "Likes tea",
        status = status,
        validFrom = 1L,
        createdAt = 1L,
        updatedAt = 1L
    )
}

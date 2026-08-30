package com.companion.cc.ui.character

import com.companion.cc.MainDispatcherRule
import com.companion.cc.data.local.entity.CharacterMemoryCapsuleEntity
import com.companion.cc.domain.character.CharacterMemoryCapsuleRepository
import com.companion.cc.domain.identity.CurrentUserProvider
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
class MemoryCapsuleVaultViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadsOnlyCurrentUsersCapsuleSummaries() = runTest(main.dispatcher) {
        val repository = mock<CharacterMemoryCapsuleRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenReturn("user-1")
        whenever(repository.observe("user-1")).thenReturn(
            flowOf(listOf(capsule("capsule-1", "user-1")))
        )
        val viewModel = MemoryCapsuleVaultViewModel(repository, users)

        viewModel.load()
        advanceUntilIdle()

        verify(repository).observe("user-1")
        assertEquals(
            listOf(CapsuleSummary("capsule-1", "character-1", 10L, "available")),
            (viewModel.state.value as MemoryCapsuleVaultState.Ready).capsules
        )
    }

    @Test
    fun userLookupFailureIsExposedWithoutShowingCapsules() = runTest(main.dispatcher) {
        val repository = mock<CharacterMemoryCapsuleRepository>()
        val users = mock<CurrentUserProvider>()
        whenever(users.requireUserId()).thenThrow(IllegalStateException("signed out"))
        val viewModel = MemoryCapsuleVaultViewModel(repository, users)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(emptyList<CapsuleSummary>(), viewModel.visibleCapsules.value)
        assertEquals(MemoryCapsuleVaultState.Failure("加载胶囊失败: signed out"), viewModel.state.value)
    }

    private fun capsule(id: String, userId: String) = CharacterMemoryCapsuleEntity(
        id = id,
        userId = userId,
        sourceCharacterId = "character-1",
        schemaVersion = 1,
        revivalTokenHash = "hash-$id",
        encryptedRevivalToken = "encrypted-token",
        encryptedPayload = "encrypted-payload",
        status = "available",
        createdAt = 10L,
        deletedAt = 11L
    )
}

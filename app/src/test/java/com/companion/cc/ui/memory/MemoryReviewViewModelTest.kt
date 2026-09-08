package com.companion.cc.ui.memory

import com.companion.cc.data.local.entity.MemoryReviewEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemoryReviewViewModelTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun acceptUsesActiveUserCharacterScope() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = mock<MemoryRepository>()
            val currentUserProvider = mock<CurrentUserProvider>()
            whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
            whenever(repository.observePendingReviews(any())).thenReturn(flowOf(emptyList()))
            whenever(
                repository.acceptReview(
                    scopeKey = any(),
                    reviewId = any(),
                    subjectRole = any(),
                    subjectKey = any(),
                    now = any()
                )
            ).thenReturn(Result.success("node:review"))
            val viewModel = MemoryReviewViewModel(
                repository,
                currentUserProvider,
                mock<com.companion.cc.domain.commitment.CommitmentRepository>()
            )
            val review = MemoryReviewEntity(
                id = "review-1",
                scopeKey = "user:user-1:companion:character-1",
                kind = "preference",
                title = "喜欢茶",
                content = "用户喜欢茶",
                confidence = 0.8,
                proposalHash = "hash-1",
                createdAt = 1L
            )

            viewModel.setCompanion("character-1")
            advanceUntilIdle()
            viewModel.accept(review)
            advanceUntilIdle()

            verify(repository).acceptReview(
                eq("user:user-1:companion:character-1"),
                eq(review.id),
                eq("user"),
                eq("user"),
                any()
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun userScopedScopeIncludesBothIdentityParts() {
        assertEquals(
            "user:user-1:companion:character-1",
            MemoryReviewViewModel.scopeFor("user-1", "character-1")
        )
    }
}

package com.companion.cc.domain.character

import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CharacterCleanupWorkRunnerTest {
    private lateinit var currentUserProvider: CurrentUserProvider
    private lateinit var runner: CharacterCleanupWorkRunner
    private var cleanupResult = CharacterCleanupResult(0, 0)

    @Before
    fun setUp() {
        currentUserProvider = mock()
        runner = CharacterCleanupWorkRunner(
            currentUserProvider
        ) { cleanupResult }
        whenever(currentUserProvider.userId).thenReturn(flowOf("user-1"))
    }

    @Test
    fun completeCleanupReturnsSuccess() = runTest {
        whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
        cleanupResult = CharacterCleanupResult(processedCount = 1, failedCount = 0)

        assertEquals(
            CharacterCleanupWorkResult.Success,
            runner.run()
        )
    }

    @Test
    fun failedCleanupReturnsRetry() = runTest {
        whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
        cleanupResult = CharacterCleanupResult(processedCount = 1, failedCount = 1)

        assertEquals(
            CharacterCleanupWorkResult.Retry,
            runner.run()
        )
    }

    @Test
    fun missingUserReturnsRetry() = runTest {
        whenever(currentUserProvider.requireUserId())
            .thenThrow(IllegalStateException("missing user"))

        assertEquals(
            CharacterCleanupWorkResult.Retry,
            runner.run()
        )
    }
}

package com.companion.cc.domain.character

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CharacterCleanupWorkerTest {
    private lateinit var currentUserProvider: CurrentUserProvider
    private lateinit var processor: CharacterCleanupProcessor

    @Before
    fun setUp() {
        currentUserProvider = mock()
        processor = mock()
        whenever(currentUserProvider.userId).thenReturn(emptyFlow())
    }

    @Test
    fun successfulProcessorResultCompletesWorker() = runTest {
        whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
        whenever(processor.processPending("user-1")).thenReturn(
            CharacterCleanupResult(processedCount = 1, failedCount = 0)
        )

        val result = buildWorker().startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun failedProcessorResultRequestsRetry() = runTest {
        whenever(currentUserProvider.requireUserId()).thenReturn("user-1")
        whenever(processor.processPending("user-1")).thenReturn(
            CharacterCleanupResult(processedCount = 1, failedCount = 1)
        )

        val result = buildWorker().startWork().get()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun providerFailureRequestsRetry() = runTest {
        whenever(currentUserProvider.requireUserId())
            .thenThrow(IllegalStateException("missing user"))

        val result = buildWorker().startWork().get()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private fun buildWorker(): CharacterCleanupWorker =
        TestListenableWorkerBuilder<CharacterCleanupWorker>(
            RuntimeEnvironment.getApplication()
        )
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? = CharacterCleanupWorker(
                    appContext,
                    workerParameters,
                    currentUserProvider,
                    processor
                )
            })
            .setId(UUID.randomUUID())
            .build()
}

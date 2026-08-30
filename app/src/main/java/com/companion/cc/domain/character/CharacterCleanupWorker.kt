package com.companion.cc.domain.character

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.domain.identity.CurrentUserProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CharacterCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val cleanupProcessor: CharacterCleanupProcessor
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val runner = CharacterCleanupWorkRunner(
            currentUserProvider = currentUserProvider,
            processPending = cleanupProcessor::processPending
        )
        return when (runner.run()) {
            CharacterCleanupWorkResult.Success -> Result.success()
            CharacterCleanupWorkResult.Retry -> Result.retry()
        }
    }
}

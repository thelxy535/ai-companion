package com.companion.cc

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.companion.cc.domain.character.CharacterCleanupProcessor
import com.companion.cc.domain.character.CharacterCleanupWorker
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.message.ProactiveMessageWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CCApplication : Application(), Configuration.Provider {
    @Inject lateinit var currentUserProvider: CurrentUserProvider
    @Inject lateinit var cleanupProcessor: CharacterCleanupProcessor
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enqueueCleanupWork()
        enqueueProactiveWork()
        applicationScope.launch {
            runCatching { currentUserProvider.requireUserId() }
                .getOrNull()
                ?.let { userId -> cleanupProcessor.processPending(userId) }
        }
    }

    private fun enqueueCleanupWork() {
        val request = OneTimeWorkRequestBuilder<CharacterCleanupWorker>()
            .setConstraints(Constraints.Builder().build())
            .setInitialDelay(CLEANUP_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                CLEANUP_BACKOFF_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            CLEANUP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun enqueueProactiveWork() {
        // V9PM 主动消息：15min 粒度周期检查（时段窗口/静默/频率门控在 Worker 内）
        val request = PeriodicWorkRequestBuilder<ProactiveMessageWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PROACTIVE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val CLEANUP_WORK_NAME = "character-cleanup"
        const val CLEANUP_INITIAL_DELAY_MINUTES = 15L
        const val CLEANUP_BACKOFF_MINUTES = 15L
        const val PROACTIVE_WORK_NAME = "proactive-message"
    }
}

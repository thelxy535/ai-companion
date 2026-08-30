package com.companion.cc.domain.schedule

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.domain.identity.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val scheduleRepository: ScheduleRepository,
    private val actionExecutor: ScheduleActionExecutor
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val characterId = inputData.getString(KEY_CHARACTER_ID) ?: return Result.failure()
        val userId = try {
            currentUserProvider.requireUserId()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return Result.retry()
        }
        val now = System.currentTimeMillis()
        val runner = ScheduleWorkRunner(
            loadDue = { scheduleRepository.due(userId, characterId, it) },
            execute = actionExecutor::execute,
            markCompleted = scheduleRepository::update
        )
        return when (runner.run(now)) {
            ScheduleWorkResult.SUCCESS -> Result.success()
            ScheduleWorkResult.RETRY -> Result.retry()
        }
    }

    companion object {
        const val KEY_CHARACTER_ID = "character_id"
    }
}

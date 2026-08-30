package com.companion.cc.domain.character

import com.companion.cc.data.local.dao.CharacterCleanupTaskDao
import com.companion.cc.data.local.entity.CharacterCleanupTaskEntity

fun interface CharacterExternalCleanup {
    suspend fun cleanup(task: CharacterCleanupTaskEntity)
}

data class CharacterCleanupResult(
    val processedCount: Int,
    val failedCount: Int
) {
    val isComplete: Boolean get() = failedCount == 0
}

class CharacterCleanupProcessor(
    private val dao: CharacterCleanupTaskDao,
    private val cleaner: CharacterExternalCleanup
) {
    suspend fun processPending(
        userId: String,
        now: Long = System.currentTimeMillis()
    ): CharacterCleanupResult {
        var processedCount = 0
        var failedCount = 0
        dao.findPending(userId).forEach { task ->
            val result = runCatching { cleaner.cleanup(task) }
            processedCount += 1
            if (result.isFailure) failedCount += 1
            dao.update(
                task.copy(
                    status = if (result.isSuccess) "completed" else "pending",
                    attempts = task.attempts + 1,
                    lastError = result.exceptionOrNull()?.message,
                    updatedAt = now
                )
            )
        }
        return CharacterCleanupResult(processedCount, failedCount)
    }
}

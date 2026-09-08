package com.companion.cc.data.local.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.companion.cc.data.local.dao.ReflectionJobDao
import com.companion.cc.data.local.entity.ReflectionJobEntity
import com.companion.cc.domain.memory.ReflectionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Enqueues one recoverable reflection job per source cursor and policy version. */
class ReflectionJobScheduler @Inject constructor(
    private val jobDao: ReflectionJobDao,
    @ApplicationContext private val context: Context?
) {
    constructor(jobDao: ReflectionJobDao) : this(jobDao, null)

    suspend fun enqueue(
        scopeKey: String,
        sourceCursor: String,
        trigger: String = "conversation_completed",
        policyVersion: String = POLICY_VERSION,
        now: Long = System.currentTimeMillis(),
        delayMs: Long = DEFAULT_DELAY_MS
    ): ReflectionJobEntity {
        require(scopeKey.isNotBlank()) { "scopeKey must not be blank" }
        require(sourceCursor.isNotBlank()) { "sourceCursor must not be blank" }
        require(policyVersion.isNotBlank()) { "policyVersion must not be blank" }

        val idempotencyKey = "$scopeKey|$sourceCursor|$policyVersion"
        val id = "reflection:${sha256(idempotencyKey)}"
        val job = ReflectionJobEntity(
            id = id,
            scopeKey = scopeKey,
            trigger = trigger,
            sourceCursor = sourceCursor,
            nextRunAt = now + delayMs.coerceAtLeast(0L),
            idempotencyKey = idempotencyKey,
            createdAt = now,
            updatedAt = now
        )
        jobDao.insert(job)
        context?.let { appContext ->
            val request = OneTimeWorkRequestBuilder<ReflectionWorker>()
                .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("scopeKey" to scopeKey))
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "memory-reflection:$scopeKey",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
        return job
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val POLICY_VERSION = "memory-reflection-v1"
        const val DEFAULT_DELAY_MS = 30_000L
    }
}

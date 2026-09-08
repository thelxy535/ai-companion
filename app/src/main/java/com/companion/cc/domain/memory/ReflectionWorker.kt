package com.companion.cc.domain.memory

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.dao.ReflectionJobDao
import com.companion.cc.data.local.entity.ReflectionJobEntity
import com.companion.cc.data.local.repository.ReflectionProposalWriter
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.repository.CustomCharacterRepository
import com.companion.cc.data.local.dao.CustomCharacterDao
import com.companion.cc.data.mapper.CustomCharacterMapper
import com.companion.cc.util.NotificationHelper
import com.companion.cc.domain.identity.CurrentUserProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class ReflectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val jobDao: ReflectionJobDao,
    private val sourceDao: MemorySourceDao,
    private val reviewDao: com.companion.cc.data.local.dao.MemoryReviewDao,
    private val customCharacterDao: CustomCharacterDao,
    private val llmClient: ReflectionLlmClient,
    private val proposalWriter: ReflectionProposalWriter,
    private val memoryRepository: MemoryRepository,
    private val characterRepository: CustomCharacterRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            ?: return Result.success()
        val owner = "worker:${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val requestedScope = inputData.getString("scopeKey")
        val scopes = if (requestedScope != null && MemoryScopeKey.belongsToUser(requestedScope, userId)) {
            listOf(requestedScope)
        } else {
            sourceDao.findAllInScopePrefix("user:$userId:companion:%")
                .map { it.scopeKey }
                .distinct()
        }
        var retryableFailure = false
        scopes.forEach scopeLoop@{ scopeKey ->
            val jobs = jobDao.findDue(scopeKey, now, MAX_JOBS_PER_RUN)
            jobs.forEach { job ->
                if (jobDao.claim(job.id, scopeKey, owner, now + LEASE_MS, now) == 0) return@scopeLoop
                runCatching {
                    val cursorSource = sourceDao.findByIdForScope(scopeKey, job.sourceCursor)
                        ?: error("找不到反思来源")
                    // 对话级批量反思：取该消息及之前最近 WINDOW_SIZE 条来源，跨轮次整体回味
                    val window = sourceDao.findRecentBefore(scopeKey, cursorSource.occurredAt, WINDOW_SIZE)
                    val transcript = window.asReversed().joinToString("\n\n") { src ->
                        "[${windowTimeFormat.format(src.occurredAt)}] ${src.contentSnapshot}"
                    }
                    val output = ReflectionResponseParser
                        .parse(llmClient.reflect(ReflectionPromptBuilder.build(transcript)))
                        .getOrThrow()
                    // 证据链覆盖整个窗口
                    val writtenReviews = proposalWriter.write(scopeKey, window.map { it.id }, output.candidates, now)
                    autoCaptureAllowedReviews(scopeKey, userId, writtenReviews)
                    notifyInbox(scopeKey, userId, now)
                    check(jobDao.markCompleted(job.id, scopeKey, owner, System.currentTimeMillis()) == 1)
                    // 同批合并：本次窗口已覆盖的同 scope 其他在途任务直接完成，避免重复反思
                    jobs.filter { it.id != job.id }.forEach { covered ->
                        if (jobDao.claim(covered.id, scopeKey, owner, now + LEASE_MS, now) == 1) {
                            jobDao.markCompleted(covered.id, scopeKey, owner, System.currentTimeMillis())
                        }
                    }
                }.onFailure { error ->
                    val transient = error is java.io.IOException || error is retrofit2.HttpException
                    if (transient) {
                        retryableFailure = true
                        jobDao.requeue(job.id, scopeKey, owner, now + RETRY_DELAY_MS, error.message, System.currentTimeMillis())
                    } else {
                        jobDao.markFailed(job.id, scopeKey, owner, error.message, System.currentTimeMillis())
                    }
                }
            }
        }
        return if (retryableFailure) Result.retry() else Result.success()
    }

    private suspend fun autoCaptureAllowedReviews(
        scopeKey: String,
        userId: String,
        reviews: List<com.companion.cc.data.local.entity.MemoryReviewEntity>
    ) {
        val characterId = scopeKey.substringAfter("companion:", "")
        val preference = MemoryCapturePreference.from(
            characterRepository.getCharacterByIdForUser(characterId, userId)?.personality?.customTraits.orEmpty()
        )
        if (preference == MemoryCapturePreference.ASK_FIRST || preference == MemoryCapturePreference.LOW_DISTURBANCE) return
        reviews.filter { it.status == "pending" && preference.mayAutoCapture(it.kind, it.confidence) }
            .forEach { review ->
                runCatching {
                    memoryRepository.acceptReview(scopeKey, review.id, "user", "user").getOrThrow()
                }
            }
    }

    private suspend fun notifyInbox(scopeKey: String, userId: String, now: Long) {
        runCatching {
            val characterId = scopeKey.substringAfter("companion:", "")
            val character = customCharacterDao.getCharacterByIdForUser(characterId, userId)
                ?.let(CustomCharacterMapper::toDomain)
            val preference = MemoryCapturePreference.from(character?.personality?.customTraits.orEmpty())
            val pending = reviewDao.countPending(scopeKey)
            NotificationHelper.sendMemoryInboxNotification(
                context = applicationContext,
                scopeKey = scopeKey,
                pendingCount = pending,
                threshold = preference.notificationThreshold,
                cooldownMs = preference.notificationCooldownMs,
                now = now
            )
        }
    }

    private val windowTimeFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)

    private companion object {
        const val MAX_JOBS_PER_RUN = 4
        const val WINDOW_SIZE = 10
        const val LEASE_MS = 5 * 60 * 1000L
        const val RETRY_DELAY_MS = 15 * 60 * 1000L
    }
}

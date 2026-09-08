package com.companion.cc.domain.commitment

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.companion.cc.data.local.entity.ScheduleEntity
import com.companion.cc.domain.schedule.ScheduleRepository
import com.companion.cc.domain.schedule.ScheduleWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 承诺与目标系统（V9PM 第 5 项）。
 *
 * 承诺 = 角色对用户作出的、可在未来执行的约定（"明天提醒你""我帮你查天气"）。
 * 复用现有 schedules 表承载（recurrence=ONCE），避免新表；创建时精确排队到期的
 * ScheduleWorker，执行后标记 fulfilled，形成复盘闭环。
 */
@Singleton
class CommitmentRepository @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    @ApplicationContext private val context: Context?
) {
    /** 测试用：无 Android 环境时跳过 WorkManager 排队。 */
    constructor(scheduleRepository: ScheduleRepository) : this(scheduleRepository, null)

    data class CommitmentView(
        val id: String,
        val userId: String,
        val characterId: String,
        val promise: String,
        val dueAt: Long,
        val status: String,
        val createdAt: Long
    )

    suspend fun create(
        userId: String,
        characterId: String,
        promise: String,
        dueAt: Long,
        now: Long = System.currentTimeMillis()
    ): String {
        require(promise.isNotBlank()) { "promise must not be blank" }
        require(dueAt > now) { "dueAt must be in the future" }
        val id = "commitment:${UUID.randomUUID()}"
        scheduleRepository.create(
            ScheduleEntity(
                id = id,
                userId = userId,
                characterId = characterId,
                prompt = "【承诺】$promise",
                recurrence = "ONCE",
                nextRunAt = dueAt,
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )
        )
        // V9PM 可靠性修复：精确排队到期的 ScheduleWorker——没有这一步承诺到期永远无人执行
        context?.let { appContext ->
            val request = OneTimeWorkRequestBuilder<ScheduleWorker>()
                .setInitialDelay((dueAt - now).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ScheduleWorker.KEY_CHARACTER_ID to characterId))
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "commitment:$id",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
        return id
    }

    /** 读取当前角色的全部承诺（含已完成）。 */
    suspend fun forCharacter(userId: String, characterId: String): List<CommitmentView> =
        scheduleRepository.observe(userId, characterId)
            .first()
            .filter { it.prompt.startsWith(PROMPT_PREFIX) }
            .map { it.toView() }

    /** 已到期待执行的承诺。 */
    suspend fun due(userId: String, characterId: String, now: Long): List<CommitmentView> =
        scheduleRepository.due(userId, characterId, now)
            .filter { it.prompt.startsWith(PROMPT_PREFIX) }
            .map { it.toView() }

    /** 标记承诺已兑现（到期提醒已发出/用户确认完成）。 */
    suspend fun markFulfilled(userId: String, characterId: String, id: String, now: Long = System.currentTimeMillis()) {
        val current = scheduleRepository.observe(userId, characterId)
            .first()
            .firstOrNull { it.id == id } ?: return
        scheduleRepository.update(
            current.copy(status = "FULFILLED", nextRunAt = current.nextRunAt, updatedAt = now)
        )
    }

    /** 标记承诺未兑现（到期未执行/用户未响应）。 */
    suspend fun markMissed(userId: String, characterId: String, id: String, now: Long = System.currentTimeMillis()) {
        val current = scheduleRepository.observe(userId, characterId)
            .first()
            .firstOrNull { it.id == id } ?: return
        scheduleRepository.update(
            current.copy(status = "MISSED", nextRunAt = current.nextRunAt, updatedAt = now)
        )
    }

    private fun ScheduleEntity.toView() = CommitmentView(
        id = id,
        userId = userId,
        characterId = characterId,
        promise = prompt.removePrefix(PROMPT_PREFIX),
        dueAt = nextRunAt,
        status = status,
        createdAt = createdAt
    )

    private companion object {
        const val PROMPT_PREFIX = "【承诺】"
    }
}

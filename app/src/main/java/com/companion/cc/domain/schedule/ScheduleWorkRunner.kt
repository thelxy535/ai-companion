package com.companion.cc.domain.schedule

import com.companion.cc.data.local.entity.ScheduleEntity
import kotlinx.coroutines.CancellationException

enum class ScheduleWorkResult { SUCCESS, RETRY }

class ScheduleWorkRunner(
    private val loadDue: suspend (Long) -> List<ScheduleEntity>,
    private val execute: suspend (ScheduleEntity) -> Unit,
    private val markCompleted: suspend (ScheduleEntity) -> Unit
) {
    suspend fun run(now: Long): ScheduleWorkResult {
        return try {
            loadDue(now).forEach { schedule ->
                execute(schedule)
                // 承诺类（prompt 以【承诺】开头）执行后标记 FULFILLED，其余沿用原语义
                val status = when {
                    schedule.prompt.startsWith("【承诺】") -> "FULFILLED"
                    schedule.recurrence == "ONCE" -> "COMPLETED"
                    else -> "ACTIVE"
                }
                val nextRunAt = if (schedule.recurrence == "DAILY") {
                    maxOf(schedule.nextRunAt, now) + 86_400_000L
                } else {
                    schedule.nextRunAt
                }
                markCompleted(schedule.copy(status = status, nextRunAt = nextRunAt, updatedAt = now))
            }
            ScheduleWorkResult.SUCCESS
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ScheduleWorkResult.RETRY
        }
    }
}

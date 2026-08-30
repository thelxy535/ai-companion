package com.companion.cc.domain.schedule

enum class ScheduleStatus { ACTIVE, PAUSED, COMPLETED, DELETED }

data class ScheduleState(
    val id: String,
    val active: Boolean,
    val status: ScheduleStatus = if (active) ScheduleStatus.ACTIVE else ScheduleStatus.PAUSED
) {
    fun pause(): ScheduleState = copy(active = false, status = ScheduleStatus.PAUSED)
    fun resume(): ScheduleState = copy(active = true, status = ScheduleStatus.ACTIVE)
    fun delete(): ScheduleState = copy(active = false, status = ScheduleStatus.DELETED)
}

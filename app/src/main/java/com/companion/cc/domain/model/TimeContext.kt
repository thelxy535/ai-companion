package com.companion.cc.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

/**
 * 时间上下文
 *
 * 记录对话的时间维度信息
 */
data class TimeContext(
    val currentTime: LocalDateTime,           // 当前时间
    val lastInteractionTime: LocalDateTime?,  // 上次对话时间
    val intervalDuration: Duration?,          // 距离上次对话的时长
    val todayInteractionCount: Int,           // 今天的对话次数
    val weeklyInteractionCount: Int,          // 本周的对话次数
    val timePeriod: TimePeriod,              // 时间段（凌晨、早上、上午等）
    val dayOfWeek: DayOfWeek,                // 星期几
    val isWeekend: Boolean                   // 是否周末
) {
    /**
     * 生成人类可读的时间上下文描述
     */
    fun toHumanReadable(): String {
        val sb = StringBuilder()

        // 时间间隔描述
        if (lastInteractionTime != null && intervalDuration != null) {
            sb.append("距离上次对话：${formatDuration(intervalDuration)}\n")
        }

        // 对话频率
        sb.append("今天第${todayInteractionCount}次对话")
        if (weeklyInteractionCount > todayInteractionCount) {
            sb.append("，本周第${weeklyInteractionCount}次对话")
        }

        return sb.toString()
    }

    /**
     * 格式化时长为人类可读格式
     */
    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds

        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
            else -> "${seconds / 86400}天${(seconds % 86400) / 3600}小时"
        }
    }

    companion object {
        /**
         * 创建默认的时间上下文（首次对话）
         */
        fun initial(currentTime: LocalDateTime): TimeContext {
            return TimeContext(
                currentTime = currentTime,
                lastInteractionTime = null,
                intervalDuration = null,
                todayInteractionCount = 1,
                weeklyInteractionCount = 1,
                timePeriod = TimePeriod.fromHour(currentTime.hour),
                dayOfWeek = currentTime.dayOfWeek,
                isWeekend = currentTime.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            )
        }
    }
}

/**
 * 时间段枚举
 */
enum class TimePeriod(val displayName: String) {
    LATE_NIGHT("凌晨"),    // 0-5
    MORNING("早上"),       // 6-8
    FORENOON("上午"),      // 9-11
    NOON("中午"),          // 12-13
    AFTERNOON("下午"),     // 14-17
    EVENING("傍晚"),       // 18-19
    NIGHT("晚上");         // 20-23

    companion object {
        /**
         * 根据小时数获取时间段
         */
        fun fromHour(hour: Int): TimePeriod {
            return when (hour) {
                in 0..5 -> LATE_NIGHT
                in 6..8 -> MORNING
                in 9..11 -> FORENOON
                in 12..13 -> NOON
                in 14..17 -> AFTERNOON
                in 18..19 -> EVENING
                in 20..23 -> NIGHT
                else -> NIGHT
            }
        }
    }
}

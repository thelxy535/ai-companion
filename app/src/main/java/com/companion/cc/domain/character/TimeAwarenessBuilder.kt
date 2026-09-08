package com.companion.cc.domain.character

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * 时间感注入（V9PM 深度③）。
 *
 * 把"现在几点、距上次聊天多久、有什么未完成的约定"渲染成紧凑的
 * Prompt 区块，让所有回复入口共享同一份时间上下文。无可用信息时返回 null。
 */
object TimeAwarenessBuilder {

    fun build(
        nowMs: Long,
        lastChatAtMs: Long?,
        commitment: String?,
        commitmentDueAtMs: Long?
    ): String? {
        val now = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
        val lines = mutableListOf(
            "现在是${now.monthValue}月${now.dayOfMonth}日 ${dayOfWeekCn(now.dayOfWeek)} " +
                String.format("%02d:%02d", now.hour, now.minute)
        )
        if (lastChatAtMs != null && lastChatAtMs > 0) {
            val gapMs = (nowMs - lastChatAtMs).coerceAtLeast(0L)
            lines.add("距离上次聊天已过 ${humanizeGap(gapMs)}")
            if (gapMs < 30 * 60_000L) {
                lines.add("仍是同一段聊天：默认延续刚才的动作、位置、穿着和正在做的事，不要自行换场")
            }
        }
        if (!commitment.isNullOrBlank() && commitmentDueAtMs != null && commitmentDueAtMs > nowMs) {
            val due = Instant.ofEpochMilli(commitmentDueAtMs).atZone(ZoneId.systemDefault())
            lines.add("有个未完成的约定：${commitment.trim()}（约定在${due.monthValue}月${due.dayOfMonth}日）")
        }
        if (lines.size == 1) return null
        return "\n\n## 时间感知\n" + lines.joinToString("\n") { "- $it" }
    }

    fun humanizeGap(gapMs: Long): String {
        val minutes = (gapMs / 60_000L).coerceAtLeast(0L)
        return when {
            minutes < 1 -> "不到一分钟"
            minutes < 60 -> "$minutes 分钟"
            minutes < 1440 -> "${minutes / 60} 小时"
            else -> "${minutes / 1440} 天"
        }
    }

    private fun dayOfWeekCn(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

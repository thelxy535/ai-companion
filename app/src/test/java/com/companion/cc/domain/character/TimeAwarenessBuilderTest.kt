package com.companion.cc.domain.character

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimeAwarenessBuilderTest {
    private fun msOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `block contains current time weekday and chat gap`() {
        val now = msOf(2026, 9, 5, 21, 30) // 周六
        val block = TimeAwarenessBuilder.build(now, lastChatAtMs = now - 2 * 3_600_000L, commitment = null, commitmentDueAtMs = null)
        assertTrue(block!!.contains("9月5日 周六 21:30"))
        assertTrue(block.contains("2 小时"))
    }

    @Test
    fun `gap humanizes minutes hours days`() {
        assertEquals("不到一分钟", TimeAwarenessBuilder.humanizeGap(30_000L))
        assertEquals("5 分钟", TimeAwarenessBuilder.humanizeGap(5 * 60_000L))
        assertEquals("3 小时", TimeAwarenessBuilder.humanizeGap(3 * 3_600_000L))
        assertEquals("2 天", TimeAwarenessBuilder.humanizeGap(2 * 86_400_000L))
    }

    @Test
    fun `short gap marks the same conversation scene`() {
        val now = msOf(2026, 9, 5, 21, 30)
        val block = TimeAwarenessBuilder.build(
            nowMs = now,
            lastChatAtMs = now - 5 * 60_000L,
            commitment = null,
            commitmentDueAtMs = null
        )

        assertTrue(block!!.contains("仍是同一段聊天"))
    }

    @Test
    fun `open commitment is included with due date`() {
        val now = msOf(2026, 9, 5, 12, 0)
        val block = TimeAwarenessBuilder.build(
            nowMs = now,
            lastChatAtMs = now - 60_000L,
            commitment = "提醒用户早点睡",
            commitmentDueAtMs = now + 86_400_000L
        )
        assertTrue(block!!.contains("提醒用户早点睡"))
        assertTrue(block.contains("9月6日"))
    }

    @Test
    fun `returns null when there is nothing to say`() {
        val now = msOf(2026, 9, 5, 12, 0)
        assertNull(TimeAwarenessBuilder.build(now, lastChatAtMs = null, commitment = null, commitmentDueAtMs = null))
        // 刚聊过且无约定 → 只有当前时间一行也视为无增益？当前实现保留时间行
    }

    @Test
    fun `past commitments are not included`() {
        val now = msOf(2026, 9, 5, 12, 0)
        val block = TimeAwarenessBuilder.build(
            nowMs = now,
            lastChatAtMs = null,
            commitment = "过期约定",
            commitmentDueAtMs = now - 1000L
        )
        // 只有当前时间一行 → 不产生区块
        assertNull(block)
    }
}

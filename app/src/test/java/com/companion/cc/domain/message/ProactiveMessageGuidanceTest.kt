package com.companion.cc.domain.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveMessageGuidanceTest {

    @Test
    fun `proactive messages feel like a passing thought instead of a task`() {
        val guidance = ProactiveMessageGuidance.build(
            motivation = "刚想到一件小事，想跟你分享",
            interruptionCost = 0.8f,
            continuity = "窗边下着雨，刚好想起上次一起买热饮"
        )

        assertTrue(guidance.contains("生活里刚冒出来的一点念头"))
        assertTrue(guidance.contains("不要像提醒、打卡或任务通知"))
        assertTrue(guidance.contains("可以随时不回复"))
    }

    @Test
    fun `proactive guidance does not force a generic greeting`() {
        val guidance = ProactiveMessageGuidance.build(
            motivation = "想跟你说说话",
            interruptionCost = 0.2f,
            continuity = ""
        )

        assertFalse(guidance.contains("必须先说早安"))
        assertTrue(guidance.contains("具体"))
    }
}

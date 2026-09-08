package com.companion.cc.domain.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProactiveTopicFingerprintTest {
    @Test
    fun `similar proactive lines share a topic fingerprint`() {
        assertEquals(
            ProactiveTopicFingerprint.of("刚想到晚饭，不知道你今天吃什么"),
            ProactiveTopicFingerprint.of("突然好奇你今晚会吃什么")
        )
    }

    @Test
    fun `different life fragments do not collapse into one topic`() {
        assertNotEquals(
            ProactiveTopicFingerprint.of("窗外下雨了，想起你"),
            ProactiveTopicFingerprint.of("刚看到一家很有意思的小店")
        )
    }
}

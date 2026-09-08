package com.companion.cc.domain.character

import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContinuityGuardTest {

    @Test
    fun `short greeting gets a no-invention guard`() {
        val guard = ConversationContinuityGuard.forQuery("hi")

        assertTrue(guard.contains("不要凭空引入新的喜好"))
        assertTrue(guard.contains("只回应当前问候"))
    }

    @Test
    fun `substantive message does not get greeting guard`() {
        assertTrue(ConversationContinuityGuard.forQuery("我今天吃了火锅").isBlank())
    }
}

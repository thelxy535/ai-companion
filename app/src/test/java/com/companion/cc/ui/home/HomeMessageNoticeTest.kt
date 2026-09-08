package com.companion.cc.ui.home

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMessageNoticeTest {
    private val proactive = Message("123_proactive", "u", "c", MessageRole.ASSISTANT, "你好", 1L)
    private val normal = proactive.copy(id = "123_assistant")
    private val userMessage = proactive.copy(id = "123_user", role = MessageRole.USER)

    @Test
    fun `any unread assistant message gets quiet notice`() {
        assertTrue(hasUnreadAssistantMessage(proactive, 0L))
        assertTrue(hasUnreadAssistantMessage(normal, 0L))
        assertFalse(hasUnreadAssistantMessage(normal, normal.timestamp))
        assertFalse(hasUnreadAssistantMessage(normal, normal.timestamp + 1L))
        assertFalse(hasUnreadAssistantMessage(userMessage, 0L))
        assertFalse(hasUnreadAssistantMessage(null, 0L))
    }
}

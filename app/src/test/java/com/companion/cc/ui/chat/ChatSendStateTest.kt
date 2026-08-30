package com.companion.cc.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSendStateTest {
    @Test
    fun cancelledStatePreservesPartialResponse() {
        val state = ChatSendState.Cancelled("character-1", "已经收到一半")

        assertEquals("character-1", state.companionId)
        assertEquals("已经收到一半", state.partialResponse)
    }

    @Test
    fun retryableFailurePreservesPartialResponse() {
        val state = ChatSendState.RetryableFailure(
            companionId = "character-1",
            partialResponse = "部分回复",
            message = "网络超时"
        )

        assertEquals("部分回复", state.partialResponse)
        assertEquals("网络超时", state.message)
    }
}

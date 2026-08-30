package com.companion.cc.ui.chat

import com.companion.cc.ui.designsystem.AuroraChatTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTextTest {
    @Test
    fun `new chunks preserve already revealed prefix`() {
        val first = reconcileStreamingTarget(StreamingRevealState(), "hel", streaming = true)
        val revealed = revealNextCharacter(first)
        val next = reconcileStreamingTarget(revealed, "hello", streaming = true)

        assertEquals("h", next.displayed)
        assertEquals("hello", next.target)
        assertEquals("he", revealNextCharacter(next).displayed)
    }

    @Test
    fun `long streaming responses use immediate display policy`() {
        val target = "x".repeat(AuroraChatTokens.RevealMaxAnimatedChars + 1)
        val state = reconcileStreamingTarget(StreamingRevealState(), target, streaming = true)

        assertEquals(target, state.displayed)
        assertTrue(!state.complete)
    }

    @Test
    fun `completion displays final target`() {
        val state = reconcileStreamingTarget(
            StreamingRevealState(target = "partial", displayed = "part"),
            "partial",
            streaming = false,
        )

        assertEquals("partial", state.displayed)
        assertTrue(state.complete)
    }
}

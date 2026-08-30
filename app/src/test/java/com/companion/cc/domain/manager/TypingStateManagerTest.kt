package com.companion.cc.domain.manager

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TypingStateManagerTest {
    @Test
    fun `typing state survives across consumers of singleton manager`() = runTest {
        val manager = TypingStateManager()

        manager.startTyping("muse")

        assertEquals(setOf("muse"), manager.typingCharacters.first())
        assertEquals(true, manager.isTyping("muse"))

        manager.stopTyping("muse")

        assertEquals(emptySet<String>(), manager.typingCharacters.first())
        assertEquals(false, manager.isTyping("muse"))
    }

    @Test
    fun `multiple companions can type independently`() = runTest {
        val manager = TypingStateManager()

        manager.startTyping("muse")
        manager.startTyping("xiaocan")
        manager.stopTyping("muse")

        assertEquals(setOf("xiaocan"), manager.typingCharacters.first())
        assertEquals(false, manager.isTyping("muse"))
        assertEquals(true, manager.isTyping("xiaocan"))
    }
}

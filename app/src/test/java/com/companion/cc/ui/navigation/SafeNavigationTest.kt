package com.companion.cc.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeNavigationTest {
    @Test
    fun `duplicate destination navigation is ignored`() {
        assertFalse(shouldNavigateSafely("chat/character-1", "chat/character-1"))
        assertTrue(shouldNavigateSafely("home", "chat/character-1"))
    }
}

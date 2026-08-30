package com.companion.cc.ui.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterNavigationPolicyTest {
    @Test
    fun savingDisablesNavigation() {
        assertFalse(canNavigateWhileSaving(isSaving = true))
    }

    @Test
    fun idleAllowsNavigation() {
        assertTrue(canNavigateWhileSaving(isSaving = false))
    }
}

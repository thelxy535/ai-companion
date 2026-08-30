package com.companion.cc.ui.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterEditSavePolicyTest {
    @Test
    fun missingEditTargetCannotFallBackToCreate() {
        assertFalse(canSaveCharacter(editingCharacterId = "missing", hasLoadedCharacter = false))
    }

    @Test
    fun createFlowCanSaveWithoutEditTarget() {
        assertTrue(canSaveCharacter(editingCharacterId = null, hasLoadedCharacter = false))
    }

    @Test
    fun loadedEditTargetCanSave() {
        assertTrue(canSaveCharacter(editingCharacterId = "custom-1", hasLoadedCharacter = true))
    }
}

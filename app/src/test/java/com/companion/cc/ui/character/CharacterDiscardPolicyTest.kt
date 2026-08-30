package com.companion.cc.ui.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDiscardPolicyTest {
    @Test
    fun cleanFormDoesNotRequireConfirmation() {
        assertFalse(shouldConfirmDiscard(hasUnsavedChanges = false, isSaving = false))
    }

    @Test
    fun dirtyFormRequiresConfirmation() {
        assertTrue(shouldConfirmDiscard(hasUnsavedChanges = true, isSaving = false))
    }

    @Test
    fun savingFormDoesNotOfferDiscardNavigation() {
        assertFalse(shouldConfirmDiscard(hasUnsavedChanges = true, isSaving = true))
    }
}

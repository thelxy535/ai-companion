package com.companion.cc.ui.character

internal fun shouldConfirmDiscard(
    hasUnsavedChanges: Boolean,
    isSaving: Boolean
): Boolean = hasUnsavedChanges && !isSaving

package com.companion.cc.ui.character

import com.companion.cc.domain.model.CustomCharacter

internal fun canSaveCharacter(
    editingCharacterId: String?,
    hasLoadedCharacter: Boolean
): Boolean = editingCharacterId == null || hasLoadedCharacter

internal fun canSaveCharacter(
    editingCharacterId: String?,
    character: CustomCharacter?
): Boolean = canSaveCharacter(editingCharacterId, character != null)

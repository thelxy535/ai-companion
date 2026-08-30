package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryScopeKeyTest {
    @Test
    fun characterScopeIncludesUserAndCharacter() {
        assertEquals(
            "user:user-1:companion:character-1",
            MemoryScopeKey.forCharacter("user-1", "character-1")
        )
    }

    @Test
    fun userPrefixMatchesOnlyItsOwner() {
        assertEquals(true, MemoryScopeKey.belongsToUser("user:user-1:companion:character-1", "user-1"))
        assertEquals(false, MemoryScopeKey.belongsToUser("user:user-2:companion:character-1", "user-1"))
        assertEquals(false, MemoryScopeKey.belongsToUser("companion:character-1", "user-1"))
    }    @Test
    fun scopePartsAreTrimmedAndRejectBlankValues() {
        assertEquals(
            "user:user-1:companion:character-1",
            MemoryScopeKey.forCharacter(" user-1 ", " character-1 ")
        )
        val result = runCatching { MemoryScopeKey.forCharacter(" ", "character-1") }
        assertEquals(true, result.isFailure)
    }
}

package com.companion.cc.domain.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterMemoryCapsuleTokenTest {

    @Test
    fun generatedTokensAreNonBlankAndHashOnlyMatchesTheOriginalToken() {
        val first = CharacterMemoryCapsuleToken.generate()
        val second = CharacterMemoryCapsuleToken.generate()

        assertTrue(first.isNotBlank())
        assertNotEquals(first, second)
        val hash = CharacterMemoryCapsuleToken.hash(first)
        assertTrue(CharacterMemoryCapsuleToken.matches(first, hash))
        assertFalse(CharacterMemoryCapsuleToken.matches(second, hash))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTokenCannotBeHashed() {
        CharacterMemoryCapsuleToken.hash(" ")
    }
}

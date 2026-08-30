package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterAvatarResolverTest {
    @Test
    fun imageReferenceIsSeparatedFromEmojiAvatar() {
        val resolved = CharacterAvatarResolver.resolve("content://avatar/1", "Moss")

        assertEquals("content://avatar/1", resolved.avatarUrl)
        assertEquals("M", resolved.emoji)
    }

    @Test
    fun emojiAvatarRemainsFallbackInsteadOfBeingLoadedAsImage() {
        val resolved = CharacterAvatarResolver.resolve("🎨", "Muse")

        assertNull(resolved.avatarUrl)
        assertEquals("🎨", resolved.emoji)
    }
}

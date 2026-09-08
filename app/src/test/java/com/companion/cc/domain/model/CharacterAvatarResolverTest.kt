package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterAvatarResolverTest {
    @Test
    fun imageReferenceIsSeparatedFromEmojiAvatar() {
        val character = ChatCharacter.Custom(
            id = "preview",
            name = "Moss",
            avatar = "content://avatar/1",
            description = "",
            personality = "",
            userId = "user-1"
        )
        val resolved = CharacterAvatarResolver.resolve(character)

        assertEquals("content://avatar/1", resolved.avatarUrl)
        assertEquals("✨", resolved.emoji)
    }

    @Test
    fun emojiAvatarRemainsFallbackInsteadOfBeingLoadedAsImage() {
        val character = ChatCharacter.BuiltIn(
            id = "muse",
            name = "Muse",
            avatar = "🎨",
            description = "",
        )
        val resolved = CharacterAvatarResolver.resolve(character)

        assertNull(resolved.avatarUrl)
        assertEquals("🎭", resolved.emoji)
    }
}

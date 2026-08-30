package com.companion.cc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCharacterDisplayResolverTest {
    @Test
    fun builtInCharacterKeepsConfiguredDisplayMetadata() {
        val display = ChatCharacterDisplayResolver.resolve(
            requestedId = "muse",
            character = ChatCharacter.BuiltIn(
                id = "muse",
                name = "Configured Muse",
                avatar = "configured-muse.png",
                description = "Configured description"
            )
        )

        requireNotNull(display)
        assertEquals("muse", display.id)
        assertEquals("缪斯", display.name)
        assertEquals("🎭", display.emoji)
        assertEquals("🎭", display.avatarFallback)
    }

    @Test
    fun customCharacterUsesItsPersistedMetadataWithoutBuiltInFallback() {
        val display = ChatCharacterDisplayResolver.resolve(
            requestedId = "custom/with spaces",
            character = ChatCharacter.Custom(
                id = "custom/with spaces",
                name = "夜航员",
                avatar = "night.png",
                description = "自定义描述",
                personality = "calm",
                userId = "user-1"
            )
        )

        requireNotNull(display)
        assertEquals("custom/with spaces", display.id)
        assertEquals("夜航员", display.name)
        assertEquals("自定义描述", display.description)
        assertEquals("night.png", display.avatarUrl)
        assertEquals("✨", display.emoji)
        assertEquals("✨", display.avatarFallback)
    }

    @Test
    fun nullCharacterDoesNotResolveToXiaoChan() {
        val display = ChatCharacterDisplayResolver.resolve(
            requestedId = "missing",
            character = null
        )

        assertNull(display)
    }

    @Test
    fun characterForDifferentIdDoesNotResolve() {
        val display = ChatCharacterDisplayResolver.resolve(
            requestedId = "muse",
            character = ChatCharacter.BuiltIn(
                id = "xiaocan",
                name = "小璨",
                avatar = null,
                description = "description"
            )
        )

        assertNull(display)
    }
}

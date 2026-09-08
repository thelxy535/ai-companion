package com.companion.cc.data.character

import com.companion.cc.domain.model.BehaviorRules
import com.companion.cc.domain.model.CustomCharacter
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.PersonalityTraits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterTransferCodecTest {
    private val character = CustomCharacter(
        id = "local-only-id",
        userId = "local-user",
        name = "夜航",
        avatar = "data:image/png;base64,local-avatar",
        description = "会在夜里写信的角色",
        personality = PersonalityTraits.default().copy(customTraits = mapOf("习惯" to "先听完再回答")),
        backstory = "住在海边",
        greetingMessage = "今晚想聊点什么？",
        exampleDialogues = listOf(ExampleDialogue("我累了", "那就先靠一会儿。")),
        voiceConfig = null,
        behaviorRules = BehaviorRules.default(),
        scenario = "雨夜的窗边",
        tags = listOf("夜晚", "陪伴")
    )

    @Test
    fun `sylora json round trip preserves private character fields without ids`() {
        val encoded = CharacterTransferCodec.serialize(character, CharacterExportFormat.SYLORA_JSON)
        val decoded = CharacterTransferCodec.parse(encoded, "new-user").getOrThrow()

        assertEquals(character.name, decoded.name)
        assertEquals(character.avatar, decoded.avatar)
        assertEquals(character.personality.customTraits, decoded.personality.customTraits)
        assertEquals(character.exampleDialogues, decoded.exampleDialogues)
        assertEquals("new-user", decoded.userId)
        assertTrue(encoded.contains("sylora_character"))
        assertTrue(!encoded.contains("local-only-id"))
        assertTrue(!encoded.contains("local-user"))
    }

    @Test
    fun `markdown and text exports remain human readable`() {
        val markdown = CharacterTransferCodec.serialize(character, CharacterExportFormat.MARKDOWN)
        val text = CharacterTransferCodec.serialize(character, CharacterExportFormat.TEXT)

        assertTrue(markdown.contains("# 夜航"))
        assertTrue(markdown.contains("## Character Book").not())
        assertTrue(text.contains("角色名：夜航"))
        assertTrue(text.contains("场景：雨夜的窗边"))
    }

    @Test
    fun `imports markdown headings and bom`() {
        val markdown = "\uFEFF" + CharacterTransferCodec.serialize(character, CharacterExportFormat.MARKDOWN)
        val decoded = CharacterTransferCodec.parse(markdown, "markdown-user").getOrThrow()

        assertEquals("夜航", decoded.name)
        assertEquals("会在夜里写信的角色", decoded.description)
        assertEquals("住在海边", decoded.backstory)
        assertEquals("雨夜的窗边", decoded.scenario)
        assertEquals("今晚想聊点什么？", decoded.greetingMessage)
    }

    @Test
    fun `markdown import preserves custom humanization traits`() {
        val markdown = """
            # 夜航
            ## 描述
            会在夜里写信的角色
            ## 背景
            住在海边
            ## 人格
            - 语言习惯：先停一下再回答，熟悉后会轻轻吐槽
            - 冲突方式：先安静，认真回应后慢慢松动
        """.trimIndent()

        val decoded = CharacterTransferCodec.parse(markdown, "traits-user").getOrThrow()

        assertEquals("先停一下再回答，熟悉后会轻轻吐槽", decoded.personality.customTraits["语言习惯"])
        assertEquals("先安静，认真回应后慢慢松动", decoded.personality.customTraits["冲突方式"])
    }

    @Test
    fun `all supported formats can be imported after export`() {
        CharacterExportFormat.values().forEach { format ->
            val encoded = CharacterTransferCodec.serialize(character, format)
            val decoded = CharacterTransferCodec.parse(encoded, "round-trip-user").getOrThrow()

            assertEquals(format.name, character.name, decoded.name)
            assertEquals(format.name, "round-trip-user", decoded.userId)
        }
    }
}

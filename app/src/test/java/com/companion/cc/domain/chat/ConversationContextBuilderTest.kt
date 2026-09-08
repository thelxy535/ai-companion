package com.companion.cc.domain.chat

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextBuilderTest {
    @Test
    fun `latest user reply keeps the proactive assistant message before it`() {
        val oldMessages = (1..60).map { index ->
            Message("old-$index", "u", "c", MessageRole.USER, "old $index", index.toLong())
        }
        val messages = oldMessages + listOf(
            Message("proactive", "u", "c", MessageRole.ASSISTANT, "刚才突然想和你说说话", 61),
            Message("reply", "u", "c", MessageRole.USER, "你刚才想说什么？", 62)
        )

        val context = ConversationContextBuilder.build(messages, maxMessages = 4)
        assertTrue(context.any { it["content"] == "刚才突然想和你说说话" })
        assertTrue(context.any { it["content"] == "你刚才想说什么？" })
    }

    @Test
    fun `relevant older message is preferred over unrelated recent history`() {
        val messages = (1..30).map { index ->
            Message("m-$index", "u", "c", MessageRole.USER, "unrelated $index", index.toLong())
        } + Message("match", "u", "c", MessageRole.ASSISTANT, "我们之前聊过火锅", 1, importance = 50)

        val context = ConversationContextBuilder.build(
            messages = messages,
            maxMessages = 6,
            relevantMessageIds = setOf("match")
        )
        assertTrue(context.any { it["content"] == "我们之前聊过火锅" })
    }

    @Test
    fun `token budget removes long unrelated messages but keeps latest exchange`() {
        val messages = listOf(
            Message("old", "u", "c", MessageRole.USER, "很长的旧内容。".repeat(500), 1),
            Message("proactive", "u", "c", MessageRole.ASSISTANT, "我刚才想起你了", 2, origin = "proactive"),
            Message("reply", "u", "c", MessageRole.USER, "你怎么突然想起我？", 3, replyToId = "proactive")
        )

        val context = ConversationContextBuilder.build(messages, maxMessages = 10, maxCharacters = 12_000, maxTokens = 50)
        assertTrue(context.any { it["content"] == "我刚才想起你了" })
        assertTrue(context.any { it["content"] == "你怎么突然想起我？" })
    }
}

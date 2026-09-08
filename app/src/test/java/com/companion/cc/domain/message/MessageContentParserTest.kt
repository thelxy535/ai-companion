package com.companion.cc.domain.message

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageContentParserTest {

    @Test
    fun `action is separated from dialogue for proactive messages`() {
        val parsed = MessageContentParser.parse("哎，你来啦！\n\n[动作: 抬起头，朝你笑了一下]")

        assertEquals("哎，你来啦！", parsed.dialogue)
        assertEquals("抬起头，朝你笑了一下", parsed.action)
    }

    @Test
    fun `parenthesized action is also separated`() {
        val parsed = MessageContentParser.parse("你好（轻轻挥手）")

        assertEquals("你好", parsed.dialogue)
        assertEquals("轻轻挥手", parsed.action)
    }

    @Test
    fun `parentheses inside dialogue are preserved`() {
        val parsed = MessageContentParser.parse("我（其实有点紧张）还是来了。")

        assertEquals("我（其实有点紧张）还是来了。", parsed.dialogue)
        assertEquals(null, parsed.action)
    }

    @Test
    fun `standalone markdown action is separated`() {
        val parsed = MessageContentParser.parse("好呀\n\n*把杯子往你这边推了推*")

        assertEquals("好呀", parsed.dialogue)
        assertEquals("把杯子往你这边推了推", parsed.action)
    }

    @Test
    fun `leading markdown action is separated from dialogue`() {
        val parsed = MessageContentParser.parse("*轻轻笑了笑* 好呀，我在听。")

        assertEquals("好呀，我在听。", parsed.dialogue)
        assertEquals("轻轻笑了笑", parsed.action)
    }

    @Test
    fun `multiple action formats are normalized into one action line`() {
        val parsed = MessageContentParser.parse("[动作: 抬眼]\n你来了。\n（轻轻笑）")

        assertEquals("你来了。", parsed.dialogue)
        assertEquals("抬眼，轻轻笑", parsed.action)
    }

    @Test
    fun `incomplete action marker is hidden while streaming`() {
        val parsed = MessageContentParser.parse("好呀\n[动作: 把杯子")

        assertEquals("好呀", parsed.dialogue)
        assertEquals(null, parsed.action)
    }

    @Test
    fun `structured scene metadata is removed from dialogue and parsed`() {
        val parsed = MessageContentParser.parse(
            "我还在这儿。\n[动作: 把杯子放回桌上]\n[场景: 地点=家; 房间=卧室; 姿态=坐着; 穿着=睡衣; 手持=无]"
        )

        assertEquals("我还在这儿。", parsed.dialogue)
        assertEquals("把杯子放回桌上", parsed.action)
        assertEquals("家", parsed.scene?.location)
        assertEquals("卧室", parsed.scene?.room)
        assertEquals("睡衣", parsed.scene?.clothing)
        assertEquals("", parsed.scene?.heldItem)
    }

    @Test
    fun `incomplete scene metadata is hidden while streaming`() {
        val parsed = MessageContentParser.parse("还没呢。\n[场景: 地点=家; 房间=")

        assertEquals("还没呢。", parsed.dialogue)
        assertEquals(null, parsed.scene)
    }
}

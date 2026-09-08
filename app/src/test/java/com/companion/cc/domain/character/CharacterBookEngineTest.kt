package com.companion.cc.domain.character

import com.companion.cc.domain.model.CharacterBookEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterBookEngineTest {
    private val engine = CharacterBookEngine()

    private fun entry(
        keys: List<String>,
        content: String,
        constant: Boolean = false,
        priority: Int = 0,
        order: Int = 0
    ) = CharacterBookEntry(
        keys = keys,
        content = content,
        enabled = true,
        constant = constant,
        priority = priority,
        insertionOrder = order
    )

    @Test
    fun `keyword entry triggers on matching current message`() {
        val book = listOf(entry(listOf("生日"), "用户的生日是 5 月 20 日。"))
        val result = engine.buildInjection(book, "我生日快到了")
        assertTrue(result.block.contains("用户的生日是 5 月 20 日"))
        assertEquals(listOf("生日"), result.triggeredKeys)
    }

    @Test
    fun `non matching keyword does not inject`() {
        val book = listOf(entry(listOf("生日"), "用户的生日是 5 月 20 日。"))
        val result = engine.buildInjection(book, "今天天气不错")
        assertEquals("", result.block)
        assertTrue(result.triggeredKeys.isEmpty())
    }

    @Test
    fun `constant entries always inject regardless of query`() {
        val book = listOf(
            entry(listOf("x"), "常驻设定：永远不会向用户说谎。", constant = true),
            entry(listOf("生日"), "用户的生日是 5 月 20 日。")
        )
        val result = engine.buildInjection(book, "随便聊聊")
        assertTrue(result.block.contains("常驻设定"))
        assertFalse(result.block.contains("用户的生日"))
    }

    @Test
    fun `higher priority entries come first`() {
        val book = listOf(
            entry(listOf("猫"), "低优先级内容", priority = 1),
            entry(listOf("猫"), "高优先级内容", priority = 10)
        )
        val result = engine.buildInjection(book, "我的猫")
        val high = result.block.indexOf("高优先级内容")
        val low = result.block.indexOf("低优先级内容")
        assertTrue(high in 0 until low)
    }

    @Test
    fun `block length is bounded`() {
        val book = (1..200).map { i ->
            entry(listOf("key$i"), "内容条目编号 $i，" + "填充".repeat(30))
        }
        val result = engine.buildInjection(book, "key199 key200")
        assertTrue(result.block.length <= 1200)
    }

    @Test
    fun `hasConstantEntries detects constant presence`() {
        assertFalse(engine.hasConstantEntries(listOf(entry(listOf("a"), "普通"))))
        assertTrue(engine.hasConstantEntries(listOf(entry(listOf("a"), "常驻", constant = true))))
    }
}

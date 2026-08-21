package com.companion.cc.domain.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisionResponseParserTest {
    @Test
    fun `parses structured response including Chinese colons`() {
        val analysis = VisionResponseParser.parse(
            rawResponse = """
                主要对象：猫、窗台
                环境: 明亮的室内
                动作：趴着，晒太阳
                文字: 无
                氛围: 安静温暖
                语境理解：用户在分享宠物照片
            """.trimIndent(),
            confidence = 1.5f
        )

        assertEquals(listOf("猫", "窗台"), analysis.mainSubjects)
        assertEquals("明亮的室内", analysis.environment)
        assertEquals(listOf("趴着", "晒太阳"), analysis.actions)
        assertNull(analysis.textContent)
        assertEquals("安静温暖", analysis.mood)
        assertEquals("用户在分享宠物照片", analysis.contextualMeaning)
        assertEquals(1f, analysis.confidence)
    }

    @Test
    fun `does not treat a prefixed field name as structured data`() {
        val analysis = VisionResponseParser.parse(
            rawResponse = """
                环境污染: 无关内容
                主要对象: 无
                动作: 跑步, 跳跃
            """.trimIndent(),
            confidence = -0.1f
        )

        assertNull(analysis.environment)
        assertEquals(emptyList<String>(), analysis.mainSubjects)
        assertEquals(listOf("跑步", "跳跃"), analysis.actions)
        assertEquals(0f, analysis.confidence)
    }
}

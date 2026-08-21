package com.companion.cc.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TagColorAdapterTest {
    @Test
    fun `parses a valid persisted tag color without changing it`() {
        assertEquals(
            Color(0xFF4CAF50),
            TagColorAdapter.parse("#4CAF50", fallback = Color.Magenta)
        )
    }

    @Test
    fun `uses fallback for a malformed historical tag color`() {
        val fallback = Color(0xFF625CD2)
        assertEquals(fallback, TagColorAdapter.parse("invalid", fallback))
    }

    @Test
    fun `chooses readable foreground for light and dark tag colors`() {
        assertEquals(Color.Black, TagColorAdapter.contentColor(Color.White))
        assertEquals(Color.White, TagColorAdapter.contentColor(Color(0xFF202020)))
    }
}

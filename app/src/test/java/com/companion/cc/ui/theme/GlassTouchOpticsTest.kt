package com.companion.cc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.geometry.Offset

class GlassTouchOpticsTest {
    @Test
    fun `clamps touch to owning surface bounds`() {
        val point = GlassTouchOptics.clampPoint(
            x = -12f,
            y = 140f,
            width = 100f,
            height = 80f
        )

        assertEquals(0f, point.x, 0f)
        assertEquals(80f, point.y, 0f)
    }

    @Test
    fun `opposing shade stays inside surface and away from contact`() {
        val shade = GlassTouchOptics.opposingPoint(
            x = 20f,
            y = 20f,
            width = 100f,
            height = 80f
        )

        assertTrue(shade.x > 50f)
        assertTrue(shade.y > 40f)
        assertTrue(shade.x <= 100f)
        assertTrue(shade.y <= 80f)
    }

    @Test
    fun `edge proximity is strongest at edge and zero beyond response band`() {
        assertEquals(1f, GlassTouchOptics.edgeProximity(0f, 40f, 100f, 80f, 24f), 0f)
        assertEquals(0f, GlassTouchOptics.edgeProximity(50f, 40f, 100f, 80f, 24f), 0f)
    }

    @Test
    fun `replica offset keeps the touched surface aligned with the scene`() {
        val offset = GlassTouchOptics.replicaOffset(Offset(48.4f, 91.6f))

        assertEquals(-48, offset.x)
        assertEquals(-92, offset.y)
    }
}

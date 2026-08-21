package com.companion.cc.data.theme

import com.companion.cc.domain.model.BackdropKind
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualBackdrop
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.ui.theme.VisualEffectsPreference
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisualCustomizationCodecTest {
    private val codec = VisualCustomizationCodec(Gson())

    @Test
    fun `round trips a valid visual customization`() {
        val customization = VisualCustomization(
            accentHex = "#7A6FF0",
            effectsPreference = VisualEffectsPreference.ENHANCED,
            glassOpacity = 0.74f,
            backdrops = mapOf(
                BackdropTarget.CHAT to VisualBackdrop(
                    kind = BackdropKind.IMAGE,
                    managedImageReference = "file:/data/user/0/com.companion.cc/files/backdrops/chat.webp",
                    imageOpacity = 0.31f,
                    scrimOpacity = 0.42f
                )
            )
        )

        assertEquals(customization, codec.decode(codec.encode(customization)))
    }

    @Test
    fun `falls back to defaults for malformed JSON`() {
        assertEquals(
            VisualCustomization.default(),
            codec.decode("{not valid json")
        )
    }

    @Test
    fun `normalizes accent and clamps backdrop controls`() {
        val decoded = codec.decode(
            """
            {
              "accentHex":"#abc",
              "effectsPreference":"AUTO",
              "glassOpacity":2.0,
              "backdrops":{
                "CHAT":{
                  "kind":"IMAGE",
                  "managedImageReference":"file:/managed.webp",
                  "imageOpacity":2.0,
                  "scrimOpacity":-1.0
                }
              }
            }
            """.trimIndent()
        )

        assertEquals("#AABBCC", decoded.accentHex)
        assertEquals(0.88f, decoded.glassOpacity)
        val backdrop = requireNotNull(decoded.backdrops[BackdropTarget.CHAT])
        assertEquals(1f, backdrop.imageOpacity)
        assertEquals(0f, backdrop.scrimOpacity)
    }

    @Test
    fun `drops invalid accent and unknown enum values`() {
        val decoded = codec.decode(
            """
            {
              "accentHex":"not-a-color",
              "effectsPreference":"UNRECOGNIZED",
              "backdrops":{
                "CHAT":{"kind":"UNKNOWN","managedImageReference":"file:/managed.webp"}
              }
            }
            """.trimIndent()
        )

        assertNull(decoded.accentHex)
        assertEquals(VisualEffectsPreference.AUTO, decoded.effectsPreference)
        assertEquals(BackdropKind.NONE, decoded.backdrops[BackdropTarget.CHAT]?.kind)
    }
}

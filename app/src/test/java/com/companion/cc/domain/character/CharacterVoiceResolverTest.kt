package com.companion.cc.domain.character

import com.companion.cc.domain.model.VoiceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterVoiceResolverTest {
    @Test
    fun `null config falls back to defaults`() {
        assertEquals(1.0f, CharacterVoiceResolver.pitch(null))
        assertEquals(1.0f, CharacterVoiceResolver.speed(null))
    }

    @Test
    fun `config values are used`() {
        val cfg = VoiceConfig(pitch = 1.4f, speed = 0.8f)
        assertEquals(1.4f, CharacterVoiceResolver.pitch(cfg))
        assertEquals(0.8f, CharacterVoiceResolver.speed(cfg))
    }

    @Test
    fun `out of range values are clamped`() {
        val cfg = VoiceConfig(pitch = 5.0f, speed = 0.1f)
        assertEquals(2.0f, CharacterVoiceResolver.pitch(cfg))
        assertEquals(0.5f, CharacterVoiceResolver.speed(cfg))
    }
}

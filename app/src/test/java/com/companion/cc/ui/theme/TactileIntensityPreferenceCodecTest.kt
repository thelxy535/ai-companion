package com.companion.cc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TactileIntensityPreferenceCodecTest {
    @Test
    fun `unknown stored value falls back to system`() {
        assertEquals(
            TactileIntensityPreference.SYSTEM,
            TactileIntensityPreferenceCodec.fromStoredValue("unsupported")
        )
    }

    @Test
    fun `preferences round trip to stable stored values`() {
        TactileIntensityPreference.values().forEach { preference ->
            assertEquals(
                preference,
                TactileIntensityPreferenceCodec.fromStoredValue(
                    TactileIntensityPreferenceCodec.toStoredValue(preference)
                )
            )
        }
    }
}

package com.companion.cc.domain.character

import com.companion.cc.domain.model.PersonalityTraits
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperamentProfileTest {

    @Test
    fun `custom traits create a reserved temperament`() {
        val profile = TemperamentProfile.fromPersonality(
            PersonalityTraits.default().copy(
                extraversion = 0.25f,
                customTraits = mapOf("相处方式" to "慢热，安静，不喜欢被连续追问")
            )
        )

        assertTrue(profile.expressiveness < 0.5f)
        assertTrue(profile.interruptionCost > 0.5f)
        assertTrue(profile.shareImpulse < 0.5f)
    }

    @Test
    fun `custom traits create a sharing temperament`() {
        val profile = TemperamentProfile.fromPersonality(
            PersonalityTraits.default().copy(
                extraversion = 0.8f,
                openness = 0.85f,
                customTraits = mapOf("习惯" to "喜欢主动分享生活，想到什么就会说")
            )
        )

        assertTrue(profile.expressiveness > 0.65f)
        assertTrue(profile.shareImpulse > 0.65f)
        assertTrue(profile.interruptionCost < 0.5f)
    }
}

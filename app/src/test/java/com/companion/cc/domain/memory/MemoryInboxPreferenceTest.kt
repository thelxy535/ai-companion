package com.companion.cc.domain.memory

import com.companion.cc.domain.model.PersonalityTraits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryInboxPreferenceTest {
    @Test
    fun `unknown or missing preference defaults to ask first`() {
        assertEquals(MemoryCapturePreference.ASK_FIRST, MemoryCapturePreference.from(emptyMap()))
        assertEquals(MemoryCapturePreference.ASK_FIRST, MemoryCapturePreference.from(mapOf("记忆偏好" to "随便看看")))
    }

    @Test
    fun `low disturbance preference changes notification behavior without changing safety`() {
        val preference = MemoryCapturePreference.from(mapOf(PersonalityTraits.MEMORY_PREFERENCE_KEY to "低打扰模式"))
        assertEquals(MemoryCapturePreference.LOW_DISTURBANCE, preference)
        assertTrue(preference.notificationCooldownMs > MemoryCapturePreference.ASK_FIRST.notificationCooldownMs)
        assertFalse(preference.mayAutoCapture("commitment", 0.99))
    }

    @Test
    fun `automatic preference captures only high confidence ordinary memories`() {
        val preference = MemoryCapturePreference.from(mapOf(PersonalityTraits.MEMORY_PREFERENCE_KEY to "自动记住日常偏好"))
        assertTrue(preference.mayAutoCapture("preference", 0.95))
        assertTrue(preference.mayAutoCapture("fact", 0.95))
        assertFalse(preference.mayAutoCapture("preference", 0.7))
        assertFalse(preference.mayAutoCapture("relationship_narrative", 0.99))
    }
}

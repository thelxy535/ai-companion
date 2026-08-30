package com.companion.cc.domain.capability

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityGateTest {
    @Test
    fun `unsupported capability is rejected before provider request`() {
        val flags = CapabilityFlags(image = true, voice = false)

        assertTrue(CapabilityGate.allows(Capability.IMAGE, flags))
        assertFalse(CapabilityGate.allows(Capability.VOICE, flags))
    }
}

package com.companion.cc.domain.capability

import org.junit.Assert.assertThrows
import org.junit.Test

class CapabilityGateFailureTest {
    @Test
    fun `disabled capability fails before provider request`() {
        assertThrows(UnsupportedOperationException::class.java) {
            CapabilityGate.requireAllowed(Capability.VOICE, CapabilityFlags())
        }
    }
}

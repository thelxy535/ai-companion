package com.companion.cc.domain.capability

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityScopeTest {
    @Test
    fun `scope key includes user and character without collision`() {
        assertEquals(
            "capability:user-1:character-1",
            CapabilityScope.key("user-1", "character-1")
        )
    }
}

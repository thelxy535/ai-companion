package com.companion.cc.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryProposalServiceTest {
    @Test
    fun proposalKindMapsToStableSchemaKind() {
        assertEquals("preference", MemoryProposalService.kindFor("PREFERENCE"))
        assertEquals("event", MemoryProposalService.kindFor("EVENT"))
    }
}

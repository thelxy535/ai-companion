package com.companion.cc.domain.character

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionContinuityPolicyTest {
    @Test
    fun `repeated physical beat is omitted`() {
        assertNull(ActionContinuityPolicy.accept("轻轻笑了一下", "轻轻笑了一下"))
    }

    @Test
    fun `new physical beat is retained`() {
        assertEquals("把杯子推过来", ActionContinuityPolicy.accept("把杯子推过来", "抬眼看你"))
    }
}

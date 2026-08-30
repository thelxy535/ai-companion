package com.companion.cc.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleStateTest {
    @Test
    fun `pause changes active schedule to paused`() {
        val schedule = ScheduleState("s1", active = true)
        assertEquals(false, schedule.pause().active)
        assertEquals(ScheduleStatus.PAUSED, schedule.pause().status)
    }
}

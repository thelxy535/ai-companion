package com.companion.cc.domain.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryInboxNotificationPolicyTest {
    @Test
    fun `notification waits for quiet threshold unless cooldown has expired`() {
        assertFalse(MemoryInboxNotificationPolicy.shouldNotify(2, now = 100, lastNotifiedAt = 0))
        assertTrue(MemoryInboxNotificationPolicy.shouldNotify(3, now = 100, lastNotifiedAt = 0))
        assertTrue(MemoryInboxNotificationPolicy.shouldNotify(1, now = 86_400_001, lastNotifiedAt = 0))
    }
}

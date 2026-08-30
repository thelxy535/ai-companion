package com.companion.cc.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test
    fun android12AndEarlierDoNotNeedRuntimeNotificationPermission() {
        assertFalse(NotificationPermissionPolicy.requiresRuntimePermission(32))
    }

    @Test
    fun android13AndLaterNeedRuntimeNotificationPermission() {
        assertTrue(NotificationPermissionPolicy.requiresRuntimePermission(33))
    }
}

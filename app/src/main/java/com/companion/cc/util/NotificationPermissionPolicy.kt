package com.companion.cc.util

import android.os.Build

object NotificationPermissionPolicy {
    fun requiresRuntimePermission(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU
}

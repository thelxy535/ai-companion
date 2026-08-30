package com.companion.cc.ui.navigation

import android.util.Log
import androidx.navigation.NavHostController

internal fun shouldNavigateSafely(currentRoute: String?, targetRoute: String): Boolean =
    currentRoute != targetRoute

internal fun NavHostController.navigateSafely(route: String) {
    if (currentDestination?.route == route) return
    try {
        Log.d("CCNavigation", "navigate route=$route")
        navigate(route)
    } catch (error: RuntimeException) {
        Log.e("CCNavigation", "navigation failed route=$route", error)
        throw error
    }
}

internal fun NavHostController.navigateTopLevel(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}


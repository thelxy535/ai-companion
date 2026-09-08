package com.companion.cc.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.companion.cc.ui.navigation.MainBottomBar
import com.companion.cc.ui.navigation.NavGraph
import com.companion.cc.ui.navigation.navigateTopLevel
import com.companion.cc.ui.navigation.shouldShowMainBottomBar
import com.companion.cc.ui.components.V7TopStatusBar
import com.companion.cc.ui.theme.AppBackdropHost
import com.companion.cc.ui.theme.VisualRoute
import com.companion.cc.ui.components.ThemeModeV7

@Composable
fun CCApp(
    themeMode: String = "system",
    openMemoryInboxCompanionId: String? = null,
    openCompanionId: String? = null,
    onThemeModeChange: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val visibleEntries by navController.visibleEntries.collectAsState()
    val showMainBottomBar = shouldShowMainBottomBar(
        currentRoute = currentRoute,
        visibleRoutes = visibleEntries.map { it.destination.route }
    )

    val rootRoute = when {
        currentRoute?.startsWith("chat/") == true || currentRoute?.startsWith("custom_chat/") == true -> VisualRoute.CHAT
        currentRoute == "home" -> VisualRoute.HOME
        else -> VisualRoute.UTILITY
    }
    val rootCompanionId = backStackEntry?.arguments?.getString("companionId")
        ?: backStackEntry?.arguments?.getString("characterId")
        ?: backStackEntry?.arguments?.getString("id")

    AppBackdropHost(
        route = rootRoute,
        companionId = rootCompanionId,
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showMainBottomBar) {
                    MainBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navController.navigateTopLevel(route) }
                    )
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                contentPadding = paddingValues,
                openMemoryInboxCompanionId = openMemoryInboxCompanionId,
                openCompanionId = openCompanionId
            )

        // V7 应用自带状态栏：时间+电量+主题三态切换（覆盖在所有内容之上）
        V7TopStatusBar(
            themeMode = when (themeMode) {
                "light" -> ThemeModeV7.LIGHT
                "dark" -> ThemeModeV7.DARK
                else -> ThemeModeV7.SYSTEM
            },
            onThemeModeChange = { mode -> onThemeModeChange(mode.name.lowercase()) },
            modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )        }
    }
}

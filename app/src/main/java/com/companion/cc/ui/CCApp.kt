package com.companion.cc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.companion.cc.ui.navigation.MainBottomBar
import com.companion.cc.ui.navigation.Screen
import com.companion.cc.ui.navigation.NavGraph
import com.companion.cc.ui.navigation.navigateTopLevel
import com.companion.cc.ui.components.V7TopStatusBar
import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.components.ThemeModeV7

@Composable
fun CCApp(
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = setOf(
        Screen.Home.route,
        Screen.CharacterList.route,
        Screen.MemoryHub.route,
        Screen.Settings.route
    )

    // V9PM 修复：极光提升到根容器层——切 Tab 时页面内容淡入淡出，极光全程连续，
    // 根治底部导航切换时周围背景黑一下（此前每页自带极光随页面淡出，露出窗口黑底）
    val rootNight = LocalVisualTheme.current.tokens.backdrop.isDark
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .auroraScreenBackground(rootNight)
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentRoute in topLevelRoutes) {
                    MainBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navController.navigateTopLevel(route) }
                    )
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                contentPadding = paddingValues
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

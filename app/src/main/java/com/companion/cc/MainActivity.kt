package com.companion.cc

import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.theme.VisualCustomizationManager
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.ui.CCApp
import com.companion.cc.ui.theme.AppVisualTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Context

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var visualCustomizationManager: VisualCustomizationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // V7 沉浸式：必须在 super.onCreate 之后设置（窗口初始化完成后才生效）
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 隐藏系统状态栏；状态栏区域由应用自带 V7 状态栏接管
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // V7 通知闭环：App 启动时检查（偏好开启 + 距上次提醒≥24h → 发聊天提醒）
        val sharedPrefs = getSharedPreferences("cc_v7_prefs", Context.MODE_PRIVATE)
        val lastReminder = sharedPrefs.getLong("last_reminder_at", 0L)
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.Main).launch {
            val prefOn = com.companion.cc.data.local.SettingsManager.isNotificationPrefEnabled(applicationContext)
            if (prefOn && now - lastReminder >= 24 * 60 * 60 * 1000L) {
                com.companion.cc.util.NotificationHelper.sendChatReminderNotification(applicationContext)
                sharedPrefs.edit().putLong("last_reminder_at", now).apply()
            }
        }

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = "system")
            val fontSize by settingsManager.fontSizeFlow.collectAsState(initial = "medium")
            val tactileIntensity by settingsManager.tactileIntensityFlow.collectAsState(
                initial = com.companion.cc.ui.theme.TactileIntensityPreference.SYSTEM
            )
            val visualCustomization by visualCustomizationManager.customizationFlow.collectAsState(
                initial = VisualCustomization.default()
            )
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemInDarkTheme  // "system" 或其他值跟随系统
            }

            AppVisualTheme(
                darkTheme = darkTheme,
                fontSize = fontSize,
                customization = visualCustomization,
                tactileIntensity = tactileIntensity
            ) {
                CCApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            settingsManager.saveThemeMode(mode)
                        }
                    }
                )
            }
        }
    }
}

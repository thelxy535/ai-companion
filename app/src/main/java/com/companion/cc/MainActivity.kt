package com.companion.cc

import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
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
import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.companion.cc.util.NotificationPermissionPolicy

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var visualCustomizationManager: VisualCustomizationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // Keep the recent-apps card in sync with the launcher and system splash icon.
        setTaskDescription(
            ActivityManager.TaskDescription(
                getString(R.string.app_name_nexus),
                launcherTaskBitmap(),
                android.graphics.Color.rgb(16, 19, 28)
            )
        )

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
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        // 状态栏区域由应用自带 V7 状态栏接管；保留透明导航栏，避免
        // MIUI 在强制隐藏导航栏时额外合成一条不连续的深色保护区。
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // V7 通知闭环：App 启动时检查（偏好开启 + 距上次提醒≥24h → 发聊天提醒）
        val sharedPrefs = getSharedPreferences("cc_v7_prefs", Context.MODE_PRIVATE)
        val lastReminder = sharedPrefs.getLong("last_reminder_at", 0L)
        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            val prefOn = settingsManager.isNotificationEnabled()
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
                    openMemoryInboxCompanionId = intent?.getStringExtra("memory_inbox_companion_id"),
                    openCompanionId = intent?.getStringExtra("open_companion_id"),
                    onThemeModeChange = { mode ->
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            settingsManager.saveThemeMode(mode)
                        }
                    }
                )
            }
        }
    }

    /** Draw the same adaptive launcher resource used by the desktop into the recent-task bitmap. */
    private fun launcherTaskBitmap(): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = resources.getDrawable(R.mipmap.ic_launcher_sylora_blue, theme)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPermissionPolicy.requiresRuntimePermission()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        if (!SettingsManager.isNotificationPrefEnabled(this)) return

        val prefs = getSharedPreferences("cc_v7_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_permission_prompted", false)) return
        prefs.edit().putBoolean("notification_permission_prompted", true).apply()
        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }, 700L)
    }
}

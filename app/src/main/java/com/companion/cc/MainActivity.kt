package com.companion.cc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.theme.VisualCustomizationManager
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.ui.CCApp
import com.companion.cc.ui.theme.AppVisualTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = "system")
            val fontSize by settingsManager.fontSizeFlow.collectAsState(initial = "medium")
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
                customization = visualCustomization
            ) {
                CCApp()
            }
        }
    }
}

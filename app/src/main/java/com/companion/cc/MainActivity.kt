package com.companion.cc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.ui.CCApp
import com.companion.cc.ui.theme.CCTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = "system")
            val fontSize by settingsManager.fontSizeFlow.collectAsState(initial = "medium")
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemInDarkTheme  // "system" 或其他值跟随系统
            }

            CCTheme(darkTheme = darkTheme, fontSize = fontSize) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CCApp()
                }
            }
        }
    }
}

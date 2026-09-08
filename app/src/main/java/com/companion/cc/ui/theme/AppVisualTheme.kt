package com.companion.cc.ui.theme

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.companion.cc.domain.model.VisualCustomization

/** Current resolved visual system for material extensions not represented by ColorScheme. */
val LocalVisualTheme = staticCompositionLocalOf {
    VisualThemeResolver.resolve(
        isDark = false,
        route = VisualRoute.HOME,
        companionId = null,
        customization = VisualCustomization.default(),
        effectTier = GlassEffectTier.STEADY
    )
}

val LocalTactileIntensityPreference = staticCompositionLocalOf {
    TactileIntensityPreference.SYSTEM
}

val LocalVisualCustomization = staticCompositionLocalOf {
    VisualCustomization.default()
}

val LocalGlassEffectProfile = staticCompositionLocalOf {
    GlassEffectProfile(
        backdropBackend = GlassBackdropBackend.NONE,
        followsPointer = false,
        releaseDurationMillis = 180
    )
}

/**
 * Application-wide Material 3 and visual-token provider.
 *
 * Route-specific scenes may provide a more specific VisualTheme later while
 * retaining the same global color, typography, and system-bar policy.
 */
@Composable
fun AppVisualTheme(
    darkTheme: Boolean,
    fontSize: String,
    customization: VisualCustomization,
    tactileIntensity: TactileIntensityPreference = TactileIntensityPreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val environment = rememberGlassEffectEnvironment(context)
    val effectTier = GlassEffectPolicy.resolve(
        preference = customization.effectsPreference,
        environment = environment
    )
    val effectProfile = GlassEffectPolicy.profile(effectTier, environment)
    val visualTheme = VisualThemeResolver.resolve(
        isDark = darkTheme,
        route = VisualRoute.HOME,
        companionId = null,
        customization = customization,
        effectTier = effectTier
    )
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalVisualTheme provides visualTheme,
        LocalVisualCustomization provides customization,
        LocalTactileIntensityPreference provides tactileIntensity,
        LocalGlassEffectProfile provides effectProfile,
        LocalFontScale provides getFontScale(fontSize)
    ) {
        // V8 ④ 强调色过渡：primary/secondary 与主题 .8s 同节奏流动
        val baseScheme = visualTheme.materialColors
        val animatedPrimary by animateColorAsState(baseScheme.primary, tween(800, easing = LinearEasing), label = "accentPrimary")
        val animatedSecondary by animateColorAsState(baseScheme.secondary, tween(800, easing = LinearEasing), label = "accentSecondary")
        // V9PM 4b：文字色与主题同节奏过渡（800ms LinearEasing）
        val animatedOnSurface by animateColorAsState(baseScheme.onSurface, tween(800, easing = LinearEasing), label = "accentOnSurface")
        val animatedOnSurfaceVariant by animateColorAsState(baseScheme.onSurfaceVariant, tween(800, easing = LinearEasing), label = "accentOnSurfaceVariant")
        MaterialTheme(
            colorScheme = baseScheme.copy(
                primary = animatedPrimary,
                secondary = animatedSecondary,
                onSurface = animatedOnSurface,
                onSurfaceVariant = animatedOnSurfaceVariant,
            ),
            typography = createTypography(fontSize)
        ) {
            // V9PM 根因修复：未显式给色的 Text 退回 LocalContentColor（默认黑），
            // 在根级绑定主题 onSurface，使默认文字跟随日夜主题（含 Dialog/Popup）。
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides animatedOnSurface
            ) {
                content()
            }
        }
    }
}

@Composable
private fun rememberGlassEffectEnvironment(context: Context): GlassEffectEnvironment {
    val activityManager = remember(context) {
        context.getSystemService(ActivityManager::class.java)
    }
    val powerManager = remember(context) {
        context.getSystemService(PowerManager::class.java)
    }
    var powerSaveMode by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode ?: false)
    }

    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    powerSaveMode = powerManager?.isPowerSaveMode ?: false
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return GlassEffectEnvironment(
        sdkInt = Build.VERSION.SDK_INT,
        isLowRamDevice = activityManager?.isLowRamDevice ?: false,
        isPowerSaveMode = powerSaveMode,
        areSystemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled()
    )
}

package com.companion.cc.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberTactileAction(
    enabled: Boolean = true,
    gesture: TactileGesture = TactileGesture.CLICK,
    action: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val effectTier = LocalVisualTheme.current.effectTier
    val tactileIntensity = LocalTactileIntensityPreference.current
    val tactileController = remember(context) { TactileFeedbackController(context) }
    return remember(tactileController, effectTier, tactileIntensity, enabled, gesture, action) {
        {
            if (TactilePolicy.shouldPerformHaptic(gesture, enabled, effectTier)) {
                tactileController.perform(
                    gesture = gesture,
                    preference = tactileIntensity,
                    effectTier = effectTier
                )
            }
            if (enabled) action()
        }
    }
}

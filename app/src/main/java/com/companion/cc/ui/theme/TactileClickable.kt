package com.companion.cc.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

/** Adds standard click semantics and one safe haptic at confirmed activation. */
fun Modifier.tactileClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val effectTier = LocalVisualTheme.current.effectTier
    val tactileIntensity = LocalTactileIntensityPreference.current
    val tactileController = remember(context) { TactileFeedbackController(context) }
    val performClick = remember(tactileController, effectTier, tactileIntensity, enabled, onClick) {
        {
            if (TactilePolicy.shouldPerformHaptic(
                    TactileGesture.CLICK,
                    enabled = enabled,
                    effectTier = effectTier
                )
            ) {
                tactileController.perform(
                    gesture = TactileGesture.CLICK,
                    preference = tactileIntensity,
                    effectTier = effectTier
                )
            }
            onClick()
        }
    }
    clickable(
        enabled = enabled,
        role = role,
        onClick = performClick
    )
}

package com.companion.cc.ui.theme

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/** Adds one accessible click/long-click owner with bounded tactile feedback. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tactileLongClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val effectTier = LocalVisualTheme.current.effectTier
    val tactileIntensity = LocalTactileIntensityPreference.current
    val tactileController = remember(context) { TactileFeedbackController(context) }
    val clickAction = remember(tactileController, effectTier, tactileIntensity, enabled, onClick) {
        {
            if (TactilePolicy.shouldPerformHaptic(TactileGesture.CLICK, enabled, effectTier)) {
                tactileController.perform(
                    gesture = TactileGesture.CLICK,
                    preference = tactileIntensity,
                    effectTier = effectTier
                )
            }
            if (enabled) onClick()
        }
    }
    val longClickAction = remember(tactileController, effectTier, tactileIntensity, enabled, onLongClick) {
        {
            if (TactileLongPressPolicy.shouldActivate(
                    elapsedMillis = TactileLongPressPolicy.defaultTimeoutMillis,
                    enabled = enabled,
                    effectTier = effectTier
                )
            ) {
                tactileController.perform(
                    gesture = TactileGesture.LONG_PRESS,
                    preference = tactileIntensity,
                    effectTier = effectTier
                )
                onLongClick()
            }
        }
    }
    combinedClickable(
        enabled = enabled,
        role = role,
        onClick = clickAction,
        onLongClick = longClickAction
    )
}

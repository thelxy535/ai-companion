package com.companion.cc.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.companion.cc.ui.designsystem.AuroraChatTokens
import com.companion.cc.ui.designsystem.AuroraDuration
import kotlinx.coroutines.delay

/** Pure display policy used by the streaming renderer and unit tests. */
data class StreamingRevealState(
    val target: String = "",
    val displayed: String = "",
    val complete: Boolean = false,
)

fun reconcileStreamingTarget(state: StreamingRevealState, target: String, streaming: Boolean): StreamingRevealState {
    if (!streaming) return state.copy(target = target, displayed = target, complete = true)
    if (target.length > AuroraChatTokens.RevealMaxAnimatedChars) {
        return state.copy(target = target, displayed = target, complete = false)
    }
    val safeDisplayed = when {
        target.startsWith(state.displayed) -> state.displayed
        else -> target.take(state.displayed.length.coerceAtMost(target.length))
    }
    return state.copy(target = target, displayed = safeDisplayed, complete = safeDisplayed == target)
}

fun revealNextCharacter(state: StreamingRevealState): StreamingRevealState {
    if (state.complete || state.displayed.length >= state.target.length) return state.copy(complete = true)
    val next = state.target.substring(0, state.displayed.length + 1)
    return state.copy(displayed = next, complete = next == state.target)
}

/**
 * Incremental type reveal. The target is deliberately not a remember key: each
 * network chunk extends the same visible prefix instead of restarting it.
 */
@Composable
fun rememberStreamingText(
    fullText: String,
    isStreaming: Boolean = true,
    streamingSpeed: Long = AuroraDuration.TypeRevealChar.toLong(),
): String {
    var state by remember { mutableStateOf(StreamingRevealState()) }

    LaunchedEffect(fullText, isStreaming) {
        state = reconcileStreamingTarget(state, fullText, isStreaming)
        if (isStreaming && fullText.length <= AuroraChatTokens.RevealMaxAnimatedChars) {
            while (state.displayed.length < state.target.length) {
                delay(streamingSpeed)
                state = revealNextCharacter(state)
            }
        }
    }
    return state.displayed
}

/** State holder retained for callers that coordinate streaming outside Compose. */
data class StreamingTextState(
    val fullText: String = "",
    val displayedText: String = "",
    val isComplete: Boolean = false,
    val isStreaming: Boolean = false,
)

class StreamingTextController {
    private val _state = mutableStateOf(StreamingTextState())
    val state: State<StreamingTextState> = _state

    fun startStreaming(text: String, speed: Long = AuroraDuration.TypeRevealChar.toLong()) {
        _state.value = StreamingTextState(fullText = text, isStreaming = true)
    }

    fun updateDisplayedText(text: String) {
        val current = _state.value
        _state.value = current.copy(
            displayedText = text,
            isComplete = text == current.fullText,
        )
    }

    fun completeStreaming() {
        _state.value = _state.value.copy(displayedText = _state.value.fullText, isComplete = true, isStreaming = false)
    }

    fun reset() {
        _state.value = StreamingTextState()
    }
}


/**
 * V7 附加：流式末字 blur 渐变（规格 §7.1 M-Type-reveal）。
 * 前缀正常渲染；正在浮现的末字用 graphicsLayer blur 随相位（1→0）渐隐。
 * 使用：在 AuroraMessageBubble 里替换流式期间的纯文本渲染。
 */
@Composable
fun StreamingBlurText(
    text: String,
    isStreaming: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    speed: Long = AuroraDuration.TypeRevealChar.toLong(),
) {
    val displayed = rememberStreamingText(text, isStreaming, speed)

    // V7-fix: 长文直出（>RevealMaxAnimatedChars）或非流式 → 普通文本，绝不进 blur 路径
    val useBlurReveal = isStreaming &&
        displayed.isNotEmpty() &&
        text.length <= AuroraChatTokens.RevealMaxAnimatedChars &&
        displayed.length < text.length  // 还在逐字浮现中

    val prefix = if (useBlurReveal) displayed.dropLast(1) else displayed
    val last = if (useBlurReveal) displayed.takeLast(1) else ""

    // blur 相位: 每次末字变化时从 1.0 → 0.0（一帧周期内完成）
    var blurPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(displayed.length, useBlurReveal) {
        if (useBlurReveal) {
            blurPhase = 1f
            val startTime = withFrameNanos { it }
            while (blurPhase > 0.01f) {
                val elapsed = withFrameNanos { it } - startTime
                blurPhase = 1f - (elapsed.toFloat() / (AuroraDuration.TypeRevealChar * 1000000L))
                if (blurPhase < 0f) blurPhase = 0f
            }
        } else {
            blurPhase = 0f
        }
    }

    val blurPx = with(androidx.compose.ui.platform.LocalDensity.current) { 1.5f.dp.toPx() * blurPhase }

    androidx.compose.foundation.layout.Column(modifier) {
        // 前缀: 永远正常渲染（无任何 renderEffect）
        if (prefix.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = prefix,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
            )
        }
        // 正在浮现的末字: 轻 blur 渐变；useBlurReveal=false 时显式清空 renderEffect
        if (last.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = last,
                color = color.copy(alpha = if (useBlurReveal) 0.55f + 0.45f * (1f - blurPhase) else 1f),
                fontSize = fontSize,
                fontWeight = fontWeight,
                modifier = Modifier.graphicsLayer {
                    renderEffect = if (useBlurReveal && blurPhase > 0.01f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
                    } else {
                        null
                    }
                },
            )
        }
    }
}

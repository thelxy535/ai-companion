package com.companion.cc.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.delay

/**
 * 流式输出效果
 * 模拟打字机效果，逐字显示文本
 */
@Composable
fun rememberStreamingText(
    fullText: String,
    isStreaming: Boolean = true,
    streamingSpeed: Long = 30L
): String {
    // 使用 remember(fullText) 确保每条新消息重置状态
    var displayText by remember(fullText) { mutableStateOf("") }
    var targetLength by remember(fullText) { mutableIntStateOf(0) }
    var isAnimating by remember(fullText) { mutableStateOf(false) }
    var hasStartedStreaming by remember(fullText) { mutableStateOf(false) }

    LaunchedEffect(fullText, isStreaming) {
        if (isStreaming) {
            // 正在流式传输：记录目标长度，开始动画
            hasStartedStreaming = true
            targetLength = fullText.length

            // 如果没有正在播放动画，启动动画
            if (!isAnimating && displayText.length < fullText.length) {
                isAnimating = true
                while (displayText.length < targetLength) {
                    delay(streamingSpeed)
                    if (displayText.length < fullText.length) {
                        displayText = fullText.substring(0, displayText.length + 1)
                    }
                }
                isAnimating = false
            }
        } else {
            // 流式结束
            if (hasStartedStreaming && displayText.length < fullText.length) {
                // 之前开始过流式传输，播放剩余内容
                isAnimating = true
                for (i in displayText.length until fullText.length) {
                    delay(streamingSpeed)
                    displayText = fullText.substring(0, i + 1)
                }
                isAnimating = false
            } else if (!hasStartedStreaming) {
                // 历史消息（从未开始流式传输）：直接显示
                displayText = fullText
            }
        }
    }

    return displayText
}

/**
 * 流式文本状态
 */
data class StreamingTextState(
    val fullText: String = "",
    val displayedText: String = "",
    val isComplete: Boolean = false,
    val isStreaming: Boolean = false
)

/**
 * 流式文本控制器
 */
class StreamingTextController {
    private val _state = mutableStateOf(StreamingTextState())
    val state: State<StreamingTextState> = _state

    fun startStreaming(text: String, speed: Long = 30L) {
        _state.value = StreamingTextState(
            fullText = text,
            displayedText = "",
            isComplete = false,
            isStreaming = true
        )
    }

    fun updateDisplayedText(text: String) {
        _state.value = _state.value.copy(
            displayedText = text,
            isComplete = text == _state.value.fullText
        )
    }

    fun completeStreaming() {
        _state.value = _state.value.copy(
            displayedText = _state.value.fullText,
            isComplete = true,
            isStreaming = false
        )
    }

    fun reset() {
        _state.value = StreamingTextState()
    }
}

package com.companion.cc.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 语音输入按钮
 *
 * 支持：
 * - 点击开始/停止录音
 * - 显示录音动画
 * - 显示音量级别
 */
@Composable
fun VoiceInputButton(
    isListening: Boolean,
    volumeLevel: Float,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 背景波纹动画
        if (isListening) {
            VoiceWaveAnimation(
                volumeLevel = volumeLevel
            )
        }

        // 按钮
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            containerColor = if (isListening)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isListening)
                    Icons.Default.Stop
                else
                    Icons.Default.Mic,
                contentDescription = if (isListening) "停止录音" else "开始录音"
            )
        }
    }
}

/**
 * 语音波形动画
 */
@Composable
fun VoiceWaveAnimation(
    volumeLevel: Float,
    modifier: Modifier = Modifier
) {
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f + (volumeLevel / 10f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // 波纹圆圈
    Box(
        modifier = modifier.size(80.dp * scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        )
    }
}

/**
 * 语音识别结果显示
 */
@Composable
fun VoiceRecognitionText(
    text: String?,
    modifier: Modifier = Modifier
) {
    if (text != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * TTS 播放状态指示器
 */
@Composable
fun TTSSpeakingIndicator(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSpeaking) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 跳动的小圆点
            repeat(3) { index ->
                val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")

                val offset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 600,
                            delayMillis = index * 100,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offset"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = offset.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Text(
                text = "播放中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

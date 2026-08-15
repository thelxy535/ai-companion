package com.companion.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * 通用头像组件
 *
 * 优先级：
 * 1. 自定义头像 URL（如果提供）
 * 2. Emoji 表情（如果提供）
 * 3. 默认图标
 */
@Composable
fun Avatar(
    avatarUrl: String?,
    emoji: String?,
    size: Dp = 40.dp,
    emojiSize: TextUnit = 24.sp,
    backgroundColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        Modifier
            .size(size)
            .clip(CircleShape)
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = backgroundColor ?: MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                // 1. 优先显示自定义头像
                !avatarUrl.isNullOrBlank() -> {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "头像",
                        modifier = Modifier.size(size),
                        contentScale = ContentScale.Crop,
                        loading = {
                            // 加载中显示进度指示器
                            CircularProgressIndicator(
                                modifier = Modifier.size(size * 0.5f),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            // 加载失败显示错误图标或降级到 emoji/默认图标
                            if (!emoji.isNullOrBlank()) {
                                Text(
                                    text = emoji,
                                    fontSize = emojiSize
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "加载失败",
                                    modifier = Modifier.size(size * 0.6f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }
                // 2. 其次显示 emoji
                !emoji.isNullOrBlank() -> {
                    Text(
                        text = emoji,
                        fontSize = emojiSize
                    )
                }
                // 3. 最后显示默认图标
                else -> {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(size),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 用户头像
 * 优先显示自定义头像，否则显示默认图标
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    Avatar(
        avatarUrl = avatarUrl,
        emoji = null,
        size = size,
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick
    )
}

/**
 * AI 伴侣头像
 * 优先显示自定义头像，其次显示 emoji
 */
@Composable
fun CompanionAvatar(
    avatarUrl: String?,
    emoji: String,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    Avatar(
        avatarUrl = avatarUrl,
        emoji = emoji,
        size = size,
        emojiSize = (size.value * 0.6).sp,
        onClick = onClick
    )
}

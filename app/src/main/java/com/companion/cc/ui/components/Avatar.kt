package com.companion.cc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onClick: (() -> Unit)? = null,
    showRing: Boolean = false,
    showStatusDot: Boolean = false,
) {
    val modifier = if (onClick != null) {
        Modifier
            .size(size)
            .clip(CircleShape)
            .semantics { contentDescription = "头像" }
            .clickable(onClick = onClick)
    } else {
        Modifier
            .size(size)
            .clip(CircleShape)
            .semantics { contentDescription = "头像" }
    }

    // V7 aura 底：radial-gradient(circle at 38% 34%, #fff 0%, aura 34%, deep 92%)
    val base = backgroundColor ?: MaterialTheme.colorScheme.primaryContainer
    // V7 aura：radial-gradient(circle at 38% 34%, #fff 0%, aura 34%, deep 92%)
    // Compose Brush.radialGradient 的 center 是像素坐标（0.38px≈0），所以之前"纯色"。
    // 用 drawBehind 手绘：白高光小圆(偏左上) + 主色大圆 + 暗边描边，三层叠加稳定可控
    val auraDeep = base.darken(0.42f)
    Box(
        modifier = modifier.drawBehind {
            val dims = this.size
            val r = minOf(dims.width, dims.height) / 2f
            val cx = dims.width / 2f
            val cy = dims.height / 2f
            // 主色底
            drawCircle(color = base, radius = r, center = Offset(cx, cy))
            // 高光白雾（中心偏左上 38%,34%，半径 0.55r）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(dims.width * 0.38f, dims.height * 0.34f),
                    radius = r * 0.85f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // 右下暗缘
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, auraDeep.copy(alpha = 0.55f)),
                    center = Offset(dims.width * 0.62f, dims.height * 0.66f),
                    radius = r * 1.05f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        },
        contentAlignment = Alignment.Center
    ) {
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
                                Box(
                                    modifier = Modifier.size(size),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = emojiSize,
                                        modifier = Modifier.wrapContentSize(Alignment.Center)
                                    )
                                }
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
                    Box(
                        modifier = Modifier.size(size),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = emojiSize,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        )
                    }
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
        // V7 头像光环 ring：白 rim（内 1.5dp）+ aura 光晕（外 2dp）双层
        if (showRing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .border(2.dp, base.copy(alpha = 0.45f), CircleShape)
            )
        }
        // V7 状态点 st：右下 11dp 绿点白边
        if (showStatusDot) {
            // 内缩 2dp 防止被 clip 裁掉，白边用内层 border 实现
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(13.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Color(0xFF2EA985), CircleShape)
                )
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
    onClick: (() -> Unit)? = null,
    showStatusDot: Boolean = false,
) {
    Avatar(
        avatarUrl = avatarUrl,
        emoji = emoji,
        size = size,
        emojiSize = (size.value * 0.6).sp,
        backgroundColor = androidx.compose.ui.graphics.Color(0xFF8FB8FF),
        onClick = onClick,
        showRing = true,
        showStatusDot = showStatusDot
    )
}

/**
 * V7 aura deep 色：按比例压暗（简单 sRGB 缩放，视觉够用）
 */
private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha
)


/**
 * V7 aura 色组：按名字稳定分配（蓝/绿/粉 循环，设计稿角色色）
 */
fun auraColorFor(name: String): Color = when (kotlin.math.abs(name.hashCode()) % 3) {
    0 -> Color(0xFF8FB8FF) // 蓝
    1 -> Color(0xFF7FD4AE) // 绿
    else -> Color(0xFFF0A8C8) // 粉
}

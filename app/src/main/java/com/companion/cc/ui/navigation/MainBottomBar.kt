package com.companion.cc.ui.navigation

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.companion.cc.ui.designsystem.pressableV5
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private data class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val visual = com.companion.cc.ui.theme.LocalVisualTheme.current
    val night = visual.tokens.backdrop.isDark
    val destinations = listOf(
        TopLevelDestination(Screen.Home, "消息", Icons.Outlined.Home),
        TopLevelDestination(Screen.CharacterList, "角色", Icons.Outlined.Person),
        TopLevelDestination(Screen.MemoryHub, "记忆", Icons.Outlined.History),
        TopLevelDestination(Screen.Settings, "设置", Icons.Outlined.Settings),
    )
    val accent = visual.tokens.accent
    val selectedColor = accent
    val idleColor = if (night) Color(0xFF8A94AC) else Color(0xFF8E93A6)

    // V7 tabbar：浮起玻璃胶囊（margin 12dp、24dp 圆角、inset hairline+shadow）+ 选中 accent dot
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // 触摸优化：底边距加大离手势条更远
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (night) Color(0xCC181D2E) else Color(0xE6FFFFFF),
                    RoundedCornerShape(28.dp)
                )
                .border(
                    1.dp,
                    if (night) Color(0x1FFFFFFF) else Color(0xB8FFFFFF),
                    RoundedCornerShape(28.dp)
                )
                .padding(vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.screen.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressableV5({ onNavigate(destination.screen.route) }, isNight = night)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = if (selected) selectedColor else idleColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = destination.label,
                        color = if (selected) selectedColor else idleColor,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    // V7 选中 dot：4dp accent 圆点 + 6dp 光晕
                    Spacer(Modifier.height(2.dp))
                    // V9PM：导航 dot spring 弹入
                    val navDotScale by animateFloatAsState(
                        if (selected) 1f else 0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "navDot"
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .graphicsLayer { scaleX = navDotScale; scaleY = navDotScale }
                            .background(
                                if (selected) selectedColor else Color.Transparent,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

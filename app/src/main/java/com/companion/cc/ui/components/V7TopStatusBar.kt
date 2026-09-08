package com.companion.cc.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.cc.ui.theme.LocalVisualTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeModeV7(val label: String, val icon: ImageVector) {
    SYSTEM("跟随系统", Icons.Default.SettingsBrightness),
    LIGHT("明亮", Icons.Default.LightMode),
    DARK("黑暗", Icons.Default.DarkMode);

    fun next(): ThemeModeV7 = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}

@Composable
fun V7TopStatusBar(
    themeMode: ThemeModeV7,
    onThemeModeChange: (ThemeModeV7) -> Unit,
    modifier: Modifier = Modifier,
) {
    val night = LocalVisualTheme.current.tokens.backdrop.isDark
    val context = LocalContext.current

    var batteryPct by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                if (level >= 0) batteryPct = level * 100 / scale
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    var timeText by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 10_000L)
            }
        }
        ticker.run()
        onDispose { handler.removeCallbacks(ticker) }
    }

    // 融入背景：常态近透明，仅保留极淡的层次
    val pillBg by animateColorAsState(
        if (night) Color(0x14202844) else Color(0x0FFFFFFF),
        tween(800, easing = androidx.compose.animation.core.LinearEasing), label = "statusPillBg"
    )
    val inkColor = if (night) Color(0xFFEBF0FA) else Color(0xFF1A2030)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            // 触摸优化：整体下移离屏幕顶边更远
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(pillBg)
                // 触摸优化：热区加高
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (batteryPct >= 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when {
                                charging -> Color(0xFF4FC49B)
                                batteryPct <= 20 -> Color(0xFFE56B62)
                                else -> inkColor.copy(alpha = 0.55f)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "$batteryPct%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = inkColor
                )
                Spacer(Modifier.size(10.dp))
            }
            Text(
                timeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = inkColor
            )
        }

        V9PMIconButton(
            icon = themeMode.icon,
            contentDescription = themeMode.label,
            onClick = { onThemeModeChange(themeMode.next()) },
            size = 48.dp,
            iconSize = 20.dp,
            shape = CircleShape,
            tint = inkColor
        )
    }
}

// ============================================================
// AuroraGlassTokens.kt · 晨雾琉璃 色彩与字体 token 层
// 来源:《AI伴侣-设计语言规范.md》§1/§2 —— 数值一一对应,不要改数
// 用法: 将 package 名改为项目包名;Day/Night 按
//       isSystemInDarkTheme() 或应用内主题开关切换
// ============================================================
package com.companion.cc.ui.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Immutable
data class AuroraColors(
    // 背景层
    val bgBase: Color,
    val bgGradientA: Color,
    val bgGradientB: Color,
    val bgGradientC: Color,
    // 文字
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    // 交互
    val accent: Color,
    val accentDeep: Color,
    // 光核
    val auroraCore: Color,
    val auroraHalo: Color,
    val auroraDeep: Color,
    val auroraOpal: Color,
    // 语义
    val success: Color,
    val warning: Color,
    val danger: Color,
    // 玻璃光学(V3 折射配方)
    val glassUltraThin: Color,
    val glassThin: Color,
    val glassRegular: Color,
    val glassThick: Color,
    val glassSolid: Color,
    val hairline: Color,        // 顶部高光描边
    val dispersion: Color,      // 色散描边(--disp)
    val innerDepth: Color,      // 底缘内影(--innerdepth)
    // 我的气泡渐变两端
    val bubbleMeStart: Color,
    val bubbleMeEnd: Color,
)

// ---------- 晨 · Day ----------
val AuroraDay = AuroraColors(
    bgBase = Color(0xFFEDF1F7),
    bgGradientA = Color(0xFFDCE7F5),
    bgGradientB = Color(0xFFEFE6F2),
    bgGradientC = Color(0xFFE4F0EF),
    ink = Color(0xFF1C2230),
    inkMuted = Color(0xFF5A6577),
    inkFaint = Color(0xFF98A1B3),
    accent = Color(0xFF4F80E8),
    accentDeep = Color(0xFF3A63C4),
    auroraCore = Color(0xFF8FB8FF),
    auroraHalo = Color(0xFFC9D9FF),
    auroraDeep = Color(0xFF5F8FDE),
    auroraOpal = Color(0xFFFFD9E8),
    success = Color(0xFF2EA985),
    warning = Color(0xFFD99A3D),
    danger = Color(0xFFD9534C),
    glassUltraThin = Color(0x52FFFFFF),  // α.32
    glassThin = Color(0x73FFFFFF),       // α.45
    glassRegular = Color(0x94FFFFFF),    // α.58
    glassThick = Color(0xBDFFFFFF),      // α.74
    glassSolid = Color(0xF0FFFFFF),      // α.94
    hairline = Color(0xA6FFFFFF),        // α.65
    dispersion = Color(0x618FB8FF),      // α.38
    innerDepth = Color(0x1F7A94CC),      // α.12
    bubbleMeStart = Color(0xFF5B8DEF),
    bubbleMeEnd = Color(0xFF4F80E8),
)

// ---------- 夜航 · Night ----------
val AuroraNight = AuroraColors(
    bgBase = Color(0xFF10141F),
    bgGradientA = Color(0xFF182136),
    bgGradientB = Color(0xFF1F1D33),
    bgGradientC = Color(0xFF12202B),
    ink = Color(0xFFE9EEF8),
    inkMuted = Color(0xFF97A2BA),
    inkFaint = Color(0xFF5B6579),
    accent = Color(0xFF6D9BF1),
    accentDeep = Color(0xFF4F80E8),
    auroraCore = Color(0xFF9CC4FF),
    auroraHalo = Color(0xFF7FA0E0),
    auroraDeep = Color(0xFF4A6DB8),
    auroraOpal = Color(0xFFF2C4DA),
    success = Color(0xFF4FC49B),
    warning = Color(0xFFE5B05C),
    danger = Color(0xFFE56B62),
    glassUltraThin = Color(0x522C3652),
    glassThin = Color(0x6B242E48),
    glassRegular = Color(0x851E2840),
    glassThick = Color(0xA8182036),
    glassSolid = Color(0xDB131A2C),
    hairline = Color(0x1AFFFFFF),        // α.10
    dispersion = Color(0x389CC4FF),      // α.22
    innerDepth = Color(0x47000000),      // α.28
    bubbleMeStart = Color(0xFF5F8DE9),
    bubbleMeEnd = Color(0xFF4A79DC),
)

// ---------- 字阶(§2.2) ----------
// 字族: 跟随厂商(HarmonyOS Sans/MiSans/OPPO Sans → FontFamily.Default 兜底),
// 信纸体仅在「记忆信件」场景挂 FontFamily.Serif
object AuroraType {
    val DisplaySm = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight(700), letterSpacing = (-0.01).sp)
    val TitlePage = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight(700), letterSpacing = (-0.005).sp)
    val NavTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight(600))
    val BodyChat = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)            // 全 App 最高频
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight(500))
    val CaptionTime = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight(500), letterSpacing = 0.02.sp)
    val MicroCaps = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight(600), letterSpacing = 0.08.sp)
}

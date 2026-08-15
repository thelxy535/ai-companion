package com.companion.cc.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义化颜色系统 - 统一管理所有UI元素的颜色
 * 所有组件都应该使用这些语义化颜色，而不是硬编码颜色值
 */
@Immutable
data class SemanticColors(
    // ===== 背景层级 =====
    val backgroundPrimary: Color,      // 主背景
    val backgroundSecondary: Color,    // 次级背景（如侧边栏、抽屉）
    val backgroundTertiary: Color,     // 三级背景（更深的层级）

    // ===== 表面/卡片 =====
    val surface: Color,                // 标准卡片/表面
    val surfaceVariant: Color,         // 变体表面（稍有不同的卡片）
    val surfaceElevated: Color,        // 悬浮表面（如对话框、底部表单）

    // ===== 文字颜色 =====
    val textPrimary: Color,            // 主要文字（标题、重要内容）
    val textSecondary: Color,          // 次要文字（副标题、说明）
    val textTertiary: Color,           // 弱化文字（辅助信息、占位符）
    val textDisabled: Color,           // 禁用文字
    val textOnPrimary: Color,          // 主色按钮上的文字
    val textOnSurface: Color,          // 表面上的文字

    // ===== 边框和分割线 =====
    val border: Color,                 // 标准边框
    val borderVariant: Color,          // 变体边框（更弱的边框）
    val divider: Color,                // 分割线

    // ===== 输入框 =====
    val inputBackground: Color,        // 输入框背景
    val inputBorder: Color,            // 输入框边框
    val inputBorderFocused: Color,     // 输入框聚焦边框
    val inputText: Color,              // 输入框文字
    val inputPlaceholder: Color,       // 输入框占位符

    // ===== 按钮 =====
    val buttonPrimary: Color,          // 主要按钮背景
    val buttonPrimaryText: Color,      // 主要按钮文字
    val buttonSecondary: Color,        // 次要按钮背景
    val buttonSecondaryText: Color,    // 次要按钮文字
    val buttonTertiary: Color,         // 三级按钮背景（如取消按钮）
    val buttonTertiaryText: Color,     // 三级按钮文字
    val buttonDisabled: Color,         // 禁用按钮背景
    val buttonDisabledText: Color,     // 禁用按钮文字

    // ===== 消息气泡 =====
    val messageBubbleUser: Color,      // 用户消息气泡背景
    val messageBubbleUserText: Color,  // 用户消息文字
    val messageBubbleAI: Color,        // AI消息气泡背景
    val messageBubbleAIText: Color,    // AI消息文字
    val messageBubbleBorder: Color,    // 消息气泡边框（如果需要）

    // ===== 状态颜色 =====
    val success: Color,                // 成功状态
    val successBackground: Color,      // 成功状态背景
    val warning: Color,                // 警告状态
    val warningBackground: Color,      // 警告状态背景
    val error: Color,                  // 错误状态
    val errorBackground: Color,        // 错误状态背景
    val info: Color,                   // 信息状态
    val infoBackground: Color,         // 信息状态背景

    // ===== 交互状态 =====
    val hover: Color,                  // 悬停状态叠加色
    val pressed: Color,                // 按下状态叠加色
    val selected: Color,               // 选中状态背景
    val selectedText: Color,           // 选中状态文字
    val focus: Color,                  // 聚焦状态边框/高亮

    // ===== 导航和工具栏 =====
    val navigationBar: Color,          // 导航栏背景
    val navigationBarText: Color,      // 导航栏文字
    val navigationBarIcon: Color,      // 导航栏图标
    val topBar: Color,                 // 顶部工具栏背景
    val topBarText: Color,             // 顶部工具栏文字
    val topBarIcon: Color,             // 顶部工具栏图标

    // ===== 特殊元素 =====
    val avatar: Color,                 // 头像背景
    val avatarBorder: Color,           // 头像边框
    val badge: Color,                  // 徽章背景
    val badgeText: Color,              // 徽章文字
    val chip: Color,                   // 标签背景
    val chipText: Color,               // 标签文字
    val progressBar: Color,            // 进度条
    val progressBarBackground: Color,  // 进度条背景

    // ===== 阴影和叠加 =====
    val shadow: Color,                 // 阴影颜色
    val scrim: Color,                  // 遮罩层（如对话框背后的半透明层）
    val overlay: Color,                // 叠加层
)

/**
 * 深色主题颜色
 */
val DarkSemanticColors = SemanticColors(
    // 背景层级 - 使用真正的深色，而不是纯黑
    backgroundPrimary = Color(0xFF0A0A0A),
    backgroundSecondary = Color(0xFF121212),
    backgroundTertiary = Color(0xFF1A1A1A),

    // 表面/卡片 - 与背景有明显区分
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF252525),
    surfaceElevated = Color(0xFF2A2A2A),

    // 文字颜色 - 确保足够的对比度
    textPrimary = Color(0xFFE8E8E8),      // 主要文字，高对比度
    textSecondary = Color(0xFFB8B8B8),    // 次要文字，中等对比度
    textTertiary = Color(0xFF888888),     // 弱化文字，低对比度但仍可读
    textDisabled = Color(0xFF555555),     // 禁用文字
    textOnPrimary = Color(0xFFFFFFFF),    // 主色按钮上的白色文字
    textOnSurface = Color(0xFFE8E8E8),    // 表面上的文字

    // 边框和分割线 - 微妙但可见
    border = Color(0xFF333333),
    borderVariant = Color(0xFF282828),
    divider = Color(0xFF2A2A2A),

    // 输入框 - 清晰的边界和高对比度
    inputBackground = Color(0xFF252525),
    inputBorder = Color(0xFF3A3A3A),
    inputBorderFocused = Color(0xFF667EEA),
    inputText = Color(0xFFE8E8E8),
    inputPlaceholder = Color(0xFF707070),

    // 按钮
    buttonPrimary = Color(0xFF667EEA),
    buttonPrimaryText = Color(0xFFFFFFFF),
    buttonSecondary = Color(0xFF2A2A2A),
    buttonSecondaryText = Color(0xFFE8E8E8),
    buttonTertiary = Color(0xFF1E1E1E),
    buttonTertiaryText = Color(0xFFB8B8B8),
    buttonDisabled = Color(0xFF2A2A2A),
    buttonDisabledText = Color(0xFF555555),

    // 消息气泡 - 清晰的背景和高对比度文字
    messageBubbleUser = Color(0xFF667EEA),
    messageBubbleUserText = Color(0xFFFFFFFF),
    messageBubbleAI = Color(0xFF2A2A2A),
    messageBubbleAIText = Color(0xFFE8E8E8),
    messageBubbleBorder = Color(0xFF3A3A3A),

    // 状态颜色
    success = Color(0xFF10B981),
    successBackground = Color(0xFF1A2F26),
    warning = Color(0xFFF59E0B),
    warningBackground = Color(0xFF2F2A1A),
    error = Color(0xFFEF4444),
    errorBackground = Color(0xFF2F1A1A),
    info = Color(0xFF3B82F6),
    infoBackground = Color(0xFF1A232F),

    // 交互状态
    hover = Color(0x0FFFFFFF),
    pressed = Color(0x1FFFFFFF),
    selected = Color(0xFF667EEA).copy(alpha = 0.2f),
    selectedText = Color(0xFF8B9BF5),
    focus = Color(0xFF667EEA),

    // 导航和工具栏
    navigationBar = Color(0xFF1E1E1E),
    navigationBarText = Color(0xFFE8E8E8),
    navigationBarIcon = Color(0xFFB8B8B8),
    topBar = Color(0xFF1E1E1E),
    topBarText = Color(0xFFE8E8E8),
    topBarIcon = Color(0xFFB8B8B8),

    // 特殊元素
    avatar = Color(0xFF2A2A2A),
    avatarBorder = Color(0xFF3A3A3A),
    badge = Color(0xFFEF4444),
    badgeText = Color(0xFFFFFFFF),
    chip = Color(0xFF2A2A2A),
    chipText = Color(0xFFB8B8B8),
    progressBar = Color(0xFF667EEA),
    progressBarBackground = Color(0xFF2A2A2A),

    // 阴影和叠加
    shadow = Color(0x40000000),
    scrim = Color(0x80000000),
    overlay = Color(0x1FFFFFFF),
)

/**
 * 浅色主题颜色
 */
val LightSemanticColors = SemanticColors(
    // 背景层级
    backgroundPrimary = Color(0xFFFFFFFF),
    backgroundSecondary = Color(0xFFF8F8F8),
    backgroundTertiary = Color(0xFFF0F0F0),

    // 表面/卡片
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F5),
    surfaceElevated = Color(0xFFFFFFFF),

    // 文字颜色
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF4A4A4A),
    textTertiary = Color(0xFF888888),
    textDisabled = Color(0xFFBBBBBB),
    textOnPrimary = Color(0xFFFFFFFF),
    textOnSurface = Color(0xFF1A1A1A),

    // 边框和分割线
    border = Color(0xFFDDDDDD),
    borderVariant = Color(0xFFE8E8E8),
    divider = Color(0xFFEEEEEE),

    // 输入框
    inputBackground = Color(0xFFF8F8F8),
    inputBorder = Color(0xFFDDDDDD),
    inputBorderFocused = Color(0xFF667EEA),
    inputText = Color(0xFF1A1A1A),
    inputPlaceholder = Color(0xFF999999),

    // 按钮
    buttonPrimary = Color(0xFF667EEA),
    buttonPrimaryText = Color(0xFFFFFFFF),
    buttonSecondary = Color(0xFFF0F0F0),
    buttonSecondaryText = Color(0xFF1A1A1A),
    buttonTertiary = Color(0xFFFFFFFF),
    buttonTertiaryText = Color(0xFF4A4A4A),
    buttonDisabled = Color(0xFFF0F0F0),
    buttonDisabledText = Color(0xFFBBBBBB),

    // 消息气泡
    messageBubbleUser = Color(0xFF667EEA),
    messageBubbleUserText = Color(0xFFFFFFFF),
    messageBubbleAI = Color(0xFFF5F5F5),
    messageBubbleAIText = Color(0xFF1A1A1A),
    messageBubbleBorder = Color(0xFFE0E0E0),

    // 状态颜色
    success = Color(0xFF10B981),
    successBackground = Color(0xFFECFDF5),
    warning = Color(0xFFF59E0B),
    warningBackground = Color(0xFFFEF3C7),
    error = Color(0xFFEF4444),
    errorBackground = Color(0xFFFEE2E2),
    info = Color(0xFF3B82F6),
    infoBackground = Color(0xFFDBEAFE),

    // 交互状态
    hover = Color(0x0F000000),
    pressed = Color(0x1F000000),
    selected = Color(0xFF667EEA).copy(alpha = 0.15f),
    selectedText = Color(0xFF667EEA),
    focus = Color(0xFF667EEA),

    // 导航和工具栏
    navigationBar = Color(0xFFFFFFFF),
    navigationBarText = Color(0xFF1A1A1A),
    navigationBarIcon = Color(0xFF4A4A4A),
    topBar = Color(0xFFFFFFFF),
    topBarText = Color(0xFF1A1A1A),
    topBarIcon = Color(0xFF4A4A4A),

    // 特殊元素
    avatar = Color(0xFFF0F0F0),
    avatarBorder = Color(0xFFDDDDDD),
    badge = Color(0xFFEF4444),
    badgeText = Color(0xFFFFFFFF),
    chip = Color(0xFFF0F0F0),
    chipText = Color(0xFF4A4A4A),
    progressBar = Color(0xFF667EEA),
    progressBarBackground = Color(0xFFE8E8E8),

    // 阴影和叠加
    shadow = Color(0x20000000),
    scrim = Color(0x60000000),
    overlay = Color(0x0F000000),
)

/**
 * CompositionLocal for accessing semantic colors
 */
val LocalSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

package com.companion.cc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.BackdropTarget
import com.companion.cc.domain.model.VisualCustomization
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.VisualAccentPresets
import com.companion.cc.ui.theme.VisualEffectsPreference

/** Appearance controls backed by the persistent visual customization manager. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisualCustomizationEditor(
    customization: VisualCustomization,
    message: String?,
    onPickBackdrop: (BackdropTarget) -> Unit,
    onClearBackdrop: (BackdropTarget) -> Unit,
    onSaveAccent: (String?) -> Unit,
    onSaveEffectsPreference: (VisualEffectsPreference) -> Unit,
    onSaveGlassOpacity: (Float) -> Unit,
    onReset: () -> Unit,
    onSaveBackdropAppearance: (BackdropTarget, Float, Float) -> Unit
) {
    var target by remember { mutableStateOf(BackdropTarget.CHAT) }
    val backdrop = customization.backdropFor(target)
    var accentInput by remember(customization.accentHex) {
        mutableStateOf(customization.accentHex.orEmpty())
    }
    var imageOpacity by remember(target, backdrop.imageOpacity) {
        mutableStateOf(backdrop.imageOpacity)
    }
    var scrimOpacity by remember(target, backdrop.scrimOpacity) {
        mutableStateOf(backdrop.scrimOpacity)
    }
    var glassOpacity by remember(customization.glassOpacity) {
        mutableStateOf(customization.glassOpacity)
    }

    UtilitySection(title = "外观与氛围", icon = Icons.Default.AutoAwesome) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "背景、Accent 与材质效果会在所有页面保持可读性。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "背景应用范围",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackdropTarget.entries.forEach { option ->
                    FilterChip(
                        selected = target == option,
                        onClick = { target = option },
                        label = { Text(option.label()) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (backdrop.managedImageReference == null) "使用默认背景" else "已设置自定义背景",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "仅保存到本机私有目录，不会上传背景图片。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPickBackdrop(target) }) {
                    Text(if (backdrop.managedImageReference == null) "选择图片" else "更换图片")
                }
                if (backdrop.managedImageReference != null) {
                    OutlinedButton(onClick = { onClearBackdrop(target) }) {
                        Text("移除")
                    }
                }
            }

            AppearanceSlider(
                label = "图片透明度",
                value = imageOpacity,
                onValueChange = { imageOpacity = it },
                onValueChangeFinished = {
                    onSaveBackdropAppearance(target, imageOpacity, scrimOpacity)
                }
            )
            AppearanceSlider(
                label = "可读性遮罩",
                value = scrimOpacity,
                onValueChange = { scrimOpacity = it },
                onValueChangeFinished = {
                    onSaveBackdropAppearance(target, imageOpacity, scrimOpacity)
                }
            )

            Text(
                text = "强调色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(VisualAccentPresets) { preset ->
                    FilterChip(
                        selected = customization.accentHex == preset.hex,
                        onClick = {
                            accentInput = preset.hex
                            onSaveAccent(preset.hex)
                        },
                        label = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(preset.color, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(preset.label)
                        }
                    )
                }
            }
            OutlinedTextField(
                value = accentInput,
                onValueChange = { accentInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("自定义 Hex 色") },
                placeholder = { Text("#625CD2") },
                singleLine = true,
                trailingIcon = {
                    accentInput.toColorOrNull()?.let { color ->
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSaveAccent(null) }) {
                    Text("使用默认")
                }
                Button(
                    onClick = { onSaveAccent(accentInput) },
                    enabled = accentInput.toColorOrNull() != null
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("应用颜色")
                }
            }

            Text(
                text = "材质效果",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisualEffectsPreference.entries.forEach { preference ->
                    FilterChip(
                        selected = customization.effectsPreference == preference,
                        onClick = { onSaveEffectsPreference(preference) },
                        label = { Text(preference.label()) }
                    )
                }
            }
            AppearanceSlider(
                label = "玻璃透光度",
                value = glassOpacity,
                onValueChange = { glassOpacity = it },
                onValueChangeFinished = { onSaveGlassOpacity(glassOpacity) }
            )
            Text(
                text = "自动模式会在旧设备、低内存或省电状态下优先保持流畅和可读。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复视觉默认")
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}

private fun BackdropTarget.label(): String = when (this) {
    BackdropTarget.CHAT -> "聊天"
    BackdropTarget.HOME -> "首页"
    BackdropTarget.UTILITY -> "工具页"
}

private fun VisualEffectsPreference.label(): String = when (this) {
    VisualEffectsPreference.AUTO -> "\u667A\u80FD\u5E73\u8861"
    VisualEffectsPreference.ENHANCED -> "\u6C89\u6D78\u52A8\u6548"
    VisualEffectsPreference.REDUCED -> "\u7A33\u5B9A\u7701\u7535"
}

private fun String.toColorOrNull(): Color? {
    val compact = trim().removePrefix("#")
    val expanded = when (compact.length) {
        3 -> compact.map { "$it$it" }.joinToString(separator = "")
        6 -> compact
        else -> return null
    }
    if (!expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return null
    }
    return Color(0xFF000000 or expanded.toLong(16))
}

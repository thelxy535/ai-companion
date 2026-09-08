package com.companion.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.companion.cc.ui.designsystem.pressableV5
import com.companion.cc.ui.designsystem.smoothCorner

@Composable
fun V9PMIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val colors = MaterialTheme.colorScheme
    val fill = when {
        selected -> colors.primary.copy(alpha = 0.18f)
        enabled -> colors.surface.copy(alpha = 0.62f)
        else -> colors.surface.copy(alpha = 0.28f)
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .size(size)
            .clip(shape)
            .background(fill, shape)
            .pressableV5(onClick = if (enabled) onClick else ({}), isNight = colors.background.luminance() < 0.5f)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
                if (selected) stateDescription = "已选中"
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = if (enabled) 1f else 0.42f), modifier = Modifier.size(iconSize))
    }
}

@Composable
fun V9PMActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    height: Dp = 48.dp,
) {
    val colors = MaterialTheme.colorScheme
    val foreground = if (destructive) colors.error else colors.primary
    val fill = when {
        !enabled -> colors.onSurface.copy(alpha = 0.06f)
        destructive -> colors.error.copy(alpha = if (selected) 0.18f else 0.10f)
        selected -> colors.primary.copy(alpha = 0.28f)
        else -> colors.primary.copy(alpha = 0.14f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(smoothCorner(16.dp))
            .background(fill)
            .pressableV5(onClick = if (enabled) onClick else ({}), isNight = colors.background.luminance() < 0.5f)
            .semantics {
                contentDescription = label
                role = Role.Button
                if (selected) stateDescription = "已选中"
                if (!enabled) disabled()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = foreground.copy(alpha = if (enabled) 1f else 0.42f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(label, color = foreground.copy(alpha = if (enabled) 1f else 0.42f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun V9PMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = smoothCorner(18.dp)
    val outline = when {
        isError -> colors.error
        focused -> colors.primary
        else -> colors.outlineVariant.copy(alpha = 0.72f)
    }
    Column(modifier = modifier) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceVariant.copy(alpha = if (enabled) 0.68f else 0.38f), shape)
                .drawBehind {
                    drawRoundRect(color = outline.copy(alpha = if (enabled) 0.9f else 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                }
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingIcon?.let { Icon(it, contentDescription = null, tint = outline, modifier = Modifier.size(20.dp)) }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    textStyle = textStyle.copy(color = colors.onSurface),
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = textStyle, color = colors.onSurfaceVariant.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                )
                trailingIcon?.let {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable(enabled = onTrailingIconClick != null, onClick = { onTrailingIconClick?.invoke() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(it, contentDescription = null, tint = outline, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (isError) colors.error else colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 5.dp))
        }
    }
}

@Composable
fun V9PMTopBar(
    title: @Composable () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    navigationContentDescription: String = "返回",
    actions: List<SceneTopBarAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.72f))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                V9PMIconButton(Icons.Default.ArrowBack, navigationContentDescription, onNavigateBack, size = 48.dp, tint = colors.onSurface)
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                title()
            }
            actions.forEach { action ->
                V9PMIconButton(
                    icon = action.icon,
                    contentDescription = action.contentDescription,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    size = 48.dp,
                    tint = colors.onSurface
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant.copy(alpha = 0.38f)))
    }
}

@Composable
fun V9PMSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(smoothCorner(16.dp))
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun V9PMDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        com.companion.cc.ui.theme.GlassDialogSurface(
            modifier = modifier.fillMaxWidth(),
            shape = smoothCorner(28.dp),
            content = content
        )
    }
}

@Composable
fun V9PMChoiceRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable BoxScope.() -> Unit = {},
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = smoothCorner(16.dp)
    val fill = when {
        !enabled -> colors.onSurface.copy(alpha = 0.04f)
        selected -> colors.primary.copy(alpha = 0.12f)
        else -> colors.surface.copy(alpha = 0.32f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                if (selected) stateDescription = "已选中"
                if (!enabled) disabled()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.42f), fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.42f)) }
        }
        Box(content = trailing)
    }
}

private fun Color.luminance(): Float = (0.2126f * red + 0.7152f * green + 0.0722f * blue)

package com.companion.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Edge-attached screen chrome without a nested Material app-bar surface. */
data class SceneTopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
fun SceneTopBar(
    title: @Composable () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    navigationContentDescription: String = "返回",
    actions: List<SceneTopBarAction> = emptyList(),
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.84f))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = navigationContentDescription
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                title()
            }
            actions.forEach { action ->
                IconButton(onClick = action.onClick, enabled = action.enabled) {
                    Icon(action.icon, contentDescription = action.contentDescription)
                }
            }
        }
        Divider(color = colors.outlineVariant.copy(alpha = 0.62f))
    }
}

enum class SceneSectionTone { Neutral, Destructive }

@Composable
fun SceneSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: SceneSectionTone = SceneSectionTone.Neutral,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (tone == SceneSectionTone.Destructive) colors.error else colors.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Divider(color = accent.copy(alpha = 0.32f))
        Spacer(Modifier.height(12.dp))
        content()
    }
}

enum class SceneActionTone { Primary, Secondary, Destructive }

@Composable
fun SceneAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    tone: SceneActionTone = SceneActionTone.Secondary
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (tone) {
        SceneActionTone.Primary -> colors.primary
        SceneActionTone.Secondary -> colors.onSurfaceVariant
        SceneActionTone.Destructive -> colors.error
    }
    val background = when (tone) {
        SceneActionTone.Primary -> colors.primary.copy(alpha = 0.10f)
        SceneActionTone.Secondary -> Color.Transparent
        SceneActionTone.Destructive -> colors.error.copy(alpha = 0.08f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("scene_action_$title")
            .semantics(mergeDescendants = true) { contentDescription = title }
            .padding(PaddingValues(horizontal = 16.dp, vertical = 14.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.SemiBold)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = accent)
    }
}

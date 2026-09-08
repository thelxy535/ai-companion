package com.companion.cc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.VisionServiceMode
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMTextField

@Composable
internal fun VisionApiConfigSection(
    mode: VisionServiceMode,
    isSelfHostedPaired: Boolean,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
    isPairing: Boolean,
    message: String?,
    onModeChange: (VisionServiceMode) -> Unit,
    onPair: () -> Unit,
    onUnpair: () -> Unit,
    visionApiKey: String,
    onVisionApiKeyChange: (String) -> Unit,
    onSaveGemini: () -> Unit,
    isGeminiKeySaved: Boolean
) {
    UtilitySection(title = "图片理解", icon = Icons.Default.Image) {
        Text(
            "选择图片理解服务。自建服务器使用本机模型，单张图片通常需要 1-3 分钟。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        SettingsSelectionRow(
            title = "Gemini",
            description = "使用 Gemini API Key 进行图片理解",
            selected = mode == VisionServiceMode.GEMINI,
            onClick = { onModeChange(VisionServiceMode.GEMINI) }
        )
        UtilityDivider()
        SettingsSelectionRow(
            title = "我的视觉服务器",
            description = if (isSelfHostedPaired) "已配对，可使用 Qwen2.5-VL 3B" else "首次使用需要一次性配对码",
            selected = mode == VisionServiceMode.SELF_HOSTED,
            onClick = { onModeChange(VisionServiceMode.SELF_HOSTED) }
        )
        UtilityDivider()

        if (mode == VisionServiceMode.GEMINI) {
            GeminiCredentials(
                apiKey = visionApiKey,
                onApiKeyChange = onVisionApiKeyChange,
                onSave = onSaveGemini,
                isSaved = isGeminiKeySaved
            )
        } else {
            SelfHostedPairing(
                paired = isSelfHostedPaired,
                pairingCode = pairingCode,
                onPairingCodeChange = onPairingCodeChange,
                isPairing = isPairing,
                onPair = onPair,
                onUnpair = onUnpair
            )
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelfHostedPaired) LocalVisualTheme.current.tokens.status.success else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GeminiCredentials(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean
) {
    V9PMTextField(
        value = apiKey,
        onValueChange = onApiKeyChange,
        modifier = Modifier.fillMaxWidth(),
        label = "Gemini API Key",
        placeholder = "粘贴您的 Gemini API Key",
        leadingIcon = Icons.Default.Key
    )
    Spacer(Modifier.height(12.dp))
    V9PMActionButton(
        label = "保存 Gemini API Key",
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Default.Check,
        enabled = apiKey.isNotBlank()
    )
    AnimatedVisibility(isSaved) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = LocalVisualTheme.current.tokens.status.success
            )
            Text(
                "Gemini API Key 已保存",
                color = LocalVisualTheme.current.tokens.status.success
            )
        }
    }
}

@Composable
private fun SelfHostedPairing(
    paired: Boolean,
    pairingCode: String,
    onPairingCodeChange: (String) -> Unit,
    isPairing: Boolean,
    onPair: () -> Unit,
    onUnpair: () -> Unit
) {
    if (paired) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = LocalVisualTheme.current.tokens.status.success
            )
            Text("已连接到我的视觉服务器", color = LocalVisualTheme.current.tokens.status.success)
        }
        Spacer(Modifier.height(12.dp))
        V9PMActionButton(
            label = "取消配对",
            onClick = onUnpair,
            modifier = Modifier.fillMaxWidth(),
            destructive = true
        )
    } else {
        V9PMTextField(
            value = pairingCode,
            onValueChange = onPairingCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = "一次性配对码",
            placeholder = "在服务器终端生成后输入",
            leadingIcon = Icons.Default.Key
        )
        Spacer(Modifier.height(12.dp))
        V9PMActionButton(
            label = if (isPairing) "正在配对..." else "连接我的视觉服务器",
            onClick = onPair,
            modifier = Modifier.fillMaxWidth(),
            icon = if (isPairing) null else Icons.Default.Link,
            enabled = pairingCode.isNotBlank() && !isPairing
        )
    }
}

package com.companion.cc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.AIProvider
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.LocalVisualTheme

@Composable
internal fun ProviderStatusSection(
    provider: AIProvider?,
    modelCount: Int,
    selectedModel: String?,
    onTestConnection: () -> Unit
) {
    val connected = provider != null
    val successColor = LocalVisualTheme.current.tokens.status.success
    UtilitySection(title = "AI 服务", icon = Icons.Default.Cloud) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (connected) successColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (connected) "已连接" else "未配置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = provider?.displayName ?: "添加 API Key 以连接服务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (connected) {
                TextButton(onClick = onTestConnection) { Text("测试") }
            }
        }
        if (connected) {
            UtilityDivider()
            selectedModel?.let { InfoRow("当前模型", it) }
            InfoRow("可用模型", "$modelCount 个")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ApiConfigSection(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isValidating: Boolean,
    validationMessage: String?,
    currentProvider: AIProvider?,
    onValidate: () -> Unit
) {
    UtilitySection(title = "API 配置", icon = Icons.Default.Key) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            placeholder = { Text("sk-xxxxxxxxxxxxxx") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "粘贴密钥后会自动识别供应商",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onValidate,
            enabled = apiKey.isNotBlank() && !isValidating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isValidating) "验证中..." else "验证并保存")
        }
        validationMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            val success = currentProvider != null
            val color = if (success) LocalVisualTheme.current.tokens.status.success else MaterialTheme.colorScheme.error
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(message, color = color, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSelectionSection(
    selectedModel: String,
    availableModels: List<String>,
    providerName: String?,
    onModelChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    UtilitySection(title = "模型", icon = Icons.Default.SmartToy) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("当前模型") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = { onModelChange(model); expanded = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "共 ${availableModels.size} 个 ${providerName.orEmpty()} 模型可用",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SupportedProvidersSection() {
    var expanded by remember { mutableStateOf(false) }
    UtilitySection(title = "支持的供应商", icon = Icons.Default.Hub) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("查看自动识别范围", modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开"
            )
        }
        AnimatedVisibility(expanded) {
            Column {
                UtilityDivider()
                Text(
                    "SiliconFlow、OpenAI、智谱 AI、DeepSeek、月之暗面、阿里云百炼和自定义服务",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "系统会自动识别 API 密钥类型。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun AdvancedSettingsSection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    UtilitySection(title = "高级", icon = Icons.Default.Tune) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("自定义 API 端点")
                Text(
                    "仅在使用兼容服务时修改",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开"
            )
        }
        AnimatedVisibility(expanded) {
            Column {
                UtilityDivider()
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true
                )
            }
        }
    }
}

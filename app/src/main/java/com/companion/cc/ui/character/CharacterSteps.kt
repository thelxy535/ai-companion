package com.companion.cc.ui.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.*

/**
 * 行为风格步骤
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorStep(viewModel: CharacterCustomizationViewModel) {
    val behaviorRules by viewModel.behaviorRules.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "行为风格设定",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            "定义角色的说话方式和行为习惯",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 回复风格
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "回复风格",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            ResponseStyle.values().forEach { style ->
                FilterChip(
                    selected = behaviorRules.responseStyle == style,
                    onClick = { viewModel.updateResponseStyle(style) },
                    label = { Text(style.displayName) }
                )
            }
        }

        Divider()

        // 表情符号频率
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "表情符号使用频率",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmojiFrequency.values().forEach { frequency ->
                    FilterChip(
                        selected = behaviorRules.emojiFrequency == frequency,
                        onClick = { viewModel.updateEmojiFrequency(frequency) },
                        label = { Text(frequency.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Divider()

        // 正式程度
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "正式程度",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormalityLevel.values().forEach { level ->
                    FilterChip(
                        selected = behaviorRules.formalityLevel == level,
                        onClick = { viewModel.updateFormalityLevel(level) },
                        label = { Text(level.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 示例对话步骤
 */
@Composable
fun ExamplesStep(viewModel: CharacterCustomizationViewModel) {
    val exampleDialogues by viewModel.exampleDialogues.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "示例对话（可选）",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            "提供一些示例对话，帮助AI更好地理解角色的说话风格",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 示例对话列表
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exampleDialogues.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "还没有示例对话",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "点击下方按钮添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                exampleDialogues.forEachIndexed { index, dialogue ->
                    ExampleDialogueCard(
                        dialogue = dialogue,
                        onDelete = { viewModel.removeExampleDialogue(index) }
                    )
                }
            }
        }

        // 添加按钮
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加示例对话")
        }
    }

    // 添加对话对话框
    if (showAddDialog) {
        AddExampleDialogueDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { user, assistant ->
                viewModel.addExampleDialogue(user, assistant)
                showAddDialog = false
            }
        )
    }
}

/**
 * 示例对话卡片
 */
@Composable
fun ExampleDialogueCard(
    dialogue: ExampleDialogue,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "示例对话",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 用户消息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        "用户",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        dialogue.user,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Divider()

            // AI回复
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        "角色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        dialogue.assistant,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 添加示例对话对话框
 */
@Composable
fun AddExampleDialogueDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var userMessage by remember { mutableStateOf("") }
    var assistantMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加示例对话") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    label = { Text("用户说") },
                    placeholder = { Text("用户会说什么...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = assistantMessage,
                    onValueChange = { assistantMessage = it },
                    label = { Text("角色回复") },
                    placeholder = { Text("角色会怎么回复...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(userMessage, assistantMessage) },
                enabled = userMessage.isNotBlank() && assistantMessage.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

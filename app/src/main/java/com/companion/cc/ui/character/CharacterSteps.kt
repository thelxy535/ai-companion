package com.companion.cc.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction

@Composable
fun BehaviorStep(viewModel: CharacterCustomizationViewModel) {
    val behaviorRules by viewModel.behaviorRules.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "行为风格设定",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "定义角色的说话方式和行为习惯",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BehaviorChoiceGroup(
            title = "回复风格",
            options = ResponseStyle.values().toList(),
            selected = behaviorRules.responseStyle,
            label = { it.displayName },
            onSelect = viewModel::updateResponseStyle
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        BehaviorChoiceGroup(
            title = "表情符号使用频率",
            options = EmojiFrequency.values().toList(),
            selected = behaviorRules.emojiFrequency,
            label = { it.displayName },
            onSelect = viewModel::updateEmojiFrequency
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        BehaviorChoiceGroup(
            title = "正式程度",
            options = FormalityLevel.values().toList(),
            selected = behaviorRules.formalityLevel,
            label = { it.displayName },
            onSelect = viewModel::updateFormalityLevel
        )
    }
}

@Composable
private fun <T> BehaviorChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            modifier = Modifier.padding(bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selected,
                    onClick = { onSelect(option) }
                )
                Text(
                    text = label(option),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ExamplesStep(viewModel: CharacterCustomizationViewModel) {
    val exampleDialogues by viewModel.exampleDialogues.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val openAddDialog = rememberTactileAction { showAddDialog = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "示例对话（可选）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "提供一些示例对话，帮助 AI 理解角色的说话风格",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (exampleDialogues.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "还没有示例对话",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "点击下方按钮添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                exampleDialogues.forEachIndexed { index, dialogue ->
                    ExampleDialogueRow(
                        dialogue = dialogue,
                        onDelete = { viewModel.removeExampleDialogue(index) }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = openAddDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加示例对话")
        }
    }

    if (showAddDialog) {
        AddExampleDialogueDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { user, assistant ->
                viewModel.addExampleDialogue(
                    ExampleDialogue(user = user, assistant = assistant)
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExampleDialogueCard(
    dialogue: ExampleDialogue,
    onDelete: () -> Unit
) {
    ExampleDialogueRow(dialogue = dialogue, onDelete = onDelete)
}

@Composable
private fun ExampleDialogueRow(
    dialogue: ExampleDialogue,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val openDeleteDialog = rememberTactileAction { showDeleteDialog = true }
    val confirmDelete = rememberTactileAction(
        gesture = TactileGesture.DESTRUCTIVE_CONFIRM
    ) {
        onDelete()
        showDeleteDialog = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
            IconButton(onClick = openDeleteDialog) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        DialogueLine(
            icon = Icons.Default.Person,
            speaker = "用户",
            message = dialogue.user,
            iconTint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        DialogueLine(
            icon = Icons.Default.Star,
            speaker = "角色",
            message = dialogue.assistant,
            iconTint = MaterialTheme.colorScheme.secondary
        )
        Divider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除示例对话") },
            text = { Text("确定要删除这条示例对话吗？") },
            confirmButton = {
                TextButton(
                    onClick = confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DialogueLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    speaker: String,
    message: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                speaker,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddExampleDialogueDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var userMessage by remember { mutableStateOf("") }
    var assistantMessage by remember { mutableStateOf("") }
    val canConfirm = userMessage.isNotBlank() && assistantMessage.isNotBlank()
    val confirmAdd = rememberTactileAction(enabled = canConfirm) {
        onConfirm(userMessage, assistantMessage)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加示例对话") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    label = { Text("用户说") },
                    placeholder = { Text("用户会说什么…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = assistantMessage,
                    onValueChange = { assistantMessage = it },
                    label = { Text("角色回复") },
                    placeholder = { Text("角色会怎么回复…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirmAdd, enabled = canConfirm) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

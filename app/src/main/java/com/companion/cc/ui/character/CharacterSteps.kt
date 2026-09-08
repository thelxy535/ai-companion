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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.components.V9PMChoiceRow
import com.companion.cc.ui.components.V9PMDialogSurface
import com.companion.cc.ui.components.V9PMIconButton
import com.companion.cc.ui.components.V9PMTextField
import com.companion.cc.domain.model.EmojiFrequency
import com.companion.cc.domain.model.ExampleDialogue
import com.companion.cc.domain.model.FormalityLevel
import com.companion.cc.domain.model.ResponseStyle
import com.companion.cc.ui.theme.TactileGesture
import com.companion.cc.ui.theme.rememberTactileAction

@Composable
fun BehaviorStep(viewModel: CharacterCustomizationViewModel) {
    val behaviorRules by viewModel.behaviorRules.collectAsState()
    val creatorNotes by viewModel.creatorNotes.collectAsState()
    val systemPromptOverride by viewModel.systemPromptOverride.collectAsState()
    val postHistoryInstructions by viewModel.postHistoryInstructions.collectAsState()

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

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        V9PMTextField(
            value = behaviorRules.topicPreferences.joinToString(", "),
            onValueChange = { value ->
                viewModel.updateBehaviorRules(
                    behaviorRules.copy(
                        topicPreferences = value.split(",", "，")
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .take(20)
                    )
                )
            },
            label = "偏好话题（可选）",
            placeholder = "例如：音乐、旅行、电影",
            modifier = Modifier.fillMaxWidth()
        )

        V9PMTextField(
            value = behaviorRules.avoidTopics.joinToString(", "),
            onValueChange = { value ->
                viewModel.updateBehaviorRules(
                    behaviorRules.copy(
                        avoidTopics = value.split(",", "，")
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .take(20)
                    )
                )
            },
            label = "避免话题（可选）",
            placeholder = "例如：不想讨论的内容",
            modifier = Modifier.fillMaxWidth()
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // V9PM 第 7 项：角色语音音色（TTS 音调/语速）
        val voiceConfig by viewModel.voiceConfig.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "语音音色",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "TA 朗读消息时的音调与语速",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("音调", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                Slider(
                    value = (voiceConfig.pitch - 0.5f) / 1.5f,
                    onValueChange = { viewModel.updateVoiceConfig(voiceConfig.copy(pitch = 0.5f + it * 1.5f)) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    String.format(java.util.Locale.US, "%.1f×", voiceConfig.pitch),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(44.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("语速", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                Slider(
                    value = (voiceConfig.speed - 0.5f) / 1.5f,
                    onValueChange = { viewModel.updateVoiceConfig(voiceConfig.copy(speed = 0.5f + it * 1.5f)) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    String.format(java.util.Locale.US, "%.1f×", voiceConfig.speed),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(44.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "高级角色卡指令",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            V9PMTextField(
                value = creatorNotes,
                onValueChange = viewModel::updateCreatorNotes,
                label = "创作者备注",
                placeholder = "记录设计意图或使用提示，不会暴露给角色",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )
            V9PMTextField(
                value = systemPromptOverride,
                onValueChange = viewModel::updateSystemPromptOverride,
                label = "系统指令追加（可选）",
                placeholder = "补充必须遵守的角色规则",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 6
            )
            V9PMTextField(
                value = postHistoryInstructions,
                onValueChange = viewModel::updatePostHistoryInstructions,
                label = "历史消息后指令（可选）",
                placeholder = "每轮对话历史之后追加的提醒",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )
        }

        CharacterBookEditor(
            entries = viewModel.characterBook.collectAsState().value,
            onAdd = viewModel::updateCharacterBook
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
            val isSelected = option == selected
            V9PMChoiceRow(
                title = label(option),
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier.padding(vertical = 2.dp),
                trailing = {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                }
            )
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

        V9PMActionButton(
            label = "添加示例对话",
            icon = Icons.Default.Add,
            onClick = openAddDialog,
            modifier = Modifier.fillMaxWidth()
        )
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
            V9PMIconButton(Icons.Default.Delete, "删除", openDeleteDialog, size = 42.dp, iconSize = 18.dp, tint = MaterialTheme.colorScheme.error)
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
        V9PMDialogSurface(onDismissRequest = { showDeleteDialog = false }) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("删除示例对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("确定要删除这条示例对话吗？", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V9PMActionButton(
                        label = "取消",
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    V9PMActionButton(
                        label = "删除",
                        onClick = confirmDelete,
                        destructive = true,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                }
            }
        }
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

    V9PMDialogSurface(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("添加示例对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            V9PMTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                label = "用户说",
                placeholder = "用户会说什么…",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            V9PMTextField(
                value = assistantMessage,
                onValueChange = { assistantMessage = it },
                label = "角色回复",
                placeholder = "角色会怎么回复…",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V9PMActionButton(label = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 40.dp)
                V9PMActionButton(label = "添加", onClick = confirmAdd, enabled = canConfirm, modifier = Modifier.weight(1f), height = 40.dp)
            }
        }
    }
}

/** V9PM 第 6 项：角色知识库（Character Book）编辑器。 */
@Composable
private fun CharacterBookEditor(
    entries: List<com.companion.cc.domain.model.CharacterBookEntry>,
    onAdd: (List<com.companion.cc.domain.model.CharacterBookEntry>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "角色知识库（Character Book）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            V9PMActionButton(
                label = "添加条目",
                icon = Icons.Default.Add,
                onClick = { showAddDialog = true },
                height = 36.dp
            )
        }
        Text(
            "常驻条目始终生效；关键词条目在用户提到关键词时自动注入角色设定。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (entries.isEmpty()) {
            Text(
                "还没有知识库条目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "关键词：${entry.keys.joinToString("、").ifBlank { "（常驻）" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.constant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    V9PMIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "删除知识库条目",
                        onClick = {
                            onAdd(entries.filterIndexed { i, _ -> i != index })
                        },
                        size = 42.dp,
                        iconSize = 18.dp,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCharacterBookEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { entry ->
                onAdd(entries + entry)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddCharacterBookEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (com.companion.cc.domain.model.CharacterBookEntry) -> Unit
) {
    var keys by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var constant by remember { mutableStateOf(false) }
    val canConfirm = content.isNotBlank()
    val confirmAdd = rememberTactileAction(enabled = canConfirm) {
        onConfirm(
            com.companion.cc.domain.model.CharacterBookEntry(
                keys = keys.split(",", "，").map(String::trim).filter(String::isNotBlank).distinct().take(10),
                content = content.trim(),
                enabled = true,
                constant = constant,
                priority = 0,
                insertionOrder = 0
            )
        )
    }

    V9PMDialogSurface(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("添加知识库条目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "常驻条目始终注入；关键词条目在用户提到关键词时触发。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            V9PMTextField(
                value = keys,
                onValueChange = { keys = it },
                label = "关键词（可选）",
                placeholder = "逗号分隔，例如：音乐、猫、上海",
                modifier = Modifier.fillMaxWidth()
            )
            V9PMTextField(
                value = content,
                onValueChange = { content = it },
                label = "条目内容 *",
                placeholder = "角色必须知道的设定或知识",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("常驻条目（始终生效）", style = MaterialTheme.typography.bodyMedium)
                com.companion.cc.ui.components.V9PMSwitch(
                    checked = constant,
                    onCheckedChange = { constant = it }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V9PMActionButton(label = "取消", onClick = onDismiss, modifier = Modifier.weight(1f), height = 40.dp)
                V9PMActionButton(label = "添加", onClick = confirmAdd, enabled = canConfirm, modifier = Modifier.weight(1f), height = 40.dp)
            }
        }
    }
}

package com.companion.cc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.companion.cc.data.local.entity.TagEntity
import com.companion.cc.ui.theme.GlassDialogSurface
import com.companion.cc.ui.theme.LocalVisualTheme
import com.companion.cc.ui.theme.CompactGlassSurface
import com.companion.cc.ui.theme.TagColorAdapter
import java.util.UUID

/**
 * 标签选择对话框
 * 用于为消息添加/移除标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSelectionDialog(
    messageId: String,
    availableTags: List<TagEntity>,
    selectedTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onTagToggle: (TagEntity) -> Unit,
    onCreateTag: (String, String) -> Unit,  // name, color
    onManageTags: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "管理标签",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标签列表
                if (availableTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无标签，点击下方创建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(availableTags) { tag ->
                            val isSelected = selectedTags.any { it.id == tag.id }
                            TagSelectionItem(
                                tag = tag,
                                isSelected = isSelected,
                                onToggle = { onTagToggle(tag) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建标签")
                    }

                    OutlinedButton(
                        onClick = onManageTags,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("管理标签")
                    }
                }
            }
        }
    }

    // 创建标签对话框
    if (showCreateDialog) {
        CreateTagDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, color ->
                onCreateTag(name, color)
                showCreateDialog = false
            }
        )
    }
}

/**
 * 单个标签选择项
 */
@Composable
private fun TagSelectionItem(
    tag: TagEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    CompactGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        onClick = onToggle,
        fillOverride = if (isSelected) {
            TagColorAdapter.parse(tag.color, LocalVisualTheme.current.tokens.accent).copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        borderOverride = if (isSelected) TagColorAdapter.parse(tag.color, LocalVisualTheme.current.tokens.accent) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签颜色指示器
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TagColorAdapter.parse(tag.color, LocalVisualTheme.current.tokens.accent))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = TagColorAdapter.parse(tag.color, LocalVisualTheme.current.tokens.accent),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 创建标签对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTagDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, color: String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(tagColors[0]) }

    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "创建标签",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标签名称
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 颜色选择
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagColors.forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (tagName.isNotBlank()) {
                                onCreate(tagName.trim(), selectedColor)
                            }
                        },
                        enabled = tagName.isNotBlank()
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}

/**
 * 颜色选择圆圈
 */
@Composable
private fun ColorCircle(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = TagColorAdapter.parse(color, LocalVisualTheme.current.tokens.accent)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(parsedColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = TagColorAdapter.contentColor(parsedColor),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 标签管理界面（编辑、删除标签）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementDialog(
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onCreateTag: (String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassDialogSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "管理所有标签",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标签列表
                if (tags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tags) { tag ->
                            TagManagementItem(
                                tag = tag,
                                onDelete = { onDeleteTag(tag) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 新建按钮
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建标签")
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTagDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, color ->
                onCreateTag(name, color)
                showCreateDialog = false
            }
        )
    }
}

/**
 * 标签管理项
 */
@Composable
private fun TagManagementItem(
    tag: TagEntity,
    onDelete: () -> Unit
) {
    CompactGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        fillOverride = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TagColorAdapter.parse(tag.color, LocalVisualTheme.current.tokens.accent))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除标签",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// 预设标签颜色
private val tagColors = listOf(
    "#2196F3", // 蓝色
    "#4CAF50", // 绿色
    "#FF9800", // 橙色
    "#F44336", // 红色
    "#9C27B0", // 紫色
    "#00BCD4", // 青色
    "#FF5722", // 深橙
    "#795548"  // 棕色
)

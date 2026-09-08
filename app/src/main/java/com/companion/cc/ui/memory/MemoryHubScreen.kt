package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.staggerRise
import com.companion.cc.ui.designsystem.rememberStaggerFirstPlay
import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.domain.character.CharacterCatalog
import com.companion.cc.domain.model.CharacterAvatarResolver
import com.companion.cc.ui.components.Avatar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.companion.cc.ui.designsystem.materialSurface
import com.companion.cc.ui.designsystem.smoothCorner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemoryHubViewModel @Inject constructor(
    characterCatalog: CharacterCatalog,
    settingsManager: com.companion.cc.data.local.SettingsManager,
    private val memoryDao: com.companion.cc.data.local.dao.MemoryDao,
    private val currentUserProvider: com.companion.cc.domain.identity.CurrentUserProvider
) : ViewModel() {
    val characters = characterCatalog.observeCharacters()
    val materialStyle = settingsManager.materialStyleFlow
        .map { name -> runCatching { com.companion.cc.ui.designsystem.MaterialStyle.valueOf(name) }
            .getOrDefault(com.companion.cc.ui.designsystem.MaterialStyle.GLASS) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly,
            com.companion.cc.ui.designsystem.MaterialStyle.GLASS)

    // 记忆库磁贴的真实条数（设计稿规格："N 条 · 搜索与筛选"）
    val memoryCount: kotlinx.coroutines.flow.StateFlow<Int> = currentUserProvider.userId
        .flatMapLatest { uid -> memoryDao.observeMemoryCount(uid) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, 0)

    // 选中角色：跨导航保留（跳转记忆库/树/回顾后返回仍是原选中）
    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: kotlinx.coroutines.flow.StateFlow<String?> = _selectedId.asStateFlow()

    fun selectCharacter(id: String) {
        _selectedId.value = if (_selectedId.value == id) null else id
    }
}

@Composable
fun MemoryHubScreen(
    onOpenMemory: (String) -> Unit,
    onOpenTree: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onOpenFavorites: (String) -> Unit,
    viewModel: MemoryHubViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsStateWithLifecycle(initialValue = emptyList())
    val materialStyleV7 by viewModel.materialStyle.collectAsStateWithLifecycle(initialValue = com.companion.cc.ui.designsystem.MaterialStyle.GLASS)
    val memoryCount by viewModel.memoryCount.collectAsStateWithLifecycle(initialValue = 0)
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle(initialValue = null)
    val selected = characters.firstOrNull { it.id == selectedId }
    val staggerPlay = rememberStaggerFirstPlay("memory")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 24.dp)
    ) {
        Text("记忆", modifier = Modifier.staggerRise(staggerPlay, 0), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.padding(4.dp))
        Text("先选择角色，再进入 TA 的记忆", modifier = Modifier.staggerRise(staggerPlay, 1, intervalMs = 60), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // V7 role-pick：角色横滑选择器（选中带 aura 光环）
        val selNight = LocalVisualTheme.current.tokens.backdrop.isDark
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(characters, key = { _, c -> c.id }) { index, character ->
                val av = CharacterAvatarResolver.resolve(character)
                val isSelected = selectedId == character.id
                // V9PM role-pick 选中定稿：accent 2dp 圆环（间隙 3dp）+底部 4dp accent 点 spring 弹入；未选 hairline 1dp 环；按压 spring 0.92
                val pickInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val pickPressed by pickInteraction.collectIsPressedAsState()
                val pickScale by animateFloatAsState(if (pickPressed) 0.92f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pickScale")
                val pickDot by animateFloatAsState(if (isSelected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pickDot")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .staggerRise(staggerPlay, index)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(interactionSource = pickInteraction, indication = null) { viewModel.selectCharacter(character.id) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .scale(pickScale)
                            .border(
                                2.dp,
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    selNight -> androidx.compose.ui.graphics.Color(0x1FFFFFFF)
                                    else -> androidx.compose.ui.graphics.Color(0x2E1A2030)
                                },
                                CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(av.avatarUrl, av.emoji, size = 52.dp, showRing = true, backgroundColor = com.companion.cc.ui.components.auraColorFor(character.name))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .scale(pickDot)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        character.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // V7 tiles：2 列玻璃网格（记忆库/记忆树/回顾/收藏）
        val tileShape = smoothCorner(24.dp)   // V9PM 连续大圆角
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MemoryTile("记忆库", "${memoryCount} 条 · 搜索与筛选", Icons.Default.MenuBook, tileShape, selNight, materialStyle = materialStyleV7, Modifier.weight(1f), accentTint = androidx.compose.ui.graphics.Color(0xFF5B8DEF)) {
                    selectedId?.let(onOpenMemory)
                }
                MemoryTile("记忆树", "拖动整理节点", Icons.Default.AccountTree, tileShape, selNight, materialStyle = materialStyleV7, Modifier.weight(1f), accentTint = androidx.compose.ui.graphics.Color(0xFF3FA7A0)) {
                    selectedId?.let(onOpenTree)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MemoryTile("回顾", "重温对话片段", Icons.Default.History, tileShape, selNight, materialStyle = materialStyleV7, Modifier.weight(1f), accentTint = androidx.compose.ui.graphics.Color(0xFF8B7FD6)) {
                    selectedId?.let(onOpenReview)
                }
                MemoryTile("收藏", "珍藏的对话", Icons.Default.Bookmark, tileShape, selNight, materialStyle = materialStyleV7, Modifier.weight(1f), accentTint = androidx.compose.ui.graphics.Color(0xFFD69A4F)) {
                    selectedId?.let(onOpenFavorites)
                }
            }
        }
    }
}

/**
 * V7 tile：玻璃磁贴（18 圆角 + hairline 描边 + 图标/标题/副文）
 */
@Composable
fun MemoryTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    shape: androidx.compose.ui.graphics.Shape,
    night: Boolean,
    materialStyle: com.companion.cc.ui.designsystem.MaterialStyle,
    modifier: Modifier = Modifier,
    accentTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val inkFaint = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = accentTint ?: MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .materialSurface(
                style = materialStyle,
                isNight = night,
                shape = shape
            )
            // V9PM：hairline 由 materialSurface 内部绘制（Generic border 位图路径有 0 尺寸崩溃隐患，已移除）
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // V7 设计稿：细线图标直接放（无圆底容器）
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ink)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = inkFaint)
    }
}


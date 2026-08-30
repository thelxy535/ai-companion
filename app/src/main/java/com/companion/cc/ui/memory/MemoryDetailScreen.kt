package com.companion.cc.ui.memory

import com.companion.cc.ui.designsystem.auroraScreenBackground
import com.companion.cc.ui.theme.LocalVisualTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryRelationEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.rememberTactileAction
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryDetailState(
    val node: MemoryNodeEntity? = null,
    val evidence: List<MemoryEvidenceEntity> = emptyList(),
    val versions: List<MemoryVersionEntity> = emptyList(),
    val relations: List<MemoryRelationEntity> = emptyList()
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    var state by mutableStateOf(MemoryDetailState())
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    private var loadedCompanionId: String? = null
    private var loadedNodeId: String? = null

    fun load(companionId: String, nodeId: String) {
        loadedCompanionId = companionId
        loadedNodeId = nodeId
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            if (userId == null) {
                state = MemoryDetailState()
                return@launch
            }
            val scopeKey = MemoryScopeKey.forCharacter(userId, companionId)
            val node = repository.findNode(scopeKey, nodeId)
            state = MemoryDetailState(
                node = node,
                evidence = node?.let { repository.getEvidence(scopeKey, nodeId) }.orEmpty(),
                versions = node?.let { repository.getVersions(scopeKey, nodeId) }.orEmpty(),
                relations = node?.let { repository.getRelations(scopeKey, nodeId) }.orEmpty()
            )
        }
    }

    fun suppressNode(now: Long = System.currentTimeMillis()) {
        mutateNode { scopeKey, nodeId -> repository.suppressNode(scopeKey, nodeId, now) }
    }

    fun restoreNode(now: Long = System.currentTimeMillis()) {
        mutateNode { scopeKey, nodeId -> repository.restoreNode(scopeKey, nodeId, now) }
    }

    fun restoreVersion(version: Int, now: Long = System.currentTimeMillis()) {
        mutateNode { scopeKey, nodeId -> repository.restoreVersion(scopeKey, nodeId, version, now) }
    }

    fun resolveRelation(
        relation: MemoryRelationEntity,
        accepted: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        val companionId = loadedCompanionId ?: return
        val nodeId = loadedNodeId ?: return
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            if (userId == null) {
                actionError = "当前用户不可用，请重试"
                return@launch
            }
            val scopeKey = MemoryScopeKey.forCharacter(userId, companionId)
            repository.resolveRelation(scopeKey, relation, accepted, now).fold(
                onSuccess = {
                    actionError = null
                    load(companionId, nodeId)
                },
                onFailure = { error ->
                    actionError = error.message ?: "关系操作失败，请重试"
                }
            )
        }
    }

    fun clearActionError() {
        actionError = null
    }

    private fun mutateNode(
        mutation: suspend (scopeKey: String, nodeId: String) -> Result<Unit>
    ) {
        val companionId = loadedCompanionId ?: return
        val nodeId = loadedNodeId ?: return
        viewModelScope.launch {
            val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            if (userId == null) {
                actionError = "当前用户不可用，请重试"
                return@launch
            }
            val scopeKey = MemoryScopeKey.forCharacter(userId, companionId)
            mutation(scopeKey, nodeId).fold(
                onSuccess = {
                    actionError = null
                    load(companionId, nodeId)
                },
                onFailure = { error ->
                    actionError = error.message ?: "记忆操作失败，请重试"
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    companionId: String,
    nodeId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(companionId, nodeId) { viewModel.load(companionId, nodeId) }
    val state = viewModel.state
    val suppressNode = rememberTactileAction {
        viewModel.suppressNode()
    }
    val restoreNode = rememberTactileAction {
        viewModel.restoreNode()
    }
    Scaffold(
        modifier = Modifier.auroraScreenBackground(LocalVisualTheme.current.tokens.backdrop.isDark),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 44.dp),
                title = { Text("记忆详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val node = state.node
        if (node == null) {
            Text("记忆不存在", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    UtilitySection(title = node.title) {
                        Text(node.content)
                        UtilityDivider()
                        Text(
                            "${node.kind} · ${node.status} · 版本 ${node.currentVersion}",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        UtilityDivider()
                        when (node.status) {
                            "active" -> Button(
                                onClick = suppressNode,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("禁止召回")
                            }
                            "do_not_recall", "deleted" -> Button(
                                onClick = restoreNode,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("恢复召回")
                            }
                        }
                        if (viewModel.actionError != null) {
                            Text(
                                viewModel.actionError.orEmpty(),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                item {
                    UtilitySection(title = "来源证据") {
                        if (state.evidence.isEmpty()) {
                            Text("暂无来源证据", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.evidence.forEachIndexed { index, evidence ->
                                if (index > 0) UtilityDivider()
                                Text("${evidence.evidenceRole} · ${evidence.summarySnapshot}")
                            }
                        }
                    }
                }
                item {
                    UtilitySection(title = "历史版本") {
                        if (state.versions.isEmpty()) {
                            Text("暂无历史版本", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.versions.forEachIndexed { index, version ->
                                if (index > 0) UtilityDivider()
                                Text("v${version.version} · ${version.changeReason} · ${version.content}")
                                Button(
                                    onClick = rememberTactileAction {
                                        viewModel.restoreVersion(version.version)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("恢复此版本")
                                }
                            }
                        }
                    }
                }
                item {
                    UtilitySection(title = "关联记忆") {
                        if (state.relations.isEmpty()) {
                            Text("暂无关联记忆", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.relations.forEachIndexed { index, relation ->
                                if (index > 0) UtilityDivider()
                                val relatedNodeId = if (relation.fromNodeId == node.id) {
                                    relation.toNodeId
                                } else {
                                    relation.fromNodeId
                                }
                                Text("${relation.relationType} · $relatedNodeId · ${relation.status}")
                                if (relation.status == "proposed" || relation.status == "conflict") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = rememberTactileAction {
                                                viewModel.resolveRelation(relation, accepted = true)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("接受关联")
                                        }
                                        OutlinedButton(
                                            onClick = rememberTactileAction {
                                                viewModel.resolveRelation(relation, accepted = false)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("拒绝关联")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

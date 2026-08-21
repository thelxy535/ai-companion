package com.companion.cc.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.companion.cc.data.local.entity.MemoryEvidenceEntity
import com.companion.cc.data.local.entity.MemoryNodeEntity
import com.companion.cc.data.local.entity.MemoryVersionEntity
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.ui.components.UtilityDivider
import com.companion.cc.ui.components.UtilitySection
import com.companion.cc.ui.theme.GlassSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryDetailState(val node: MemoryNodeEntity? = null, val evidence: List<MemoryEvidenceEntity> = emptyList(), val versions: List<MemoryVersionEntity> = emptyList())

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(private val repository: MemoryRepository) : ViewModel() {
    var state by mutableStateOf(MemoryDetailState())
        private set

    fun load(nodeId: String) {
        viewModelScope.launch {
            val node = repository.findNode(nodeId)
            state = MemoryDetailState(node, repository.getEvidence(nodeId), repository.getVersions(nodeId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    nodeId: String,
    onNavigateBack: () -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(nodeId) { viewModel.load(nodeId) }
    val state = viewModel.state
    Scaffold(topBar = {
        GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), useStrongFill = true) {
            TopAppBar(
                title = { Text("记忆详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    }) { padding ->
        val node = state.node
        if (node == null) {
            Text("记忆不存在", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    UtilitySection(title = node.title) {
                        Text(node.content)
                        UtilityDivider()
                        Text(
                            "${node.kind} · ${node.status} · 版本 ${node.currentVersion}",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            }
                        }
                    }
                }
            }
        }
    }
}

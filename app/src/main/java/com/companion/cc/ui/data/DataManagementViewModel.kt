package com.companion.cc.ui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.data.local.dao.MemoryDao
import com.companion.cc.data.local.dao.MemoryNodeDao
import com.companion.cc.data.local.dao.VectorMemoryDao
import com.companion.cc.data.local.repository.MemoryCapsuleV2Transfer
import com.companion.cc.data.local.repository.MemoryCapsuleV2Importer
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.memory.MemoryScopeKey
import com.companion.cc.domain.usecase.ExportDataUseCase
import com.companion.cc.domain.usecase.ImportDataUseCase
import com.companion.cc.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.io.File
import javax.inject.Inject

data class CapsuleExport(
    val fileName: String,
    val json: String
)

data class StorageInfo(
    val databaseSize: String = "计算中...",
    val messageCount: String = "计算中...",
    val memoryCount: String = "计算中...",
    val cacheSize: String = "计算中..."
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val settingsManager: SettingsManager,
    private val memoryDao: MemoryDao,
    private val memoryNodeDao: MemoryNodeDao,
    private val vectorMemoryDao: VectorMemoryDao,
    private val currentUserProvider: CurrentUserProvider,
    @ApplicationContext private val context: Context,
    private val capsuleTransfer: MemoryCapsuleV2Transfer,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {
    private var storageJob: Job? = null

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    private val _pendingCapsuleExport = MutableStateFlow<CapsuleExport?>(null)
    val pendingCapsuleExport: StateFlow<CapsuleExport?> = _pendingCapsuleExport.asStateFlow()

    private val _pendingDataExport = MutableStateFlow<CapsuleExport?>(null)
    val pendingDataExport: StateFlow<CapsuleExport?> = _pendingDataExport.asStateFlow()

    private val _lastCapsuleImportReport = MutableStateFlow<MemoryCapsuleV2Importer.Report?>(null)
    val lastCapsuleImportReport: StateFlow<MemoryCapsuleV2Importer.Report?> =
        _lastCapsuleImportReport.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    init {
        loadStorageInfo()
    }

    /**
     * 加载存储信息
     */
    fun loadStorageInfo() {
        storageJob?.cancel()
        storageJob = viewModelScope.launch {
            val userId = currentUserProvider.requireUserId()
            combine(
                messageRepository.observeMessageCount(userId),
                memoryDao.observeMemoryCount(userId),
                memoryNodeDao.observeActiveCount(userId),
                vectorMemoryDao.observeCount(userId)
            ) { messageCount, legacyMemoryCount, nodeCount, vectorCount ->
                val dbFile = context.getDatabasePath("cc_database")
                StorageInfo(
                    databaseSize = formatFileSize(databaseSizeBytes(dbFile)),
                    messageCount = messageCount.toString(),
                    memoryCount = (legacyMemoryCount + nodeCount + vectorCount).toString(),
                    cacheSize = formatFileSize(getFolderSize(context.cacheDir))
                )
            }.collect { _storageInfo.value = it }
        }
    }

    /**
     * 清除所有对话记录
     */
    fun clearAllMessages() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                messageRepository.deleteAllMessages(currentUserProvider.requireUserId())
                _resultMessage.value = "所有对话记录已清除"
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "清除消息失败", e)
                _resultMessage.value = "清除失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 导出 Memory Capsule v2。文件写入由 UI 通过 SAF 完成。
     */
    fun exportMemoryCapsule(companionId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val userId = currentUserProvider.requireUserId()
                val scopeKey = MemoryScopeKey.forCharacter(userId, companionId)
                val json = capsuleTransfer.exportCapsuleJson(scopeKey).getOrThrow()
                _pendingCapsuleExport.value = CapsuleExport(
                    fileName = "memory_capsule_${System.currentTimeMillis()}.json",
                    json = json
                )
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "Memory Capsule 导出失败", e)
                _resultMessage.value = "记忆胶囊导出失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 导入 Memory Capsule v2。调用方负责从 SAF 读取 JSON。
     */
    fun importMemoryCapsule(json: String, companionId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val userId = currentUserProvider.requireUserId()
                val scopeKey = MemoryScopeKey.forCharacter(userId, companionId)
                val report = capsuleTransfer.importCapsuleJson(json, scopeKey).getOrThrow()
                _lastCapsuleImportReport.value = report
                _resultMessage.value = "记忆胶囊导入完成：新增 ${report.insertedNodes} 个记忆节点"
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "Memory Capsule 导入失败", e)
                _resultMessage.value = "记忆胶囊导入失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val json = exportDataUseCase()
                _pendingDataExport.value = CapsuleExport(
                    fileName = "cc_switch_export_${System.currentTimeMillis()}.json",
                    json = json
                )
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "导出失败", e)
                _resultMessage.value = "导出失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** 导入由 SAF 读取的 JSON 数据。 */
    fun importData(jsonData: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                when (val result = importDataUseCase(jsonData)) {
                    is ImportResult.Success -> {
                        _resultMessage.value = "成功导入 ${result.messagesImported} 条消息"
                    }
                    is ImportResult.Error -> {
                        _resultMessage.value = "导入失败: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "导入失败", e)
                _resultMessage.value = "导入失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 重置设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                settingsManager.resetUserPreferences()
                _resultMessage.value = "设置已重置"
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "重置设置失败", e)
                _resultMessage.value = "重置失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun consumePendingCapsuleExport() {
        _pendingCapsuleExport.value = null
    }

    fun consumePendingDataExport() {
        _pendingDataExport.value = null
    }

    fun clearResultMessage() {
        _resultMessage.value = null
    }

    companion object {
        fun validateImportOwner(
            payloadUserId: String,
            currentUserId: String
        ): Result<Unit> = runCatching {
            val payload = payloadUserId.trim()
            val current = currentUserId.trim()
            require(payload.isNotBlank()) {
                "导入数据缺少用户标识"
            }
            require(current.isNotBlank()) {
                "当前用户标识为空"
            }
            require(payload == current) {
                "导入数据属于其他用户"
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun databaseSizeBytes(databaseFile: File): Long = listOf(
        databaseFile,
        File(databaseFile.path + "-wal"),
        File(databaseFile.path + "-shm")
    ).filter(File::exists).sumOf(File::length)

    private fun getFolderSize(folder: File): Long {
        var size = 0L
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
}

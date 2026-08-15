package com.companion.cc.ui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.companion.cc.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class StorageInfo(
    val databaseSize: String = "计算中...",
    val messageCount: String = "计算中...",
    val memoryCount: String = "计算中...",
    val cacheSize: String = "计算中..."
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    init {
        loadStorageInfo()
    }

    /**
     * 加载存储信息
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                // 数据库大小
                val dbFile = context.getDatabasePath("companion.db")
                val dbSize = if (dbFile.exists()) {
                    formatFileSize(dbFile.length())
                } else {
                    "0 B"
                }

                // 消息数量
                val messageCount = messageRepository.getMessageCount("default")

                // 记忆数量（暂时设为 0，需要 MemoryRepository）
                val memoryCount = 0

                // 缓存大小
                val cacheDir = context.cacheDir
                val cacheSize = formatFileSize(getFolderSize(cacheDir))

                _storageInfo.value = StorageInfo(
                    databaseSize = dbSize,
                    messageCount = messageCount.toString(),
                    memoryCount = memoryCount.toString(),
                    cacheSize = cacheSize
                )
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "加载存储信息失败", e)
            }
        }
    }

    /**
     * 清除所有对话记录
     */
    fun clearAllMessages(userId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                messageRepository.deleteAllMessages(userId)
                _resultMessage.value = "所有对话记录已清除"
                loadStorageInfo() // 刷新存储信息
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "清除消息失败", e)
                _resultMessage.value = "清除失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 导出数据
     */
    fun exportData(userId: String, companionId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // 获取所有消息
                val messages = messageRepository.getMessages(
                    userId = userId,
                    companionId = companionId,
                    limit = 10000
                ).first()

                // 创建导出数据结构
                val exportData = mapOf(
                    "version" to "1.0",
                    "exportTime" to System.currentTimeMillis(),
                    "userId" to userId,
                    "companionId" to companionId,
                    "messages" to messages.map { msg ->
                        mapOf(
                            "id" to msg.id,
                            "role" to msg.role.name,
                            "content" to msg.content,
                            "timestamp" to msg.timestamp
                        )
                    }
                )

                // 序列化为 JSON
                val json = com.google.gson.Gson().toJson(exportData)

                // 保存到文件
                val fileName = "companion_export_${System.currentTimeMillis()}.json"
                val file = File(context.getExternalFilesDir(null), fileName)
                file.writeText(json)

                _resultMessage.value = "数据已导出到: ${file.absolutePath}"
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "导出失败", e)
                _resultMessage.value = "导出失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 导入数据
     */
    fun importData(jsonData: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // 解析 JSON
                val gson = com.google.gson.Gson()
                val data = gson.fromJson(jsonData, Map::class.java) as Map<*, *>

                // 验证数据格式
                if (data["version"] != "1.0") {
                    _resultMessage.value = "不支持的数据版本"
                    return@launch
                }

                // 获取消息列表
                val messagesData = data["messages"] as? List<*>
                if (messagesData == null) {
                    _resultMessage.value = "数据格式错误：缺少消息列表"
                    return@launch
                }

                // 转换为 Message 对象
                val messages = messagesData.mapNotNull { msgData ->
                    try {
                        val msgMap = msgData as Map<*, *>
                        com.companion.cc.domain.model.Message(
                            id = msgMap["id"] as String,
                            userId = data["userId"] as String,
                            companionId = data["companionId"] as String,
                            role = com.companion.cc.domain.model.MessageRole.valueOf(msgMap["role"] as String),
                            content = msgMap["content"] as String,
                            timestamp = (msgMap["timestamp"] as Number).toLong(),
                            emotion = msgMap["emotion"] as? String,
                            importance = (msgMap["importance"] as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("DataManagementVM", "解析消息失败", e)
                        null
                    }
                }

                // 导入到数据库
                messageRepository.saveMessages(messages)

                _resultMessage.value = "成功导入 ${messages.size} 条消息"
                loadStorageInfo() // 刷新存储信息
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
                val prefs = context.getSharedPreferences("companion_settings", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                _resultMessage.value = "设置已重置"
            } catch (e: Exception) {
                android.util.Log.e("DataManagementVM", "重置设置失败", e)
                _resultMessage.value = "重置失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearResultMessage() {
        _resultMessage.value = null
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

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

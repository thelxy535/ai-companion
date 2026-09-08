package com.companion.cc.domain.memory

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.data.local.dao.MemorySourceDao
import com.companion.cc.data.local.repository.MemoryRepository
import com.companion.cc.domain.identity.CurrentUserProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 叙事定期自演化（V9PM 深度②）。
 *
 * 每日检查每个角色 scope：若当前叙事之后有新对话，则让模型基于
 * "当前叙事 + 近期对话窗口" 判断关系理解是否需要更新；有实质变化时
 * 直接演化写入（版本化 + 证据可追溯）。模型输出不合法时跳过该 scope，不写脏数据。
 */
@HiltWorker
class NarrativeEvolutionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val sourceDao: MemorySourceDao,
    private val repository: MemoryRepository,
    private val llmClient: ReflectionLlmClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            ?: return Result.success()
        val now = System.currentTimeMillis()
        val scopes = sourceDao.findAllInScopePrefix("user:$userId:companion:%")
            .map { it.scopeKey }
            .distinct()
        var retryableFailure = false

        scopes.forEach { scopeKey ->
            val current = runCatching {
                repository.getActiveNodesByKind(scopeKey, NarrativeKinds.RELATIONSHIP, 1, now).firstOrNull()
            }.getOrNull()
            val latest = sourceDao.findRecentBefore(scopeKey, Long.MAX_VALUE, 1).firstOrNull()
                ?: return@forEach
            // 无新对话：叙事保持不变
            if (current != null && latest.occurredAt <= current.updatedAt) return@forEach

            val window = sourceDao.findRecentBefore(scopeKey, now, WINDOW_SIZE)
            if (window.isEmpty()) return@forEach
            val transcript = window.asReversed().joinToString("\n\n") { src ->
                "[${windowTimeFormat.format(src.occurredAt)}] ${src.contentSnapshot}"
            }
            val currentText = current?.content ?: "（还没有叙事，这是第一段关系理解）"

            runCatching {
                val raw = llmClient.complete(
                    systemPrompt = "你只输出合法 JSON。",
                    userPrompt = NarrativeEvolutionParser.evolutionPrompt(currentText, transcript),
                    maxTokens = 400
                )
                val parsed = NarrativeEvolutionParser.parse(raw).getOrThrow()
                if (parsed.changed && parsed.narrative.isNotBlank()) {
                    repository.evolveNarrative(
                        scopeKey = scopeKey,
                        content = parsed.narrative,
                        confidence = 0.7,
                        sourceIds = window.map { it.id },
                        now = System.currentTimeMillis()
                    ).getOrThrow()
                }
            }.onFailure { error ->
                val transient = error is java.io.IOException || error is retrofit2.HttpException
                if (transient) retryableFailure = true
                // 解析失败/无内容：跳过该 scope，不写脏数据
            }
        }
        return if (retryableFailure) Result.retry() else Result.success()
    }

    private val windowTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)

    private companion object {
        const val WINDOW_SIZE = 30
    }
}

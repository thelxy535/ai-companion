package com.companion.cc.domain.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.identity.CurrentUserProvider
import com.companion.cc.domain.model.Message
import com.companion.cc.domain.model.MessageRole
import com.companion.cc.domain.repository.MessageRepository
import com.companion.cc.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * V9PM 主动消息 v1：TA 先开口
 *
 * 触发规则：时段窗口 9–11 / 20–22；静默 22:30–8:00（窗口外一律不打扰）；
 * 24h ≤2 条且间隔 ≥4h；总开关 SettingsManager.isNotificationPrefEnabled。
 * 消息以 ASSISTANT 角色入库（timestamp=now），用户打开 App 时已在对话流里；
 * 通知文案 = 消息内容本身（像微信）。
 */
@HiltWorker
class ProactiveMessageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currentUserProvider: CurrentUserProvider,
    private val messageRepository: MessageRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!SettingsManager.isNotificationPrefEnabled(context)) return Result.success()

        val now = LocalDateTime.now()
        val hourOfDay = now.hour + now.minute / 60.0
        val inWindow = hourOfDay in 9.0..11.0 || hourOfDay in 20.0..22.0
        if (!inWindow) return Result.success()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = now.toLocalDate().toString()
        val sentToday = if (prefs.getString(KEY_DATE, null) == today) prefs.getInt(KEY_COUNT, 0) else 0
        if (sentToday >= MAX_PER_DAY) return Result.success()
        val lastAt = prefs.getLong(KEY_LAST_AT, 0L)
        if (System.currentTimeMillis() - lastAt < MIN_GAP_MS) return Result.success()

        val userId = runCatching { currentUserProvider.requireUserId() }.getOrNull()
            ?: return Result.success()

        // v1：发给最近对话过的那个 TA（多角色轮换策略留给 v2）
        val companionId = messageRepository.getAllMessages(userId)
            .first()
            .maxByOrNull { it.timestamp }
            ?.companionId
            ?: return Result.success()

        val used = prefs.getString(KEY_USED, "")
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val picked = ProactiveMessagePool.pick(companionId, isMorning = now.hour < 12, recentUsed = used)

        messageRepository.saveMessage(
            Message(
                id = "${System.currentTimeMillis()}_proactive",
                userId = userId,
                companionId = companionId,
                role = MessageRole.ASSISTANT,
                content = picked.content,
                timestamp = System.currentTimeMillis(),
            )
        )

        prefs.edit()
            .putLong(KEY_LAST_AT, System.currentTimeMillis())
            .putString(KEY_DATE, today)
            .putInt(KEY_COUNT, sentToday + 1)
            .putString(KEY_USED, (used + picked.content).takeLast(10).joinToString(SEP))
            .apply()

        NotificationHelper.sendProactiveMessageNotification(context, picked.companionName, picked.content)
        return Result.success()
    }

    private companion object {
        const val PREFS_NAME = "proactive_messages"
        const val KEY_LAST_AT = "last_proactive_at"
        const val KEY_DATE = "proactive_date"
        const val KEY_COUNT = "proactive_count_today"
        const val KEY_USED = "proactive_used_recent"
        const val SEP = "\u0001"
        const val MAX_PER_DAY = 2
        const val MIN_GAP_MS = 4L * 3_600_000L
    }
}

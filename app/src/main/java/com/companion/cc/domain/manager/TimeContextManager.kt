package com.companion.cc.domain.manager

import com.companion.cc.data.local.dao.InteractionTimeDao
import com.companion.cc.data.local.entity.InteractionTimeEntity
import com.companion.cc.domain.model.TimeContext
import com.companion.cc.domain.model.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时间上下文管理器
 *
 * 追踪和分析对话的时间维度
 */
@Singleton
class TimeContextManager @Inject constructor(
    private val interactionTimeDao: InteractionTimeDao
) {

    /**
     * 记录交互时间
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param sessionId 会话ID
     * @param messageCount 本次交互的消息数
     */
    suspend fun recordInteraction(
        userId: String,
        companionId: String,
        sessionId: String,
        messageCount: Int = 1
    ) = withContext(Dispatchers.IO) {
        val entity = InteractionTimeEntity(
            userId = userId,
            companionId = companionId,
            timestamp = LocalDateTime.now(),
            messageCount = messageCount,
            sessionId = sessionId
        )

        interactionTimeDao.insert(entity)
        android.util.Log.d("TimeContextManager", "记录交互时间：$userId - $companionId")
    }

    /**
     * 获取时间上下文（在当前交互之前）
     *
     * 显式命名，强调读取的是"上一次交互"的上下文，不包括尚未记录的当前消息
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 时间上下文
     */
    suspend fun getContextBeforeInteraction(
        userId: String,
        companionId: String
    ): TimeContext = getTimeContext(userId, companionId)

    /**
     * 获取时间上下文
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 时间上下文
     */
    suspend fun getTimeContext(
        userId: String,
        companionId: String
    ): TimeContext = withContext(Dispatchers.IO) {
        val currentTime = LocalDateTime.now()

        // 获取上次交互时间
        val lastInteraction = interactionTimeDao.getLastInteraction(userId, companionId)

        // 计算时间间隔
        val intervalDuration = lastInteraction?.let {
            Duration.between(it.timestamp, currentTime)
        }

        // 获取今天的交互次数
        val startOfDay = currentTime.truncatedTo(ChronoUnit.DAYS)
        val todayCount = interactionTimeDao.getTodayInteractionCount(
            userId = userId,
            companionId = companionId,
            startOfDay = startOfDay
        )

        // 获取本周的交互次数
        val startOfWeek = currentTime.minusDays(currentTime.dayOfWeek.value.toLong() - 1)
            .truncatedTo(ChronoUnit.DAYS)
        val weeklyCount = interactionTimeDao.getWeeklyInteractionCount(
            userId = userId,
            companionId = companionId,
            startOfWeek = startOfWeek
        )

        // 构建时间上下文
        TimeContext(
            currentTime = currentTime,
            lastInteractionTime = lastInteraction?.timestamp,
            intervalDuration = intervalDuration,
            todayInteractionCount = todayCount,
            weeklyInteractionCount = weeklyCount,
            timePeriod = TimePeriod.fromHour(currentTime.hour),
            dayOfWeek = currentTime.dayOfWeek,
            isWeekend = currentTime.dayOfWeek.value in 6..7
        ).also {
            android.util.Log.d("TimeContextManager", "时间上下文：${it.toHumanReadable()}")
        }
    }

    /**
     * 清理过期的交互记录
     *
     * @param daysToKeep 保留天数（默认30天）
     */
    suspend fun cleanupOldRecords(daysToKeep: Int = 30) = withContext(Dispatchers.IO) {
        val before = LocalDateTime.now().minusDays(daysToKeep.toLong())
        val deletedCount = interactionTimeDao.cleanupOldRecords(before)
        android.util.Log.d("TimeContextManager", "清理过期记录：$deletedCount 条")
    }

    /**
     * 删除指定用户和伴侣的所有交互记录
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     */
    suspend fun clearInteractions(
        userId: String,
        companionId: String
    ) = withContext(Dispatchers.IO) {
        val deletedCount = interactionTimeDao.deleteAllForUserAndCompanion(userId, companionId)
        android.util.Log.d("TimeContextManager", "清除交互记录：$deletedCount 条")
    }
}

package com.companion.cc.domain.manager

import com.companion.cc.data.local.dao.UserEventDao
import com.companion.cc.data.local.entity.UserEventEntity
import com.companion.cc.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 事件追踪器
 *
 * 追踪和记录用户行为事件
 */
@Singleton
class EventTracker @Inject constructor(
    private val userEventDao: UserEventDao,
    private val gson: Gson
) {

    /**
     * 记录事件
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param eventType 事件类型
     * @param metadata 元数据
     */
    suspend fun trackEvent(
        userId: String,
        companionId: String,
        eventType: EventType,
        metadata: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val entity = UserEventEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            companionId = companionId,
            eventType = eventType,
            timestamp = LocalDateTime.now(),
            metadata = gson.toJson(metadata)
        )

        userEventDao.insert(entity)
        android.util.Log.d("EventTracker", "记录事件：${eventType.displayName}")
    }

    /**
     * 获取最近的事件
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param limit 数量限制
     * @return 事件列表
     */
    suspend fun getRecentEvents(
        userId: String,
        companionId: String,
        limit: Int = 20
    ): List<UserEvent> = withContext(Dispatchers.IO) {
        userEventDao.getRecentEvents(userId, companionId, limit)
            .map { it.toDomainModel(gson) }
    }

    /**
     * 分析消息节奏
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @param minutesWindow 时间窗口（分钟）
     * @return 消息节奏分析
     */
    suspend fun analyzeMessageRhythm(
        userId: String,
        companionId: String,
        minutesWindow: Int = 5
    ): MessageRhythm = withContext(Dispatchers.IO) {
        val since = LocalDateTime.now().minusMinutes(minutesWindow.toLong())
        val recentMessages = userEventDao.getRecentMessageEvents(userId, companionId, since)

        if (recentMessages.isEmpty()) {
            return@withContext MessageRhythm(
                recentMessageCount = 0,
                averageInterval = 0L,
                isRapidFire = false,
                isPaused = false,
                urgencyLevel = UrgencyLevel.NORMAL
            )
        }

        val messageCount = recentMessages.size

        // 计算平均间隔（秒）
        val averageInterval = if (messageCount > 1) {
            val firstTime = recentMessages.last().timestamp
            val lastTime = recentMessages.first().timestamp
            val totalSeconds = Duration.between(firstTime, lastTime).seconds
            totalSeconds / (messageCount - 1)
        } else {
            0L
        }

        // 判断是否连续快速发送（平均间隔小于10秒，且数量>=3）
        val isRapidFire = messageCount >= 3 && averageInterval < 10

        // 判断是否停顿很久（平均间隔大于60秒）
        val isPaused = averageInterval > 60

        // 判断急迫程度
        val urgencyLevel = when {
            isRapidFire -> UrgencyLevel.URGENT
            averageInterval < 30 -> UrgencyLevel.EAGER
            averageInterval > 60 -> UrgencyLevel.RELAXED
            else -> UrgencyLevel.NORMAL
        }

        MessageRhythm(
            recentMessageCount = messageCount,
            averageInterval = averageInterval,
            isRapidFire = isRapidFire,
            isPaused = isPaused,
            urgencyLevel = urgencyLevel
        )
    }

    /**
     * 分析用户状态
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 用户状态
     */
    suspend fun analyzeUserState(
        userId: String,
        companionId: String
    ): UserState = withContext(Dispatchers.IO) {
        // 获取最近5分钟的所有事件
        val since = LocalDateTime.now().minusMinutes(5)
        val recentEvents = userEventDao.getEventsSince(userId, companionId, since)

        // 判断是否活跃（最近5分钟有事件）
        val isActive = recentEvents.isNotEmpty()

        // 分析消息节奏以获取急迫程度
        val messageRhythm = analyzeMessageRhythm(userId, companionId, 5)
        val urgencyLevel = messageRhythm.urgencyLevel

        // 判断专注程度
        val attentionLevel = when {
            recentEvents.isEmpty() -> AttentionLevel.CASUAL
            recentEvents.size >= 10 -> AttentionLevel.IMMERSED
            recentEvents.size >= 5 -> AttentionLevel.FOCUSED
            else -> AttentionLevel.CASUAL
        }

        UserState(
            isActive = isActive,
            urgencyLevel = urgencyLevel,
            attentionLevel = attentionLevel
        )
    }

    /**
     * 获取事件上下文
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     * @return 事件上下文
     */
    suspend fun getEventContext(
        userId: String,
        companionId: String
    ): EventContext = withContext(Dispatchers.IO) {
        val recentEvents = getRecentEvents(userId, companionId, 20)
        val messageRhythm = analyzeMessageRhythm(userId, companionId, 5)
        val userState = analyzeUserState(userId, companionId)

        EventContext(
            recentEvents = recentEvents,
            messageRhythm = messageRhythm,
            userState = userState
        )
    }

    /**
     * 清理过期的事件记录
     *
     * @param daysToKeep 保留天数（默认30天）
     */
    suspend fun cleanupOldEvents(daysToKeep: Int = 30) = withContext(Dispatchers.IO) {
        val before = LocalDateTime.now().minusDays(daysToKeep.toLong())
        val deletedCount = userEventDao.cleanupOldEvents(before)
        android.util.Log.d("EventTracker", "清理过期事件：$deletedCount 条")
    }

    /**
     * 清除指定用户和伴侣的所有事件
     *
     * @param userId 用户ID
     * @param companionId 伴侣ID
     */
    suspend fun clearEvents(
        userId: String,
        companionId: String
    ) = withContext(Dispatchers.IO) {
        val deletedCount = userEventDao.deleteAllForUserAndCompanion(userId, companionId)
        android.util.Log.d("EventTracker", "清除事件记录：$deletedCount 条")
    }
}

/**
 * 扩展函数：将实体转换为领域模型
 */
private fun UserEventEntity.toDomainModel(gson: Gson): UserEvent {
    val metadataMap = try {
        gson.fromJson(metadata, Map::class.java) as? Map<String, String> ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }

    return UserEvent(
        id = id,
        userId = userId,
        companionId = companionId,
        eventType = eventType,
        timestamp = timestamp,
        metadata = metadataMap
    )
}

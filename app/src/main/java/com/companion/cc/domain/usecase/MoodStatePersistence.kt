package com.companion.cc.domain.usecase

import androidx.room.*
import com.companion.cc.data.local.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 角色状态实体
 */
@Entity(tableName = "mood_states")
data class MoodStateEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val companionId: String,

    // 缪斯专用
    val coldness: Int = 0,
    val perfunctoryCount: Int = 0,
    val deepTopicCount: Int = 0,

    // 小璨专用
    val warmth: Int? = null,
    val careLevel: Int? = null,
    val negativeCount: Int? = null,
    val positiveEventCount: Int? = null,

    // 共用
    val relationshipLevel: Int = 1,
    val lastInteractionTime: Long = 0,
    val updatedAt: Long = 0
)

/**
 * 状态DAO
 */
@Dao
interface MoodStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: MoodStateEntity)

    @Query("SELECT * FROM mood_states WHERE id = :id")
    suspend fun getState(id: String): MoodStateEntity?
}

/**
 * 状态持久化管理器
 */
@Singleton
class MoodStatePersistence @Inject constructor(
    private val database: AppDatabase
) {

    suspend fun saveMuseState(userId: String, companionId: String, state: MuseMoodState) {
        val entity = MoodStateEntity(
            id = "${userId}_${companionId}",
            userId = userId,
            companionId = companionId,
            coldness = state.coldness,
            relationshipLevel = state.relationshipLevel,
            lastInteractionTime = state.lastInteractionTime,
            perfunctoryCount = state.perfunctoryCount,
            deepTopicCount = state.deepTopicCount,
            updatedAt = System.currentTimeMillis()
        )
        database.moodStateDao().insertOrUpdate(entity)
    }

    suspend fun loadMuseState(userId: String, companionId: String): MuseMoodState {
        val entity = database.moodStateDao().getState("${userId}_${companionId}")

        return if (entity != null) {
            MuseMoodState(
                coldness = entity.coldness,
                relationshipLevel = entity.relationshipLevel,
                lastInteractionTime = entity.lastInteractionTime,
                perfunctoryCount = entity.perfunctoryCount,
                deepTopicCount = entity.deepTopicCount
            )
        } else {
            MuseMoodState()
        }
    }

    suspend fun saveXiaoCanState(userId: String, companionId: String, state: XiaoCanMoodState) {
        val entity = MoodStateEntity(
            id = "${userId}_${companionId}",
            userId = userId,
            companionId = companionId,
            warmth = state.warmth,
            careLevel = state.getCareLevel(),  // 计算得出
            relationshipLevel = state.relationshipLevel,
            lastInteractionTime = state.lastInteractionTime,
            negativeCount = state.negativeCount,
            positiveEventCount = state.positiveEventCount,
            updatedAt = System.currentTimeMillis()
        )
        database.moodStateDao().insertOrUpdate(entity)
    }

    suspend fun loadXiaoCanState(userId: String, companionId: String): XiaoCanMoodState {
        val entity = database.moodStateDao().getState("${userId}_${companionId}")

        return if (entity != null) {
            XiaoCanMoodState(
                warmth = entity.warmth ?: 50,
                relationshipLevel = entity.relationshipLevel,
                lastInteractionTime = entity.lastInteractionTime,
                negativeCount = entity.negativeCount ?: 0,
                positiveEventCount = entity.positiveEventCount ?: 0
            )
        } else {
            XiaoCanMoodState()
        }
    }
}

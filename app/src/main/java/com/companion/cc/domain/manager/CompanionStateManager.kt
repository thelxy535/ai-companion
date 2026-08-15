package com.companion.cc.domain.manager

import com.companion.cc.domain.model.Message
import com.companion.cc.domain.usecase.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 角色状态管理器
 * 统一管理缪斯和小璨的状态
 */
@Singleton
class CompanionStateManager @Inject constructor(
    private val moodStatePersistence: MoodStatePersistence,
    private val museColdnessEngine: MuseColdnessEngine,
    private val xiaoCanWarmthEngine: XiaoCanWarmthEngine
) {

    /**
     * 加载角色状态
     */
    suspend fun loadState(userId: String, companionId: String): Any {
        return when (companionId) {
            "muse" -> moodStatePersistence.loadMuseState(userId, companionId)
            "xiaocan" -> moodStatePersistence.loadXiaoCanState(userId, companionId)
            else -> throw IllegalArgumentException("Unknown companion: $companionId")
        }
    }

    /**
     * 更新角色状态
     */
    suspend fun updateState(
        userId: String,
        companionId: String,
        currentState: Any,
        userInput: String,
        conversationHistory: List<Message>
    ): Any {
        return when (companionId) {
            "muse" -> {
                val state = currentState as MuseMoodState
                val behavior = museColdnessEngine.detectBehavior(userInput, conversationHistory)
                val updatedState = museColdnessEngine.updateColdness(state, userInput, behavior)

                // 保存到数据库
                moodStatePersistence.saveMuseState(userId, companionId, updatedState)

                updatedState
            }
            "xiaocan" -> {
                val state = currentState as XiaoCanMoodState
                val behavior = xiaoCanWarmthEngine.detectBehavior(userInput, conversationHistory)
                val updatedState = xiaoCanWarmthEngine.updateWarmth(state, userInput, behavior)

                // 保存到数据库
                moodStatePersistence.saveXiaoCanState(userId, companionId, updatedState)

                updatedState
            }
            else -> currentState
        }
    }

    /**
     * 检测特殊模式
     */
    fun detectSpecialMode(
        companionId: String,
        state: Any,
        userInput: String
    ): String? {
        return when (companionId) {
            "muse" -> {
                val museState = state as MuseMoodState
                when {
                    museState.shouldEnableColdMode() -> "cold"
                    museState.shouldEnableVerboseMode(userInput) -> "verbose"
                    else -> null
                }
            }
            "xiaocan" -> {
                val xiaoCanState = state as XiaoCanMoodState
                when {
                    xiaoCanState.shouldEnableCareMode() -> "care"
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * 获取API参数
     */
    fun getApiParameters(
        companionId: String,
        state: Any,
        specialMode: String?
    ): ApiParameters {
        return when (companionId) {
            "muse" -> {
                val museState = state as MuseMoodState
                MuseResponseStyler().getApiParameters(museState, specialMode)
            }
            "xiaocan" -> {
                val xiaoCanState = state as XiaoCanMoodState
                XiaoCanResponseStyler().getApiParameters(xiaoCanState, specialMode)
            }
            else -> ApiParameters(0.7, 0.8, 100, 0.4, 0.2)
        }
    }
}

/**
 * API参数
 */
data class ApiParameters(
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
    val frequencyPenalty: Double,
    val presencePenalty: Double
)

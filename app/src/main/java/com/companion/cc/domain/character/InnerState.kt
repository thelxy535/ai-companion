package com.companion.cc.domain.character

import com.companion.cc.data.local.SettingsManager
import com.companion.cc.domain.model.EmotionalState
import com.companion.cc.domain.model.Mood
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/** A small, human-shaped continuity layer for what the character is carrying forward. */
data class InnerState(
    val caringAbout: String = "",
    val unfinishedThought: String = "",
    val lookingForwardTo: String = "",
    val avoiding: String = "",
    val scene: String = "",
    val lifeThread: String = "",
    val emotionalAftertaste: String = "",
    val relationshipStage: String = "熟悉",
    val updatedAt: Long = 0L,
    val currentNeed: String = "",
    val shareImpulse: Float = 0.5f,
    val hesitation: Float = 0.5f,
    val lastInteractionMeaning: String = "",
    val socialBattery: Float = 0.65f,
    val energy: Float = 0.65f,
    val sleepDebt: Float = 0f,
    val lastMemoryResonance: String = "",
    /** The last observable action, retained so a reply can continue rather than reset it. */
    val recentAction: String = "",
    val currentActivity: String = "",
    val sceneUpdatedAt: Long = 0L,
    /** Nullable for backward-compatible Gson reads of states saved before this field existed. */
    val physicalScene: SceneState? = null
)

data class SceneState(
    val location: String = "",
    val room: String = "",
    val posture: String = "",
    val clothing: String = "",
    val heldItem: String = "",
    val activity: String = "",
    val transitionedAt: Long = 0L
) {
    fun isEmpty(): Boolean = location.isBlank() && room.isBlank() && posture.isBlank() &&
        clothing.isBlank() && heldItem.isBlank() && activity.isBlank()
}

/** Reads new structured state, or derives a conservative snapshot from legacy fields. */
fun InnerState.effectiveSceneState(): SceneState {
    physicalScene?.takeUnless { it.isEmpty() }?.let { return it }
    val legacy = scene.trim()
    return SceneState(
        location = when {
            legacy.contains("家") -> "家"
            legacy.contains("外面") -> "外面"
            else -> ""
        },
        room = when {
            legacy.contains("厨房") -> "厨房"
            legacy.contains("书桌") -> "书房"
            legacy.contains("房间") || legacy.contains("床") -> "房间"
            else -> ""
        },
        posture = when {
            legacy.contains("床") -> "躺着"
            else -> ""
        },
        activity = currentActivity,
        transitionedAt = sceneUpdatedAt
    )
}

/** Small, gradual state changes shared by chat and proactive-message entry points. */
object InnerStateTransition {

    fun afterInteraction(
        previous: InnerState,
        temperament: TemperamentProfile,
        emotionalState: EmotionalState,
        userMessage: String,
        now: Long = System.currentTimeMillis()
    ): InnerState {
        val tiredness = (1f - emotionalState.energy).coerceIn(0f, 1f)
        val warmth = when (emotionalState.mood) {
            Mood.HAPPY, Mood.EXCITED, Mood.CONTENT -> 0.18f
            Mood.SAD, Mood.ANXIOUS -> -0.04f
            Mood.TIRED -> -0.08f
            Mood.CALM -> 0.04f
        }
        val hesitation = (previous.hesitation * 0.72f +
            (temperament.interruptionCost * 0.18f) + tiredness * 0.18f).coerceIn(0f, 1f)
        val shareImpulse = (previous.shareImpulse * 0.72f +
            temperament.shareImpulse * 0.18f + warmth).coerceIn(0f, 1f)
        val meaning = userMessage.trim().take(80)
        return previous.copy(
            shareImpulse = shareImpulse,
            hesitation = hesitation,
            emotionalAftertaste = when {
                emotionalState.mood == Mood.TIRED -> "还带着一点疲惫，不急着把话说满"
                warmth > 0.1f -> "这段互动让心情亮了一点"
                else -> previous.emotionalAftertaste
            },
            lastInteractionMeaning = meaning.ifBlank { previous.lastInteractionMeaning },
            updatedAt = now
        )
    }
}

@Singleton
class InnerStateRepository @Inject constructor(
    private val settings: SettingsManager,
    private val gson: Gson
) {
    suspend fun load(userId: String, characterId: String): InnerState =
        settings.loadInnerState(key(userId, characterId))?.let {
            runCatching { gson.fromJson(it, InnerState::class.java) }.getOrNull()
        } ?: InnerState()

    suspend fun save(userId: String, characterId: String, state: InnerState) {
        settings.saveInnerState(key(userId, characterId), gson.toJson(state))
    }

    private fun key(userId: String, characterId: String) = "$userId:$characterId"
}

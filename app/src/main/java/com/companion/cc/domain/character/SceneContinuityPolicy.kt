package com.companion.cc.domain.character

/**
 * Keeps small physical details coherent without turning the character into a
 * deterministic stage manager. A scene changes only when the reply contains
 * an explicit transition; ordinary location words are not enough.
 */
object SceneContinuityPolicy {
    private val transitionPatterns = listOf(
        Regex("(?:准备出门|要出门了|已经出门|刚出门|走出门|来到|到了|回到家|回家了|进了厨房|走进厨房|回到书桌|坐到书桌|上床|躺到床上)") to { _: String -> "发生了转场" },
        Regex("(?:在外面|在厨房|在自己的桌边|在房间里|在床上)") to { text: String -> text.take(40) }
    )

    fun update(
        previous: InnerState,
        assistantText: String,
        action: String?,
        now: Long,
        declaredScene: SceneState? = null
    ): InnerState {
        val combined = listOf(assistantText, action.orEmpty()).joinToString(" ").trim()
        if (combined.isBlank()) return previous.copy(updatedAt = now)

        val explicitScene = transitionPatterns.firstNotNullOfOrNull { (pattern, formatter) ->
            pattern.findAll(combined).firstOrNull { match ->
                val prefix = combined.substring(0, match.range.first).takeLast(3)
                prefix.none { it == '不' || it == '没' || it == '非' }
            }?.value?.let { matched -> matched to formatter(matched) }
        }
        val nextScene = if (explicitScene != null) {
            val (matchedTransition, fallbackScene) = explicitScene
            when {
                matchedTransition.contains("回家") || matchedTransition.contains("到家") -> "刚回到家"
                matchedTransition.contains("厨房") -> "在厨房"
                matchedTransition.contains("书桌") -> "在自己的桌边"
                matchedTransition.contains("床") -> "在房间里，状态放松"
                matchedTransition.contains("房间") -> "在房间里"
                matchedTransition.contains("准备出门") || matchedTransition.contains("要出门") -> previous.scene
                matchedTransition.contains("已经出门") || matchedTransition.contains("刚出门") ||
                    matchedTransition.contains("走出门") || matchedTransition.contains("外面") -> "在外面"
                else -> fallbackScene
            }
        } else previous.scene

        val base = previous.effectiveSceneState()
        val inferred = inferScene(base, combined, explicitScene?.first, action, now)
        val physicalScene = mergeDeclared(inferred, declaredScene, now)
        return previous.copy(
            scene = nextScene,
            currentActivity = action?.trim()?.takeIf { it.isNotBlank() } ?: previous.currentActivity,
            recentAction = action?.trim()?.takeIf { it.isNotBlank() } ?: previous.recentAction,
            sceneUpdatedAt = if (nextScene != previous.scene) now else previous.sceneUpdatedAt,
            physicalScene = physicalScene,
            updatedAt = now
        )
    }

    private fun inferScene(
        base: SceneState,
        combined: String,
        transition: String?,
        action: String?,
        now: Long
    ): SceneState {
        val location = when {
            transition?.contains("回家") == true || transition?.contains("到家") == true -> "家"
            transition?.contains("已经出门") == true || transition?.contains("刚出门") == true ||
                transition?.contains("走出门") == true || transition?.contains("外面") == true -> "外面"
            else -> base.location
        }
        val room = when {
            transition?.contains("厨房") == true -> "厨房"
            transition?.contains("书桌") == true -> "书房"
            transition?.contains("床") == true -> "卧室"
            else -> base.room
        }
        val posture = when {
            combined.contains("起身") || combined.contains("站起") -> "站着"
            combined.contains("躺下") || combined.contains("躺到") -> "躺着"
            combined.contains("坐下") || combined.contains("坐到") -> "坐着"
            else -> base.posture
        }
        val clothing = Regex("""(?:换上|穿上|穿好)([^，。！？s]{1,8})""")
            .find(combined)?.groupValues?.get(1)?.trim().orEmpty().ifBlank { base.clothing }
        val heldItem = when {
            Regex("(?:放下|放回|搁下)").containsMatchIn(combined) -> ""
            else -> Regex("""(?:拿起|端起|握住)([^，。！？s]{1,8})""")
                .find(combined)?.groupValues?.get(1)?.trim().orEmpty().ifBlank { base.heldItem }
        }
        return base.copy(
            location = location,
            room = room,
            posture = posture,
            clothing = clothing,
            heldItem = heldItem,
            activity = action?.trim()?.takeIf { it.isNotBlank() } ?: base.activity,
            transitionedAt = if (transition != null) now else base.transitionedAt
        )
    }

    private fun mergeDeclared(base: SceneState, declared: SceneState?, now: Long): SceneState {
        if (declared == null) return base
        return base.copy(
            location = declared.location.ifBlank { base.location },
            room = declared.room.ifBlank { base.room },
            posture = declared.posture.ifBlank { base.posture },
            clothing = declared.clothing.ifBlank { base.clothing },
            heldItem = declared.heldItem.ifBlank { base.heldItem },
            activity = declared.activity.ifBlank { base.activity },
            transitionedAt = now
        )
    }
}

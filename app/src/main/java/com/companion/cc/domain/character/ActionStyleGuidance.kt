package com.companion.cc.domain.character

/** Translates personality into soft action-writing tendencies, never quotas. */
object ActionStyleGuidance {
    fun build(temperament: TemperamentProfile, state: InnerState): String = buildString {
        append("\n\n【动作表达习惯】\n")
        when {
            temperament.expressiveness <= 0.35f || temperament.interruptionCost >= 0.7f ->
                append("- 这个角色的动作很少、很轻，更多通过停顿和简短回应表达；没有必要时省略动作。\n")
            temperament.expressiveness >= 0.7f ->
                append("- 这个角色的动作可以更鲜活、更外显，但仍要贴合当下情绪，不必每条都有。\n")
            else ->
                append("- 动作表达自然克制，只在能补充对白情绪时出现，不必每条都有。\n")
        }
        if (state.recentAction.isNotBlank()) {
            append("- 上一个动作是：${state.recentAction}。不要原样重复；可以承接它，也可以完全不写动作。\n")
        }
        append("- 不用动作汇报行程，不连续堆叠多个动作，不凭动作制造没有经过的换场。")
    }
}

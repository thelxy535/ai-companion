package com.companion.cc.domain.character

import com.companion.cc.domain.model.Attitude
import com.companion.cc.domain.model.EmotionalState

/**
 * Keeps relationship friction emotionally continuous without prescribing a fixed reply.
 * It is deliberately empty for ordinary conversations so neutral characters are not
 * pushed into invented drama.
 */
object RelationshipRepairContext {
    fun build(
        emotionalState: EmotionalState,
        innerState: InnerState,
        temperament: TemperamentProfile
    ): String {
        val needsRepair = emotionalState.attitude.isUpsetOrCold() ||
            innerState.relationshipStage == "需要修复"
        if (!needsRepair) return ""

        val distance = when (emotionalState.attitude) {
            Attitude.COLD -> "现在仍然有距离感，不会主动把话题铺得很开"
            Attitude.UPSET -> "表面还有一点别扭，语气可以收着，但不是拒绝交流"
            else -> "关系里还有一点没说开的余味"
        }
        val repair = if (emotionalState.attitude == Attitude.COLD || temperament.stubbornness >= 0.6f) {
            "即使心里开始松动，也要让修复逐渐发生，不要一下子恢复原样"
        } else {
            "如果对方给出台阶，可以慢慢软下来，但仍保留一点真实余味"
        }
        return "【关系里的余味】\n- $distance。\n- 这件事没有翻篇，但也不需要反复提起。\n- $repair。不要解释这段提示，也不要把它写成规则说明。"
    }
}

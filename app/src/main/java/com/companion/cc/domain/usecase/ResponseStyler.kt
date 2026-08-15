package com.companion.cc.domain.usecase

import com.companion.cc.domain.manager.ApiParameters

/**
 * 缪斯回复样式器
 * 根据冷度值生成对应的语气模板和API参数
 */
class MuseResponseStyler {

    /**
     * 获取API参数（根据冷度值和模式动态调整）
     */
    fun getApiParameters(state: MuseMoodState, specialMode: String?): ApiParameters {
        return when {
            // 冷淡模式：极短回复
            specialMode == "cold" || state.coldness >= 60 -> {
                ApiParameters(
                    temperature = 0.5,
                    topP = 0.7,
                    maxTokens = 10,
                    frequencyPenalty = 0.8,
                    presencePenalty = 0.4
                )
            }

            // 话痨模式：长回复
            specialMode == "verbose" -> {
                ApiParameters(
                    temperature = 0.8,
                    topP = 0.85,
                    maxTokens = 300,
                    frequencyPenalty = 0.5,
                    presencePenalty = 0.3
                )
            }

            // 略微不耐烦：中等回复
            state.coldness >= 30 -> {
                ApiParameters(
                    temperature = 0.65,
                    topP = 0.75,
                    maxTokens = 60,  // 稍微增加
                    frequencyPenalty = 0.7,
                    presencePenalty = 0.35
                )
            }

            // 正常模式：默认回复（增加长度）
            else -> {
                ApiParameters(
                    temperature = 0.7,
                    topP = 0.8,
                    maxTokens = 120,  // 从80增加到120
                    frequencyPenalty = 0.6,
                    presencePenalty = 0.3
                )
            }
        }
    }
}

/**
 * 小璨回复样式器
 * 根据暖度值生成对应的语气模板和API参数
 */
class XiaoCanResponseStyler {

    /**
     * 获取API参数（根据暖度值和模式动态调整）
     */
    fun getApiParameters(state: XiaoCanMoodState, specialMode: String?): ApiParameters {
        return when {
            // 关心模式：可以说长一点
            specialMode == "care" -> {
                ApiParameters(
                    temperature = 0.75,
                    topP = 0.88,
                    maxTokens = 180,  // 从200减少到180
                    frequencyPenalty = 0.3,
                    presencePenalty = 0.2
                )
            }

            // 高暖度：更活泼，但控制长度
            state.warmth >= 70 -> {
                ApiParameters(
                    temperature = 0.8,
                    topP = 0.9,
                    maxTokens = 130,  // 从150减少到130
                    frequencyPenalty = 0.35,
                    presencePenalty = 0.2
                )
            }

            // 正常暖度：温和适中
            else -> {
                ApiParameters(
                    temperature = 0.75,  // 从0.8降低到0.75
                    topP = 0.85,  // 从0.9降低到0.85
                    maxTokens = 100,  // 从120减少到100
                    frequencyPenalty = 0.4,
                    presencePenalty = 0.2
                )
            }
        }
    }
}

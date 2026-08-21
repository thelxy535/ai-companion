package com.companion.cc.domain.model

/**
 * 图片理解服务来源。
 *
 * 该设置只控制视觉理解，不影响主对话模型供应商。
 */
enum class VisionServiceMode {
    GEMINI,
    SELF_HOSTED;

    companion object {
        fun fromStoredValue(value: String?): VisionServiceMode =
            entries.firstOrNull { it.name == value } ?: GEMINI
    }
}

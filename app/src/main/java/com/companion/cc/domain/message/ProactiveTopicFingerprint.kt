package com.companion.cc.domain.message

/** Small deterministic topic buckets used only to diversify fallback proactive messages. */
object ProactiveTopicFingerprint {
    fun of(text: String): String {
        val value = text.lowercase()
        return when {
            value.containsAny("吃", "饭", "喝", "水", "饿") -> "food"
            value.containsAny("睡", "早安", "起床", "晚安", "夜") -> "sleep"
            value.containsAny("雨", "云", "月亮", "天气", "窗") -> "weather"
            value.containsAny("画", "书", "看到", "小店", "音乐") -> "interest"
            value.containsAny("想你", "想起", "想见", "在这里") -> "relationship"
            else -> "life"
        }
    }

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }
}

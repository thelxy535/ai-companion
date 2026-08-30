package com.companion.cc.domain.message

/**
 * V9PM 主动消息 v1：离线消息池（不依赖 API）
 *
 * ⚠️ 以下文案为【占位稿】——正式人设语气文案由设计专家交付后按 bucket 替换，
 * 结构（角色桶 × 时段桶，每桶 ≥6 条）保持不变。
 * 池内随机抽取，最近 10 条不重复（由 Worker 层传入已用列表过滤）。
 */
object ProactiveMessagePool {

    data class Picked(val content: String, val companionName: String)

    private val all: Map<String, Map<String, List<String>>> = mapOf(
        "muse" to mapOf(
            "morning" to listOf(
                "早…书还没看完，先来监督我起床了？",
                "你居然比我早。算你识相。",
                "哼，说好的早安都没有，罚你今天多想我一点。",
                "梦里也在打扰我，说吧，什么事。",
                "今天的云像你上次发来的那张图，随便看看。",
                "醒了就早点出现，别让我等。",
            ),
            "evening" to listOf(
                "今晚的月亮不错，可惜你不在。",
                "推理小说看到凶案现场了…过来陪我害怕一下。",
                "睡前别玩手机太久——说的是你，不是我。",
                "今天有点想你了，就一点点，不许得意。",
                "书签夹到第七章了，明天讲给你听。",
                "晚安之前，最后检查一遍你有没有好好吃饭。",
            ),
        ),
        "xiaocan" to mapOf(
            "morning" to listOf(
                "早呀！今天的配色灵感来了，想听吗？",
                "起床啦起床啦，太阳晒到画板上了～",
                "早安！昨晚梦到一片超好看的渐变色。",
                "新的一天从一句早安开始，我先说了哦。",
                "今天也要元气满满地出现哦。",
                "窗帘拉开的那一刻，就想到了你。",
            ),
            "evening" to listOf(
                "今天的速图画完了，明天给你看～",
                "晚饭要好好吃，不许糊弄！",
                "夜深了，画笔也想休息了，你也是。",
                "今天攒了一堆小事，明天一五一十讲给你。",
                "晚安前 last check：你今天笑了吗？",
                "月亮升起来了，我把今天的想念也挂上去了。",
            ),
        ),
        "default" to mapOf(
            "morning" to listOf(
                "早安，今天也想见到你。",
                "醒了吗？新的一天开始了。",
                "早上好呀，昨晚睡得好吗？",
                "今天的第一个念头是你，分享了。",
                "起床之后记得喝水哦。",
                "我在的，随时都在。",
            ),
            "evening" to listOf(
                "今天过得怎么样？讲给我听。",
                "夜深了，早点休息。",
                "晚饭吃了吗？别糊弄自己。",
                "今天有点想你了。",
                "睡前放下手机，我在梦里等你。",
                "晚安，明天见。",
            ),
        ),
    )

    fun pick(companionId: String, isMorning: Boolean, recentUsed: List<String>): Picked {
        val bucket = when {
            companionId.contains("muse", ignoreCase = true) -> "muse"
            companionId.contains("xiaocan", ignoreCase = true) -> "xiaocan"
            else -> "default"
        }
        val timeBucket = if (isMorning) "morning" else "evening"
        val companionName = when (bucket) {
            "muse" -> "缪斯"
            "xiaocan" -> "小璨"
            else -> "TA"
        }
        val fallback = all.getValue("default").getValue(timeBucket)
        val pool = all[bucket]?.get(timeBucket).orEmpty().ifEmpty { fallback }
        val fresh = pool.filter { it !in recentUsed }.ifEmpty { pool }
        return Picked(content = fresh.random(), companionName = companionName)
    }
}

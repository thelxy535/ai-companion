package com.companion.cc.domain.character

/** Context guard for short inputs that otherwise invite the model to imitate unrelated examples. */
object ConversationContinuityGuard {
    private val shortGreeting = Regex(
        "^(hi|hello|hey|嗨|你好|哈喽|在吗|早上好|晚上好)[!！。,.，～~ ]*$",
        RegexOption.IGNORE_CASE
    )

    fun forQuery(query: String): String {
        if (!shortGreeting.matches(query.trim())) return ""
        return """

【当前输入很短】
这只是一个问候。只回应当前问候或自然地问候对方，不要凭空引入新的喜好、食物、经历、人物或话题；不要把角色设定中的示例当成当前事实，也不要假设用户刚刚说过示例里的内容。
""".trimEnd()
    }
}

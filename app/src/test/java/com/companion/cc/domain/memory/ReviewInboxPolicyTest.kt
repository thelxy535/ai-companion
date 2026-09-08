package com.companion.cc.domain.memory

import com.companion.cc.data.local.entity.MemoryReviewEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewInboxPolicyTest {
    @Test
    fun `similar candidates are grouped into one readable inbox item`() {
        val reviews = listOf(
            review("1", "用户最近喜欢火锅", "用户最近多次提到火锅"),
            review("2", "周末想吃火锅", "用户说周末还想吃火锅"),
            review("3", "最近在看书", "用户提到正在看一本书")
        )

        val groups = ReviewInboxPolicy.group(reviews)

        assertEquals(2, groups.size)
        assertEquals(2, groups.first { it.items.size == 2 }.items.size)
        assertTrue(groups.any { it.items.size == 1 && it.items.single().id == "3" })
    }

    @Test
    fun `sensitive and commitment candidates remain separate`() {
        val groups = ReviewInboxPolicy.group(
            listOf(
                review("1", "工作地点", "用户在上海工作" , kind = "fact"),
                review("2", "明天提醒", "明天记得开会", kind = "commitment")
            )
        )

        assertEquals(2, groups.size)
    }

    @Test
    fun `sensitive commitment and relationship items require deliberate decision`() {
        assertTrue(ReviewInboxPolicy.isBatchable(review("1", "喜欢茶", "用户喜欢茶")))
        assertTrue(!ReviewInboxPolicy.isBatchable(review("2", "明天提醒", "明天开会", "commitment")))
        assertTrue(!ReviewInboxPolicy.isBatchable(review("3", "我们越来越熟悉", "关系变得更自然", NarrativeKinds.RELATIONSHIP)))
    }

    private fun review(id: String, title: String, content: String, kind: String = "preference") =
        MemoryReviewEntity(
            id = id,
            scopeKey = "user:u:character:c",
            kind = kind,
            title = title,
            content = content,
            confidence = 0.8,
            proposalHash = id,
            createdAt = id.toLong()
        )
}

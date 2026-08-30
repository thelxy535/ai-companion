package com.companion.cc.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavGraphRouteTest {
    @Test
    fun `memory detail route preserves companion id`() {
        assertEquals(
            "memory_detail/node-1?companionId=xiaocan",
            Screen.MemoryDetail.createRoute("xiaocan", "node-1")
        )
    }
    @Test
    fun `menu routes preserve companion id arguments`() {
        val companionId = "xiaocan"
        assertEquals("chat/xiaocan", Screen.Chat.createRoute(companionId))
        assertEquals("companion_detail/xiaocan", Screen.CompanionDetail.createRoute(companionId))
        assertEquals("memory/xiaocan", Screen.Memory.createRoute(companionId))
        assertEquals("memory_review/xiaocan", Screen.MemoryReview.createRoute(companionId))
        assertEquals("memory_library/xiaocan", Screen.MemoryLibrary.createRoute(companionId))
        assertEquals("stats/xiaocan", Screen.Stats.createRoute(companionId))
        assertEquals("data_management/xiaocan", Screen.DataManagement.createRoute(companionId))
        assertEquals("favorites/xiaocan", Screen.Favorites.createRoute(companionId))
        assertEquals("custom_chat/custom-1", Screen.CustomCharacterChat.createRoute("custom-1"))
        assertEquals("character_edit/custom-1", Screen.CharacterEdit.createRoute("custom-1"))
    }
}

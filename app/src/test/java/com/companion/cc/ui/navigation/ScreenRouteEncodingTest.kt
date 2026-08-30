package com.companion.cc.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenRouteEncodingTest {
    @Test
    fun `route arguments encode path separators and spaces`() {
        val characterId = "custom/role 1"

        assertEquals("chat/custom%2Frole%201", Screen.Chat.createRoute(characterId))
        assertEquals("custom_chat/custom%2Frole%201", Screen.CustomCharacterChat.createRoute(characterId))
        assertEquals("memory/custom%2Frole%201", Screen.Memory.createRoute(characterId))
    }

    @Test
    fun `all parameterized routes encode the same argument`() {
        val id = "role/with space"
        val encoded = "role%2Fwith%20space"

        assertEquals("companion_detail/$encoded", Screen.CompanionDetail.createRoute(id))
        assertEquals("memory_review/$encoded", Screen.MemoryReview.createRoute(id))
        assertEquals("memory_library/$encoded", Screen.MemoryLibrary.createRoute(id))
        assertEquals("memory_detail/node-1?companionId=$encoded", Screen.MemoryDetail.createRoute(id, "node-1"))
        assertEquals("stats/$encoded", Screen.Stats.createRoute(id))
        assertEquals("data_management/$encoded", Screen.DataManagement.createRoute(id))
        assertEquals("favorites/$encoded", Screen.Favorites.createRoute(id))
        assertEquals("character_edit/$encoded", Screen.CharacterEdit.createRoute(id))
        assertEquals("character_farewell/$encoded", Screen.CharacterFarewell.createRoute(id))
        assertEquals("memory_capsule_vault", Screen.MemoryCapsuleVault.route)
        assertEquals("character_revival", Screen.CharacterRevival.route)
    }

    @Test
    fun `missing or blank route arguments are not replaced with a default character`() {
        assertNull(routeArgumentOrNull(null))
        assertNull(routeArgumentOrNull("  "))
        assertEquals("muse", routeArgumentOrNull("muse"))
    }
}

package com.companion.cc.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarVisibilityPolicyTest {
    @Test
    fun `shows bottom bar on a stable top level destination`() {
        assertTrue(
            shouldShowMainBottomBar(
                currentRoute = Screen.Home.route,
                visibleRoutes = listOf(Screen.Home.route)
            )
        )
    }

    @Test
    fun `shows bottom bar for every top level destination`() {
        listOf(
            Screen.Home.route,
            Screen.CharacterList.route,
            Screen.MemoryHub.route,
            Screen.Settings.route
        ).forEach { route ->
            assertTrue(
                "Expected bottom bar for $route",
                shouldShowMainBottomBar(route, listOf(route))
            )
        }
    }

    @Test
    fun `hides bottom bar while chat is still visible during pop`() {
        assertFalse(
            shouldShowMainBottomBar(
                currentRoute = Screen.Home.route,
                visibleRoutes = listOf(Screen.Chat.route, Screen.Home.route)
            )
        )
    }

    @Test
    fun `hides bottom bar while custom chat is still visible during pop`() {
        assertFalse(
            shouldShowMainBottomBar(
                currentRoute = Screen.CharacterList.route,
                visibleRoutes = listOf(
                    Screen.CustomCharacterChat.route,
                    Screen.CharacterList.route
                )
            )
        )
    }

    @Test
    fun `hides bottom bar on chat and child destinations`() {
        assertFalse(
            shouldShowMainBottomBar(
                currentRoute = Screen.Chat.route,
                visibleRoutes = listOf(Screen.Chat.route)
            )
        )
        assertFalse(
            shouldShowMainBottomBar(
                currentRoute = Screen.Stats.route,
                visibleRoutes = listOf(Screen.Stats.route)
            )
        )
    }

    @Test
    fun `hides bottom bar for null or empty navigation state`() {
        assertFalse(shouldShowMainBottomBar(null, emptyList()))
        assertFalse(shouldShowMainBottomBar(Screen.Home.route, emptyList()))
    }

    @Test
    fun `keeps bottom bar visible between top level destinations`() {
        assertTrue(
            shouldShowMainBottomBar(
                currentRoute = Screen.Settings.route,
                visibleRoutes = listOf(Screen.Home.route, Screen.Settings.route)
            )
        )
    }
}

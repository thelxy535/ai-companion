package com.companion.cc.ui.navigation

private val mainBottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.CharacterList.route,
    Screen.MemoryHub.route,
    Screen.Settings.route
)

private val chatRoutes = setOf(
    Screen.Chat.route,
    Screen.CustomCharacterChat.route
)

/**
 * Keeps the root bottom bar out of an outgoing chat page during its pop transition.
 *
 * The current route changes before the outgoing destination is removed from the
 * visible back stack. Using both values prevents the root Scaffold from changing
 * the NavHost bottom inset while the chat page is still fading out.
 */
internal fun shouldShowMainBottomBar(
    currentRoute: String?,
    visibleRoutes: Collection<String?>
): Boolean {
    return currentRoute in mainBottomBarRoutes &&
        visibleRoutes.isNotEmpty() &&
        visibleRoutes.none { it in chatRoutes }
}

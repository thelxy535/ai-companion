package com.companion.cc.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Chat : Screen("chat/{companionId}") {
        fun createRoute(companionId: String) = "chat/$companionId"
    }
    // 自定义角色聊天 - 使用角色ID而不是companionId
    object CustomCharacterChat : Screen("custom_chat/{characterId}") {
        fun createRoute(characterId: String) = "custom_chat/$characterId"
    }
    object CompanionDetail : Screen("companion_detail/{companionId}") {
        fun createRoute(companionId: String) = "companion_detail/$companionId"
    }
    object Settings : Screen("settings")
    object Memory : Screen("memory/{companionId}") {
        fun createRoute(companionId: String) = "memory/$companionId"
    }
    object MemoryReview : Screen("memory_review/{companionId}") {
        fun createRoute(companionId: String) = "memory_review/$companionId"
    }
    object MemoryLibrary : Screen("memory_library/{companionId}") {
        fun createRoute(companionId: String) = "memory_library/$companionId"
    }
    object MemoryDetail : Screen("memory_detail/{nodeId}") {
        fun createRoute(nodeId: String) = "memory_detail/$nodeId"
    }
    object Stats : Screen("stats/{companionId}") {
        fun createRoute(companionId: String) = "stats/$companionId"
    }
    object DataManagement : Screen("data_management/{companionId}") {
        fun createRoute(companionId: String) = "data_management/$companionId"
    }
    object Favorites : Screen("favorites/{companionId}") {
        fun createRoute(companionId: String) = "favorites/$companionId"
    }
    object CharacterList : Screen("character_list")
    object CharacterCreate : Screen("character_create")
    object CharacterEdit : Screen("character_edit/{characterId}") {
        fun createRoute(characterId: String) = "character_edit/$characterId"
    }
}

package com.companion.cc.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object RouteArgumentCodec {
    fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
}

internal fun routeArgumentOrNull(value: String?): String? =
    value?.takeIf { it.isNotBlank() }

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Chat : Screen("chat/{companionId}") {
        fun createRoute(companionId: String) = "chat/${RouteArgumentCodec.encode(companionId)}"
    }
    // 自定义角色聊天 - 使用角色ID而不是companionId
    object CharacterTestChat : Screen("character_test_chat")

    object CustomCharacterChat : Screen("custom_chat/{characterId}") {
        fun createRoute(characterId: String) =
            "custom_chat/${RouteArgumentCodec.encode(characterId)}"
    }
    object CompanionDetail : Screen("companion_detail/{companionId}") {
        fun createRoute(companionId: String) =
            "companion_detail/${RouteArgumentCodec.encode(companionId)}"
    }
    object Settings : Screen("settings")
    object MemoryHub : Screen("memory_hub")
    object Memory : Screen("memory/{companionId}") {
        fun createRoute(companionId: String) = "memory/${RouteArgumentCodec.encode(companionId)}"
    }
    object MemoryReview : Screen("memory_review/{companionId}") {
        fun createRoute(companionId: String) =
            "memory_review/${RouteArgumentCodec.encode(companionId)}"
    }
    object MemoryLibrary : Screen("memory_library/{companionId}") {
        fun createRoute(companionId: String) =
            "memory_library/${RouteArgumentCodec.encode(companionId)}"
    }
    object MemoryDetail : Screen("memory_detail/{nodeId}?companionId={companionId}") {
        fun createRoute(companionId: String, nodeId: String) =
            "memory_detail/${RouteArgumentCodec.encode(nodeId)}?companionId=${RouteArgumentCodec.encode(companionId)}"
    }
    object MemoryRetrievalTrace : Screen("memory_retrieval_trace/{traceId}?companionId={companionId}") {
        fun createRoute(companionId: String, traceId: String) =
            "memory_retrieval_trace/${RouteArgumentCodec.encode(traceId)}?companionId=${RouteArgumentCodec.encode(companionId)}"
    }
    object Stats : Screen("stats/{companionId}") {
        fun createRoute(companionId: String) = "stats/${RouteArgumentCodec.encode(companionId)}"
    }
    object DataManagement : Screen("data_management/{companionId}") {
        fun createRoute(companionId: String) =
            "data_management/${RouteArgumentCodec.encode(companionId)}"
    }
    object Favorites : Screen("favorites/{companionId}") {
        fun createRoute(companionId: String) =
            "favorites/${RouteArgumentCodec.encode(companionId)}"
    }
    object CharacterList : Screen("character_list")
    object CharacterCreate : Screen("character_create")
    object CharacterEdit : Screen("character_edit/{characterId}") {
        fun createRoute(characterId: String) =
            "character_edit/${RouteArgumentCodec.encode(characterId)}"
    }
    object CharacterFarewell : Screen("character_farewell/{characterId}") {
        fun createRoute(characterId: String) =
            "character_farewell/${RouteArgumentCodec.encode(characterId)}"
    }
    object MemoryCapsuleVault : Screen("memory_capsule_vault")
    object CharacterRevival : Screen("character_revival")
}

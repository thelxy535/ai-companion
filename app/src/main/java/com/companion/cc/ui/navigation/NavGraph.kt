package com.companion.cc.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.companion.cc.ui.chat.NaturalChatScreen
import com.companion.cc.ui.companion.CompanionDetailScreen
import com.companion.cc.ui.data.ImmersiveDataManagementScreen
import com.companion.cc.ui.favorites.FavoritesScreen
import com.companion.cc.ui.home.ImmersiveHomeScreen
import com.companion.cc.ui.memory.MemoryTreeScreen
import com.companion.cc.ui.memory.MemoryReviewScreen
import com.companion.cc.ui.memory.MemoryLibraryScreen
import com.companion.cc.ui.memory.MemoryDetailScreen
import com.companion.cc.ui.memory.MemoryRetrievalTraceScreen
import com.companion.cc.ui.memory.MemoryHubScreen
import com.companion.cc.ui.settings.SettingsScreen
import com.companion.cc.ui.splash.SplashScreen
import com.companion.cc.ui.stats.ImmersiveStatsScreen
import com.companion.cc.ui.theme.VisualRoute
import com.companion.cc.ui.components.V9PMActionButton
import com.companion.cc.ui.theme.VisualScene

@Composable
fun NavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues = PaddingValues(),
    openMemoryInboxCompanionId: String? = null,
    openCompanionId: String? = null
) {
    // 页面级唯一转场参数：内容只做一次轻量 m-rise，背景和 Root Chrome 保持稳定
    val riseOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.roundToPx() }
    NavHost(
        navController = navController,
        modifier = Modifier
            .fillMaxSize()
            // V9PM 修复：去掉此处不透明底色——根容器极光需要透出，否则切换时露出纯色底（黑闪）
            .padding(bottom = contentPadding.calculateBottomPadding()),
        startDestination = Screen.Splash.route,
        // V9PM：统一页面转场。聊天不再额外 scale/26dp/initialAlpha，避免重列表进入时卡顿与跳动
        enterTransition = {
            if (initialState.destination.route == Screen.Splash.route &&
                targetState.destination.route == Screen.Home.route
            ) {
                androidx.compose.animation.fadeIn(
                    androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
                ) + androidx.compose.animation.slideInVertically(
                    androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
                ) { riseOffsetPx / 2 }
            } else {
                androidx.compose.animation.fadeIn(
                    androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
                ) + androidx.compose.animation.slideInVertically(
                    androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
                ) { riseOffsetPx }
            }
        },
        exitTransition = {
            if (initialState.destination.route == Screen.Splash.route &&
                targetState.destination.route == Screen.Home.route
            ) androidx.compose.animation.ExitTransition.None
            else androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
            )
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
            )
        },
        popExitTransition = {
            androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))
            )
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    val destination = openMemoryInboxCompanionId
                        ?.takeIf { it.isNotBlank() }
                        ?.let(Screen.MemoryReview::createRoute)
                        ?: openCompanionId
                            ?.takeIf { it.isNotBlank() }
                            ?.let(Screen.Chat::createRoute)
                        ?: Screen.Home.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            VisualScene(route = VisualRoute.HOME) {
                ImmersiveHomeScreen(
                    onCompanionSelected = { companionId ->
                        navController.navigate(Screen.Chat.createRoute(companionId))
                    },
                    onCustomCharacterSelected = { characterId ->
                        navController.navigate(Screen.CustomCharacterChat.createRoute(characterId))
                    },
                    onNavigateToCharacterList = {
                        navController.navigateTopLevel(Screen.CharacterList.route)
                    }
                )
            }
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.CHAT, companionId = companionId) {
                NaturalChatScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                     onNavigateToMemory = { navController.navigateSafely(Screen.Memory.createRoute(companionId)) },
                     onNavigateToStats = { navController.navigateSafely(Screen.Stats.createRoute(companionId)) },
                     onNavigateToData = { navController.navigateSafely(Screen.DataManagement.createRoute(companionId)) },
                     onNavigateToFavorites = { navController.navigateSafely(Screen.Favorites.createRoute(companionId)) },
                    onNavigateToCompanionDetail = { navController.navigate(Screen.CompanionDetail.createRoute(companionId)) }
                )
            }
        }

        composable(Screen.Settings.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                SettingsScreen()
            }
        }

        composable(Screen.MemoryHub.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                MemoryHubScreen(
                    onOpenMemory = { id -> navController.navigate(Screen.MemoryLibrary.createRoute(id)) },
                    onOpenTree = { id -> navController.navigate(Screen.Memory.createRoute(id)) },
                    onOpenReview = { id -> navController.navigate(Screen.MemoryReview.createRoute(id)) },
                    onOpenFavorites = { id -> navController.navigate(Screen.Favorites.createRoute(id)) }
                )
            }
        }

        composable(Screen.Memory.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryTreeScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToReview = { navController.navigate(Screen.MemoryReview.createRoute(companionId)) },
                    onNavigateToLibrary = { navController.navigate(Screen.MemoryLibrary.createRoute(companionId)) }
                )
            }
        }

        composable(Screen.MemoryReview.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryReviewScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.MemoryLibrary.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryLibraryScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                    onOpenDetail = { nodeId -> navController.navigate(Screen.MemoryDetail.createRoute(companionId, nodeId)) }
                )
            }
        }

        composable(Screen.MemoryDetail.route) { backStackEntry ->
            val nodeId = routeArgumentOrNull(backStackEntry.arguments?.getString("nodeId"))
            val companionId = routeArgumentOrNull(backStackEntry.arguments?.getString("companionId"))
            if (nodeId == null) {
                InvalidRouteScreen("nodeId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryDetailScreen(
                    companionId = companionId,
                    nodeId = nodeId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.MemoryRetrievalTrace.route) { backStackEntry ->
            val traceId = routeArgumentOrNull(backStackEntry.arguments?.getString("traceId"))
            val companionId = routeArgumentOrNull(backStackEntry.arguments?.getString("companionId"))
            if (traceId == null) {
                InvalidRouteScreen("traceId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryRetrievalTraceScreen(
                    companionId = companionId,
                    traceId = traceId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.Stats.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                ImmersiveStatsScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.DataManagement.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                ImmersiveDataManagementScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.Favorites.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                FavoritesScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.CompanionDetail.route) { backStackEntry ->
            val companionId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("companionId")
            )
            if (companionId == null) {
                InvalidRouteScreen("companionId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                CompanionDetailScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.CharacterList.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                com.companion.cc.ui.character.CharacterListScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onCreateCharacter = { navController.navigate(Screen.CharacterCreate.route) },
                    onEditCharacter = { characterId ->
                        navController.navigate(Screen.CharacterEdit.createRoute(characterId))
                    },
                    onDeleteCharacter = { characterId ->
                        navController.navigate(Screen.CharacterFarewell.createRoute(characterId))
                    },
                    onStartChat = { characterId ->
                        navController.navigate(Screen.CustomCharacterChat.createRoute(characterId))
                    }
                )
            }
        }

        composable(Screen.CharacterFarewell.route) { backStackEntry ->
            val characterId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("characterId")
            )
            if (characterId == null) {
                InvalidRouteScreen("characterId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = characterId) {
                com.companion.cc.ui.character.CharacterFarewellScreen(
                    characterId = characterId,
                    onNavigateBack = { navController.navigateUp() },
                    onOpenCapsuleVault = {
                        navController.navigate(Screen.MemoryCapsuleVault.route)
                    }
                )
            }
        }

        composable(Screen.MemoryCapsuleVault.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                com.companion.cc.ui.character.MemoryCapsuleVaultScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onOpenRevival = { navController.navigate(Screen.CharacterRevival.route) }
                )
            }
        }

        composable(Screen.CharacterRevival.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                com.companion.cc.ui.character.CharacterRevivalScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.CustomCharacterChat.route) { backStackEntry ->
            val characterId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("characterId")
            )
            if (characterId == null) {
                InvalidRouteScreen("characterId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.CHAT, companionId = characterId) {
                NaturalChatScreen(
                    companionId = characterId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToMemory = { navController.navigateSafely(Screen.Memory.createRoute(characterId)) },
                    onNavigateToStats = { navController.navigateSafely(Screen.Stats.createRoute(characterId)) },
                    onNavigateToData = { navController.navigateSafely(Screen.DataManagement.createRoute(characterId)) },
                    onNavigateToFavorites = { navController.navigateSafely(Screen.Favorites.createRoute(characterId)) },
                    onNavigateToCompanionDetail = { navController.navigateSafely(Screen.CompanionDetail.createRoute(characterId)) }
                )
            }
        }

        composable(Screen.CharacterTestChat.route) {
            com.companion.cc.ui.character.CharacterTestChatScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.CharacterCreate.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                com.companion.cc.ui.character.CharacterCustomizationScreen(
                    characterId = null,
                    onNavigateBack = { navController.navigateUp() },
                    onTestChat = { navController.navigate(Screen.CharacterTestChat.route) }
                )
            }
        }

        composable(Screen.CharacterEdit.route) { backStackEntry ->
            val characterId = routeArgumentOrNull(
                backStackEntry.arguments?.getString("characterId")
            )
            if (characterId == null) {
                InvalidRouteScreen("characterId", onNavigateBack = { navController.navigateUp() })
                return@composable
            }
            VisualScene(route = VisualRoute.UTILITY, companionId = characterId) {
                com.companion.cc.ui.character.CharacterCustomizationScreen(
                    characterId = characterId,
                    onNavigateBack = { navController.navigateUp() },
                    onTestChat = { navController.navigate(Screen.CharacterTestChat.route) }
                )
            }
        }
    }
}

@Composable
internal fun InvalidRouteScreen(
    argumentName: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "无效的导航参数：$argumentName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        V9PMActionButton(
            label = "返回",
            onClick = onNavigateBack,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            height = 44.dp
        )
    }
}

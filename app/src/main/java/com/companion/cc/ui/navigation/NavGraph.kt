package com.companion.cc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.util.Log
import com.companion.cc.ui.chat.NaturalChatScreen
import com.companion.cc.ui.companion.CompanionDetailScreen
import com.companion.cc.ui.data.ImmersiveDataManagementScreen
import com.companion.cc.ui.favorites.FavoritesScreen
import com.companion.cc.ui.home.ImmersiveHomeScreen
import com.companion.cc.ui.memory.MemoryTreeScreen
import com.companion.cc.ui.memory.MemoryReviewScreen
import com.companion.cc.ui.memory.MemoryLibraryScreen
import com.companion.cc.ui.memory.MemoryDetailScreen
import com.companion.cc.ui.settings.SettingsScreen
import com.companion.cc.ui.splash.SplashScreen
import com.companion.cc.ui.stats.ImmersiveStatsScreen
import com.companion.cc.ui.theme.VisualRoute
import com.companion.cc.ui.theme.VisualScene

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
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
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCharacterList = {
                        navController.navigate(Screen.CharacterList.route)
                    }
                )
            }
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.CHAT, companionId = companionId) {
                NaturalChatScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                     onNavigateToMemory = { navController.navigateSafely(Screen.Memory.createRoute(companionId)) },
                     onNavigateToStats = { navController.navigateSafely(Screen.Stats.createRoute(companionId)) },
                     onNavigateToData = { navController.navigateSafely(Screen.DataManagement.createRoute(companionId)) },
                     onNavigateToSettings = { navController.navigateSafely(Screen.Settings.route) },
                     onNavigateToFavorites = { navController.navigateSafely(Screen.Favorites.createRoute(companionId)) },
                    onNavigateToCompanionDetail = { navController.navigate(Screen.CompanionDetail.createRoute(companionId)) }
                )
            }
        }

        composable(Screen.Settings.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.Memory.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryTreeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToReview = { navController.navigate(Screen.MemoryReview.createRoute(companionId)) },
                    onNavigateToLibrary = { navController.navigate(Screen.MemoryLibrary.createRoute(companionId)) }
                )
            }
        }

        composable(Screen.MemoryReview.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryReviewScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.MemoryLibrary.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                MemoryLibraryScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                    onOpenDetail = { nodeId -> navController.navigate(Screen.MemoryDetail.createRoute(nodeId)) }
                )
            }
        }

        composable(Screen.MemoryDetail.route) { backStackEntry ->
            val nodeId = backStackEntry.arguments?.getString("nodeId") ?: ""
            VisualScene(route = VisualRoute.UTILITY) {
                MemoryDetailScreen(nodeId = nodeId, onNavigateBack = { navController.navigateUp() })
            }
        }

        composable(Screen.Stats.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                ImmersiveStatsScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.DataManagement.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                ImmersiveDataManagementScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.Favorites.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                FavoritesScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.CompanionDetail.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            VisualScene(route = VisualRoute.UTILITY, companionId = companionId) {
                CompanionDetailScreen(
                    companionId = companionId,
                    onNavigateBack = { navController.navigateUp() },
                    onEditAvatar = {
                        navController.navigateUp()
                    }
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
                    onStartChat = { characterId ->
                        navController.navigate(Screen.CustomCharacterChat.createRoute(characterId))
                    }
                )
            }
        }

        composable(Screen.CustomCharacterChat.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
            VisualScene(route = VisualRoute.CHAT, companionId = characterId) {
                NaturalChatScreen(
                    companionId = characterId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToMemory = { navController.navigateSafely(Screen.Memory.createRoute(characterId)) },
                    onNavigateToStats = { navController.navigateSafely(Screen.Stats.createRoute(characterId)) },
                    onNavigateToData = { navController.navigateSafely(Screen.DataManagement.createRoute(characterId)) },
                    onNavigateToSettings = { navController.navigateSafely(Screen.Settings.route) },
                    onNavigateToFavorites = { navController.navigateSafely(Screen.Favorites.createRoute(characterId)) },
                    onNavigateToCompanionDetail = { navController.navigateSafely(Screen.CompanionDetail.createRoute(characterId)) }
                )
            }
        }

        composable(Screen.CharacterCreate.route) {
            VisualScene(route = VisualRoute.UTILITY) {
                com.companion.cc.ui.character.CharacterCustomizationScreen(
                    characterId = null,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable(Screen.CharacterEdit.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")
            VisualScene(route = VisualRoute.UTILITY, companionId = characterId) {
                com.companion.cc.ui.character.CharacterCustomizationScreen(
                    characterId = characterId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}

internal fun NavHostController.navigateSafely(route: String) {
    try {
        Log.d("CCNavigation", "navigate route=$route")
        navigate(route)
    } catch (error: RuntimeException) {
        Log.e("CCNavigation", "navigation failed route=$route", error)
        throw error
    }
}

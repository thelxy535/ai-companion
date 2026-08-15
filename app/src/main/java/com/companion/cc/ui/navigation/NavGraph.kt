package com.companion.cc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.companion.cc.ui.splash.SplashScreen
import com.companion.cc.ui.home.ImmersiveHomeScreen
import com.companion.cc.ui.chat.NaturalChatScreen
import com.companion.cc.ui.companion.CompanionDetailScreen
import com.companion.cc.ui.memory.MemoryTreeScreen
import com.companion.cc.ui.stats.ImmersiveStatsScreen
import com.companion.cc.ui.data.ImmersiveDataManagementScreen
import com.companion.cc.ui.settings.SettingsScreen
import com.companion.cc.ui.favorites.FavoritesScreen

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

        composable(Screen.Chat.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            NaturalChatScreen(
                companionId = companionId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToMemory = { navController.navigate(Screen.Memory.createRoute(companionId)) },
                onNavigateToStats = { navController.navigate(Screen.Stats.createRoute(companionId)) },
                onNavigateToData = { navController.navigate(Screen.DataManagement.createRoute(companionId)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.createRoute(companionId)) },
                onNavigateToCompanionDetail = { navController.navigate(Screen.CompanionDetail.createRoute(companionId)) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Memory.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            MemoryTreeScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Stats.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            ImmersiveStatsScreen(
                companionId = companionId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.DataManagement.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            ImmersiveDataManagementScreen(
                companionId = companionId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Favorites.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            FavoritesScreen(
                companionId = companionId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.CompanionDetail.route) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: "xiaocan"
            CompanionDetailScreen(
                companionId = companionId,
                onNavigateBack = { navController.navigateUp() },
                onEditAvatar = {
                    // 返回到聊天界面的设置
                    navController.navigateUp()
                }
            )
        }

        // 角色自定义功能
        composable(Screen.CharacterList.route) {
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

        // 自定义角色聊天
        composable(Screen.CustomCharacterChat.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
            NaturalChatScreen(
                companionId = characterId,
                isCustomCharacter = true,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToMemory = { navController.navigate(Screen.Memory.createRoute(characterId)) },
                onNavigateToStats = { navController.navigate(Screen.Stats.createRoute(characterId)) },
                onNavigateToData = { navController.navigate(Screen.DataManagement.createRoute(characterId)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.createRoute(characterId)) },
                onNavigateToCompanionDetail = { navController.navigate(Screen.CompanionDetail.createRoute(characterId)) }
            )
        }

        composable(Screen.CharacterCreate.route) {
            com.companion.cc.ui.character.CharacterCustomizationScreen(
                characterId = null,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.CharacterEdit.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")
            com.companion.cc.ui.character.CharacterCustomizationScreen(
                characterId = characterId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

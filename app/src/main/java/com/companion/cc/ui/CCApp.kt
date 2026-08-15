package com.companion.cc.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.companion.cc.ui.navigation.NavGraph

@Composable
fun CCApp() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}

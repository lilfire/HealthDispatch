package com.healthdispatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.setup.SetupScreen
import com.healthdispatch.ui.settings.SettingsScreen

object Routes {
    const val SETUP = "setup?editMode={editMode}"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"

    fun setup(editMode: Boolean = false) = "setup?editMode=$editMode"
}

@Composable
fun HealthDispatchNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.setup()) {
        composable(
            route = Routes.SETUP,
            arguments = listOf(
                navArgument("editMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditSupabaseConfig = {
                    navController.navigate(Routes.setup(editMode = true))
                },
                onRerunSetup = {
                    navController.navigate(Routes.setup(editMode = false)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

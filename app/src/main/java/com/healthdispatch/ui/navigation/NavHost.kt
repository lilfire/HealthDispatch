package com.healthdispatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.setup.SetupScreen
import com.healthdispatch.ui.settings.SettingsScreen

object Routes {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun HealthDispatchNavHost() {
    val navController = rememberNavController()
    var isConfigured by rememberSaveable { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    isConfigured = true
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onSkip = {
                    isConfigured = false
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                isConfigured = isConfigured,
                onSetupBannerClick = {
                    navController.navigate(Routes.SETUP)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

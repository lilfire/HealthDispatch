package com.healthdispatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.onboarding.OnboardingWizard
import com.healthdispatch.ui.settings.SettingsScreen

object Routes {
    const val SETUP = "setup"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun HealthDispatchNavHost(healthConnectRepository: HealthConnectRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingWizard(
                healthConnectRepository = healthConnectRepository,
                onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}

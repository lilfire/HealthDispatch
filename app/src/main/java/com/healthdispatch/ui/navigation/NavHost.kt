package com.healthdispatch.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthdispatch.data.cloud.CloudConfigRepository
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.onboarding.OnboardingWizard
import com.healthdispatch.ui.settings.SettingsScreen
import com.healthdispatch.ui.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun HealthDispatchNavHost(
    healthConnectRepository: HealthConnectRepository,
    cloudConfigRepository: CloudConfigRepository
) {
    val onboardingComplete by cloudConfigRepository.onboardingCompleteFlow
        .collectAsState(initial = null)

    when (onboardingComplete) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val startDestination = if (onboardingComplete == true) Routes.DASHBOARD else Routes.ONBOARDING
            HealthDispatchNavContent(
                startDestination = startDestination,
                healthConnectRepository = healthConnectRepository
            )
        }
    }
}

@Composable
private fun HealthDispatchNavContent(
    startDestination: String,
    healthConnectRepository: HealthConnectRepository
) {
    val navController = rememberNavController()
    var isConfigured by rememberSaveable { mutableStateOf(startDestination == Routes.DASHBOARD) }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingWizard(
                healthConnectRepository = healthConnectRepository,
                onComplete = {
                    isConfigured = true
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
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
                onBack = { navController.popBackStack() },
                onRerunSetup = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

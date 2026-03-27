package com.healthdispatch.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthdispatch.data.auth.AuthState
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.onboarding.OnboardingWizard
import com.healthdispatch.ui.permission.HealthPermissionScreen
import com.healthdispatch.ui.setup.SetupScreen
import com.healthdispatch.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val HEALTH_PERMISSIONS = "health_permissions"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun HealthDispatchNavHost(
    healthConnectRepository: HealthConnectRepository,
    navViewModel: NavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by navViewModel.authState.collectAsStateWithLifecycle()
    val onboardingComplete by navViewModel.onboardingComplete.collectAsStateWithLifecycle()

    // N1: Show splash/loading screen while auth state is resolving
    if (authState is AuthState.Unknown) {
        SplashScreen()
        return
    }

    val startDestination = when {
        authState is AuthState.Authenticated -> Routes.DASHBOARD
        !onboardingComplete -> Routes.ONBOARDING
        else -> Routes.SETUP
    }

    // If auth state changes to unauthenticated (e.g. sign out), navigate to setup
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate(Routes.SETUP) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingWizard(
                healthConnectRepository = healthConnectRepository,
                onComplete = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    // N2: Prompt for Health Connect permissions after auth
                    navController.navigate(Routes.HEALTH_PERMISSIONS) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HEALTH_PERMISSIONS) {
            HealthPermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.HEALTH_PERMISSIONS) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.HEALTH_PERMISSIONS) { inclusive = true }
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
                onSignedOut = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Loading, please wait" },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

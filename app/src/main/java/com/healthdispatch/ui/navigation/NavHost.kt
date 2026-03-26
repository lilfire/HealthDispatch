package com.healthdispatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthdispatch.data.auth.AuthState
import com.healthdispatch.ui.dashboard.DashboardScreen
import com.healthdispatch.ui.setup.SetupScreen
import com.healthdispatch.ui.settings.SettingsScreen

object Routes {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@Composable
fun HealthDispatchNavHost(
    navViewModel: NavViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by navViewModel.authState.collectAsStateWithLifecycle()

    val startDestination = when (authState) {
        is AuthState.Authenticated -> Routes.DASHBOARD
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
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SETUP) { inclusive = true }
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

package com.nothingness.bruhpatcher.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nothingness.bruhpatcher.model.PatchingState
import com.nothingness.bruhpatcher.ui.components.LiquidGlassFloatingBar
import com.nothingness.bruhpatcher.ui.components.LiquidNavDestination
import com.nothingness.bruhpatcher.ui.screens.ConfigScreen
import com.nothingness.bruhpatcher.ui.screens.DashboardScreen
import com.nothingness.bruhpatcher.ui.screens.ProgressScreen
import com.nothingness.bruhpatcher.ui.screens.SettingsScreen
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Config : Screen("config")
    data object Progress : Screen("progress")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route
    val patchingState by viewModel.patchingState.collectAsState()

    val isPatchingActive = patchingState !is PatchingState.Idle &&
            patchingState !is PatchingState.Success &&
            patchingState !is PatchingState.Error

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToConfig = {
                        navController.navigate(Screen.Config.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToProgress = {
                        navController.navigate(Screen.Progress.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(Screen.Config.route) {
                ConfigScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onStartPatching = {
                        navController.navigate(Screen.Progress.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(Screen.Progress.route) {
                ProgressScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack(Screen.Dashboard.route, false)
                    },
                    onComplete = {
                        navController.popBackStack(Screen.Dashboard.route, false)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // MIUIX Liquid Glass Floating Bar overlay with light refraction & dynamic contrast
        LiquidGlassFloatingBar(
            currentRoute = currentRoute,
            isPatchingActive = isPatchingActive,
            isHighDynamicContrast = true,
            onNavigate = { destination ->
                val targetRoute = when (destination) {
                    LiquidNavDestination.DASHBOARD -> Screen.Dashboard.route
                    LiquidNavDestination.CONFIG -> Screen.Config.route
                    LiquidNavDestination.PROGRESS -> Screen.Progress.route
                    LiquidNavDestination.SETTINGS -> Screen.Settings.route
                }
                if (currentRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Dashboard.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

package com.nothingness.bruhpatcher.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nothingness.bruhpatcher.model.PatchingState
import com.nothingness.bruhpatcher.ui.components.LiquidGlassFloatingBar
import com.nothingness.bruhpatcher.ui.components.LiquidNavDestination
import com.nothingness.bruhpatcher.ui.components.liquid.BarBlurHost
import com.nothingness.bruhpatcher.ui.components.liquid.LocalBarBlurBackdrop
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixNavigationBar
import com.nothingness.bruhpatcher.ui.screens.ConfigScreen
import com.nothingness.bruhpatcher.ui.screens.DashboardScreen
import com.nothingness.bruhpatcher.ui.screens.ProgressScreen
import com.nothingness.bruhpatcher.ui.screens.SettingsScreen
import com.nothingness.bruhpatcher.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import top.yukonga.miuix.kmp.blur.layerBackdrop

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Config : Screen("config")
    data object Progress : Screen("progress")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController? = null,
    viewModel: MainViewModel = viewModel()
) {
    val patchingState by viewModel.patchingState.collectAsState()
    val isPatchingActive = patchingState !is PatchingState.Idle &&
            patchingState !is PatchingState.Success &&
            patchingState !is PatchingState.Error
    val useLiquidGlassNavbar by viewModel.useLiquidGlassNavbar.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }

    // Derive continuous scroll position for silky smooth 120Hz liquid bubble synchronization
    val pagerPosition by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }

    val currentRoute = when (pagerState.currentPage) {
        0 -> Screen.Dashboard.route
        1 -> Screen.Config.route
        2 -> Screen.Progress.route
        3 -> Screen.Settings.route
        else -> Screen.Dashboard.route
    }

    // Handle system back gesture: return to Dashboard if on another tab
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val onNavigateToDestination: (LiquidNavDestination) -> Unit = { destination ->
        val targetPage = when (destination) {
            LiquidNavDestination.DASHBOARD -> 0
            LiquidNavDestination.CONFIG -> 1
            LiquidNavDestination.PROGRESS -> 2
            LiquidNavDestination.SETTINGS -> 3
        }
        if (pagerState.currentPage != targetPage) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 550f
                    )
                )
            }
        }
    }

    BarBlurHost(
        liquidGlassEnabled = useLiquidGlassNavbar
    ) {
        val backdrop = LocalBarBlurBackdrop.current
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null && useLiquidGlassNavbar) Modifier.layerBackdrop(backdrop)
                        else Modifier
                    )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToConfig = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onNavigateToSettings = {
                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            },
                            onNavigateToProgress = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                        1 -> ConfigScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onStartPatching = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                        2 -> ProgressScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onComplete = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                    }
                }
            }

            if (useLiquidGlassNavbar) {
                // MIUIX Liquid Glass Floating Bar overlay with real-time backdrop blur & light refraction
                LiquidGlassFloatingBar(
                    currentRoute = currentRoute,
                    targetPosition = pagerPosition,
                    isPatchingActive = isPatchingActive,
                    isHighDynamicContrast = true,
                    onNavigate = onNavigateToDestination,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            } else {
                // Standard docked MIUIX Navigation Bar (HyperOS Alive Design)
                MiuixNavigationBar(
                    currentRoute = currentRoute,
                    isPatchingActive = isPatchingActive,
                    onNavigate = onNavigateToDestination,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

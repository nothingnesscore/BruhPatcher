package com.nothingness.bruhpatcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nothingness.bruhpatcher.ui.navigation.AppNavigation
import com.nothingness.bruhpatcher.ui.theme.BruhPatcherTheme
import com.nothingness.bruhpatcher.viewmodel.MainViewModel
import com.topjohnwu.superuser.Shell

class MainActivity : ComponentActivity() {

    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setTimeout(30)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            val themeEngine by viewModel.themeEngine.collectAsState()
            val useAmoledMode by viewModel.useAmoledMode.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (themeMode) {
                com.nothingness.bruhpatcher.data.ThemeMode.SYSTEM -> isSystemDark
                com.nothingness.bruhpatcher.data.ThemeMode.LIGHT -> false
                com.nothingness.bruhpatcher.data.ThemeMode.DARK -> true
            }

            com.nothingness.bruhpatcher.ui.theme.AutoPatcherTheme(
                engine = themeEngine,
                darkTheme = isDark,
                dynamicColor = useDynamicColor,
                amoledMode = useAmoledMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
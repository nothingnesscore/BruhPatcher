package com.nothingness.bruhpatcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothingness.bruhpatcher.ui.theme.AppColors
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.DarkBackground,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        },
        containerColor = AppColors.DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bruh Patcher",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "v2.0.0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.HyperOsCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Universal Android Edition (Android 8.0 - 17, AOSP, HyperOS & All ROMs)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.PrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bruh Patcher is an all-in-one, universal Android framework patcher supporting Android 8.0 through Android 17 (Baklava / API 37) across all custom and stock OEM ROMs (AOSP, Pixel, Xiaomi MIUI & HyperOS 1-4, Samsung OneUI, OxygenOS, ColorOS, etc.). It is not limited to any single OS or version—it can patch all available features across any supported device. Automates patching on-device with DynamicInstaller and remotely via cloud workflows, featuring Kaorios Toolbox v2.0.6.0 and CorePatch signature bypass.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // How it works
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "How It Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val steps = listOf(
                        "1. Select Local DynamicInstaller mode or Cloud mode in Patches tab",
                        "2. Auto-extract framework JARs with Root or select them manually",
                        "3. Toggle desired features (Kaorios v2.0.6.0, CorePatch, A17 Fix)",
                        "4. Tap Start Patching - bytecode hooks are applied with live logs",
                        "5. Magisk/KSU module is generated and saved directly to Downloads",
                        "6. Flash module in Magisk / KernelSU / APatch and reboot"
                    )
                    
                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Features
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Supported Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val features = listOf(
                        "Kaorios Toolbox v2.0.6.0" to "Hardware Keybox attestation, Play Integrity Fix, Game 120 FPS spoof, caller-aware stealth isolation",
                        "Signature Verification Bypass" to "Disable APK signature checks, digest mismatches, and downgrade restrictions on A13-A17 & HyperOS 4",
                        "Android 17 Build Reflection Patch" to "Unfinalizes static-final fields in Build & Build\$VERSION for HyperOS 4 / Android 17",
                        "Disable Secure Flag" to "Enable screenshots, screen recording, and casting in DRM and banking apps",
                        "HyperOS CN Notification Fix" to "Eliminates push notification freezes and delays on MIUI/HyperOS China ROMs",
                        "Google Photos Unlimited" to "Enables unlimited original quality cloud backup by spoofing Pixel XL"
                    )
                    
                    features.forEach { (name, desc) ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextMuted
                            )
                        }
                    }
                }
            }

            // Credits
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Credits & Developers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Bruh Patcher: nothingnesscore",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kaorios Toolbox: hzzmonetvn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "FrameworkPatcher base: Jefino9488",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GitHub: github.com/nothingnesscore/BruhPatcher",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.HyperOsCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for Liquid Glass Floating Bar
        }
    }
}

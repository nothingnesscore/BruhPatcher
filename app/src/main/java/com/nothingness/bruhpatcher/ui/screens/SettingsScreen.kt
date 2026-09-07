package com.nothingness.bruhpatcher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nothingness.bruhpatcher.R
import com.nothingness.bruhpatcher.ui.theme.AppColors
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboxStatus by viewModel.keyboxStatus.collectAsState()
    val isSyncingKeybox by viewModel.isSyncingKeybox.collectAsState()
    val isNoMountInstalled by viewModel.isNoMountInstalled.collectAsState()
    val isNoMountGuardTripped by viewModel.isNoMountGuardTripped.collectAsState()
    val isResettingNoMount by viewModel.isResettingNoMount.collectAsState()

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_hyperos_patch_logo),
                                contentDescription = "HyperOS Patching Logo",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column {
                                Text(
                                    text = "Bruh Patcher",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "HyperOS Alive Design Edition",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.HyperOsCyan
                                )
                            }
                        }
                        Text(
                            text = "v2.0.7",
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

            // Feedback & Diagnostics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Diagnostics & Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Having errors or patching issues? Tap below to copy a complete diagnostic report (device specs, root status, keybox info, and recent terminal logs) to your clipboard, and paste it directly into your chat or GitHub issue for instant diagnosis and fixes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val report = viewModel.copyDiagnosticReport(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("BruhPatcher Diagnostics", report))
                                Toast.makeText(context, "Diagnostic report copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.HyperOsBlue)
                        ) {
                            Text("Copy Report")
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncLatestKeybox() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSyncingKeybox
                        ) {
                            if (isSyncingKeybox) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.HyperOsCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...")
                            } else {
                                Text("Sync Keybox")
                            }
                        }
                    }

                    if (keyboxStatus != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Keybox Hub: ${keyboxStatus?.status?.uppercase()} (${keyboxStatus?.strongCount} Strong / ${keyboxStatus?.totalKeys} Keys)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.HyperOsCyan
                        )
                    }
                }
            }

            // NoMount VFS Engine Status & Auto-Recovery
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NoMount VFS Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        val statusText = when {
                            !isNoMountInstalled -> "Not Detected"
                            isNoMountGuardTripped -> "Tripped (Disabled)"
                            else -> "Active & Mounting"
                        }
                        val statusBg = when {
                            !isNoMountInstalled -> AppColors.TextMuted.copy(alpha = 0.2f)
                            isNoMountGuardTripped -> AppColors.Error.copy(alpha = 0.2f)
                            else -> AppColors.Success.copy(alpha = 0.2f)
                        }
                        val statusColor = when {
                            !isNoMountInstalled -> AppColors.TextMuted
                            isNoMountGuardTripped -> AppColors.Error
                            else -> AppColors.Success
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bruh Patcher generates modules 100% compatible with NoMount (maxsteeel/nomount & Bouteillepleine/NoMount-Suite) using transparent VFS path redirection. Legacy bind-mounts and mount_mirrors calls have been eliminated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )

                    if (isNoMountGuardTripped) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚠️ NoMount bootloop protection triggered on a previous crash and disabled mounting. Tap 'Re-arm Guard' below to reset bootcount and remove disable flags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resetNoMountGuard() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isResettingNoMount,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNoMountGuardTripped) AppColors.Error else AppColors.HyperOsBlue
                            )
                        ) {
                            if (isResettingNoMount) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-arming...")
                            } else {
                                Text("Re-arm Guard")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.refreshNoMountStatus() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refresh Status")
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

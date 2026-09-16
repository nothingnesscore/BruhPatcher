package com.nothingness.bruhpatcher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothingness.bruhpatcher.R
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixCard
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixCategoryHeader
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixGroupCard
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixItemPosition
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixPreferenceItem
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixStatusBadge
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixSwitchPreference
import com.nothingness.bruhpatcher.ui.theme.AppColors
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val keyboxStatus by viewModel.keyboxStatus.collectAsState()
    val isSyncingKeybox by viewModel.isSyncingKeybox.collectAsState()
    val isNoMountInstalled by viewModel.isNoMountInstalled.collectAsState()
    val isNoMountGuardTripped by viewModel.isNoMountGuardTripped.collectAsState()
    val isResettingNoMount by viewModel.isResettingNoMount.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val useLiquidGlassNavbar by viewModel.useLiquidGlassNavbar.collectAsState()
    val useAmoledMode by viewModel.useAmoledMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Design", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Card (MIUIX Alive Design)
            MiuixCard {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "HyperOS Alive Design • Universal",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor
                            )
                        }
                    }
                    MiuixStatusBadge(
                        text = "v2.4.0",
                        containerColor = primaryColor.copy(alpha = 0.15f),
                        contentColor = primaryColor,
                        showDot = true
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Universal Android Framework Patcher (Android 8–17, HyperOS 1–4, AOSP & OEM ROMs)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All-in-one framework modifier with on-device DynamicInstaller and cloud workflow compilation. Incorporates Kaorios Toolbox v2.0.6.0, Google Photos Unlimited Backup, and NoMount VFS transparent redirection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Appearance & Navigation Controls
            MiuixCategoryHeader(title = "Appearance & Navigation")
            val currentThemeEngine by viewModel.themeEngine.collectAsState()
            MiuixGroupCard {
                MiuixPreferenceItem(
                    title = "UI Theme Engine",
                    subtitle = "${currentThemeEngine.label} • ${when (currentThemeEngine) {
                        com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AUTO -> "Auto-detects HyperOS or AOSP"
                        com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX -> "Xiaomi HyperOS 4 Alive Design"
                        com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AOSP_MATERIAL3 -> "Google Material 3 MD3"
                    }}",
                    icon = Icons.Rounded.FormatPaint,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.TOP,
                    onClick = {
                        val next = when (currentThemeEngine) {
                            com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AUTO -> com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX
                            com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX -> com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AOSP_MATERIAL3
                            com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AOSP_MATERIAL3 -> com.nothingness.bruhpatcher.ui.theme.ThemeEngine.AUTO
                        }
                        viewModel.setThemeEngine(next)
                        Toast.makeText(context, "Switched Theme Engine to ${next.label}", Toast.LENGTH_SHORT).show()
                    }
                )
                MiuixSwitchPreference(
                    title = "Monet Dynamic Color",
                    subtitle = "Harmonize interface palette dynamically with system wallpaper accent tones (Android 12+ / Monet)",
                    icon = Icons.Rounded.ColorLens,
                    iconTint = primaryColor,
                    checked = useDynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    position = MiuixItemPosition.MIDDLE
                )
                MiuixSwitchPreference(
                    title = "Pure Black AMOLED Mode",
                    subtitle = "Apply deep #000000 black to surfaces and backgrounds in dark theme to conserve battery on OLED displays",
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = AppColors.HyperOsCyan,
                    checked = useAmoledMode,
                    onCheckedChange = { viewModel.setAmoledMode(it) },
                    position = MiuixItemPosition.MIDDLE
                )
                MiuixSwitchPreference(
                    title = "Liquid Glass Floating Navbar",
                    subtitle = "Switch between floating optical refraction glass bar and docked MIUIX navigation bar",
                    icon = Icons.Rounded.Layers,
                    iconTint = AppColors.HyperOsCyan,
                    checked = useLiquidGlassNavbar,
                    onCheckedChange = { viewModel.setLiquidGlassNavbar(it) },
                    position = MiuixItemPosition.BOTTOM
                )
            }

            // Design Philosophy & Citations
            MiuixCategoryHeader(title = "Design Philosophy & Citations")
            MiuixGroupCard {
                MiuixPreferenceItem(
                    title = "Kyant0/AndroidLiquidGlass",
                    subtitle = "Foundational optical glass physics: SDF squircle curvature, circleMap lens refraction, 7-band chromatic dispersion & damped spring drag.",
                    icon = Icons.Rounded.ColorLens,
                    iconTint = AppColors.HyperOsCyan,
                    iconBackground = AppColors.HyperOsCyan.copy(alpha = 0.15f),
                    position = MiuixItemPosition.TOP,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Source", "https://github.com/Kyant0/AndroidLiquidGlass"))
                        Toast.makeText(context, "Copied Kyant0 GitHub URL!", Toast.LENGTH_SHORT).show()
                    }
                )
                MiuixPreferenceItem(
                    title = "compose-miuix-ui (yukonga)",
                    subtitle = "Compose Multiplatform port of Liquid Glass navbar (IosLiquidGlassNavigationBar) & top.yukonga.miuix.kmp design guidelines.",
                    icon = Icons.Rounded.Layers,
                    iconTint = primaryColor,
                    iconBackground = primaryColor.copy(alpha = 0.15f),
                    position = MiuixItemPosition.MIDDLE,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Source", "https://github.com/compose-miuix-ui/miuix"))
                        Toast.makeText(context, "Copied compose-miuix-ui GitHub URL!", Toast.LENGTH_SHORT).show()
                    }
                )
                MiuixPreferenceItem(
                    title = "SukiSU-Ultra (SukiSU Manager)",
                    subtitle = "Production FloatingBottomBar with AGSL InteractiveHighlight bloom, DampedDragAnimation & MIUIX Alive Design.",
                    icon = Icons.Rounded.Hub,
                    iconTint = primaryColor,
                    iconBackground = primaryColor.copy(alpha = 0.15f),
                    position = MiuixItemPosition.BOTTOM,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Source", "https://github.com/SukiSU-Ultra/SukiSU-Ultra"))
                        Toast.makeText(context, "Copied SukiSU-Ultra GitHub URL!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Diagnostics & Feedback
            MiuixCategoryHeader(title = "Diagnostics & Troubleshooting")
            MiuixCard {
                Text(
                    text = "Diagnostics & Live Reporting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generate and copy a diagnostic dump (device build props, root status, keybox hub, and recent terminal logs) to your clipboard for easy debugging and GitHub issues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
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
                                color = primaryColor
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
                        color = primaryColor
                    )
                }
            }

            // NoMount VFS Engine Status
            MiuixCategoryHeader(title = "Virtual Filesystem (NoMount)")
            MiuixCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NoMount VFS Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val statusText = when {
                        !isNoMountInstalled -> "Not Detected"
                        isNoMountGuardTripped -> "Tripped"
                        else -> "Active"
                    }
                    val statusColor = when {
                        !isNoMountInstalled -> AppColors.TextMuted
                        isNoMountGuardTripped -> AppColors.Error
                        else -> AppColors.Success
                    }

                    MiuixStatusBadge(
                        text = statusText,
                        contentColor = statusColor,
                        containerColor = statusColor.copy(alpha = 0.15f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Modules are built with full NoMount (maxsteeel/nomount & Bouteillepleine/NoMount-Suite) compatibility using transparent VFS path redirection. Bind-mount collisions are completely avoided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isNoMountGuardTripped) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠️ Bootloop protection triggered. Tap 'Re-arm Guard' below to reset the counter and re-enable mounting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetNoMountGuard() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isResettingNoMount,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNoMountGuardTripped) AppColors.Error else primaryColor
                        )
                    ) {
                        if (isResettingNoMount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
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

            // Project Credits
            MiuixCategoryHeader(title = "Credits & Maintainers")
            MiuixGroupCard {
                MiuixPreferenceItem(
                    title = "Bruh Patcher Maintainer",
                    subtitle = "nothingnesscore",
                    icon = Icons.Rounded.Code,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.TOP
                )
                MiuixPreferenceItem(
                    title = "Kaorios Toolbox v2.0.6.0",
                    subtitle = "hzzmonetvn (Hardware Keybox attestation, stealth isolation)",
                    icon = Icons.Rounded.Security,
                    iconTint = AppColors.HyperOsCyan,
                    position = MiuixItemPosition.MIDDLE
                )
                MiuixPreferenceItem(
                    title = "FrameworkPatcher Base",
                    subtitle = "Jefino9488",
                    icon = Icons.Rounded.DataObject,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.BOTTOM
                )
            }

            Spacer(modifier = Modifier.height(140.dp)) // Space for Liquid Glass Floating Bar
        }
    }
}

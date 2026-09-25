package com.nothingness.bruhpatcher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nothingness.bruhpatcher.data.ThemeMode
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixCard
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixCategoryHeader
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixGroupCard
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixItemPosition
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixPreferenceItem
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixStatusBadge
import com.nothingness.bruhpatcher.ui.components.miuix.MiuixSwitchPreference
import com.nothingness.bruhpatcher.ui.theme.ThemeEngine
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

/**
 * Settings & About screen crafted with HyperOS Alive Design / Material 3.
 * Features dialog selection for Theme Mode and Theme Engine, Monet accents,
 * AMOLED mode, and credits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val useLiquidGlassNavbar by viewModel.useLiquidGlassNavbar.collectAsState()
    val useAmoledMode by viewModel.useAmoledMode.collectAsState()
    val currentThemeEngine by viewModel.themeEngine.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()

    var showThemeEngineDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }

    // Theme Engine Selection Dialog
    if (showThemeEngineDialog) {
        AlertDialog(
            onDismissRequest = { showThemeEngineDialog = false },
            title = {
                Text(
                    text = "Select UI Theme Engine",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeEngine.values().forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeEngine(engine)
                                    showThemeEngineDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentThemeEngine == engine),
                                onClick = {
                                    viewModel.setThemeEngine(engine)
                                    showThemeEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = engine.label,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (engine) {
                                        ThemeEngine.AUTO -> "Auto-detects HyperOS or AOSP"
                                        ThemeEngine.HYPEROS_MIUIX -> "Xiaomi HyperOS 4 Alive Design"
                                        ThemeEngine.AOSP_MATERIAL3 -> "Google Material 3 MD3"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeEngineDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Theme Mode Selection Dialog
    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = {
                Text(
                    text = "Select Theme Mode",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeModeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentThemeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = mode.label,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "Syncs with system dark/light mode"
                                        ThemeMode.LIGHT -> "Always light palette"
                                        ThemeMode.DARK -> "Always dark obsidian palette"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

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
                    text = "All-in-one framework modifier with on-device DynamicInstaller and cloud workflow compilation. Incorporates Kaorios Toolbox v3.0, Google Photos Unlimited Backup, and universal Magisk / KernelSU / APatch module generation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Appearance & Navigation Controls
            MiuixCategoryHeader(title = "Appearance & Navigation")
            MiuixGroupCard {
                MiuixPreferenceItem(
                    title = "Theme Mode",
                    subtitle = "${currentThemeMode.label} • ${when (currentThemeMode) {
                        ThemeMode.SYSTEM -> "Follow System"
                        ThemeMode.LIGHT -> "Always Light"
                        ThemeMode.DARK -> "Always Dark"
                    }}",
                    icon = Icons.Rounded.DarkMode,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.TOP,
                    onClick = {
                        showThemeModeDialog = true
                    }
                )
                MiuixPreferenceItem(
                    title = "UI Theme Engine",
                    subtitle = "${currentThemeEngine.label} • ${when (currentThemeEngine) {
                        ThemeEngine.AUTO -> "Auto-detects HyperOS or AOSP"
                        ThemeEngine.HYPEROS_MIUIX -> "Xiaomi HyperOS 4 Alive Design"
                        ThemeEngine.AOSP_MATERIAL3 -> "Google Material 3 MD3"
                    }}",
                    icon = Icons.Rounded.FormatPaint,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.MIDDLE,
                    onClick = {
                        showThemeEngineDialog = true
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
                    iconTint = primaryColor,
                    checked = useAmoledMode,
                    onCheckedChange = { viewModel.setAmoledMode(it) },
                    position = MiuixItemPosition.MIDDLE
                )
                MiuixSwitchPreference(
                    title = "Liquid Glass Floating Navbar",
                    subtitle = "Switch between floating optical refraction glass bar and docked MIUIX navigation bar",
                    icon = Icons.Rounded.Layers,
                    iconTint = primaryColor,
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
                    subtitle = "Foundational optical glass physics: SDF squircle curvature, circleMap lens refraction & damped spring drag.",
                    icon = Icons.Rounded.ColorLens,
                    iconTint = primaryColor,
                    iconBackground = primaryColor.copy(alpha = 0.15f),
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
                        clipboard.setPrimaryClip(ClipData.newPlainText("Source", "https://github.com/yukonga/compose-miuix-ui"))
                        Toast.makeText(context, "Copied yukonga GitHub URL!", Toast.LENGTH_SHORT).show()
                    }
                )
                MiuixPreferenceItem(
                    title = "Keybox Hub Integration",
                    subtitle = "Hardware attestation certificates provided by keybox.hzzmonet.io.vn and KaoriOS Toolbox ecosystem.",
                    icon = Icons.Rounded.Hub,
                    iconTint = primaryColor,
                    iconBackground = primaryColor.copy(alpha = 0.15f),
                    position = MiuixItemPosition.BOTTOM,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Source", "https://keybox.hzzmonet.io.vn"))
                        Toast.makeText(context, "Copied Keybox Hub URL!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Project Credits
            MiuixCategoryHeader(title = "Credits & Maintainers")
            MiuixGroupCard {
                MiuixPreferenceItem(
                    title = "Bruh Patcher Maintainer",
                    subtitle = "nothingnesscore (Lead Developer & Maintainer)",
                    icon = Icons.Rounded.Code,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.TOP
                )
                MiuixPreferenceItem(
                    title = "Kaorios Toolbox Framework",
                    subtitle = "hzzmonetvn (Hardware Keybox attestation, stealth isolation & Android 17 smali engine)",
                    icon = Icons.Rounded.Security,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.MIDDLE
                )
                MiuixPreferenceItem(
                    title = "Kaorios Toolbox Upstream Base",
                    subtitle = "Wuang26 (Original Toolbox Architecture & Utilities)",
                    icon = Icons.Rounded.DataObject,
                    iconTint = primaryColor,
                    position = MiuixItemPosition.BOTTOM
                )
            }

            Spacer(modifier = Modifier.height(140.dp)) // Space for Liquid Glass Floating Bar
        }
    }
}

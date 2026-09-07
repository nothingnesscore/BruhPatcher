package com.nothingness.bruhpatcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import com.nothingness.bruhpatcher.R
import com.nothingness.bruhpatcher.data.api.GitHubRelease
import com.nothingness.bruhpatcher.model.PatchingState
import com.nothingness.bruhpatcher.ui.components.DeviceInfoCard
import com.nothingness.bruhpatcher.ui.components.RootStatusCard
import com.nothingness.bruhpatcher.ui.components.StatusBanner
import com.nothingness.bruhpatcher.ui.theme.AppColors
import com.nothingness.bruhpatcher.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProgress: () -> Unit = {}
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val magiskVersion by viewModel.magiskVersion.collectAsState()
    val patchingState by viewModel.patchingState.collectAsState()
    val matchingReleases by viewModel.matchingReleases.collectAsState()
    val isLoadingReleases by viewModel.isLoadingReleases.collectAsState()
    val keyboxStatus by viewModel.keyboxStatus.collectAsState()
    val isSyncingKeybox by viewModel.isSyncingKeybox.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Proceed regardless of result - repository has fallback logic
        // But we want to try getting permission first
    }

    val isLoading = patchingState is PatchingState.CheckingRoot || patchingState is PatchingState.Scanning

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_hyperos_patch_logo),
                            contentDescription = "HyperOS Patching Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Bruh Patcher",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDeviceInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Info, contentDescription = "About")
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
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AppColors.HyperOsBlue.copy(alpha = 0.25f),
                                AppColors.HyperOsPurple.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_hyperos_patch_logo),
                                contentDescription = "HyperOS Patching Logo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column {
                                Text(
                                    text = "Bruh Patcher",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "HyperOS Alive Design • Universal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.HyperOsCyan
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AppColors.HyperOsBlue, AppColors.HyperOsCyan)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MIUIX GLASS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Universal Android Framework Patcher (Android 8–17, HyperOS 1–4, AOSP & All OEM ROMs)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // Root status
            RootStatusCard(
                isRootAvailable = isRootAvailable,
                magiskVersion = magiskVersion
            )

            // Manual mode hint if root not available
            if (!isRootAvailable && !isLoading) {
                StatusBanner(
                    title = "Manual Mode Available",
                    subtitle = "You can still use manual file selection without root"
                )
            }

            // Device info
            if (!isLoading && deviceInfo != com.nothingness.bruhpatcher.model.DeviceInfo.Empty) {
                DeviceInfoCard(deviceInfo = deviceInfo)
            }

            // Kaorios Toolbox v2.0.6.0 Feature Showcase
            if (!isLoading) {
                com.nothingness.bruhpatcher.ui.components.KaoriosShowcaseCard(
                    keyboxStatus = keyboxStatus,
                    isSyncingKeybox = isSyncingKeybox,
                    onSyncKeybox = { viewModel.syncLatestKeybox() }
                )
            }

            // Existing Releases Section
            if (!isLoading && (matchingReleases.isNotEmpty() || isLoadingReleases)) {
                ExistingReleasesSection(
                    releases = matchingReleases,
                    isLoading = isLoadingReleases,
                    onDownloadClick = { release ->
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        viewModel.downloadAndInstallRelease(release)
                        onNavigateToProgress()
                    },
                    onRefreshClick = { viewModel.fetchMatchingReleases() }
                )
            }

            // Loading state
            if (isLoading) {
                StatusBanner(
                    title = when (patchingState) {
                        is PatchingState.CheckingRoot -> "Checking root access..."
                        is PatchingState.Scanning -> "Scanning device..."
                        else -> "Loading..."
                    },
                    subtitle = "Please wait"
                )
            }

            // Error state
            if (patchingState is PatchingState.Error) {
                StatusBanner(
                    title = "Error",
                    subtitle = (patchingState as PatchingState.Error).message,
                    isError = true
                )
            }

            // Start button
            if (!isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToConfig,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        if (isRootAvailable) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        } else {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configure Patching",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for Liquid Glass Floating Bar
        }
    }
}

@Composable
private fun ExistingReleasesSection(
    releases: List<GitHubRelease>,
    isLoading: Boolean,
    onDownloadClick: (GitHubRelease) -> Unit,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Existing Builds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                IconButton(onClick = onRefreshClick, modifier = Modifier.size(32.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Found ${releases.size} compatible build(s) - download and install directly",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            releases.take(5).forEach { release ->
                ReleaseItem(
                    release = release,
                    onDownloadClick = { onDownloadClick(release) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReleaseItem(
    release: GitHubRelease,
    onDownloadClick: () -> Unit
) {
    val moduleAsset = release.findModuleZip()
    val sizeText = moduleAsset?.let { 
        val sizeMB = it.size / (1024.0 * 1024.0)
        "%.1f MB".format(sizeMB)
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkSurfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = AppColors.Success,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = release.tagName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sizeText.isNotEmpty()) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted
                    )
                }
            }
            OutlinedButton(
                onClick = onDownloadClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Install")
            }
        }
    }
}

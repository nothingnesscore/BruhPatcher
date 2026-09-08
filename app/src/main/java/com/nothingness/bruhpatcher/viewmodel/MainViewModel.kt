package com.nothingness.bruhpatcher.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nothingness.bruhpatcher.BuildConfig
import com.nothingness.bruhpatcher.core.ApiKeyManager
import com.nothingness.bruhpatcher.core.DiExecutor
import com.nothingness.bruhpatcher.core.DiInstaller
import com.nothingness.bruhpatcher.core.FeatureManager
import com.nothingness.bruhpatcher.core.FeatureUpdater
import com.nothingness.bruhpatcher.core.KeyboxManager
import com.nothingness.bruhpatcher.core.KeyboxStatus
import com.nothingness.bruhpatcher.core.ModuleGenerator
import com.nothingness.bruhpatcher.core.PatchFeature
import com.nothingness.bruhpatcher.core.UserFeatureImporter
import com.nothingness.bruhpatcher.core.RootManager
import com.nothingness.bruhpatcher.core.SystemInspector
import com.nothingness.bruhpatcher.data.api.GitHubRelease
import com.nothingness.bruhpatcher.data.repository.UploadRepository
import com.nothingness.bruhpatcher.data.repository.WorkflowRepository
import com.nothingness.bruhpatcher.model.DeviceInfo
import com.nothingness.bruhpatcher.model.Feature
import com.nothingness.bruhpatcher.model.LogEntry
import com.nothingness.bruhpatcher.model.LogTag
import com.nothingness.bruhpatcher.model.PatchingMode
import com.nothingness.bruhpatcher.model.PatchingState
import com.nothingness.bruhpatcher.model.SelectedFile
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import com.nothingness.bruhpatcher.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.io.FileOutputStream

/**
 * Main ViewModel for orchestrating the patching flow
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val useDynamicColor: StateFlow<Boolean> = settingsRepository.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val useLiquidGlassNavbar: StateFlow<Boolean> = settingsRepository.useLiquidGlassNavbar
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeEngine: StateFlow<com.nothingness.bruhpatcher.ui.theme.ThemeEngine> = settingsRepository.themeEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX)

    fun setThemeEngine(engine: com.nothingness.bruhpatcher.ui.theme.ThemeEngine) {
        viewModelScope.launch {
            settingsRepository.setThemeEngine(engine)
        }
    }

    private val workflowRepository = WorkflowRepository()
    private val uploadRepository: UploadRepository by lazy {
        UploadRepository(ApiKeyManager.getPixeldrainApiKey())
    }

    // State
    private val _deviceInfo = MutableStateFlow(DeviceInfo.Empty)
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _magiskVersion = MutableStateFlow<String?>(null)
    val magiskVersion: StateFlow<String?> = _magiskVersion.asStateFlow()

    private val _features = MutableStateFlow<List<Feature>>(emptyList())
    val features: StateFlow<List<Feature>> = _features.asStateFlow()

    private val _patchingState = MutableStateFlow<PatchingState>(PatchingState.Idle)
    val patchingState: StateFlow<PatchingState> = _patchingState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _downloadedModulePath = MutableStateFlow<String?>(null)
    val downloadedModulePath: StateFlow<String?> = _downloadedModulePath.asStateFlow()

    // Patching mode
    private val _patchingMode = MutableStateFlow(PatchingMode.AUTO_EXTRACT)
    val patchingMode: StateFlow<PatchingMode> = _patchingMode.asStateFlow()

    // Manually selected files
    private val _selectedFiles = MutableStateFlow<Map<String, SelectedFile>>(emptyMap())
    val selectedFiles: StateFlow<Map<String, SelectedFile>> = _selectedFiles.asStateFlow()

    // Existing releases that match this device
    private val _matchingReleases = MutableStateFlow<List<GitHubRelease>>(emptyList())
    val matchingReleases: StateFlow<List<GitHubRelease>> = _matchingReleases.asStateFlow()

    private val _isLoadingReleases = MutableStateFlow(false)
    val isLoadingReleases: StateFlow<Boolean> = _isLoadingReleases.asStateFlow()

    // Use local patching (default true) vs cloud workflow
    private val _useLocalPatching = MutableStateFlow(true)
    val useLocalPatching: StateFlow<Boolean> = _useLocalPatching.asStateFlow()

    // Local patch features from features folder
    private val _localPatchFeatures = MutableStateFlow<List<com.nothingness.bruhpatcher.core.LocalPatchFeature>>(emptyList())
    val localPatchFeatures: StateFlow<List<com.nothingness.bruhpatcher.core.LocalPatchFeature>> = _localPatchFeatures.asStateFlow()

    // Feature update state
    private val _isUpdatingFeatures = MutableStateFlow(false)
    val isUpdatingFeatures: StateFlow<Boolean> = _isUpdatingFeatures.asStateFlow()

    // Keybox Hub status & sync state
    private val _keyboxStatus = MutableStateFlow<KeyboxStatus?>(null)
    val keyboxStatus: StateFlow<KeyboxStatus?> = _keyboxStatus.asStateFlow()

    private val _isSyncingKeybox = MutableStateFlow(false)
    val isSyncingKeybox: StateFlow<Boolean> = _isSyncingKeybox.asStateFlow()

    // NoMount VFS Engine state
    private val _isNoMountInstalled = MutableStateFlow(false)
    val isNoMountInstalled: StateFlow<Boolean> = _isNoMountInstalled.asStateFlow()

    private val _isNoMountGuardTripped = MutableStateFlow(false)
    val isNoMountGuardTripped: StateFlow<Boolean> = _isNoMountGuardTripped.asStateFlow()

    private val _isResettingNoMount = MutableStateFlow(false)
    val isResettingNoMount: StateFlow<Boolean> = _isResettingNoMount.asStateFlow()

    init {
        try {
            val app = getApplication<Application>()
            val prefs = app.getSharedPreferences("bruh_patcher_prefs", Context.MODE_PRIVATE)
            val lastVersion = prefs.getInt("last_version_code", 0)
            if (lastVersion < BuildConfig.VERSION_CODE) {
                FeatureManager.clearUpdatedFeatures(app)
                prefs.edit().putInt("last_version_code", BuildConfig.VERSION_CODE).apply()
            }
        } catch (_: Exception) {}
        checkRootAndScan()
        loadLocalPatchFeatures()
        fetchKeyboxStatus()
    }

    private fun loadLocalPatchFeatures() {
        viewModelScope.launch {
            _localPatchFeatures.value = FeatureManager.getLocalPatchFeatures(getApplication())
        }
    }

    fun refreshNoMountStatus() {
        if (!_isRootAvailable.value) return
        viewModelScope.launch {
            val installed = RootManager.isNoMountInstalled()
            val tripped = RootManager.isNoMountDisabledByGuard()
            _isNoMountInstalled.value = installed
            _isNoMountGuardTripped.value = tripped
        }
    }

    fun resetNoMountGuard() {
        viewModelScope.launch {
            _isResettingNoMount.value = true
            addLog(LogTag.INFO, "Resetting NoMount bootloop protector flags...")
            val result = RootManager.resetNoMountBootloopGuard()
            _isResettingNoMount.value = false
            if (result.isSuccess) {
                _isNoMountGuardTripped.value = false
                addLog(LogTag.SUCCESS, "NoMount guard reset successfully! Metamodule is re-armed.")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                addLog(LogTag.ERROR, "Failed to reset NoMount guard: $err")
            }
        }
    }

    fun setPatchingMode(mode: PatchingMode) {
        _patchingMode.value = mode
        if (mode == PatchingMode.AUTO_EXTRACT) {
            _selectedFiles.value = emptyMap()
        }
    }

    fun addSelectedFile(fileType: String, uri: Uri, name: String, size: Long) {
        _selectedFiles.value = _selectedFiles.value + (fileType to SelectedFile(name, uri, size))
    }

    fun removeSelectedFile(fileType: String) {
        _selectedFiles.value = _selectedFiles.value - fileType
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyMap()
    }

    private fun checkRootAndScan() {
        viewModelScope.launch {
            _patchingState.value = PatchingState.CheckingRoot

            val hasRoot = RootManager.requestRoot()
            _isRootAvailable.value = hasRoot

            if (hasRoot) {
                _magiskVersion.value = RootManager.getMagiskVersion()
                addLog(LogTag.INFO, "Root access granted")

                val nmInstalled = RootManager.isNoMountInstalled()
                val nmTripped = RootManager.isNoMountDisabledByGuard()
                _isNoMountInstalled.value = nmInstalled
                _isNoMountGuardTripped.value = nmTripped

                if (nmInstalled) {
                    if (nmTripped) {
                        addLog(LogTag.WARN, "NoMount metamodule detected, but its bootloop protector is TRIPPED! Reset guard before rebooting.")
                    } else {
                        addLog(LogTag.INFO, "NoMount VFS metamodule detected and ACTIVE")
                    }
                }

                _patchingState.value = PatchingState.Scanning("Scanning device...")
                val info = SystemInspector.getDeviceInfo()
                _deviceInfo.value = info
                _features.value = FeatureManager.getAvailableFeatures(info.hasMiuiServicesJar)

                addLog(LogTag.INFO, "Device: ${info.deviceName} (${info.deviceCodename})")
                addLog(LogTag.INFO, "Android ${info.androidVersion} (API ${info.apiLevel})")

                if (info.hasFrameworkJar) addLog(LogTag.INFO, "Found framework.jar")
                if (info.hasServicesJar) addLog(LogTag.INFO, "Found services.jar")
                if (info.hasMiuiServicesJar) addLog(LogTag.INFO, "Found miui-services.jar")

                // Check for existing releases
                fetchMatchingReleases(info)
            } else {
                addLog(LogTag.ERROR, "Root access denied - Manual mode available")
                _patchingState.value = PatchingState.Scanning("Collecting device info...")
                val info = SystemInspector.getDeviceInfo()
                _deviceInfo.value = info
                _features.value = FeatureManager.getAvailableFeatures(false)
                
                // Still check for releases even without root
                fetchMatchingReleases(info)
            }

            _patchingState.value = PatchingState.Idle
        }
    }

    /**
     * Fetch existing releases that match this device
     */
    fun fetchMatchingReleases(deviceInfo: DeviceInfo = _deviceInfo.value) {
        if (deviceInfo == DeviceInfo.Empty) return
        
        viewModelScope.launch {
            _isLoadingReleases.value = true
            
            val result = workflowRepository.findMatchingReleases(
                deviceCodename = deviceInfo.deviceCodename,
                versionSafe = deviceInfo.safeVersionName
            )
            
            result.fold(
                onSuccess = { releases ->
                    _matchingReleases.value = releases
                    if (releases.isNotEmpty()) {
                        addLog(LogTag.INFO, "Found ${releases.size} existing build(s) for your device")
                    }
                },
                onFailure = { 
                    _matchingReleases.value = emptyList()
                }
            )
            
            _isLoadingReleases.value = false
        }
    }

    /**
     * Download and install an existing release
     */
    fun downloadAndInstallRelease(release: GitHubRelease) {
        viewModelScope.launch {
            try {
                val asset = release.findModuleZip()
                if (asset == null) {
                    _patchingState.value = PatchingState.Error("No module zip found in this release", recoverable = true)
                    return@launch
                }

                addLog(LogTag.INFO, "Downloading existing release: ${release.tagName}")
                downloadModule(asset.browserDownloadUrl, asset.name)

            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Error: ${e.message}")
                _patchingState.value = PatchingState.Error(e.message ?: "Unknown error", recoverable = true)
            }
        }
    }

    fun updateFeature(featureId: String, enabled: Boolean) {
        _features.value = FeatureManager.updateFeature(_features.value, featureId, enabled)
    }

    fun updateLocalPatchFeature(featureId: String, enabled: Boolean) {
        _localPatchFeatures.value = _localPatchFeatures.value.map { feature ->
            if (feature.id == featureId) feature.copy(isEnabled = enabled) else feature
        }
    }

    /**
     * Import a user-provided script file for local patching
     */
    fun importUserFeature(uri: Uri, name: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    UserFeatureImporter.import(context, input, name)
                    loadLocalPatchFeatures() // Refresh the list
                }
            } catch (e: Exception) {
                // Import failed silently - script won't appear in list
            }
        }
    }

    /**
     * Refresh the local patch features list
     */
    fun refreshLocalPatchFeatures() {
        loadLocalPatchFeatures()
    }

    /**
     * Delete a user-imported feature script
     */
    fun deleteUserFeature(featureId: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (FeatureManager.deleteUserFeature(context, featureId)) {
                loadLocalPatchFeatures() // Refresh the list
            }
        }
    }

    /**
     * Update feature scripts from the GitHub repository
     */
    fun updateFeatureScripts() {
        viewModelScope.launch {
            _isUpdatingFeatures.value = true
            try {
                val context = getApplication<Application>()
                val result = FeatureUpdater.updateScripts(context)
                result.fold(
                    onSuccess = { count ->
                        addLog(LogTag.INFO, "Updated $count script(s) from repository")
                        loadLocalPatchFeatures() // Refresh the list
                    },
                    onFailure = { error ->
                        addLog(LogTag.ERROR, "Failed to update scripts: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Update failed: ${e.message}")
            } finally {
                _isUpdatingFeatures.value = false
            }
        }
    }

    fun startPatching() {
        viewModelScope.launch {
            val mode = _patchingMode.value

            try {
                val extractedFiles = when (mode) {
                    PatchingMode.AUTO_EXTRACT -> {
                        val info = _deviceInfo.value
                        if (!info.hasFrameworkJar || !info.hasServicesJar) {
                            _patchingState.value = PatchingState.Error("Required framework files not found", recoverable = false)
                            return@launch
                        }
                        extractFrameworkFiles()
                    }
                    PatchingMode.MANUAL_SELECT -> {
                        val selected = _selectedFiles.value
                        if (!selected.containsKey("framework.jar") || !selected.containsKey("services.jar")) {
                            _patchingState.value = PatchingState.Error("Please select framework.jar and services.jar", recoverable = true)
                            return@launch
                        }
                        copySelectedFiles()
                    }
                }

                if (extractedFiles.isEmpty()) {
                    _patchingState.value = PatchingState.Error("No files were extracted", recoverable = false)
                    return@launch
                }

                val uploadedUrls = uploadFiles(extractedFiles)
                if (uploadedUrls.isEmpty()) {
                    _patchingState.value = PatchingState.Error("File upload failed", recoverable = true)
                    return@launch
                }

                val info = _deviceInfo.value
                val featureString = FeatureManager.buildFeatureString(_features.value)
                triggerWorkflow(info, uploadedUrls, featureString)
                waitForRelease(info)

            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Error: ${e.message}")
                _patchingState.value = PatchingState.Error(e.message ?: "Unknown error", recoverable = true)
            } finally {
                RootManager.cleanup(getApplication<Application>().filesDir)
            }
        }
    }

    private suspend fun extractFrameworkFiles(requiredJars: List<String> = listOf("framework.jar")): Map<String, File> {
        val files = mutableMapOf<String, File>()
        val filesDir = getApplication<Application>().filesDir
        val frameworkPaths = SystemInspector.getAvailableFrameworkPaths()
        
        // Filter to only required JARs
        val pathsToExtract = frameworkPaths.filter { requiredJars.contains(it.key) }
        val totalFiles = pathsToExtract.size
        var extracted = 0

        for ((name, path) in pathsToExtract) {
            _patchingState.value = PatchingState.Extracting(name, extracted, totalFiles)
            addLog(LogTag.EXTRACT, "Extracting $name...")

            val result = RootManager.extractSystemFile(path, filesDir, name)
            result.fold(
                onSuccess = { file ->
                    files[name] = file
                    extracted++
                    addLog(LogTag.EXTRACT, "Extracted $name (${file.length() / 1024} KB)")
                },
                onFailure = { error ->
                    addLog(LogTag.ERROR, "Failed to extract $name: ${error.message}")
                }
            )
        }

        return files
    }

    private suspend fun copySelectedFiles(): Map<String, File> {
        val files = mutableMapOf<String, File>()
        val filesDir = getApplication<Application>().filesDir
        val context = getApplication<Application>()
        val selected = _selectedFiles.value
        val totalFiles = selected.size
        var copied = 0

        for ((name, selectedFile) in selected) {
            _patchingState.value = PatchingState.Extracting(name, copied, totalFiles)
            addLog(LogTag.EXTRACT, "Copying $name from storage...")

            try {
                val destFile = File(filesDir, name)
                context.contentResolver.openInputStream(selectedFile.uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                files[name] = destFile
                copied++
                addLog(LogTag.EXTRACT, "Copied $name (${destFile.length() / 1024} KB)")
            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Failed to copy $name: ${e.message}")
            }
        }

        return files
    }

    private suspend fun uploadFiles(files: Map<String, File>): Map<String, String> {
        val totalFiles = files.size
        var uploaded = 0

        val result = uploadRepository.uploadFiles(files) { fileName, progress ->
            _patchingState.value = PatchingState.Uploading(fileName, progress, uploaded, totalFiles)
        }

        return result.fold(
            onSuccess = { urls ->
                urls.forEach { (name, url) ->
                    uploaded++
                    addLog(LogTag.UPLOAD, "$name uploaded: $url")
                }
                urls
            },
            onFailure = { error ->
                addLog(LogTag.ERROR, "Upload failed: ${error.message}")
                emptyMap()
            }
        )
    }

    private suspend fun triggerWorkflow(
        deviceInfo: DeviceInfo,
        urls: Map<String, String>,
        features: String
    ) {
        _patchingState.value = PatchingState.TriggeringWorkflow
        addLog(LogTag.REMOTE, "Triggering cloud patching workflow...")

        val result = workflowRepository.triggerWorkflow(
            deviceInfo = deviceInfo,
            frameworkUrl = urls["framework.jar"] ?: "",
            servicesUrl = urls["services.jar"] ?: "",
            miuiServicesUrl = urls["miui-services.jar"] ?: "",
            features = features
        )

        result.fold(
            onSuccess = { response ->
                addLog(LogTag.REMOTE, "Workflow triggered! Run ID: ${response.runId}")
                _patchingState.value = PatchingState.WaitingForBuild(runId = response.runId?.toString())
            },
            onFailure = { error ->
                throw error
            }
        )
    }

    private suspend fun waitForRelease(deviceInfo: DeviceInfo) {
        addLog(LogTag.WAITING, "Waiting for build to complete...")

        val result = workflowRepository.pollForRelease(
            deviceCodename = deviceInfo.deviceCodename,
            versionSafe = deviceInfo.safeVersionName,
            onPoll = { attempt ->
                _patchingState.value = PatchingState.WaitingForBuild(
                    elapsedSeconds = attempt * 30
                )
                if (attempt % 2 == 0) {
                    addLog(LogTag.WAITING, "Still waiting... (${attempt * 30}s elapsed)")
                }
            }
        )

        result.fold(
            onSuccess = { release ->
                addLog(LogTag.SUCCESS, "Build complete! Release: ${release.tagName}")
                val asset = release.findModuleZip()
                if (asset != null) {
                    downloadModule(asset.browserDownloadUrl, asset.name)
                } else {
                    throw Exception("No module zip found in release")
                }
            },
            onFailure = { error ->
                throw error
            }
        )
    }

    private suspend fun downloadModule(url: String, fileName: String) {
        addLog(LogTag.DOWNLOAD, "Downloading module...")
        _patchingState.value = PatchingState.Downloading(0)

        val context = getApplication<Application>()
        val result = workflowRepository.downloadFile(context, url, fileName) { progress ->
            _patchingState.value = PatchingState.Downloading(progress)
        }

        result.fold(
            onSuccess = { file ->
                addLog(LogTag.DOWNLOAD, "Downloaded: ${file.absolutePath}")
                _downloadedModulePath.value = file.absolutePath
                _patchingState.value = PatchingState.ReadyToInstall(file.absolutePath)
            },
            onFailure = { error ->
                throw error
            }
        )
    }

    fun reboot() {
        viewModelScope.launch {
            RootManager.reboot()
        }
    }

    private val _savedLogPath = MutableStateFlow<String?>(null)
    val savedLogPath: StateFlow<String?> = _savedLogPath.asStateFlow()

    fun resetState() {
        _patchingState.value = PatchingState.Idle
        _logs.value = emptyList()
        _downloadedModulePath.value = null
        _savedLogPath.value = null
    }

    private fun addLog(tag: LogTag, message: String) {
        _logs.value = _logs.value + LogEntry(tag, message)
    }

    /**
     * Formats all log entries with device info and timestamps
     */
    fun formatLogs(): String {
        val info = _deviceInfo.value
        return buildString {
            appendLine("=== BRUH PATCHER DIAGNOSTIC & PATCH LOGS ===")
            appendLine("Generated: ${java.util.Date()}")
            appendLine("App Version: v2.0.7 (Universal Edition)")
            appendLine("Device: ${info.deviceName} (${info.deviceCodename})")
            appendLine("Brand/Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${info.androidVersion} (API ${info.apiLevel})")
            appendLine("OS Badge: ${info.osBadgeText} (${info.versionName})")
            appendLine("NoMount Metamodule Installed: ${RootManager.isNoMountInstalled()}")
            appendLine("NoMount Bootloop Guard Tripped: ${_isNoMountGuardTripped.value}")
            appendLine("=" .repeat(60))
            appendLine()
            
            _logs.value.forEach { entry ->
                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                val time = timeFormat.format(java.util.Date(entry.timestamp))
                appendLine("$time [${entry.tag.displayName}] ${entry.message}")
            }
        }
    }

    /**
     * Internal implementation of saveLogs that writes both timestamped and latest logs to Downloads
     */
    suspend fun saveLogsInternal(customName: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val fileName = customName ?: "BruhPatcher_logs_$timestamp.txt"
            
            val logContent = formatLogs()
            
            val tempFile = File(context.cacheDir, fileName)
            tempFile.writeText(logContent)
            
            val result = RootManager.moveToDownloads(tempFile, fileName)
            
            // Also write / update fixed "BruhPatcher_latest.log" in Downloads
            try {
                val latestTemp = File(context.cacheDir, "BruhPatcher_latest.log")
                latestTemp.writeText(logContent)
                RootManager.moveToDownloads(latestTemp, "BruhPatcher_latest.log")
            } catch (_: Exception) {}

            if (result.isSuccess) {
                val savedFile = result.getOrThrow()
                _savedLogPath.value = savedFile.absolutePath
                addLog(LogTag.SUCCESS, "Patch log saved to: Downloads/$fileName")
            } else {
                addLog(LogTag.WARN, "Failed to save log to Downloads: ${result.exceptionOrNull()?.message}")
            }
            result
        } catch (e: Exception) {
            addLog(LogTag.ERROR, "Failed to save logs: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Save logs to Downloads folder on demand
     */
    fun saveLogs() {
        viewModelScope.launch {
            saveLogsInternal()
        }
    }

    /**
     * Exports full diagnostic report to clipboard for easy feedback/bug reporting
     */
    fun copyDiagnosticReport(context: Context): String {
        val info = _deviceInfo.value
        val report = buildString {
            appendLine("=== BRUH PATCHER DIAGNOSTIC REPORT ===")
            appendLine("Timestamp: ${java.util.Date()}")
            appendLine("App Version: v2.0.3 (Universal Edition - Apktool 2.11.1 Engine & NoMount VFS)")
            appendLine()
            appendLine("--- Device Information ---")
            appendLine("Device: ${info.deviceName} (${info.deviceCodename})")
            appendLine("Brand/Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${info.androidVersion} (API ${info.apiLevel})")
            appendLine("OS Badge: ${info.osBadgeText} (${info.versionName})")
            appendLine("Framework JARs: framework=${info.hasFrameworkJar}, services=${info.hasServicesJar}, miui-services=${info.hasMiuiServicesJar}, miui-framework=${info.hasMiuiFrameworkJar}")
            appendLine()
            appendLine("--- Root & Environment ---")
            appendLine("Root Available: ${_isRootAvailable.value}")
            appendLine("Root Manager: ${RootManager.getRootManagerType()}")
            appendLine("Magisk/KSU Version: ${_magiskVersion.value ?: "N/A"}")
            appendLine("NoMount Metamodule Installed: ${_isNoMountInstalled.value}")
            appendLine("NoMount Bootloop Guard Tripped: ${_isNoMountGuardTripped.value}")
            appendLine()
            appendLine("--- Keybox Hub ---")
            val kb = _keyboxStatus.value
            appendLine("Keybox Status: ${kb?.status ?: "Unknown"} (Strong: ${kb?.strongCount ?: 0}, Device: ${kb?.deviceCount ?: 0}, Banned: ${kb?.bannedCount ?: 0})")
            appendLine()
            appendLine("--- Patching State ---")
            appendLine("Mode: ${_patchingMode.value}")
            appendLine("Local Mode Enabled: ${_useLocalPatching.value}")
            appendLine("Active State: ${_patchingState.value}")
            appendLine("Selected Features: ${_localPatchFeatures.value.filter { it.isEnabled }.joinToString { it.name }}")
            appendLine()
            appendLine("--- Recent Logs ---")
            _logs.value.takeLast(60).forEach { entry ->
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                appendLine("$time [${entry.tag.displayName}] ${entry.message}")
            }
            appendLine("======================================")
        }

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("BruhPatcher_Diagnostics", report)
            clipboard.setPrimaryClip(clip)
            addLog(LogTag.SUCCESS, "Diagnostic report copied to clipboard!")
        } catch (e: Exception) {
            addLog(LogTag.ERROR, "Failed to copy to clipboard: ${e.message}")
        }
        return report
    }

    /**
     * Queries Keybox Hub (https://keybox.hzzmonet.io.vn/api/status)
     */
    fun fetchKeyboxStatus() {
        viewModelScope.launch {
            val result = KeyboxManager.getStatus()
            result.fold(
                onSuccess = { status ->
                    _keyboxStatus.value = status
                    addLog(LogTag.INFO, "Keybox Hub: ${status.status} (${status.strongCount} Strong / ${status.deviceCount} Device)")
                },
                onFailure = { _ ->
                    // Offline or server unreachable
                }
            )
        }
    }

    /**
     * Downloads and applies the latest verified Strong Keybox from https://keybox.hzzmonet.io.vn/api/download
     */
    fun syncLatestKeybox() {
        viewModelScope.launch {
            _isSyncingKeybox.value = true
            addLog(LogTag.INFO, "Connecting to Keybox Hub (https://keybox.hzzmonet.io.vn)...")
            try {
                val context = getApplication<Application>()
                val result = KeyboxManager.fetchAndApplyLatestKeybox(context)
                result.fold(
                    onSuccess = { msg ->
                        addLog(LogTag.SUCCESS, msg)
                        fetchKeyboxStatus()
                    },
                    onFailure = { error ->
                        addLog(LogTag.ERROR, "Keybox sync failed: ${error.message}")
                    }
                )
            } finally {
                _isSyncingKeybox.value = false
            }
        }
    }

    fun refreshDeviceInfo() {
        checkRootAndScan()
        fetchKeyboxStatus()
    }

    /**
     * Install module directly using root shell commands (fallback for permission issues)
     */
    fun installModuleDirectly(modulePath: String) {
        viewModelScope.launch {
            try {
                _patchingState.value = PatchingState.Installing
                addLog(LogTag.INSTALL, "Installing module directly via root...")

                val result = RootManager.installMagiskModule(modulePath)

                result.fold(
                    onSuccess = {
                        addLog(LogTag.SUCCESS, "Module installed successfully!")
                        _patchingState.value = PatchingState.Success
                    },
                    onFailure = { error ->
                        addLog(LogTag.ERROR, "Installation failed: ${error.message}")
                        _patchingState.value = PatchingState.Error(error.message ?: "Installation failed", recoverable = true)
                    }
                )
            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Error: ${e.message}")
                _patchingState.value = PatchingState.Error(e.message ?: "Unknown error", recoverable = true)
            }
        }
    }

    /**
     * Toggle between local and cloud patching
     */
    fun setUseLocalPatching(useLocal: Boolean) {
        _useLocalPatching.value = useLocal
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setLiquidGlassNavbar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiquidGlassNavbar(enabled)
        }
    }

    /**
     * Applies the complete recommended AutoPatcher preset:
     * - CorePatch (Signature Verification)
     * - Kaorios Toolbox v2.0.6.0
     * - Android 17 Build Reflection Unfinalize
     * - HyperOS CN Notification Fix (if HyperOS/MIUI)
     * - Disable Secure Flag
     * - Google Photos Unlimited
     */
    fun applyAutoPatcherPreset() {
        val info = _deviceInfo.value
        _localPatchFeatures.value = _localPatchFeatures.value.map { feature ->
            val shouldEnable = when (feature.id) {
                "disable_signature_verification" -> true
                "kaorios_toolbox" -> true
                "disable_flag_secure" -> true
                "google_photos_unlimited" -> true
                "android17_build_unfinalize" -> true
                "cn_notification_fix" -> info.isHyperOS || info.hasMiuiServicesJar
                else -> feature.isEnabled
            }
            feature.copy(isEnabled = shouldEnable)
        }

        _features.value = _features.value.map { feature ->
            val shouldEnable = when (feature.id) {
                Feature.DISABLE_SIGNATURE_VERIFICATION.id -> true
                Feature.KAORIOS_TOOLBOX.id -> true
                Feature.DISABLE_SECURE_FLAG.id -> true
                Feature.GOOGLE_PHOTOS_UNLIMITED.id -> true
                Feature.ANDROID17_BUILD_UNFINALIZE.id -> true
                Feature.CN_NOTIFICATION_FIX.id -> info.isHyperOS || info.hasMiuiServicesJar
                else -> feature.isEnabled
            }
            feature.copy(isEnabled = shouldEnable)
        }
        addLog(LogTag.INFO, "Applied AutoPatcher recommended preset for ${info.osBadgeText}")
    }

    /**
     * Automated 1-Tap AutoPatcher for Android 17 / HyperOS 4 (Poco F6 / Redmi Turbo 3):
     * 1. Checks root access
     * 2. Sets mode to AUTO_EXTRACT
     * 3. Enables all 6 core patches via preset
     * 4. Triggers local on-device patching pipeline
     */
    fun startAutoPatcher() {
        viewModelScope.launch {
            if (!_isRootAvailable.value) {
                _patchingState.value = PatchingState.Error("Root access is required for AutoPatcher", recoverable = true)
                return@launch
            }

            _useLocalPatching.value = true
            _patchingMode.value = PatchingMode.AUTO_EXTRACT
            applyAutoPatcherPreset()

            val info = _deviceInfo.value
            val targetNotice = if (info.isPocoF6OrTurbo3) {
                "Targeting Xiaomi Redmi Turbo 3 / Poco F6 (peridot) [Snapdragon 8s Gen 3]"
            } else {
                "Targeting ${info.deviceName} (${info.deviceCodename})"
            }
            addLog(LogTag.INFO, "⚡ 1-Tap AutoPatcher launched! $targetNotice")
            startLocalPatching()
        }
    }

    /**
     * Start local on-device patching using DynamicInstaller.
     * 
     * Execution flow:
     * 1. Extract/copy framework files
     * 2. Install DynamicInstaller if needed
     * 3. Deploy feature scripts to safe runtime directory
     * 4. Create isolated job directory with proper structure
     * 5. Generate and execute run.sh (ONLY entry point for patching)
     * 6. Build Magisk module from patched output
     * 7. Cleanup
     */
    fun startLocalPatching() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val info = _deviceInfo.value
            var jobDir: File? = null

            try {
                // Get enabled local features and determine required JARs
                val enabledFeatures = _localPatchFeatures.value.filter { it.isEnabled }
                if (enabledFeatures.isEmpty()) {
                    _patchingState.value = PatchingState.Error("No features selected", recoverable = true)
                    return@launch
                }

                // Collect all unique required JARs from enabled features
                val requiredJars = enabledFeatures.flatMap { it.requiredJars }.distinct()
                addLog(LogTag.INFO, "Starting local patching workflow...")
                addLog(LogTag.INFO, "Required JARs: ${requiredJars.joinToString(", ")}")

                // Step 1: Extract or copy only the required framework files
                val extractedFiles = when (_patchingMode.value) {
                    PatchingMode.AUTO_EXTRACT -> {
                        requiredJars.forEach { jar ->
                            val available = when (jar) {
                                "framework.jar" -> info.hasFrameworkJar
                                "services.jar" -> info.hasServicesJar
                                "miui-services.jar" -> info.hasMiuiServicesJar
                                else -> true
                            }
                            if (!available) {
                                _patchingState.value = PatchingState.Error("$jar not found on device", recoverable = false)
                                return@launch
                            }
                        }
                        extractFrameworkFiles(requiredJars)
                    }
                    PatchingMode.MANUAL_SELECT -> {
                        val selected = _selectedFiles.value
                        requiredJars.forEach { jar ->
                            if (!selected.containsKey(jar)) {
                                _patchingState.value = PatchingState.Error("Please select $jar", recoverable = true)
                                return@launch
                            }
                        }
                        copySelectedFiles()
                    }
                }

                if (extractedFiles.isEmpty()) {
                    _patchingState.value = PatchingState.Error("No files were extracted", recoverable = false)
                    return@launch
                }

                // Step 2: Install DynamicInstaller if needed
                _patchingState.value = PatchingState.InstallingDI()
                addLog(LogTag.DI, "Setting up DynamicInstaller...")

                val diInstalled = withContext(Dispatchers.IO) {
                    DiInstaller.installIfNeeded(context) { msg ->
                        viewModelScope.launch { addLog(LogTag.DI, msg) }
                    }
                }

                if (!diInstalled) {
                    _patchingState.value = PatchingState.Error("Failed to install DynamicInstaller", recoverable = true)
                    return@launch
                }

                addLog(LogTag.DI, "DynamicInstaller ready")

                // Step 3: Deploy feature scripts to safe runtime directory
                addLog(LogTag.PATCH, "Deploying feature scripts...")
                val enabledFeatureIds = enabledFeatures.map { it.id }

                val featureScripts = withContext(Dispatchers.IO) {
                    FeatureManager.getEnabledScripts(context, enabledFeatureIds)
                }

                if (featureScripts.isEmpty()) {
                    _patchingState.value = PatchingState.Error("No feature scripts found for selected features", recoverable = true)
                    return@launch
                }

                addLog(LogTag.PATCH, "Found ${featureScripts.size} patch(es): ${featureScripts.joinToString(", ") { it.name }}")

                // Step 4: Create isolated job directory
                val jobId = System.currentTimeMillis().toString()
                jobDir = File("/data/local/tmp/bruhpatcher/jobs/$jobId")
                
                _patchingState.value = PatchingState.Patching("Setting up job", 0, featureScripts.size)
                addLog(LogTag.PATCH, "Creating job directory: ${jobDir.name}")
                
                withContext(Dispatchers.IO) {
                    Shell.cmd(
                        "mkdir -p ${jobDir!!.absolutePath}/input",
                        "mkdir -p ${jobDir!!.absolutePath}/work",
                        "mkdir -p ${jobDir!!.absolutePath}/output",
                        "chmod -R 777 ${jobDir!!.absolutePath}"
                    ).exec()
                }

                // Copy ALL extracted files to job input directory
                val jobInputFiles = mutableMapOf<String, String>()

                extractedFiles.forEach { (name, sourceFile) ->
                    val jobPath = "${jobDir!!.absolutePath}/input/$name"
                    withContext(Dispatchers.IO) {
                        Shell.cmd("cp ${sourceFile.absolutePath} $jobPath").exec()
                    }
                    jobInputFiles[name] = jobPath
                    addLog(LogTag.PATCH, "Prepared input: $name")
                }

                // Step 5: Generate and execute run.sh
                _patchingState.value = PatchingState.Patching("Generating job script", 1, featureScripts.size + 2)
                addLog(LogTag.PATCH, "Generating run.sh...")

                val runScript = withContext(Dispatchers.IO) {
                    DiExecutor.generateRunScript(
                        context = context,
                        jobDir = jobDir!!,
                        inputFiles = jobInputFiles, // Pass the map of all files
                        features = featureScripts,
                        apiLevel = info.apiLevel,
                        deviceCodename = info.deviceCodename
                    )
                }
                addLog(LogTag.PATCH, "run.sh generated with ${featureScripts.size} feature(s)")

                // Execute the job
                _patchingState.value = PatchingState.Patching("Executing patches", 2, featureScripts.size + 2)
                addLog(LogTag.PATCH, "Executing job...")
                
                val exitCode = withContext(Dispatchers.IO) {
                    DiExecutor.runJob(jobDir!!) { line ->
                        viewModelScope.launch { addLog(LogTag.PATCH, line) }
                    }
                }

                if (exitCode != 0) {
                    addLog(LogTag.ERROR, "Job failed with exit code $exitCode")
                    _patchingState.value = PatchingState.Error("Patching failed with fatal error (exit code $exitCode)", recoverable = true)
                    saveLogsInternal()
                    return@launch
                }

                addLog(LogTag.PATCH, "All patches applied successfully")

                // Step 6: Build Magisk module from output
                _patchingState.value = PatchingState.BuildingModule
                addLog(LogTag.MODULE, "Collecting patched files...")

                // Collect patched JARs from output directory (need root to access)
                val patchedJars = mutableMapOf<String, File>()

                extractedFiles.keys.forEach { fileName ->
                    val outputPath = "${jobDir!!.absolutePath}/output/$fileName"

                    // Check if file exists in output (strictly modified by run.sh)
                    val checkResult = Shell.cmd("test -f $outputPath && echo YES").exec()
                    val hasOutput = checkResult.out.any { it.contains("YES") }

                    if (hasOutput) {
                        val localFile = File(context.cacheDir, "patched_$fileName")
                        // Copy from output to app cache
                        Shell.cmd(
                            "cp $outputPath ${localFile.absolutePath}",
                            "chmod 644 ${localFile.absolutePath}"
                        ).exec()

                        if (localFile.exists() && localFile.length() > 0) {
                            patchedJars[fileName] = localFile
                            addLog(LogTag.MODULE, "Verified patched $fileName (${localFile.length() / 1024} KB)")
                        } else {
                            addLog(LogTag.ERROR, "Failed to copy patched $fileName from output")
                        }
                    } else {
                        addLog(LogTag.WARN, "$fileName was not modified by selected patches (omitted from module)")
                    }
                }

                if (patchedJars.isEmpty()) {
                    addLog(LogTag.ERROR, "Zero patched JARs generated. Aborting module creation to prevent bootloop.")
                    _patchingState.value = PatchingState.Error("No patched JAR files generated. Aborted to prevent bootloop.", recoverable = true)
                    saveLogsInternal()
                    return@launch
                }

                addLog(LogTag.MODULE, "Building NoMount VFS compatible module with ${patchedJars.size} patched JAR(s)...")

                // Pass the job output directory where module_extras.conf is located
                val jobOutputDirFile = File(jobDir, "output")
                
                val moduleResult = ModuleGenerator.generateModule(
                    context = context,
                    patchedJars = patchedJars,
                    deviceCodename = info.deviceCodename,
                    androidVersion = info.androidVersion,
                    jobOutputDir = jobOutputDirFile,
                    patchLog = formatLogs()
                ) { msg ->
                    viewModelScope.launch { addLog(LogTag.MODULE, msg) }
                }

                moduleResult.fold(
                    onSuccess = { moduleFile ->
                        addLog(LogTag.MODULE, "Module created: ${moduleFile.name}")

                        // Move to Downloads
                        val downloadResult = ModuleGenerator.moveToDownloads(moduleFile)
                        downloadResult.fold(
                            onSuccess = { finalFile ->
                                addLog(LogTag.SUCCESS, "Module saved to: ${finalFile.absolutePath}")
                                _downloadedModulePath.value = finalFile.absolutePath
                                _patchingState.value = PatchingState.ModuleReady(finalFile.absolutePath)
                            },
                            onFailure = { error ->
                                addLog(LogTag.ERROR, "Failed to save module: ${error.message}")
                                // Still use the cache file as fallback
                                _downloadedModulePath.value = moduleFile.absolutePath
                                _patchingState.value = PatchingState.ModuleReady(moduleFile.absolutePath)
                            }
                        )

                        // Automatically save patch logs to Downloads upon successful module creation
                        saveLogsInternal()

                        // Cleanup job directory
                        Shell.cmd("rm -rf ${jobDir!!.absolutePath}").exec()
                    },
                    onFailure = { error ->
                        addLog(LogTag.ERROR, "Module generation failed: ${error.message}")
                        _patchingState.value = PatchingState.Error("Module generation failed: ${error.message}", recoverable = true)
                        saveLogsInternal()
                        // Cleanup job directory on failure too
                        Shell.cmd("rm -rf ${jobDir!!.absolutePath}").exec()
                    }
                )

            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Error: ${e.message}")
                _patchingState.value = PatchingState.Error(e.message ?: "Unknown error", recoverable = true)
                saveLogsInternal()
            } finally {
                // Step 7: Cleanup
                withContext(Dispatchers.IO) {
                    // Cleanup job directory
                    jobDir?.let { Shell.cmd("rm -rf ${it.absolutePath}").exec() }
                    // Cleanup feature runtime if needed
                    FeatureManager.cleanup()
                    // Cleanup app files dir
                    RootManager.cleanup(context.filesDir)
                }
            }
        }
    }

    /**
     * Install the generated module using root manager
     */
    fun installGeneratedModule(modulePath: String) {
        viewModelScope.launch {
            try {
                _patchingState.value = PatchingState.Installing
                addLog(LogTag.INSTALL, "Installing module...")

                val result = RootManager.installMagiskModule(modulePath)

                result.fold(
                    onSuccess = {
                        // Re-arm NoMount bootloop protector if metamodule is present
                        if (RootManager.isNoMountInstalled()) {
                            RootManager.resetNoMountBootloopGuard()
                            _isNoMountGuardTripped.value = false
                            addLog(LogTag.SUCCESS, "Re-armed NoMount VFS metamodule")
                        }
                        addLog(LogTag.SUCCESS, "Module installed successfully!")
                        addLog(LogTag.SUCCESS, "Please reboot your device to apply changes")
                        _patchingState.value = PatchingState.Success
                    },
                    onFailure = { error ->
                        addLog(LogTag.ERROR, "Installation failed: ${error.message}")
                        _patchingState.value = PatchingState.Error(error.message ?: "Installation failed", recoverable = true)
                    }
                )
            } catch (e: Exception) {
                addLog(LogTag.ERROR, "Error: ${e.message}")
                _patchingState.value = PatchingState.Error(e.message ?: "Unknown error", recoverable = true)
            }
        }
    }
}


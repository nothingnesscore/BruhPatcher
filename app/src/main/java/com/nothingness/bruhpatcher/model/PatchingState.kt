package com.nothingness.bruhpatcher.model

/**
 * Represents the current state of the patching process
 */
sealed class PatchingState {
    data object Idle : PatchingState()
    data object CheckingRoot : PatchingState()

    data class Scanning(val message: String) : PatchingState()

    data class Extracting(
        val currentFile: String,
        val filesExtracted: Int,
        val totalFiles: Int
    ) : PatchingState()

    data class Uploading(
        val currentFile: String,
        val progress: Int,
        val filesUploaded: Int,
        val totalFiles: Int
    ) : PatchingState()

    data object TriggeringWorkflow : PatchingState()

    data class WaitingForBuild(
        val elapsedSeconds: Int = 0,
        val runId: String? = null
    ) : PatchingState()

    data class Downloading(val progress: Int) : PatchingState()

    data object Installing : PatchingState()

    data class ReadyToInstall(val filePath: String) : PatchingState()

    data object Success : PatchingState()

    data class Error(val message: String, val recoverable: Boolean = true) : PatchingState()

    // Local patching states
    data class InstallingDI(val progress: String = "Setting up DynamicInstaller...") : PatchingState()

    data class Patching(
        val featureName: String,
        val current: Int,
        val total: Int
    ) : PatchingState()

    data object BuildingModule : PatchingState()

    data class ModuleReady(val filePath: String) : PatchingState()
}

/**
 * Represents a log entry for the progress terminal view
 */
data class LogEntry(
    val tag: LogTag,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogTag(val displayName: String) {
    INFO("INFO"),
    EXTRACT("EXTRACT"),
    UPLOAD("UPLOAD"),
    REMOTE("REMOTE"),
    WAITING("WAITING"),
    DOWNLOAD("DOWNLOAD"),
    INSTALL("INSTALL"),
    SUCCESS("SUCCESS"),
    ERROR("ERROR"),
    // Local patching tags
    DI("DI"),
    PATCH("PATCH"),
    MODULE("MODULE")
}

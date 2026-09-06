package com.nothingness.bruhpatcher.model

/**
 * Represents the patching mode selection
 */
enum class PatchingMode {
    /**
     * Automatically extract files from /system using root
     */
    AUTO_EXTRACT,

    /**
     * Manually select files from device storage
     */
    MANUAL_SELECT
}

/**
 * Represents a manually selected file
 */
data class SelectedFile(
    val name: String,
    val uri: android.net.Uri,
    val size: Long = 0
)

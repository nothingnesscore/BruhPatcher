package com.nothingness.bruhpatcher.core

import android.content.Context
import com.nothingness.bruhpatcher.data.NetworkModule
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

/**
 * Data class representing system status from https://keybox.hzzmonet.io.vn/api/status
 */
@Serializable
data class KeyboxStatus(
    val status: String = "unknown",
    @SerialName("total_valid") val totalValid: Int = 0,
    @SerialName("strong_count") val strongCount: Int = 0,
    @SerialName("device_count") val deviceCount: Int = 0,
    @SerialName("banned_count") val bannedCount: Int = 0,
    @SerialName("total_keys") val totalKeys: Int = 0,
    @SerialName("last_updated") val lastUpdated: String = ""
)

/**
 * Manages Keybox hardware attestation synchronization with Keybox Hub (https://keybox.hzzmonet.io.vn)
 * Supports:
 * - Querying live status (/api/status)
 * - Fetching verified Strong Keybox XML (/api/download)
 * - Local persistence and live Magisk / KernelSU / APatch (/data/adb/kaorios/Keybox.xml) deployment
 */
object KeyboxManager {

    private const val BASE_URL = "https://keybox.hzzmonet.io.vn"
    private const val STATUS_URL = "$BASE_URL/api/status"
    private const val DOWNLOAD_URL = "$BASE_URL/api/download"

    private const val LOCAL_KAORIOS_DIR = "kaorios"
    private const val KEYBOX_FILE_NAME = "Keybox.xml"

    private const val RUNTIME_KAORIOS_DIR = "/data/local/tmp/bruhpatcher/kaorios"
    private const val SYSTEM_KAORIOS_DIR = "/data/adb/kaorios"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val httpClient = NetworkModule.downloadClient

    /**
     * Checks status from https://keybox.hzzmonet.io.vn/api/status
     */
    suspend fun getStatus(): Result<KeyboxStatus> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(STATUS_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "BruhPatcher-KeyboxClient")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty status response"))
            val status = json.decodeFromString<KeyboxStatus>(body)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the latest Strong keybox from https://keybox.hzzmonet.io.vn/api/download
     * Validates and saves to app internal storage, runtime directory, and /data/adb/kaorios/ if rooted.
     */
    suspend fun fetchAndApplyLatestKeybox(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(DOWNLOAD_URL)
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "BruhPatcher-KeyboxClient")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val xmlContent = response.body?.string() ?: return@withContext Result.failure(Exception("Empty Keybox content"))

            // Validate XML structure
            if (!xmlContent.contains("<AndroidAttestation") || !xmlContent.contains("<Keybox")) {
                return@withContext Result.failure(Exception("Invalid Keybox XML received from server"))
            }

            // Save to app internal directory
            val appKaoriosDir = File(context.filesDir, LOCAL_KAORIOS_DIR)
            if (!appKaoriosDir.exists()) appKaoriosDir.mkdirs()

            val keyboxFile = File(appKaoriosDir, KEYBOX_FILE_NAME)
            keyboxFile.writeText(xmlContent)

            // Deploy to runtime safe directory
            Shell.cmd(
                "mkdir -p $RUNTIME_KAORIOS_DIR",
                "cp ${keyboxFile.absolutePath} $RUNTIME_KAORIOS_DIR/$KEYBOX_FILE_NAME",
                "chmod 644 $RUNTIME_KAORIOS_DIR/$KEYBOX_FILE_NAME"
            ).exec()

            // If root is active, also sync directly to /data/adb/kaorios/Keybox.xml
            if (RootManager.isRootAvailable()) {
                Shell.cmd(
                    "mkdir -p $SYSTEM_KAORIOS_DIR",
                    "cp ${keyboxFile.absolutePath} $SYSTEM_KAORIOS_DIR/$KEYBOX_FILE_NAME",
                    "chmod 644 $SYSTEM_KAORIOS_DIR/$KEYBOX_FILE_NAME"
                ).exec()
            }

            Result.success("Keybox updated successfully from Keybox Hub (Strong Integrity active)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the current local Keybox XML file (either updated or bundled)
     */
    fun getLocalKeybox(context: Context): File? {
        val updated = File(context.filesDir, "$LOCAL_KAORIOS_DIR/$KEYBOX_FILE_NAME")
        if (updated.exists() && updated.length() > 500) {
            return updated
        }
        return null
    }
}

package com.nothingness.bruhpatcher.data.repository

import android.content.Context
import com.nothingness.bruhpatcher.BuildConfig
import com.nothingness.bruhpatcher.data.NetworkModule
import com.nothingness.bruhpatcher.data.api.GitHubRelease
import com.nothingness.bruhpatcher.data.api.WorkflowInputs
import com.nothingness.bruhpatcher.data.api.WorkflowRequest
import com.nothingness.bruhpatcher.data.api.WorkflowResponse
import com.nothingness.bruhpatcher.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.nothingness.bruhpatcher.core.RootManager

/**
 * Repository for workflow operations and release management
 */
class WorkflowRepository {

    private val workflowApi = NetworkModule.workflowApi
    private val gitHubApi = NetworkModule.gitHubApi
    private val downloadClient = NetworkModule.downloadClient

    /**
     * Trigger the patching workflow
     */
    suspend fun triggerWorkflow(
        deviceInfo: DeviceInfo,
        frameworkUrl: String,
        servicesUrl: String,
        miuiServicesUrl: String,
        features: String
    ): Result<WorkflowResponse> = withContext(Dispatchers.IO) {
        try {
            val request = WorkflowRequest(
                version = deviceInfo.workflowVersion,
                inputs = WorkflowInputs(
                    apiLevel = deviceInfo.apiLevel.toString(),
                    deviceName = deviceInfo.deviceName,
                    deviceCodename = deviceInfo.deviceCodename,
                    versionName = deviceInfo.versionName,
                    frameworkUrl = frameworkUrl,
                    servicesUrl = servicesUrl,
                    miuiServicesUrl = miuiServicesUrl,
                    features = features
                )
            )

            val response = workflowApi.triggerWorkflow(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: body.message ?: "Workflow trigger failed"))
                }
            } else {
                Result.failure(
                    Exception("Workflow trigger failed: ${response.code()} - ${response.errorBody()?.string()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find existing releases that match the device using ONLY incremental build prop
     */
    suspend fun findMatchingReleases(
        deviceCodename: String,
        versionSafe: String
    ): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val response = gitHubApi.getReleases(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO
            )

            if (response.isSuccessful) {
                val releases = response.body() ?: emptyList()
                
                // Release tag format: {codename}_{version}_{timestamp}
                // Match releases where tag starts with codename_ AND contains version
                val matchingReleases = releases.filter { release ->
                    val hasModule = release.findModuleZip() != null
                    val tag = release.tagName
                    
                    // Tag must start with codename (codename_version_timestamp format)
                    val startsWithCodename = tag.startsWith("${deviceCodename}_", ignoreCase = true)
                    // Tag must also contain the version (after codename_)
                    val containsVersion = tag.contains(versionSafe.take(20), ignoreCase = true)
                    
                    hasModule && startsWithCodename && containsVersion
                }
                
                Result.success(matchingReleases)
            } else {
                Result.failure(Exception("Failed to fetch releases: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll for a matching release after workflow trigger
     */
    suspend fun pollForRelease(
        deviceCodename: String,
        versionSafe: String,
        pollIntervalMs: Long = 30_000,
        maxAttempts: Int = 60,
        onPoll: ((Int) -> Unit)? = null
    ): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        
        var lastKnownReleaseId: Long? = null
        try {
            val initialResponse = gitHubApi.getReleases(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO
            )
            if (initialResponse.isSuccessful) {
                lastKnownReleaseId = initialResponse.body()?.firstOrNull()?.id
            }
        } catch (e: Exception) {
            // Continue anyway
        }
        // Match using codename_version format (workflow creates: codename_version_timestamp)
        val tagPrefix = "${deviceCodename}_"

        repeat(maxAttempts) { attempt ->
            onPoll?.invoke(attempt + 1)

            try {
                val response = gitHubApi.getReleases(
                    owner = BuildConfig.GITHUB_OWNER,
                    repo = BuildConfig.GITHUB_REPO
                )

                if (response.isSuccessful) {
                    val releases = response.body() ?: emptyList()
                    
                    val newReleases = releases.filter { release ->
                        val isNew = lastKnownReleaseId == null || release.id != lastKnownReleaseId
                        val hasModule = release.findModuleZip() != null
                        isNew && hasModule
                    }
                    
                    if (newReleases.isNotEmpty()) {
                        // Match: tag starts with codename_ AND contains version
                        val exactMatch = newReleases.firstOrNull { release ->
                            release.tagName.startsWith(tagPrefix, ignoreCase = true) &&
                            release.tagName.contains(versionSafe.take(20), ignoreCase = true)
                        }
                        if (exactMatch != null) {
                            return@withContext Result.success(exactMatch)
                        }
                        
                        // Fallback: just matching codename prefix after 3 attempts
                        val mostRecent = newReleases.firstOrNull { release ->
                            release.tagName.startsWith(tagPrefix, ignoreCase = true)
                        }
                        if (mostRecent != null && attempt >= 3) {
                            return@withContext Result.success(mostRecent)
                        }
                    }
                    
                    // Check all releases for matching tag format
                    val matchingRelease = releases.firstOrNull { release ->
                        release.tagName.startsWith(tagPrefix, ignoreCase = true) &&
                        release.tagName.contains(versionSafe.take(20), ignoreCase = true) &&
                        release.findModuleZip() != null
                    }

                    if (matchingRelease != null) {
                        return@withContext Result.success(matchingRelease)
                    }
                }
            } catch (e: Exception) {
                // Continue polling
            }

            if (attempt < maxAttempts - 1) {
                delay(pollIntervalMs)
            }
        }

        Result.failure(Exception("Timeout waiting for release"))
    }

    /**
     * Download a file to app's cache directory (no storage permission needed)
     */
    suspend fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = downloadClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Download failed: ${response.code}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            // Try public Downloads directory first
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            downloadDir.mkdirs()
            val destinationFile = File(downloadDir, fileName)
            
            val contentLength = body.contentLength()

            try {
                // Attempt direct write
                downloadToStream(body, destinationFile, contentLength, onProgress)
                Result.success(destinationFile)
            } catch (e: Exception) {
                // Check if it's a permission issue (common on Android 11+)
                val isPermissionIssue = e is java.io.FileNotFoundException && (e.message?.contains("EACCES") == true || e.message?.contains("Permission denied") == true) || e is SecurityException
                
                if (isPermissionIssue && RootManager.isRootAvailable()) {
                    // Fallback: Download to cache then move with root
                    val cacheDir = File(context.cacheDir, "temp_download")
                    cacheDir.mkdirs()
                    val tempFile = File(cacheDir, fileName)
                    
                    // Re-request because body stream is consumed/closed
                    val newResponse = downloadClient.newCall(request.newBuilder().build()).execute()
                    val newBody = newResponse.body ?: throw Exception("Empty body on retry")
                    
                    downloadToStream(newBody, tempFile, newBody.contentLength(), onProgress)
                    
                    // Move using root
                    val moveResult = RootManager.moveToDownloads(tempFile, fileName)
                    if (moveResult.isSuccess) {
                        Result.success(moveResult.getOrThrow())
                    } else {
                        Result.failure(Exception("Download successful but failed to move file to Downloads: ${moveResult.exceptionOrNull()?.message}"))
                    }
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadToStream(
        body: okhttp3.ResponseBody,
        file: File,
        contentLength: Long,
        onProgress: ((Int) -> Unit)?
    ) {
        var bytesWritten = 0L
        var lastProgress = -1

        body.byteStream().use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8192)
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    bytesWritten += read

                    if (contentLength > 0) {
                        val progress = ((bytesWritten * 100) / contentLength).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress?.invoke(progress)
                        }
                    }
                }
            }
        }
    }


    /**
     * Overload for backward compatibility
     */
    suspend fun downloadFile(
        url: String,
        fileName: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        // This version requires a context to be passed separately
        // For now, create a temp file in system temp dir
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = downloadClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Download failed: ${response.code}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            val destinationFile = File.createTempFile("module_", ".zip")

            val contentLength = body.contentLength()
            var bytesWritten = 0L
            var lastProgress = -1

            body.byteStream().use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesWritten += read

                        if (contentLength > 0) {
                            val progress = ((bytesWritten * 100) / contentLength).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress?.invoke(progress)
                            }
                        }
                    }
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent releases
     */
    suspend fun getRecentReleases(limit: Int = 10): Result<List<GitHubRelease>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = gitHubApi.getReleases(
                    owner = BuildConfig.GITHUB_OWNER,
                    repo = BuildConfig.GITHUB_REPO
                )

                if (response.isSuccessful) {
                    Result.success(response.body()?.take(limit) ?: emptyList())
                } else {
                    Result.failure(
                        Exception("Failed to fetch releases: ${response.code()}")
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

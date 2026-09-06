package com.nothingness.bruhpatcher.core

import android.content.Context
import com.nothingness.bruhpatcher.data.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.io.File

/**
 * Updates feature scripts from the FrameworkForgeFeatures GitHub repository
 */
object FeatureUpdater {

    private const val REPO_OWNER = "nothingnesscore"
    private const val REPO_NAME = "BruhPatcher"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/app/src/main/assets/features"
    
    private val httpClient = NetworkModule.downloadClient

    /**
     * Data class for GitHub file info
     */
    data class GitHubFile(
        val name: String,
        val downloadUrl: String
    )

    /**
     * Fetches and updates all .sh scripts from the repository
     * Scripts are saved to the features_updated directory, overriding built-in scripts
     * 
     * @return Result with count of updated scripts or error
     */
    suspend fun updateScripts(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Fetch file list from GitHub API
            val files = fetchFileList()
            if (files.isEmpty()) {
                return@withContext Result.failure(Exception("No scripts found in repository"))
            }

            // Prepare updated features directory (overrides built-in)
            val updatedDir = File(context.filesDir, "features_updated")
            if (!updatedDir.exists()) updatedDir.mkdirs()

            var updatedCount = 0

            // Download each .sh file
            for (file in files) {
                if (!file.name.endsWith(".sh")) continue
                
                val result = downloadScript(file, updatedDir)
                if (result.isSuccess) {
                    updatedCount++
                }
            }

            if (updatedCount == 0) {
                Result.failure(Exception("Failed to download any scripts"))
            } else {
                Result.success(updatedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the list of files from the repository
     */
    private fun fetchFileList(): List<GitHubFile> {
        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch file list: ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response")
        val jsonArray = JSONArray(body)
        
        val files = mutableListOf<GitHubFile>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val name = item.getString("name")
            val downloadUrl = item.optString("download_url", "")
            
            if (name.endsWith(".sh") && downloadUrl.isNotEmpty()) {
                files.add(GitHubFile(name, downloadUrl))
            }
        }
        
        return files
    }

    /**
     * Downloads a single script file
     */
    private fun downloadScript(file: GitHubFile, destDir: File): Result<File> {
        return try {
            val request = Request.Builder()
                .url(file.downloadUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Download failed: ${response.code}"))
            }

            val destFile = File(destDir, file.name)
            response.body?.byteStream()?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Make executable
            destFile.setExecutable(true)
            
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

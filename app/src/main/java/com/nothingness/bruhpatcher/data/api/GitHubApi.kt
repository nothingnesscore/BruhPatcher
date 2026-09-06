package com.nothingness.bruhpatcher.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub API for polling releases
 * Base URL: https://api.github.com/
 */
interface GitHubApi {

    /**
     * Get releases for a repository
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubRelease>>

    /**
     * Get a specific release by tag
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String
    ): Response<GitHubRelease>
}

@Serializable
data class GitHubRelease(
    @SerialName("id") val id: Long,
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
    @SerialName("html_url") val htmlUrl: String? = null
) {
    /**
     * Find the module zip asset
     */
    fun findModuleZip(): GitHubAsset? {
        return assets.firstOrNull { it.name.endsWith(".zip") }
    }
}

@Serializable
data class GitHubAsset(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size") val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

package com.nothingness.bruhpatcher.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Pixeldrain API for file uploads
 * Base URL: https://pixeldrain.com/api/
 */
interface PixeldrainApi {

    /**
     * Upload a file to Pixeldrain
     * Uses Basic Auth (username: "", password: API_KEY)
     */
    @Multipart
    @POST("file")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<PixeldrainResponse>
}

@Serializable
data class PixeldrainResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("id") val id: String? = null,
    @SerialName("message") val message: String? = null
) {
    /**
     * Gets the full download URL
     */
    val downloadUrl: String
        get() = "https://pixeldrain.com/u/$id"

    /**
     * Gets the direct API download URL for automated downloads
     */
    val apiDownloadUrl: String
        get() = "https://pixeldrain.com/api/file/$id"
}

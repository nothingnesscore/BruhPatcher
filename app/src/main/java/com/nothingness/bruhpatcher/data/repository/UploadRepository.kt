package com.nothingness.bruhpatcher.data.repository

import com.nothingness.bruhpatcher.data.NetworkModule
import com.nothingness.bruhpatcher.data.api.PixeldrainApi
import com.nothingness.bruhpatcher.data.api.PixeldrainResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.buffer
import java.io.File

/**
 * Repository for handling file uploads to Pixeldrain
 */
class UploadRepository(
    private val apiKey: String
) {
    private val pixeldrainApi: PixeldrainApi by lazy {
        NetworkModule.createPixeldrainApi(apiKey)
    }

    /**
     * Upload a file to Pixeldrain
     *
     * @param file The file to upload
     * @param onProgress Progress callback (0-100)
     * @return Result with PixeldrainResponse containing the file ID
     */
    suspend fun uploadFile(
        file: File,
        onProgress: ((Int) -> Unit)? = null
    ): Result<PixeldrainResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = file.asRequestBody("application/java-archive".toMediaTypeOrNull())
            
            // Create a progress-tracking request body
            val progressRequestBody = ProgressRequestBody(requestBody, file.length()) { progress ->
                onProgress?.invoke(progress)
            }

            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                progressRequestBody
            )

            val response = pixeldrainApi.uploadFile(multipartBody)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Upload failed: ${response.code()} - ${response.errorBody()?.string()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload multiple files sequentially
     *
     * @param files Map of filename to File
     * @param onFileProgress Callback for each file's progress
     * @return Map of filename to download URL
     */
    suspend fun uploadFiles(
        files: Map<String, File>,
        onFileProgress: ((String, Int) -> Unit)? = null
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        val urls = mutableMapOf<String, String>()

        for ((name, file) in files) {
            val result = uploadFile(file) { progress ->
                onFileProgress?.invoke(name, progress)
            }

            result.fold(
                onSuccess = { response ->
                    response.id?.let { id ->
                        urls[name] = response.downloadUrl
                    } ?: return@withContext Result.failure(
                        Exception("Upload succeeded but no file ID returned for $name")
                    )
                },
                onFailure = { error ->
                    return@withContext Result.failure(
                        Exception("Failed to upload $name: ${error.message}")
                    )
                }
            )
        }

        Result.success(urls)
    }
}

/**
 * RequestBody wrapper that reports upload progress
 */
class ProgressRequestBody(
    private val delegate: okhttp3.RequestBody,
    private val contentLength: Long,
    private val onProgress: (Int) -> Unit
) : okhttp3.RequestBody() {

    override fun contentType() = delegate.contentType()

    override fun contentLength() = contentLength

    override fun writeTo(sink: okio.BufferedSink) {
        val countingSink = CountingSink(sink, contentLength, onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}

private class CountingSink(
    delegate: okio.Sink,
    private val contentLength: Long,
    private val onProgress: (Int) -> Unit
) : okio.ForwardingSink(delegate) {

    private var bytesWritten = 0L
    private var lastReportedProgress = -1

    override fun write(source: okio.Buffer, byteCount: Long) {
        super.write(source, byteCount)
        bytesWritten += byteCount

        val progress = if (contentLength > 0) {
            ((bytesWritten * 100) / contentLength).toInt()
        } else {
            0
        }

        if (progress != lastReportedProgress) {
            lastReportedProgress = progress
            onProgress(progress)
        }
    }
}

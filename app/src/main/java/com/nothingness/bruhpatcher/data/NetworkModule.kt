package com.nothingness.bruhpatcher.data

import com.nothingness.bruhpatcher.BuildConfig
import com.nothingness.bruhpatcher.data.api.GitHubApi
import com.nothingness.bruhpatcher.data.api.PixeldrainApi
import com.nothingness.bruhpatcher.data.api.WorkflowApi
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network module providing configured API clients
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val baseOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * OkHttp client for Pixeldrain with Basic Auth
     */
    fun createPixeldrainClient(apiKey: String): OkHttpClient {
        return baseOkHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", Credentials.basic("", apiKey))
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * Pixeldrain API instance
     */
    fun createPixeldrainApi(apiKey: String): PixeldrainApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.PIXELDRAIN_BASE_URL)
            .client(createPixeldrainClient(apiKey))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PixeldrainApi::class.java)
    }

    /**
     * Workflow API instance (Vercel proxy)
     */
    val workflowApi: WorkflowApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.WORKFLOW_PROXY_BASE_URL)
            .client(baseOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WorkflowApi::class.java)
    }

    /**
     * GitHub API instance
     */
    val gitHubApi: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.GITHUB_API_BASE_URL)
            .client(baseOkHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .build()
                    chain.proceed(request)
                }
                .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApi::class.java)
    }

    /**
     * Plain OkHttp client for file downloads
     */
    val downloadClient: OkHttpClient by lazy {
        baseOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // 5 min for large downloads
            .build()
    }
}

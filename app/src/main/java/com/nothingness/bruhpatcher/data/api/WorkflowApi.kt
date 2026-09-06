package com.nothingness.bruhpatcher.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Workflow API for triggering GitHub Actions via Vercel proxy
 * Base URL: https://framework-patcher-v2.vercel.app/
 */
interface WorkflowApi {

    /**
     * Trigger the patching workflow
     */
    @POST("api/trigger-workflow")
    suspend fun triggerWorkflow(
        @Body request: WorkflowRequest
    ): Response<WorkflowResponse>
}

@Serializable
data class WorkflowRequest(
    @SerialName("version") val version: String, // e.g., "android14", "android15"
    @SerialName("inputs") val inputs: WorkflowInputs
)

@Serializable
data class WorkflowInputs(
    @SerialName("api_level") val apiLevel: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_codename") val deviceCodename: String,
    @SerialName("version_name") val versionName: String,
    @SerialName("framework_url") val frameworkUrl: String,
    @SerialName("services_url") val servicesUrl: String,
    @SerialName("miui_services_url") val miuiServicesUrl: String = "", // Empty if not found
    @SerialName("features") val features: String // Comma-separated feature IDs
)

@Serializable
data class WorkflowResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("run_id") val runId: Long? = null,
    @SerialName("workflow_url") val workflowUrl: String? = null,
    @SerialName("error") val error: String? = null
)

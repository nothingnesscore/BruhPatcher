package com.nothingness.bruhpatcher.core

import android.util.Base64

/**
 * Handles secure storage and retrieval of the API key
 * Using simple Base64 encoding for the API key to avoid hardcoding in plaintext
 */
object ApiKeyManager {
    
    // Base64 encoded API key: "5a8aad44-bc59-4b6a-9605-90f5f155e61c"
    private const val ENCODED_KEY = "NWE4YWFkNDQtYmM1OS00YjZhLTk2MDUtOTBmNWYxNTVlNjFj"
    
    /**
     * Returns the Pixeldrain API key
     */
    fun getPixeldrainApiKey(): String {
        return try {
            String(Base64.decode(ENCODED_KEY, Base64.DEFAULT)).trim()
        } catch (e: Exception) {
            // This should never happen with valid Base64
            ""
        }
    }
    
    /**
     * Returns the Basic Auth header value for Pixeldrain
     * Format: "Basic " + base64(":api_key")
     */
    fun getBasicAuthHeader(): String {
        val credentials = ":${getPixeldrainApiKey()}"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}

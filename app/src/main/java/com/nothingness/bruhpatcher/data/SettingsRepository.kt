package com.nothingness.bruhpatcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for persisting settings
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val PIXELDRAIN_API_KEY = stringPreferencesKey("pixeldrain_api_key")
}

class SettingsRepository(private val context: Context) {

    val pixeldrainApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.PIXELDRAIN_API_KEY] ?: ""
    }

    suspend fun setPixeldrainApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.PIXELDRAIN_API_KEY] = apiKey
        }
    }

    suspend fun hasPixeldrainApiKey(): Boolean {
        var hasKey = false
        context.dataStore.data.collect { preferences ->
            hasKey = !preferences[SettingsKeys.PIXELDRAIN_API_KEY].isNullOrBlank()
        }
        return hasKey
    }
}

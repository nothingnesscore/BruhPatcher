package com.nothingness.bruhpatcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore for persisting settings
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val PIXELDRAIN_API_KEY = stringPreferencesKey("pixeldrain_api_key")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val USE_LIQUID_GLASS_NAVBAR = booleanPreferencesKey("use_liquid_glass_navbar")
    val THEME_ENGINE = stringPreferencesKey("theme_engine")
}

class SettingsRepository(private val context: Context) {

    val pixeldrainApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.PIXELDRAIN_API_KEY] ?: ""
    }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.USE_DYNAMIC_COLOR] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }

    val useLiquidGlassNavbar: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.USE_LIQUID_GLASS_NAVBAR] ?: true
    }

    val themeEngine: Flow<com.nothingness.bruhpatcher.ui.theme.ThemeEngine> = context.dataStore.data.map { preferences ->
        val raw = preferences[SettingsKeys.THEME_ENGINE]
        try {
            if (raw != null) com.nothingness.bruhpatcher.ui.theme.ThemeEngine.valueOf(raw) else com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX
        } catch (_: Exception) {
            com.nothingness.bruhpatcher.ui.theme.ThemeEngine.HYPEROS_MIUIX
        }
    }

    suspend fun setThemeEngine(engine: com.nothingness.bruhpatcher.ui.theme.ThemeEngine) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_ENGINE] = engine.name
        }
    }

    suspend fun setPixeldrainApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.PIXELDRAIN_API_KEY] = apiKey
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.USE_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setLiquidGlassNavbar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.USE_LIQUID_GLASS_NAVBAR] = enabled
        }
    }

    suspend fun hasPixeldrainApiKey(): Boolean {
        val preferences = context.dataStore.data.first()
        return !preferences[SettingsKeys.PIXELDRAIN_API_KEY].isNullOrBlank()
    }
}

package com.example.ototp.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val THEME_PREFERENCES_NAME = "theme_preferences"
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = THEME_PREFERENCES_NAME)

internal class ThemePreferences(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
    }

    val themeMode: Flow<Settings.ThemeMode> = context.themeDataStore.data.map { preferences ->
        val value = preferences[THEME_MODE_KEY] ?: Settings.ThemeMode.SYSTEM.name
        Settings.ThemeMode.valueOf(value)
    }

    val dynamicColorsEnabled: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS_KEY] ?: true
    }

    suspend fun setThemeMode(mode: Settings.ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_KEY] = enabled
        }
    }
}

package com.example.healthguard.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreferences {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val THEME_KEY = booleanPreferencesKey("dark_theme_enabled")

    suspend fun saveTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = isDark
        }
    }

    val themeFlow: (Context) -> Flow<Boolean> = { context ->
        context.dataStore.data
            .map { prefs -> prefs[THEME_KEY] ?: false } // default is Light
    }
}
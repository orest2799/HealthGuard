package com.example.healthguard.ui.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object LanguagePreferences {
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    const val LANG_EN = "en"
    const val LANG_EL = "el"

    suspend fun saveLanguage(dataStore: DataStore<Preferences>, lang: String) {
        dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = lang
        }
    }

    fun getLanguage(dataStore: DataStore<Preferences>): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[LANGUAGE_KEY] ?: LANG_EN
        }
    }
}
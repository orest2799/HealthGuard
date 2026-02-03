package com.example.healthguard.viewmodel

import ThemePreferences
import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Define the dataStore delegate at the top level of the file or in a central location
private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            // 2. Changed 'themeFlow' to 'getTheme' to match your object
            ThemePreferences.getTheme(getApplication<Application>().dataStore).collectLatest { isDark ->
                _isDarkTheme.value = isDark
            }
        }
    }

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            // 3. Pass the dataStore property instead of the context itself
            ThemePreferences.saveTheme(getApplication<Application>().dataStore, enabled)
        }
    }
}
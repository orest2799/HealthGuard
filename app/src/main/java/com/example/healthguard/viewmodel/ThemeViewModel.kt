package com.example.healthguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            ThemePreferences.themeFlow(application).collect {
                _isDarkTheme.value = it
            }
        }
    }

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreferences.saveTheme(getApplication(), enabled)
        }
    }
}
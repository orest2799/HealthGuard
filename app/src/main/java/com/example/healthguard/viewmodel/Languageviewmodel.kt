package com.example.healthguard.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.presentation.pills.dataStore
import com.example.healthguard.ui.theme.LanguagePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val _language = MutableStateFlow(LanguagePreferences.LANG_EN)
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            LanguagePreferences.getLanguage(getApplication<Application>().dataStore)
                .collectLatest { lang ->
                    _language.value = lang
                    applyLocale(getApplication(), lang)
                }
        }
    }

    fun toggleLanguage(activity: Activity) {
        val next = if (_language.value == LanguagePreferences.LANG_EN)
            LanguagePreferences.LANG_EL
        else
            LanguagePreferences.LANG_EN

        // Apply to both Application AND Activity context
        applyLocale(getApplication(), next)
        applyLocale(activity, next)

        // Update StateFlow — triggers key(language) recomposition
        _language.value = next

        // Persist
        viewModelScope.launch {
            LanguagePreferences.saveLanguage(getApplication<Application>().dataStore, next)
        }
    }

    // Call this from MainActivity.onCreate to restore saved language on app start
    fun applyCurrentLocale(activity: Activity) {
        applyLocale(getApplication(), _language.value)
        applyLocale(activity, _language.value)
    }

    private fun applyLocale(context: Context, lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
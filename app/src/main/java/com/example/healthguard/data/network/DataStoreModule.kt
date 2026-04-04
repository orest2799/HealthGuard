package com.example.healthguard.data.network


import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore


val Context.dataStore by preferencesDataStore(name = "settings")
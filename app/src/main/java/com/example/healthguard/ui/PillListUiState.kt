package com.example.healthguard.ui

import com.example.healthguard.data.network.pills.PillReminder

data class PillListUiState(
    val reminders: List<PillReminder> = emptyList(),
    val takenTimeKeysToday: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
package com.example.healthguard.data.network.emergency




data class EmergencyContact(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val priority: Int = 1
)

data class EmergencyUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val shareLocation: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val setupComplete: Boolean = false,
    val navigateBack: Boolean = false
)
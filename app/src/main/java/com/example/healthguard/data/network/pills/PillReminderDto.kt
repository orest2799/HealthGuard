package com.example.healthguard.data.network.pills

data class PillReminderDto(
    val id: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val enabled: Boolean = true,
    val reminderTimes: List<ReminderTimeDto> = emptyList()
)

data class ReminderTimeDto(
    val hour: Int = 0,
    val minute: Int = 0,
    val daysOfWeek: List<Int> = emptyList()
)
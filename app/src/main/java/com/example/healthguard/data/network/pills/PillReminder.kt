package com.example.healthguard.data.network.pills



data class PillReminder(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val reminderTimes: List<ReminderTime>,
    val enabled: Boolean = true
)
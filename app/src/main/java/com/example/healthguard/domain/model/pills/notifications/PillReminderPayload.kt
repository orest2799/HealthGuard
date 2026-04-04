package com.example.healthguard.domain.model.pills.notifications

data class PillReminderPayload(
    val pillId: String,
    val medicineName: String,
    val dosage: String,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: Int
)
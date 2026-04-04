package com.example.healthguard.domain.model.pills.notifications


object PillNotificationConstants {
    const val CHANNEL_ID = "pill_reminder_channel"
    const val CHANNEL_NAME = "Pill Reminders"
    const val CHANNEL_DESCRIPTION = "Notifications for pill reminders"

    const val EXTRA_PILL_ID = "extra_pill_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"

    const val REQUEST_CODE_SEPARATOR = 10_000
}
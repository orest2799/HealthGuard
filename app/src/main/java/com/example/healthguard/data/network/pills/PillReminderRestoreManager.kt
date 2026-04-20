package com.example.healthguard.data.network.pills

import android.content.Context

class PillReminderRestoreManager(
    private val dataSource: PillReminderFirebaseDataSource = PillReminderFirebaseDataSource()
) {
    suspend fun restoreAll(context: Context) {
        val reminders = dataSource.getAllReminders().map { it.toModel() }
        val scheduler = PillAlarmScheduler(context)

        reminders.forEach { reminder ->
            scheduler.cancelAll(reminder)
            if (reminder.enabled) {
                scheduler.scheduleAll(reminder)
            }
        }
    }
}
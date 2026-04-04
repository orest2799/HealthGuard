package com.example.healthguard.domain.model.pills.notifications

import android.content.Context
import com.example.healthguard.data.network.pills.PillAlarmScheduler
import com.example.healthguard.data.network.pills.PillReminderFirebaseDataSource
import com.example.healthguard.data.network.pills.toModel

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
package com.example.healthguard.data.network.appointments

import android.content.Context

class AppointmentRestoreManager(
    private val dataSource: AppointmentFirebaseDataSource = AppointmentFirebaseDataSource()
) {
    suspend fun restoreAll(context: Context) {
        val appointments = dataSource.getAllAppointments()
        val scheduler = AppointmentScheduler(context)

        appointments.forEach { appointment ->
            scheduler.cancelAll(appointment)
            scheduler.scheduleAll(appointment)
        }
    }
}
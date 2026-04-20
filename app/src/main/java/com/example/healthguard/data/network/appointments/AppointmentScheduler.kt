package com.example.healthguard.data.network.appointments

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AppointmentScheduler(
    private val context: Context
) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAll(appointment: Appointment) {
        appointment.reminderOffsets.forEachIndexed { index, offset ->
            val triggerTime = appointment.timestamp - offset

            if (triggerTime <= System.currentTimeMillis()) {
                Log.d(
                    "APPOINTMENT_DEBUG",
                    "Skipping past reminder for ${appointment.title}, offset=$offset"
                )
                return@forEachIndexed
            }

            val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
                putExtra("appointment_id", appointment.id)
                putExtra("title", appointment.title)
                putExtra("location", appointment.location)
                putExtra("timestamp", appointment.timestamp)
                putExtra("offset", offset)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.id.hashCode() + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                scheduleReminder(triggerTime, pendingIntent)
                Log.d(
                    "APPOINTMENT_DEBUG",
                    "Scheduled ${appointment.title} reminder at trigger=$triggerTime offset=$offset"
                )
            } catch (e: SecurityException) {
                Log.e("APPOINTMENT_DEBUG", "Failed to schedule appointment reminder", e)
            }
        }
    }

    fun cancelAll(appointment: Appointment) {
        appointment.reminderOffsets.forEachIndexed { index, offset ->
            val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
                putExtra("appointment_id", appointment.id)
                putExtra("offset", offset)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.id.hashCode() + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleReminder(
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
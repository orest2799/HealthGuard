package com.example.healthguard.data.network.pills

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.healthguard.domain.model.pills.notifications.PillNotificationConstants
import com.example.healthguard.domain.model.pills.notifications.PillReminderPayload
import com.example.healthguard.domain.model.pills.notifications.PillReminderReceiver
import java.util.Calendar

class PillAlarmScheduler(
    private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(reminder: PillReminder) {
        if (!reminder.enabled) return

        reminder.reminderTimes.forEach { reminderTime ->
            reminderTime.daysOfWeek.forEach { dayOfWeek ->
                val payload = PillReminderPayload(
                    pillId = reminder.id,
                    medicineName = reminder.medicineName,
                    dosage = reminder.dosage,
                    hour = reminderTime.hour,
                    minute = reminderTime.minute,
                    dayOfWeek = dayOfWeek
                )
                scheduleExact(payload)
            }
        }
    }

    fun cancelAll(reminder: PillReminder) {
        reminder.reminderTimes.forEach { reminderTime ->
            reminderTime.daysOfWeek.forEach { dayOfWeek ->
                val payload = PillReminderPayload(
                    pillId = reminder.id,
                    medicineName = reminder.medicineName,
                    dosage = reminder.dosage,
                    hour = reminderTime.hour,
                    minute = reminderTime.minute,
                    dayOfWeek = dayOfWeek
                )
                cancelExact(payload)
            }
        }
    }
    private fun scheduleExact(payload: PillReminderPayload) {
        val pendingIntent = buildPendingIntent(payload)
        val triggerTime = calculateNextTriggerTime(payload.dayOfWeek, payload.hour, payload.minute)

        Log.d("PILL_DEBUG", "Scheduling ${payload.medicineName} for day=${payload.dayOfWeek}, time=${payload.hour}:${payload.minute}, trigger=$triggerTime")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("PILL_DEBUG", "Exact alarm permission error (using inexact fallback)", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
        }
    }

    fun scheduleNextWeek(payload: PillReminderPayload) {
        val pendingIntent = buildPendingIntent(payload)
        val triggerAtMillis = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, payload.dayOfWeek)
            set(Calendar.HOUR_OF_DAY, payload.hour)
            set(Calendar.MINUTE, payload.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis

        Log.d("PILL_DEBUG", "Rescheduling next week for ${payload.medicineName}, day=${payload.dayOfWeek}, time=${payload.hour}:${payload.minute}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("PILL_DEBUG", "Exact alarm permission error on reschedule", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    private fun cancelExact(payload: PillReminderPayload) {
        val pendingIntent = buildPendingIntent(payload)
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(payload: PillReminderPayload): PendingIntent {
        val intent = Intent(context, PillReminderReceiver::class.java).apply {
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, payload.pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, payload.medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, payload.dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, payload.hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, payload.minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, payload.dayOfWeek)
        }

        return PendingIntent.getBroadcast(
            context,
            buildRequestCode(payload),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildRequestCode(payload: PillReminderPayload): Int {
        val base = payload.pillId.hashCode()
        val timePart = payload.hour * 100 + payload.minute
        return base + (payload.dayOfWeek * PillNotificationConstants.REQUEST_CODE_SEPARATOR) + timePart
    }

    private fun calculateNextTriggerTime(
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ): Long {
        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
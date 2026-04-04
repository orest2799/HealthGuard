package com.example.healthguard.data.network.pills

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.healthguard.domain.model.pills.notifications.PillNotificationConstants
import com.example.healthguard.domain.model.pills.notifications.PillNotificationHelper
import com.example.healthguard.domain.model.pills.notifications.PillReminderReceiver
import java.util.Calendar

class PillActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getStringExtra(PillNotificationConstants.EXTRA_PILL_ID).orEmpty()
        val medicineName = intent.getStringExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME).orEmpty()
        val dosage = intent.getStringExtra(PillNotificationConstants.EXTRA_DOSAGE).orEmpty()
        val hour = intent.getIntExtra(PillNotificationConstants.EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(PillNotificationConstants.EXTRA_MINUTE, 0)
        val dayOfWeek = intent.getIntExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, 0)

        when (intent.action) {
            ACTION_TAKEN -> {
                Log.d("PILL_DEBUG", "Taken pressed for $pillId")

                cancelRepeatAlarm(context, pillId)

                PillNotificationHelper(context).cancelNotification(pillId)

                Toast.makeText(context, "$medicineName marked as taken", Toast.LENGTH_SHORT).show()

                // Later:
                // save "taken" state to Firebase here
            }

            ACTION_SNOOZE -> {
                Log.d("PILL_DEBUG", "Snooze pressed for $pillId")

                PillNotificationHelper(context).cancelNotification(pillId)

                scheduleSnoozeAlarm(
                    context = context,
                    pillId = pillId,
                    medicineName = medicineName,
                    dosage = dosage,
                    hour = hour,
                    minute = minute,
                    dayOfWeek = dayOfWeek
                )

                Toast.makeText(context, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleSnoozeAlarm(
        context: Context,
        pillId: String,
        medicineName: String,
        dosage: String,
        hour: Int,
        minute: Int,
        dayOfWeek: Int
    ) {
        val intent = Intent(context, PillReminderReceiver::class.java).apply {
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra("is_repeat_alarm", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }.timeInMillis

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e("PILL_DEBUG", "Snooze scheduling failed", e)
        }
    }

    private fun cancelRepeatAlarm(context: Context, pillId: String) {
        val repeatIntent = Intent(context, PillReminderReceiver::class.java)

        val repeatPendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 999,
            repeatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(repeatPendingIntent)
    }

    companion object {
        const val ACTION_TAKEN = "com.example.healthguard.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.example.healthguard.ACTION_SNOOZE"
    }
}
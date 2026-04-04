package com.example.healthguard.domain.model.pills.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.healthguard.data.network.pills.PillAlarmScheduler

class PillReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PILL_DEBUG", "PillReminderReceiver triggered")

        val pillId = intent.getStringExtra(PillNotificationConstants.EXTRA_PILL_ID).orEmpty()
        val medicineName = intent.getStringExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME).orEmpty()
        val dosage = intent.getStringExtra(PillNotificationConstants.EXTRA_DOSAGE).orEmpty()
        val hour = intent.getIntExtra(PillNotificationConstants.EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(PillNotificationConstants.EXTRA_MINUTE, 0)
        val dayOfWeek = intent.getIntExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, 0)

        Log.d(
            "PILL_DEBUG",
            "Received reminder: id=$pillId, name=$medicineName, dosage=$dosage, day=$dayOfWeek, time=$hour:$minute"
        )

        val alarmIntent = Intent(context, PillAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

        context.startActivity(alarmIntent)

        if (pillId.isNotBlank() && dayOfWeek != 0) {
            val scheduler = PillAlarmScheduler(context)
            scheduler.scheduleNextWeek(
                PillReminderPayload(
                    pillId = pillId,
                    medicineName = medicineName,
                    dosage = dosage,
                    hour = hour,
                    minute = minute,
                    dayOfWeek = dayOfWeek
                )
            )
        }
    }
}
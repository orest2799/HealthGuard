package com.example.healthguard.domain.model.pills.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.healthguard.data.network.pills.PillActionReceiver

class PillNotificationHelper(
    private val context: Context
) {


    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                PillNotificationConstants.CHANNEL_ID,
                PillNotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = PillNotificationConstants.CHANNEL_DESCRIPTION
                enableVibration(true)


                setSound(null, null)

                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    fun showAlarmNotification(
        pillId: String,
        medicineName: String,
        dosage: String,
        hour: Int,
        minute: Int,
        dayOfWeek: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val fullScreenIntent = Intent(context, PillAlarmActivity::class.java).apply {
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, dayOfWeek)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            pillId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        val takenIntent = Intent(context, PillActionReceiver::class.java).apply {
            action = PillActionReceiver.ACTION_TAKEN
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, PillActionReceiver::class.java).apply {
            action = PillActionReceiver.ACTION_SNOOZE
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, PillNotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pill Alarm")
            .setContentText("Time to take $dosage of $medicineName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)



            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePendingIntent)

            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        NotificationManagerCompat.from(context).notify(pillId.hashCode(), notification)
    }




    fun cancelNotification(pillId: String) {
        NotificationManagerCompat.from(context).cancel(pillId.hashCode())
    }
}
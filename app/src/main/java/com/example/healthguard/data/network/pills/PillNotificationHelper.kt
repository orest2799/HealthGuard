package com.example.healthguard.data.network.pills

import android.Manifest
import android.app.Notification
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
import com.example.healthguard.R

class PillNotificationHelper(private val context: Context) {

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PillNotificationConstants.CHANNEL_ID,
                context.getString(R.string.pill_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.pill_channel_description)
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
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
            putExtra(PillNotificationConstants.EXTRA_HOUR, hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, minute)
        }

        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            PillNotificationConstants.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.pill_notification_title))
            .setContentText(
                context.getString(
                    R.string.pill_notification_body,
                    dosage,
                    medicineName
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.alarm_taken),
                takenPendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(pillId.hashCode(), notification)
    }

    fun cancelNotification(pillId: String) {
        NotificationManagerCompat.from(context).cancel(pillId.hashCode())
    }
}
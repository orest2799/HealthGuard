package com.example.healthguard.data.network.appointments

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
import androidx.core.graphics.toColorInt
import com.example.healthguard.MainActivity
import com.example.healthguard.R

class AppointmentNotificationHelper(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "appointments_channel"
        const val CHANNEL_NAME = "Appointments"
        const val CHANNEL_DESCRIPTION = "Appointment reminders"
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                // This ensures it shows up on the lock screen
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    fun showNotification(
        title: String,
        location: String,
        message: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val brandTeal = "#4DB6AC".toColorInt()


        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // 1. Ensure ic_notification_shield is the Vector (.xml) you made via Clip Art
            .setSmallIcon(R.drawable.ic_notification)

            // 2. This sets the accent color of the icon circle to your teal
            .setColor(brandTeal)

            // 3. This applies the brand color to the notification elements
            .setColorized(true)

            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build() // Added missing closing parenthesis here

        // Use NotificationManagerCompat to trigger the notification
        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
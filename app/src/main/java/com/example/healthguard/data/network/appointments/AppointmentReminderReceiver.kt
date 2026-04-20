package com.example.healthguard.data.network.appointments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthguard.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title").orEmpty()
        val location = intent.getStringExtra("location").orEmpty()
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val offset = intent.getLongExtra("offset", 0L)

        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val appointmentTime = if (timestamp > 0) formatter.format(Date(timestamp)) else ""

        val offsetText = when (offset) {
            24 * 60 * 60 * 1000L -> context.getString(R.string.appointment_offset_24h)
            2 * 60 * 60 * 1000L -> context.getString(R.string.appointment_offset_2h)
            60 * 60 * 1000L -> context.getString(R.string.appointment_offset_1h)
            else -> context.getString(R.string.appointment_offset_soon)
        }

        AppointmentNotificationHelper(context).createChannel()
        AppointmentNotificationHelper(context).showNotification(
            title = context.getString(R.string.appointment_notification_title),
            location = location,
            message = context.getString(
                R.string.appointment_notification_message,
                title,
                offsetText,
                appointmentTime
            )
        )
    }
}
package com.example.healthguard.data.network.appointments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            24 * 60 * 60 * 1000L -> "in 24 hours"
            2 * 60 * 60 * 1000L -> "in 2 hours"
            60 * 60 * 1000L -> "in 1 hour"
            else -> "soon"
        }

        AppointmentNotificationHelper(context).createChannel()
        AppointmentNotificationHelper(context).showNotification(
            title = "Upcoming appointment",
            location = location,
            message = "$title is scheduled $offsetText at $appointmentTime"
        )
    }
}
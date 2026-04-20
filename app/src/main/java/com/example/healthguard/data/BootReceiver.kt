package com.example.healthguard.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.healthguard.data.network.appointments.AppointmentRestoreManager
import com.example.healthguard.data.network.pills.PillReminderRestoreManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BOOT_DEBUG", "BOOT_COMPLETED received")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d("BOOT_DEBUG", "No logged-in user after reboot, skipping restore")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                PillReminderRestoreManager().restoreAll(context)
                Log.d("PILL_DEBUG", "Pill reminders restored after reboot")
            } catch (e: Exception) {
                Log.e("PILL_DEBUG", "Failed to restore pill reminders after reboot", e)
            }

            try {
                AppointmentRestoreManager().restoreAll(context)
                Log.d("APPOINTMENT_DEBUG", "Appointments restored after reboot")
            } catch (e: Exception) {
                Log.e("APPOINTMENT_DEBUG", "Failed to restore appointments after reboot", e)
            }
        }
    }
}
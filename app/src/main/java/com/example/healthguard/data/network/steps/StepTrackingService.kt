package com.example.healthguard.data.network.steps



import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.healthguard.MainActivity
import com.example.healthguard.R

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepTrackingService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP  -> stop()
        }
        return START_STICKY // Restart automatically if killed by system
    }

    private fun start() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Tracking your steps…"))

        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("StepService", "✅ Step sensor registered")
        } ?: Log.w("StepService", "⚠️ No step counter sensor found")
    }

    private fun stop() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalSinceBoot = event.values[0].toInt()
        val today = LocalDate.now().toString()
        val uid = auth.currentUser?.uid ?: return

        serviceScope.launch {
            try {
                val repo = Injection.provideStepRepository(applicationContext, uid)
                val baseline = repo.getOrCreateBaseline(today, totalSinceBoot)
                val todaySteps = (totalSinceBoot - baseline).coerceAtLeast(0)

                repo.updateSteps(todaySteps)

                // Update notification with current step count
                updateNotification("Today: $todaySteps steps")

                Log.d("StepService", "Steps updated: $todaySteps")
            } catch (e: Exception) {
                Log.e("StepService", "Error saving steps: ${e.message}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracking",
                NotificationManager.IMPORTANCE_LOW // LOW = no sound, just persistent icon
            ).apply {
                description = "Keeps track of your daily steps"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthGuard")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification) // your notification icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)      // can't be dismissed by user
            .setSilent(true)       // no sound/vibration
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        private const val CHANNEL_ID     = "step_tracking_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
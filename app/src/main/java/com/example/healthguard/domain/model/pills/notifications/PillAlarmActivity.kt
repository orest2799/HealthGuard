package com.example.healthguard.domain.model.pills.notifications

import ThemePreferences
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import com.example.healthguard.R
import com.example.healthguard.data.network.dataStore
import com.example.healthguard.presentation.pills.AlarmScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.AlarmViewModel
import java.util.Calendar

class PillAlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var handled = false

    private lateinit var pillId: String
    private lateinit var medicineName: String
    private lateinit var dosage: String
    private var hour: Int = 0
    private var minute: Int = 0
    private var dayOfWeek: Int = 0

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            snoozeAndFinish()
        }
        showOverLockScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pillId = intent.getStringExtra(PillNotificationConstants.EXTRA_PILL_ID).orEmpty()
        medicineName = intent.getStringExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME).orEmpty()
        dosage = intent.getStringExtra(PillNotificationConstants.EXTRA_DOSAGE).orEmpty()
        hour = intent.getIntExtra(PillNotificationConstants.EXTRA_HOUR, 0)
        minute = intent.getIntExtra(PillNotificationConstants.EXTRA_MINUTE, 0)
        dayOfWeek = intent.getIntExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, 0)

        Log.d("PILL_DEBUG", "PillAlarmActivity opened for $medicineName")

        viewModel.init(
            pillId = pillId,
            hour = hour,
            minute = minute,
            medName = medicineName,
            dosage = dosage,
            dayOfWeek = dayOfWeek
        )

        setContent {
            val isDarkTheme by ThemePreferences
                .getTheme(applicationContext.dataStore)
                .collectAsState(initial = false)

            AppTheme(darkTheme = isDarkTheme) {
                AlarmScreen(
                    viewModel = viewModel,
                    onTaken = {
                        handled = true
                        cancelRepeatAlarm(this)
                        stopAlarm()
                    },
                    onSnooze = {
                        handled = true
                        stopAlarm()
                    }
                )
            }
        }
        startAlarmSound()

        window.decorView.postDelayed({
            if (!handled) {
                Log.d("PILL_DEBUG", "No action taken, auto-snoozing for 10 minutes")
                stopAlarm()
                scheduleSnooze(this, 10)
                finish()
            }
        }, 60_000L)
    }

    private fun startAlarmSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                val uri = "android.resource://$packageName/${R.raw.shaking_pillbox}".toUri()
                setDataSource(this@PillAlarmActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleSnooze(context: Context, minutes: Int) {
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
            add(Calendar.MINUTE, minutes)
        }.timeInMillis

        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun cancelRepeatAlarm(context: Context) {
        val intent = Intent(context, PillReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pillId.hashCode() + 999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
    private fun snoozeAndFinish() {
        if (handled) return
        handled = true
        stopAlarm()
        scheduleSnooze(this, 10)
        finish()
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (!handled) {
            handled = true
            stopAlarm()
            viewModel.onSnooze(this) {
                finish()
            }
        }
    }


    override fun onStop() {
        super.onStop()
        if (!isFinishing && !handled) {
            snoozeAndFinish()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
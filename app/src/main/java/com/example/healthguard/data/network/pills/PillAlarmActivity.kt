package com.example.healthguard.data.network.pills

import ThemePreferences
import android.app.KeyguardManager
import android.content.Intent
import android.media.AudioManager
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
import com.example.healthguard.data.dataStore
import com.example.healthguard.presentation.pills.AlarmScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.AlarmViewModel

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

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
                        cancelRepeatAlarm()
                        stopAlarm()
                        finish()
                    },
                    onSnooze = {
                        handled = true
                        stopAlarm()
                        viewModel.onSnooze(this@PillAlarmActivity) {
                            finish()
                        }
                    }
                )
            }
        }

        startAlarmSound()

        window.decorView.postDelayed({
            if (!handled) {
                Log.d("PILL_DEBUG", "No action taken, auto-snoozing for 10 minutes")
                stopAlarm()
                viewModel.onSnooze(this) {
                    finish()
                }
            }
        }, 60_000L)
    }

    private fun startAlarmSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_ALARM)
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

    private fun cancelRepeatAlarm() {
        val intent = Intent(this, PillReminderReceiver::class.java)

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            pillId.hashCode() + 999,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(KeyguardManager::class.java)
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
        viewModel.onSnooze(this) {
            finish()
        }
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
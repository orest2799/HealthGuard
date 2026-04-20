package com.example.healthguard.viewmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.pills.AlarmState
import com.example.healthguard.data.network.pills.PillLogFirebaseDataSource
import com.example.healthguard.data.network.pills.PillNotificationConstants
import com.example.healthguard.data.network.pills.PillReminderReceiver
import com.example.healthguard.data.network.pills.buildTimeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmState())
    val uiState = _uiState.asStateFlow()

    fun init(
        pillId: String,
        hour: Int,
        minute: Int,
        medName: String,
        dosage: String,
        dayOfWeek: Int
    ) {
        _uiState.value = AlarmState(
            pillId = pillId,
            hour = hour,
            minute = minute,
            medName = medName,
            dosage = dosage,
            dayOfWeek = dayOfWeek
        )
    }

    fun onTaken(
        onDone: () -> Unit
    ) {
        val state = _uiState.value

        val timeKey = buildTimeKey(
            reminderId = state.pillId,
            hour = state.hour,
            minute = state.minute
        )

        viewModelScope.launch {
            try {
                PillLogFirebaseDataSource().markDoseTaken(timeKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onDone()
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun onSnooze(
        context: Context,
        onDone: () -> Unit
    ) {
        val state = _uiState.value

        val intent = Intent(context, PillReminderReceiver::class.java).apply {
            putExtra(PillNotificationConstants.EXTRA_PILL_ID, state.pillId)
            putExtra(PillNotificationConstants.EXTRA_MEDICINE_NAME, state.medName)
            putExtra(PillNotificationConstants.EXTRA_DOSAGE, state.dosage)
            putExtra(PillNotificationConstants.EXTRA_HOUR, state.hour)
            putExtra(PillNotificationConstants.EXTRA_MINUTE, state.minute)
            putExtra(PillNotificationConstants.EXTRA_DAY_OF_WEEK, state.dayOfWeek)
            putExtra("is_repeat_alarm", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            state.pillId.hashCode() + 999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }.timeInMillis

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                onDone()
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        onDone()
    }
}
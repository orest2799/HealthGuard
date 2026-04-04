package com.example.healthguard.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.pills.PillLogFirebaseDataSource
import com.example.healthguard.data.network.pills.PillReminderFirebaseDataSource
import com.example.healthguard.data.network.pills.toModel
import com.example.healthguard.data.network.pills.PillReminder
import com.example.healthguard.data.network.pills.PillAlarmScheduler
import com.example.healthguard.data.network.pills.buildTimeKey
import com.example.healthguard.ui.PillListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class PillListViewModel(
    private val firebaseDataSource: PillReminderFirebaseDataSource = PillReminderFirebaseDataSource(),
    private val pillLogDataSource: PillLogFirebaseDataSource = PillLogFirebaseDataSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PillListUiState())
    val uiState: StateFlow<PillListUiState> = _uiState

    init {
        loadAll()
    }

    fun loadAll() {
        loadReminders()
        loadTakenToday()
    }

    fun loadReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("PILL_DEBUG", "Loading reminders from Firebase")

                val reminders = firebaseDataSource.getAllReminders()
                    .map { it.toModel() }
                    .sortedBy { it.medicineName.lowercase() }

                Log.d("PILL_DEBUG", "Loaded reminders count = ${reminders.size}")

                _uiState.update {
                    it.copy(
                        reminders = reminders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PILL_DEBUG", "Failed loading reminders", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load reminders"
                    )
                }
            }
        }
    }

    fun loadTakenToday() {
        viewModelScope.launch {
            try {
                val takenKeys = pillLogDataSource.getTakenDoseKeysToday()
                Log.d("PILL_DEBUG", "Loaded taken keys in ViewModel: $takenKeys")

                _uiState.update {
                    it.copy(takenTimeKeysToday = takenKeys)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load taken doses")
                }
            }
        }
    }

    fun deleteReminder(context: Context, reminder: PillReminder) {
        viewModelScope.launch {
            try {
                PillAlarmScheduler(context).cancelAll(reminder)
                firebaseDataSource.deleteReminder(reminder.id)
                loadReminders()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete reminder")
                }
            }
        }
    }

    fun markDoseTaken(timeKey: String) {
        viewModelScope.launch {
            try {
                pillLogDataSource.markDoseTaken(timeKey)
                _uiState.update {
                    it.copy(
                        takenTimeKeysToday = it.takenTimeKeysToday + timeKey
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to mark dose taken")
                }
            }
        }
    }

    fun unmarkDoseTaken(timeKey: String) {
        viewModelScope.launch {
            try {
                pillLogDataSource.unmarkDoseTaken(timeKey)
                _uiState.update {
                    it.copy(
                        takenTimeKeysToday = it.takenTimeKeysToday - timeKey
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to unmark dose")
                }
            }
        }
    }

    fun toggleDoseTaken(timeKey: String) {
        if (_uiState.value.takenTimeKeysToday.contains(timeKey)) {
            unmarkDoseTaken(timeKey)
        } else {
            markDoseTaken(timeKey)
        }
    }

    fun getTodayDoseProgress(): Pair<Int, Int> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val validTodayKeys = _uiState.value.reminders
            .filter { it.enabled }
            .flatMap { reminder ->
                reminder.reminderTimes
                    .filter { today in it.daysOfWeek }
                    .map { time ->
                        buildTimeKey(
                            reminderId = reminder.id,
                            hour = time.hour,
                            minute = time.minute
                        )
                    }
            }
            .toSet()

        val total = validTodayKeys.size
        val taken = _uiState.value.takenTimeKeysToday.count { it in validTodayKeys }

        return taken to total
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
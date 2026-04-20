package com.example.healthguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.R
import com.example.healthguard.data.network.pills.PillAlarmScheduler
import com.example.healthguard.data.network.pills.PillReminder
import com.example.healthguard.data.network.pills.PillReminderFirebaseDataSource
import com.example.healthguard.data.network.pills.ReminderTime
import com.example.healthguard.data.network.pills.toDto
import com.example.healthguard.data.network.pills.toModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TimeEntryUi(
    val id: Long,
    val hour: Int,
    val minute: Int
)

data class AddEditPillUiState(
    val reminderId: String? = null,
    val medicineName: String = "",
    val dosage: String = "",
    val selectedDays: Set<Int> = emptySet(),
    val times: List<TimeEntryUi> = emptyList(),
    val enabled: Boolean = true,
    val medicineNameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null,
    val daysError: String? = null,
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false
)

class AddEditPillViewModel(
    private val firebaseDataSource: PillReminderFirebaseDataSource = PillReminderFirebaseDataSource(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditPillUiState())
    val uiState: StateFlow<AddEditPillUiState> = _uiState

    private var originalReminder: PillReminder? = null

    fun onMedicineNameChange(value: String) {
        _uiState.update {
            it.copy(medicineName = value, medicineNameError = null, generalError = null)
        }
    }

    fun onDosageChange(value: String) {
        _uiState.update {
            it.copy(dosage = value, dosageError = null, generalError = null)
        }
    }

    fun loadReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                val dto = firebaseDataSource.getReminder(reminderId) ?: return@launch
                val reminder = dto.toModel()
                originalReminder = reminder

                _uiState.update {
                    it.copy(
                        reminderId = reminder.id,
                        medicineName = reminder.medicineName,
                        dosage = reminder.dosage,
                        selectedDays = reminder.reminderTimes
                            .firstOrNull()?.daysOfWeek?.toSet() ?: emptySet(),
                        times = reminder.reminderTimes.mapIndexed { index, time ->
                            TimeEntryUi(id = index.toLong(), hour = time.hour, minute = time.minute)
                        },
                        enabled = reminder.enabled,
                        isEditMode = true,
                        generalError = null
                    )
                }
            } catch (e: Exception) {
                // Error message will be set with context in saveReminder;
                // for load we just store the raw message as fallback
                _uiState.update { it.copy(generalError = e.message) }
            }
        }
    }

    fun onEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    fun onDayToggle(day: Int) {
        _uiState.update { state ->
            val newDays = state.selectedDays.toMutableSet()
            if (newDays.contains(day)) newDays.remove(day) else newDays.add(day)
            state.copy(selectedDays = newDays, daysError = null, generalError = null)
        }
    }

    fun addTime(hour: Int, minute: Int) {
        _uiState.update { state ->
            val exists = state.times.any { it.hour == hour && it.minute == minute }
            if (exists) state
            else state.copy(
                times = (state.times + TimeEntryUi(
                    id = System.currentTimeMillis(),
                    hour = hour,
                    minute = minute
                )).sortedWith(compareBy({ it.hour }, { it.minute })),
                timesError = null,
                generalError = null
            )
        }
    }

    fun removeTime(id: Long) {
        _uiState.update { state ->
            state.copy(
                times = state.times.filterNot { it.id == id },
                timesError = null,
                generalError = null
            )
        }
    }

    fun saveReminder(
        context: Context,
        onSaved: (PillReminder) -> Unit = {}
    ) {
        val state = _uiState.value

        // Use context.getString so errors are always in the current language
        val medicineNameError = if (state.medicineName.isBlank())
            context.getString(R.string.pill_error_name_required) else null
        val dosageError = if (state.dosage.isBlank())
            context.getString(R.string.pill_error_dosage_required) else null
        val timesError = if (state.times.isEmpty())
            context.getString(R.string.pill_error_time_required) else null
        val daysError = if (state.selectedDays.isEmpty())
            context.getString(R.string.pill_error_days_required) else null
        val authError = if (auth.currentUser == null)
            context.getString(R.string.pill_error_sign_in) else null

        if (medicineNameError != null || dosageError != null ||
            timesError != null || daysError != null || authError != null
        ) {
            _uiState.update {
                it.copy(
                    medicineNameError = medicineNameError,
                    dosageError = dosageError,
                    timesError = timesError,
                    daysError = daysError,
                    generalError = authError
                )
            }
            return
        }

        val reminder = PillReminder(
            id = state.reminderId ?: UUID.randomUUID().toString(),
            medicineName = state.medicineName.trim(),
            dosage = state.dosage.trim(),
            reminderTimes = state.times.map { entry ->
                ReminderTime(
                    hour = entry.hour,
                    minute = entry.minute,
                    daysOfWeek = state.selectedDays.toList().sorted()
                )
            },
            enabled = state.enabled
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }

            try {
                val scheduler = PillAlarmScheduler(context)
                originalReminder?.let { scheduler.cancelAll(it) }
                firebaseDataSource.saveReminder(reminder.toDto())
                if (reminder.enabled) scheduler.scheduleAll(reminder)
                originalReminder = reminder

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        reminderId = reminder.id,
                        isEditMode = true
                    )
                }
                onSaved(reminder)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        generalError = e.message
                            ?: context.getString(R.string.pill_error_save_failed)
                    )
                }
            }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun consumeGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }
}
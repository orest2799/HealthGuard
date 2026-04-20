package com.example.healthguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.appointments.Appointment
import com.example.healthguard.data.network.appointments.AppointmentFirebaseDataSource
import com.example.healthguard.data.network.appointments.AppointmentScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


data class AppointmentUiState(
    val appointments: List<Appointment> = emptyList(), // Fixes unresolved ref
    val isLoading: Boolean = false,                   // Fixes unresolved ref
    val isSaving: Boolean = false,                    // Fixes image_572c04.png error
    val title: String = "",
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reminder24h: Boolean = false,
    val reminder2h: Boolean = false,
    val reminder1h: Boolean = false,
    val appointmentId: String? = null,
    val error: String? = null,
    val saveSuccess: Boolean = false
)
class AppointmentViewModel(
    private val dataSource: AppointmentFirebaseDataSource = AppointmentFirebaseDataSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    fun onLocationChange(value: String) {
        _uiState.update { it.copy(location = value, error = null) }
    }

    fun onTimestampChange(value: Long) {
        _uiState.update { it.copy(timestamp = value, error = null) }
    }

    fun onReminder24hChange(value: Boolean) {
        _uiState.update { it.copy(reminder24h = value) }
    }

    fun onReminder2hChange(value: Boolean) {
        _uiState.update { it.copy(reminder2h = value) }
    }

    fun onReminder1hChange(value: Boolean) {
        _uiState.update { it.copy(reminder1h = value) }
    }

    fun saveAppointment(context: Context) {
        val state = _uiState.value
        if (state.title.isBlank()) return


        val finalId = state.appointmentId ?: UUID.randomUUID().toString()

        val appointment = Appointment(
            id = finalId,
            title = state.title.trim(),
            location = state.location.trim(),
            timestamp = state.timestamp,
            reminderOffsets = buildList {
                if (state.reminder24h) add(24 * 60 * 60 * 1000L)
                if (state.reminder2h) add(2 * 60 * 60 * 1000L)
                if (state.reminder1h) add(60 * 60 * 1000L)
            }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {

                AppointmentScheduler(context).cancelAll(appointment)


                dataSource.saveAppointment(appointment)

                AppointmentScheduler(context).scheduleAll(appointment)

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun loadAllAppointments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = dataSource.getAllAppointments()
            _uiState.update { it.copy(appointments = list, isLoading = false) }
        }
    }

    fun deleteAppointment(context: Context, appointment: Appointment) {
        viewModelScope.launch {

            AppointmentScheduler(context).cancelAll(appointment)

            dataSource.deleteAppointment(appointment.id)

            loadAllAppointments()
        }
    }
    fun loadAppointment(id: String) {
        viewModelScope.launch {
            val appointment = dataSource.getAppointment(id) ?: return@launch

            _uiState.update {
                it.copy(
                    appointmentId = id,
                    title = appointment.title,
                    location = appointment.location,
                    timestamp = appointment.timestamp,
                    reminder24h = appointment.reminderOffsets.contains(24 * 60 * 60 * 1000L),
                    reminder2h = appointment.reminderOffsets.contains(2 * 60 * 60 * 1000L),
                    reminder1h = appointment.reminderOffsets.contains(60 * 60 * 1000L)
                )
            }
        }
    }
    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
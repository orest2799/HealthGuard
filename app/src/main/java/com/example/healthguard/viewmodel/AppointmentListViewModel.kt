package com.example.healthguard.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.appointments.Appointment
import com.example.healthguard.data.network.appointments.AppointmentFirebaseDataSource
import com.example.healthguard.data.network.appointments.AppointmentScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppointmentListViewModel(
    private val dataSource: AppointmentFirebaseDataSource = AppointmentFirebaseDataSource()
) : ViewModel() {

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadAppointments() {
        viewModelScope.launch {
            _isLoading.value = true
            _appointments.value = dataSource.getAllAppointments()
            _isLoading.value = false
        }
    }

    fun deleteAppointment(appointment: Appointment, context: Context) {
        viewModelScope.launch {
            AppointmentScheduler(context).cancelAll(appointment)
            dataSource.deleteAppointment(appointment.id)
            loadAppointments() // Refresh list
        }
    }
}
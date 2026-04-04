package com.example.healthguard.presentation.appointments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentScreen(
    viewModel: AppointmentViewModel, // Required for MVVM
    navController: NavController,
    appointmentId: String? = null // Passed from NavGraph
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState() // Observe VM State
    val calendar = remember { Calendar.getInstance() }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Trigger data load if editing
    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            viewModel.loadAppointment(appointmentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (appointmentId == null) "New Appointment" else "Edit Appointment") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DATE PICKER
            Button(
                onClick = {
                    calendar.timeInMillis = state.timestamp
                    DatePickerDialog(context, { _, y, m, d ->
                        calendar.set(y, m, d)
                        viewModel.onTimestampChange(calendar.timeInMillis)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${dateFormatter.format(Date(state.timestamp))}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TIME PICKER
            Button(
                onClick = {
                    calendar.timeInMillis = state.timestamp
                    TimePickerDialog(context, { _, h, m ->
                        calendar.set(Calendar.HOUR_OF_DAY, h)
                        calendar.set(Calendar.MINUTE, m)
                        viewModel.onTimestampChange(calendar.timeInMillis)
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${timeFormatter.format(Date(state.timestamp))}")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Reminders", style = MaterialTheme.typography.titleMedium)

            ReminderCheckbox(state.reminder24h, "24 hours before") { viewModel.onReminder24hChange(it) }
            ReminderCheckbox(state.reminder2h, "2 hours before") { viewModel.onReminder2hChange(it) }
            ReminderCheckbox(state.reminder1h, "1 hour before") { viewModel.onReminder1hChange(it) }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveAppointment(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (appointmentId == null) "Save Appointment" else "Update Appointment")
            }
        }
    }

    // Handle navigation after save
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveSuccess()
            navController.popBackStack()
        }
    }
}

@Composable
private fun ReminderCheckbox(checked: Boolean, text: String, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text)
    }
}
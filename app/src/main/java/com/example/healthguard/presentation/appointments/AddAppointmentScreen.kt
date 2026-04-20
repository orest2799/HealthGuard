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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentScreen(
    viewModel: AppointmentViewModel,
    navController: NavController,
    appointmentId: String? = null
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val calendar = remember { Calendar.getInstance() }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Pre-read strings for use inside non-Composable callbacks
    val strSaved = stringResource(R.string.appointment_saved)

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            viewModel.loadAppointment(appointmentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (appointmentId == null)
                            stringResource(R.string.appointment_new)
                        else
                            stringResource(R.string.appointment_edit)
                    )
                }
            )
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
                label = { Text(stringResource(R.string.appointment_title)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text(stringResource(R.string.appointment_location)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                Text(stringResource(R.string.appointment_date, dateFormatter.format(Date(state.timestamp))))
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                Text(stringResource(R.string.appointment_time, timeFormatter.format(Date(state.timestamp))))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(stringResource(R.string.appointment_reminders), style = MaterialTheme.typography.titleMedium)

            ReminderCheckbox(
                checked = state.reminder24h,
                text = stringResource(R.string.appointment_reminder_24h),
                onCheckedChange = { viewModel.onReminder24hChange(it) }
            )
            ReminderCheckbox(
                checked = state.reminder2h,
                text = stringResource(R.string.appointment_reminder_2h),
                onCheckedChange = { viewModel.onReminder2hChange(it) }
            )
            ReminderCheckbox(
                checked = state.reminder1h,
                text = stringResource(R.string.appointment_reminder_1h),
                onCheckedChange = { viewModel.onReminder1hChange(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveAppointment(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (appointmentId == null)
                        stringResource(R.string.appointment_save)
                    else
                        stringResource(R.string.appointment_update)
                )
            }
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, strSaved, Toast.LENGTH_SHORT).show()
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
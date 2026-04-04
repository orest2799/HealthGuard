package com.example.healthguard.presentation.pills

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthguard.viewmodel.AddEditPillViewModel
import java.util.Calendar


private fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.canScheduleExactAlarms() == true
    } else {
        true
    }
}

private fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPillScreen(
    viewModel: AddEditPillViewModel,
    onSavedNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val now = remember { Calendar.getInstance() }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.addTime(hourOfDay, minute)
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        )
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Pill reminder saved", Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveSuccess()
            onSavedNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Edit Pill Reminder" else "Add Pill Reminder")
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Medicine Info",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = uiState.medicineName,
                        onValueChange = viewModel::onMedicineNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Medicine name") },
                        singleLine = true,
                        isError = uiState.medicineNameError != null,
                        supportingText = {
                            uiState.medicineNameError?.let { Text(it) }
                        }
                    )

                    OutlinedTextField(
                        value = uiState.dosage,
                        onValueChange = viewModel::onDosageChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dosage") },
                        singleLine = true,
                        isError = uiState.dosageError != null,
                        supportingText = {
                            uiState.dosageError?.let { Text(it) }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Days of week",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    DaySelectorRow(
                        selectedDays = uiState.selectedDays,
                        onDayClick = viewModel::onDayToggle
                    )

                    uiState.daysError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Reminder times",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (uiState.times.isNotEmpty()) {
                        TimeChipRow(
                            times = uiState.times,
                            onRemoveTime = viewModel::removeTime
                        )
                    }

                    uiState.timesError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    TextButton(
                        onClick = { timePickerDialog.show() }
                    ) {
                        Text("Add time")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Options",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reminder enabled")
                        Switch(
                            checked = uiState.enabled,
                            onCheckedChange = viewModel::onEnabledChange
                        )
                    }
                }
            }
            uiState.generalError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = {
                    if (canScheduleExactAlarms(context)) {
                        viewModel.saveReminder(context)
                    }else {
                        Toast.makeText(
                            context,
                            "Please allow exact alarms, then come back and tap Save again.",
                            Toast.LENGTH_LONG
                        ).show()
                        requestExactAlarmPermission(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(
                    if (uiState.isSaving) {
                        "Saving..."
                    } else {
                        if (uiState.isEditMode) "Update Reminder" else "Save Reminder"
                    }
                )
            }
        }
    }
}
package com.example.healthguard.presentation.pills

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthguard.R
import com.example.healthguard.viewmodel.AddEditPillViewModel
import java.util.Calendar

private const val PILL_PERMISSION_PREFS = "pill_permission_prefs"
private const val KEY_XIAOMI_DIALOG_SHOWN = "xiaomi_dialog_shown"

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
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun hasShownXiaomiDialog(context: Context): Boolean {
    return context.getSharedPreferences(PILL_PERMISSION_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_XIAOMI_DIALOG_SHOWN, false)
}

private fun setShownXiaomiDialog(context: Context) {
    context.getSharedPreferences(PILL_PERMISSION_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_XIAOMI_DIALOG_SHOWN, true)
        .apply()
}

private fun openXiaomiPermissionSettings(context: Context) {
    try {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
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

    var showXiaomiPermissionDialog by remember { mutableStateOf(false) }

    val strSaved = stringResource(R.string.pill_reminder_saved)
    val strExactAlarmWarning = stringResource(R.string.pill_exact_alarm_warning)

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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val allowed = notificationManager?.canUseFullScreenIntent() == true

            if (!allowed) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }

        if (!hasShownXiaomiDialog(context)) {
            setShownXiaomiDialog(context)
            showXiaomiPermissionDialog = true
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, strSaved, Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveSuccess()
            onSavedNavigateBack()
        }
    }

    if (showXiaomiPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showXiaomiPermissionDialog = false },
            title = {
                Text(stringResource(R.string.permissions_required_title))
            },
            text = {
                Text(stringResource(R.string.permissions_required_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openXiaomiPermissionSettings(context)
                        showXiaomiPermissionDialog = false
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showXiaomiPermissionDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) {
                            stringResource(R.string.pill_edit_title)
                        } else {
                            stringResource(R.string.pill_add_title)
                        }
                    )
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
                        text = stringResource(R.string.pill_medicine_info),
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = uiState.medicineName,
                        onValueChange = viewModel::onMedicineNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.pill_medicine_name)) },
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
                        label = { Text(stringResource(R.string.pill_dosage)) },
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
                        text = stringResource(R.string.pill_schedule),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(R.string.pill_days_of_week),
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
                        text = stringResource(R.string.pill_reminder_times),
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
                        Text(stringResource(R.string.pill_add_time))
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
                        text = stringResource(R.string.pill_options),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.pill_reminder_enabled))
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
                    } else {
                        Toast.makeText(context, strExactAlarmWarning, Toast.LENGTH_LONG).show()
                        requestExactAlarmPermission(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(
                    if (uiState.isSaving) {
                        stringResource(R.string.pill_saving)
                    } else {
                        if (uiState.isEditMode) {
                            stringResource(R.string.pill_update_reminder)
                        } else {
                            stringResource(R.string.pill_save_reminder)
                        }
                    }
                )
            }
        }
    }
}
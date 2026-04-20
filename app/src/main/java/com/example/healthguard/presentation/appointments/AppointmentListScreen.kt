package com.example.healthguard.presentation.appointments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.AppointmentViewModel

private const val PREFS_NAME = "appointments_permissions"
private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"

private fun hasAskedNotifications(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_NOTIFICATIONS_ASKED, false)
}

private fun setAskedNotifications(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit {
            putBoolean(KEY_NOTIFICATIONS_ASKED, true)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    navController: NavController,
    viewModel: AppointmentViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    LaunchedEffect(Unit) {
        viewModel.loadAllAppointments()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted && !hasAskedNotifications(context)) {
                setAskedNotifications(context)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val calendar = java.util.Calendar.getInstance()

    val startOfToday = calendar.apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfToday = startOfToday + (24 * 60 * 60 * 1000)

    val todayAppointments = state.appointments.filter {
        it.timestamp in startOfToday until endOfToday
    }.sortedBy { it.timestamp }

    val upcomingAppointments = state.appointments.filter {
        it.timestamp >= endOfToday
    }.sortedBy { it.timestamp }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.appointment_list_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_appointment") }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.appointment_add_fab)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.appointments.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(R.string.appointment_empty),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (todayAppointments.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.appointment_section_today),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(items = todayAppointments, key = { it.id }) { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                onDelete = { viewModel.deleteAppointment(context, appointment) },
                                onEdit = {
                                    navController.navigate("add_appointment/${appointment.id}")
                                }
                            )
                        }
                    }

                    if (upcomingAppointments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.appointment_section_upcoming),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(items = upcomingAppointments, key = { it.id }) { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                onDelete = { viewModel.deleteAppointment(context, appointment) },
                                onEdit = {
                                    navController.navigate("add_appointment/${appointment.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
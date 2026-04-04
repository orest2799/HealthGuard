package com.example.healthguard.presentation.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.viewmodel.AppointmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    navController: NavController,
    viewModel: AppointmentViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadAllAppointments()
    }

    // --- TIME GROUPING LOGIC ---
    val calendar = java.util.Calendar.getInstance()

    // Start of Today (00:00:00)
    val startOfToday = calendar.apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    // End of Today (23:59:59)
    val endOfToday = startOfToday + (24 * 60 * 60 * 1000)

    // Filter appointments into two lists
    val todayAppointments = state.appointments.filter {
        it.timestamp in startOfToday until endOfToday
    }.sortedBy { it.timestamp }

    val upcomingAppointments = state.appointments.filter {
        it.timestamp >= endOfToday
    }.sortedBy { it.timestamp }
    // ---------------------------

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Appointments") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_appointment") }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.appointments.isEmpty() && !state.isLoading) {
                Text(
                    text = "No appointments yet.\nTap + to add your first.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: TODAY
                    if (todayAppointments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(items = todayAppointments, key = { it.id }) { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                onDelete = { viewModel.deleteAppointment(context, appointment) },
                                onEdit = { navController.navigate("add_appointment/${appointment.id}") }
                            )
                        }
                    }

                    // SECTION 2: UPCOMING
                    if (upcomingAppointments.isNotEmpty()) {
                        item {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Upcoming",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(items = upcomingAppointments, key = { it.id }) { appointment ->
                            AppointmentItem(
                                appointment = appointment,
                                onDelete = { viewModel.deleteAppointment(context, appointment) },
                                onEdit = { navController.navigate("add_appointment/${appointment.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}
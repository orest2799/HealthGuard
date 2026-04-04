package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.healthguard.data.network.pills.buildTimeKey
import com.example.healthguard.viewmodel.PillListViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillListScreen(
    viewModel: PillListViewModel,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.loadAll()
        }
    }

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    val validTodayKeys = uiState.reminders
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

    val totalToday = validTodayKeys.size
    val takenToday = uiState.takenTimeKeysToday.count { it in validTodayKeys }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pill Reminders") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add reminder"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.reminders.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "No reminders yet.\nTap + to add your first pill.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PillSummaryCard(
                            reminders = uiState.reminders,
                            takenToday = takenToday,
                            totalToday = totalToday
                        )
                    }

                    item {
                        Text(
                            text = "All Reminders",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(uiState.reminders, key = { it.id }) { reminder ->
                        PillReminderCard(
                            reminder = reminder,
                            onEditClick = { onEditClick(reminder.id) },
                            onDeleteClick = {
                                viewModel.deleteReminder(context, reminder)
                            },
                            onMarkTakenClick = { timeKey ->
                                viewModel.toggleDoseTaken(timeKey)
                            },
                            takenTimeKeysToday = uiState.takenTimeKeysToday
                        )
                    }
                }
            }
        }
    }
}
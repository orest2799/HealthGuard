package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthguard.data.network.pills.PillReminder
import java.util.Calendar

@Composable
fun PillSummaryCard(
    reminders: List<PillReminder>,
    takenToday: Int,
    totalToday: Int
) {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    val todayCount = reminders.count { reminder ->
        reminder.enabled && reminder.reminderTimes.any { today in it.daysOfWeek }
    }

    val progress = if (totalToday > 0) {
        takenToday.toFloat() / totalToday.toFloat()
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$takenToday/$totalToday doses completed",
                style = MaterialTheme.typography.headlineSmall
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Active reminders: ${reminders.count { it.enabled }}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Medicines for today: $todayCount",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthguard.data.network.pills.PillReminder
import com.example.healthguard.data.network.pills.buildTimeKey
import java.util.Calendar

@Composable
fun PillReminderCard(
    reminder: PillReminder,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkTakenClick: ((String) -> Unit)? = null,
    takenTimeKeysToday: Set<String> = emptySet()
) {
    val nextTimeKey = getNextTimeKey(reminder)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = reminder.dosage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatDays(reminder),
                        style = MaterialTheme.typography.bodySmall
                    )

                    getNextTime(reminder)?.let {
                        Text(
                            text = "Next: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reminder.reminderTimes.forEach { time ->
                    val timeKey = buildTimeKey(
                        reminderId = reminder.id,
                        hour = time.hour,
                        minute = time.minute
                    )

                    val isNext = timeKey == nextTimeKey
                    val isTaken = takenTimeKeysToday.contains(timeKey)

                    FilterChip(
                        selected = isNext || isTaken,
                        onClick = {
                            onMarkTakenClick?.invoke(timeKey)
                        },
                        label = {
                            Text(
                                text = when {
                                    isTaken -> "%02d:%02d ✓".format(time.hour, time.minute)
                                    else -> "%02d:%02d".format(time.hour, time.minute)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}


private fun getNextTimeKey(reminder: PillReminder): String? {
    val now = Calendar.getInstance()

    val upcoming = reminder.reminderTimes.mapNotNull { time ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val validToday = time.daysOfWeek.contains(now.get(Calendar.DAY_OF_WEEK))
        if (!validToday) return@mapNotNull null
        if (cal.before(now)) return@mapNotNull null

        Triple(buildTimeKey(reminder.id, time.hour, time.minute), cal, time)
    }.minByOrNull { it.second.timeInMillis }

    return upcoming?.first
}

private fun getNextTime(reminder: PillReminder): String? {
    val now = Calendar.getInstance()

    val upcoming = reminder.reminderTimes.mapNotNull { time ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val validToday = time.daysOfWeek.contains(now.get(Calendar.DAY_OF_WEEK))
        if (!validToday) return@mapNotNull null
        if (cal.before(now)) return@mapNotNull null

        cal
    }.minByOrNull { it.timeInMillis }

    return upcoming?.let {
        "%02d:%02d".format(
            it.get(Calendar.HOUR_OF_DAY),
            it.get(Calendar.MINUTE)
        )
    }
}
private fun formatDays(reminder: PillReminder): String {
    val days = reminder.reminderTimes
        .firstOrNull()
        ?.daysOfWeek
        ?.sorted()
        ?: emptyList()

    if (days.isEmpty()) return "No days selected"

    val labels = days.map {
        when (it) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "?"
        }
    }

    return labels.joinToString(", ")
}
package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun DaySelectorRow(
    selectedDays: Set<Int>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
        Calendar.SUNDAY to "Sun"
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (dayValue, label) ->
            FilterChip(
                selected = selectedDays.contains(dayValue),
                onClick = { onDayClick(dayValue) },
                label = { Text(label) }
            )
        }
    }
}
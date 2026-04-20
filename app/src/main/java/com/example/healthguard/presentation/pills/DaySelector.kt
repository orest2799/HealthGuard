package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthguard.R
import java.util.Calendar

@Composable
fun DaySelectorRow(
    selectedDays: Set<Int>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Built inside the Composable so stringResource() can be called
    val days = listOf(
        Calendar.MONDAY    to stringResource(R.string.day_mon),
        Calendar.TUESDAY   to stringResource(R.string.day_tue),
        Calendar.WEDNESDAY to stringResource(R.string.day_wed),
        Calendar.THURSDAY  to stringResource(R.string.day_thu),
        Calendar.FRIDAY    to stringResource(R.string.day_fri),
        Calendar.SATURDAY  to stringResource(R.string.day_sat),
        Calendar.SUNDAY    to stringResource(R.string.day_sun)
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
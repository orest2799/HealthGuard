package com.example.healthguard.presentation.pills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.healthguard.viewmodel.TimeEntryUi


@Composable
fun TimeChipRow(
    times: List<TimeEntryUi>,
    onRemoveTime: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        times.forEach { time ->
            AssistChip(
                onClick = { onRemoveTime(time.id) },
                label = {
                    Text(
                        text = "%02d:%02d  ✕".format(time.hour, time.minute)
                    )
                }
            )
        }
    }
}
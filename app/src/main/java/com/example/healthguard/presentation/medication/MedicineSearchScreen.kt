package com.example.healthguard.presentation.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MedicineSearchScreen(viewModel: MedicineViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val results by viewModel.results.collectAsState()

    Column {
        results.forEach { med ->
            Text(text = med.brand ?: med.generic ?: "Unknown")
            Text(text = med.strength ?: "")
            Text(text = med.form ?: "")
            Spacer(Modifier.height(8.dp))
        }
    }
}

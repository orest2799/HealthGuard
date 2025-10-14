package com.example.healthguard.presentation.medication

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthguard.R

import com.example.healthguard.domain.model.Medication
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PillsScreen() {
    val context = LocalContext.current
    var medications by remember { mutableStateOf(listOf<Medication>()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingMed by remember { mutableStateOf<Medication?>(null) }

    // Load medications from Firebase
    LaunchedEffect(Unit) {
        MedicationFirebaseStorage.fetchMedications {
            medications = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.healthguard_icon2),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Manage Your Medications",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                editingMed = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(Modifier.width(8.dp))
            Text("Add Medication")
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            items(medications, key = { it.id }) { med ->
                MedicationCardSimple(
                    med = med,
                    onEdit = {
                        editingMed = med
                        showDialog = true
                    },
                    onDelete = {
                        MedicationFirebaseStorage.deleteMedication(med.id) { success ->
                            if (success) {
                                medications = medications.filterNot { it.id == med.id }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showDialog) {
        AddOrEditMedicationDialogSimple(
            context = context,
            initialMedication = editingMed,
            onDismiss = { showDialog = false },
            onSave = { med ->
                val onSuccess = {
                    MedicationFirebaseStorage.fetchMedications {
                        medications = it
                        showDialog = false
                    }
                }
                MedicationFirebaseStorage.saveMedication(med) { if (it) onSuccess() }
            }
        )
    }
}

@Composable
fun MedicationCardSimple(med: Medication, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(med.time, style = MaterialTheme.typography.bodyLarge)
                Text(med.name, style = MaterialTheme.typography.bodyMedium)
                Text(med.dose, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun AddOrEditMedicationDialogSimple(
    context: Context,
    initialMedication: Medication?,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit
) {
    var name by remember { mutableStateOf(initialMedication?.name ?: "") }
    var dose by remember { mutableStateOf(initialMedication?.dose ?: "") }
    var time by remember {
        mutableStateOf(
            initialMedication?.time ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }

    val localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val id = initialMedication?.id ?: ""
                onSave(Medication(id, name, dose, time))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Medication Details") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dose") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            time = LocalTime.of(h, m).format(DateTimeFormatter.ofPattern("HH:mm"))
                        },
                        localTime.hour,
                        localTime.minute,
                        false
                    ).show()
                }) {
                    val displayTime = localTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                    Text("Pick Time: $displayTime")
                }
            }
        }
    )
}
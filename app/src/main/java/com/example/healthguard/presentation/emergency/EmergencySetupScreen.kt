
package com.example.healthguard.presentation.emergency

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.data.network.emergency.EmergencyContact
import com.example.healthguard.viewmodel.EmergencyViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySetupScreen(
    viewModel: EmergencyViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) {
            navController.navigate("emergency") {
                popUpTo("emergency_setup") { inclusive = true }
            }
            viewModel.clearNavigation()
        }
    }
    var firstName by remember { mutableStateOf(viewModel.selectedContact?.firstName ?: "") }
    var lastName by remember { mutableStateOf(viewModel.selectedContact?.lastName ?: "") }
    // Using takeLast(10) to strip country code if editing
    var phonePart by remember { mutableStateOf(viewModel.selectedContact?.phoneNumber?.takeLast(10) ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.selectedContact != null) "Edit Contact" else "Add Contact", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // --- First Name ---
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Last Name ---
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Phone Row ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(viewModel.selectedCountryCode, color = textColor)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.availableCountryCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    viewModel.selectedCountryCode = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = phonePart,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phonePart = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- THE UPDATED SAVE BUTTON ---
            androidx.compose.material3.Button(
                onClick = {
                    val finalPhone = viewModel.selectedCountryCode + phonePart
                    val contact = EmergencyContact(
                        firstName = firstName,
                        lastName = lastName,
                        phoneNumber = finalPhone,
                        priority = viewModel.selectedContact?.priority ?: (state.contacts.size + 1)
                    )

                    // Just call the save. The LaunchedEffect above handles the "Going Back"
                    viewModel.saveOrUpdateContact(contact)
                },
                enabled = firstName.isNotBlank() && phonePart.length == 10,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save & Complete Setup")
            }
        }
    }
}
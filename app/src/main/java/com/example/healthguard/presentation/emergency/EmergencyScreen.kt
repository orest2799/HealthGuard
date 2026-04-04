package com.example.healthguard.presentation.emergency

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.healthguard.viewmodel.EmergencyViewModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.prepareForEdit(null)
        navController.navigate("emergency_setup")
    }

    fun launchWithPermissionCheck() {
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation && hasSms) {
            viewModel.prepareForEdit(null)
            navController.navigate("emergency_setup")
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency SOS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (state.contacts.isNotEmpty()) {
                        IconButton(onClick = { navController.navigate("home") {
                            popUpTo("emergency") { inclusive = true }
                        }}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.contacts.isEmpty() && !state.isLoading) {
                // --- 1. EMPTY STATE ---
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Emergency Contacts",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                    Text(
                        "You must add at least one contact to use SOS features.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { launchWithPermissionCheck() }) {
                        Text("Add First Contact")
                    }
                }
            } else {
                // --- 2. ACTIVE SOS STATE ---
                Text(
                    "Emergency Mode Active",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE45745),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(0.3f))

                // SOS BUTTON
                Surface(
                    modifier = Modifier.size(220.dp),
                    shape = CircleShape,
                    color = Color(0xFFE45745).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.isLocationLoading = true

                                    val locationRequest = CurrentLocationRequest.Builder()
                                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                        .setDurationMillis(5000)
                                        .build()

                                    fusedLocationClient.getCurrentLocation(locationRequest, null)
                                        .addOnSuccessListener { location ->
                                            viewModel.isLocationLoading = false
                                            viewModel.startEmergencyProtocol(
                                                context,
                                                location?.latitude,
                                                location?.longitude
                                            )
                                        }
                                        .addOnFailureListener {
                                            viewModel.isLocationLoading = false
                                            viewModel.startEmergencyProtocol(context, null, null)
                                        }
                                } else {
                                    viewModel.startEmergencyProtocol(context, null, null)
                                }
                            },
                            modifier = Modifier.size(180.dp),
                            enabled = !viewModel.isLocationLoading,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE45745)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            if (viewModel.isLocationLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "SOS",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                Button(
                    onClick = { viewModel.initiateEmergencyCall(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CALL PRIORITY CONTACT NOW", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // CONTACTS LIST CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contacts (Hold to move to top)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.contacts.size < 3) {
                                IconButton(onClick = { launchWithPermissionCheck() }) {
                                    Icon(
                                        Icons.Default.Add,
                                        "Add contact",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        state.contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { viewModel.moveContactToTop(contact) }
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (contact.priority == 1) Color(0xFFE45745)
                                            else Color.Gray.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        contact.priority.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${contact.firstName} ${contact.lastName}",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteContact(contact) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Delete",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    viewModel.prepareForEdit(contact)
                                    navController.navigate("emergency_setup")
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        "Edit",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "SOS pre-fills an SMS with your location. Tap 'Call' to dial manually.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
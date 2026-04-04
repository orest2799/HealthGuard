package com.example.healthguard.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.emergency.EmergencyContact
import com.example.healthguard.data.network.emergency.EmergencyUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmergencyViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState

    var selectedContact by mutableStateOf<EmergencyContact?>(null)
    var selectedCountryCode by mutableStateOf("+30")
    val availableCountryCodes = listOf("+30", "+357", "+44", "+1", "+49")

    // This creates its own setter automatically
    var isLocationLoading by mutableStateOf(false)

    init { loadContacts() }

    fun loadContacts() {
        val currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            _uiState.update { it.copy(contacts = emptyList()) } // Clear if no user
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Fetch from the SPECIFIC current userId path
                val snapshot = database.child("users").child(currentUserId).child("emergency").get().await()
                val contacts = mutableListOf<EmergencyContact>()

                if (snapshot.exists()) {
                    snapshot.child("contacts").children.forEach { child ->
                        child.getValue(EmergencyContact::class.java)?.let { contacts.add(it) }
                    }
                }
                // Update with the NEW user's data (replaces old data)
                _uiState.update { it.copy(contacts = contacts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun moveContactToTop(contact: EmergencyContact) {
        val currentList = _uiState.value.contacts.toMutableList()
        val removed = currentList.removeAll { it.phoneNumber == contact.phoneNumber }
        if (removed) {
            currentList.add(0, contact)
            val updatedList = currentList.mapIndexed { index, item ->
                item.copy(priority = index + 1)
            }
            saveToFirebase(updatedList)
        }
    }

    fun prepareForEdit(contact: EmergencyContact?) {
        selectedContact = contact
        selectedCountryCode = "+30"
    }

    // Add the '= {}' here to make it a default empty argument
    fun saveOrUpdateContact(newContact: EmergencyContact) {
        val currentList = _uiState.value.contacts.toMutableList()

        if (selectedContact != null) {
            val index = currentList.indexOfFirst { it.phoneNumber == selectedContact!!.phoneNumber }
            if (index != -1) currentList[index] = newContact
        } else {
            if (currentList.size < 3) currentList.add(newContact)
        }

        viewModelScope.launch {
            try {
                database.child("users").child(userId).child("emergency").child("contacts")
                    .setValue(currentList).await()

                // UPDATE STATE HERE
                _uiState.update { it.copy(
                    contacts = currentList,
                    navigateBack = true // <--- Trigger the navigation
                ) }

                Log.d("EmergencyVM", "SAVE_DONE: List size is ${currentList.size}")
            } catch (e: Exception) {
                Log.e("EmergencyVM", "SAVE_ERROR: ${e.message}")
            }
        }
        selectedContact = null
    }

    // Add this helper to reset the flag
    fun clearNavigation() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    fun deleteContact(contact: EmergencyContact) {
        val currentList = _uiState.value.contacts.toMutableList()
        currentList.removeIf { it.phoneNumber == contact.phoneNumber }
        saveToFirebase(currentList)
    }

    private fun saveToFirebase(contacts: List<EmergencyContact>) {
        viewModelScope.launch {
            try {
                database.child("users").child(userId).child("emergency").child("contacts").setValue(contacts).await()
                loadContacts()
            } catch (e: Exception) { Log.e("FIREBASE", "Save failed: ${e.message}") }
        }
    }

    fun startEmergencyProtocol(context: Context, latitude: Double?, longitude: Double?) {
        val contact = _uiState.value.contacts.firstOrNull() ?: return

        // FIXED: Use the 'q=' parameter for a reliable map pin
        val locationLink = if (latitude != null && longitude != null) {
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        } else {
            "Location Unavailable (GPS signal lost)"
        }

        val message = "EMERGENCY! I need help. My location: $locationLink"

        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contact.phoneNumber}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Log.e("SOS", "SMS Failed: ${e.message}")
        }
    }

    fun initiateEmergencyCall(context: Context) {
        val contact = _uiState.value.contacts.firstOrNull() ?: return
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Log.e("SOS", "Call failed: ${e.message}")
        }
    }
}
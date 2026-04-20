package com.example.healthguard.viewmodel

// NEW: imports for backend calls
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthguard.data.network.dto.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val birthday: String = "",
    val email: String = ""
)

class UserViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val dbRoot = FirebaseDatabase.getInstance().reference

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    // backing state for sign-in status
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    // NEW: backend health status (exposed to UI)
    private val _backendStatus = MutableStateFlow("Idle")
    val backendStatus: StateFlow<String> = _backendStatus

    // Listen to FirebaseAuth changes and react
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _isSignedIn.value = (user != null)
        if (user != null) {
            loadUserData(user.uid)
        } else {
            _userProfile.value = null
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.uid?.let { loadUserData(it) }
    }

    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun markSignedIn() {
        _isSignedIn.value = true
    }

    fun markSignedOut() {
        _isSignedIn.value = false
        _userProfile.value = null
    }

    /** Public helper if you only know the UID later. */
    fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        loadUserData(uid)
    }

    /** Load profile from Realtime Database. */
    private fun loadUserData(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = dbRoot.child("users").child(uid).get().await()
                val user = snapshot.getValue(UserProfile::class.java)
                withContext(Dispatchers.Main) {
                    _userProfile.value = user
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to load user data for $uid", e)
            }
        }
    }


    fun upsertUserProfile(
        uid: String? = auth.currentUser?.uid,
        profile: UserProfile
    ) {
        if (uid == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dbRoot.child("users").child(uid).setValue(profile).await()
                withContext(Dispatchers.Main) {
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to upsert profile for $uid", e)
            }
        }
    }


    fun pingBackend() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.health.getHealth()

                val text: String = if (response.isSuccessful) {
                    response.body()?.status ?: "Empty"
                } else {
                    "Error: ${response.code()} ${response.message()}"
                }

                withContext(Dispatchers.Main) {
                    _backendStatus.value = text
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _backendStatus.value = "Error: ${e.message}"
                }
            }
        }
    }


    fun pingBackendSecure() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.health.getHealth()

                val text = if (response.isSuccessful) {
                    response.body()?.status ?: "OK"
                } else {
                    "Error: ${response.code()}"
                }

                withContext(Dispatchers.Main) {
                    _backendStatus.value = text
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _backendStatus.value = "Error: ${e.message}"
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}

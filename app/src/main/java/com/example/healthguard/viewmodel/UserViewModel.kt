package com.example.healthguard

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
import okhttp3.OkHttpClient
import okhttp3.Request

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

    /** Manually set in-memory profile (e.g., right after creating/updating it). */
    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun markSignedIn() { _isSignedIn.value = true }

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

    /** Create or update the profile in DB and cache it locally. */
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

    // ---------------------------
    //  BACKEND CALLS (NEW)
    // ---------------------------

    /**
     * Simple health ping (no auth). Uses your Retrofit service if available.
     * If you haven't added ApiClient.health yet, the "fallback" OkHttp code below still works.
     */
    fun pingBackend() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Preferred: via Retrofit service (requires HealthService + ApiClient.health)
                val retrofitOk = runCatching { ApiClient.health.getHealth() }.getOrNull()
                val text = when {
                    retrofitOk != null && retrofitOk.isSuccessful -> retrofitOk.body() ?: "Empty"
                    retrofitOk != null -> "Error: ${retrofitOk.code()} ${retrofitOk.message()}"
                    else -> {
                        // Fallback: raw OkHttp GET to /health (works even without Retrofit service)
                        val base = getBaseUrl() // keep in one place
                        OkHttpClient().newCall(
                            Request.Builder().url("${base}health").build()
                        ).execute().use { resp ->
                            if (!resp.isSuccessful) "Error: ${resp.code}" else (resp.body?.string() ?: "Empty")
                        }
                    }
                }
                withContext(Dispatchers.Main) { _backendStatus.value = text as String }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _backendStatus.value = "Error: ${e.message}" }
            }
        }
    }

    /**
     * Authenticated health ping: attaches Firebase ID token as Bearer.
     * Use this when your backend enforces authentication.
     */
    fun pingBackendSecure() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
                val token = user.getIdToken(false).await().token ?: throw IllegalStateException("No ID token")

                val base = getBaseUrl()
                val req = Request.Builder()
                    .url("${base}health")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                OkHttpClient().newCall(req).execute().use { resp ->
                    val text = if (!resp.isSuccessful) "Error: ${resp.code}" else (resp.body?.string() ?: "Empty")
                    withContext(Dispatchers.Main) { _backendStatus.value = text }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _backendStatus.value = "Auth error: ${e.message}" }
            }
        }
    }


    private fun getBaseUrl(): String {
        // 🔧 Keep these in sync with ApiClient.kt
        val useLocal = true
        val useEmulator = false  // ← Set to false for physical device

        val localEmulator = "http://10.0.2.2:8080/"
        val localDevice = "http://192.168.1.146:8080/"  // ← UPDATE if your computer IP is different
        val prodUrl = "https://healthguard-backend-192038493071.europe-west8.run.app/"

        return when {
            !useLocal -> prodUrl
            useEmulator -> localEmulator
            else -> localDevice  // ← Will now use this!
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}

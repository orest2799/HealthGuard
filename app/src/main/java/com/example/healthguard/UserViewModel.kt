package com.example.healthguard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _isSignedIn = MutableStateFlow(auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    // Listen to FirebaseAuth changes and react
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _isSignedIn.value = (user != null)
        if (user != null) {
            // Load user data whenever we get a signed-in user
            loadUserData(user.uid)
        } else {
            _userProfile.value = null
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        // If already signed in when VM is created, load immediately
        auth.currentUser?.uid?.let { loadUserData(it) }
    }

    /** Manually set in-memory profile (e.g., right after creating/updating it). */
    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    /** Mark signed-in (useful if you prefer explicit signaling from Activity). */
    fun markSignedIn() { _isSignedIn.value = true }

    /** Mark signed-out (clear local state). */
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
    fun upsertUserProfile(uid: String? = auth.currentUser?.uid, profile: UserProfile) {
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

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}

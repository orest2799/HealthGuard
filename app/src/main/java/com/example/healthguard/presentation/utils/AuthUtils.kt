package com.example.healthguard.presentation.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import com.google.firebase.auth.FirebaseAuth

/** Sign out of Firebase and clear Google sign-in state (Credential Manager). */
suspend fun signOutGoogle(context: Context) {
    // Firebase sign-out (sync)
    FirebaseAuth.getInstance().signOut()

    // Provider-side state (suspend)
    try {
        CredentialManager.create(context)
            .clearCredentialState(ClearCredentialStateRequest())
    } catch (_: Exception) { /* ignore */ }
}


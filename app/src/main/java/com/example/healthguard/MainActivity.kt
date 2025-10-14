package com.example.healthguard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.ui.theme.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

// Credential Manager + Google ID
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager
    private val userViewModel: UserViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        credentialManager = CredentialManager.create(this)

        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value
            AppTheme(darkTheme = isDarkTheme) {
                MyApp(
                    userViewModel = userViewModel,
                    themeViewModel = themeViewModel,
                    onGoogleSignIn = { signInWithGoogle() } // called from LoginScreen
                )
            }
        }
    }

    /** Entry: try login first (authorized accounts only), then fallback to full picker (signup allowed). */
    fun signInWithGoogle() = lifecycleScope.launch {
        val loggedIn = trySignIn(filterAuthorizedOnly = true)
        if (!loggedIn) {
            // No authorized account (or user cancelled auto-select) → open full picker for signup/login
            trySignIn(filterAuthorizedOnly = false)
        }
    }

    /** Optional sign-out helper */
    fun signOutAll() = lifecycleScope.launch {
        FirebaseAuth.getInstance().signOut()
        try { credentialManager.clearCredentialState(ClearCredentialStateRequest()) } catch (_: Exception) {}
        Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT).show()
    }

    /** Core sign-in with Credential Manager + Google ID */
    private suspend fun trySignIn(filterAuthorizedOnly: Boolean): Boolean {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id)) // **Web** client ID
            .setFilterByAuthorizedAccounts(filterAuthorizedOnly)          // true = login-only, false = also signup
            .setAutoSelectEnabled(true)                                   // auto-pick returning user if possible
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(this, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleId.idToken)
                true
            } else {
                false
            }
        } catch (e: GetCredentialCancellationException) {
            // User dismissed the selector → no toast, just treat as "not handled"
            false
        } catch (e: NoCredentialException) {
            // No matching credentials (e.g., none authorized when filter=true) → let caller decide fallback
            false
        } catch (e: GetCredentialException) {
            // Other credential errors (network, provider, etc.)
            Toast.makeText(this, "Sign-in failed: ${e.errorMessage ?: e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Toast.makeText(this, "Sign-in error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /** Exchange Google ID token for Firebase credential and persist profile */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    FirebaseAuth.getInstance().currentUser?.let { user ->
                        val uid = user.uid
                        val db = FirebaseDatabase.getInstance().reference
                        val profile = UserProfile(
                            firstName = user.displayName?.split(" ")?.firstOrNull().orEmpty(),
                            lastName  = user.displayName?.split(" ")?.getOrNull(1).orEmpty(),
                            email     = user.email.orEmpty(),
                            birthday  = "" // update later if you collect it
                        )
                        db.child("users").child(uid).setValue(profile)
                        userViewModel.setUserProfile(profile)
                        Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}






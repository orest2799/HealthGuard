package com.example.healthguard

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.dto.MedicineScanRequest
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.ThemeViewModel
import com.example.healthguard.viewmodel.UserProfile
import com.example.healthguard.viewmodel.UserViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager
    private val userViewModel: UserViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        // This is safe for API 26+ (it handles versions internally)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)
        testAllEndpoints()

        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value
            AppTheme(darkTheme = isDarkTheme) {
                // Ensure MyApp does not have a @RequiresApi(28) tag on its definition
                MyApp(
                    userViewModel = userViewModel,
                    themeViewModel = themeViewModel,
                    onGoogleSignIn = { signInWithGoogle() }
                )
            }
        }
    }

    /**
     * 🧪 COMPREHENSIVE BACKEND TEST
     * Verifies connectivity to Cloud Run production URL
     */
    private fun testAllEndpoints() {
        lifecycleScope.launch {
            val results = mutableListOf<String>()
            // FIX 2: Point logs to your actual Cloud Run URL
            val prodUrl = "https://medicine-backend-192038493071.us-central1.run.app/"

            try {
                Log.d("TEST", "═══════════════════════════════════════")
                Log.d("TEST", "🧪 TESTING CLOUD RUN BACKEND")
                Log.d("TEST", "📍 URL: $prodUrl")
                Log.d("TEST", "═══════════════════════════════════════")

                // TEST 1: HEALTH
                try {
                    val health = ApiClient.health.getHealth()
                    if (health.isSuccessful) {
                        Log.d("TEST", "1️⃣ Health: ✅ OK (${health.code()})")
                        results.add("✅ Health: OK")
                    } else {
                        Log.e("TEST", "1️⃣ Health: ❌ FAILED (${health.code()})")
                        results.add("❌ Health: ${health.code()}")
                    }
                } catch (e: Exception) {
                    results.add("❌ Health: Error")
                }

                // TEST 2: MEDICINE SEARCH
                try {
                    val meds = ApiClient.med.search("aspirin")
                    Log.d("TEST", "2️⃣ Med Search: ✅ Found ${meds.results.size} results")
                    results.add("✅ Med Search: OK")
                } catch (e: Exception) {
                    results.add("❌ Med Search: Error")
                }

                // TEST 3: SCAN (Note: using scanMedicine logic)
                try {
                    val scanReq = MedicineScanRequest(ocrText = "Test Scan", imageBase64 = null)
                    ApiClient.scan.saveScan(scanReq)
                    Log.d("TEST", "3️⃣ Scan Save: ✅ OK")
                    results.add("✅ Scan Save: OK")
                } catch (e: Exception) {
                    results.add("❌ Scan Save: Error")
                }

                // TEST 4: CHAT
                try {
                    val chatResp = ApiClient.chat.chat(ChatRequest(text = "Hello"))
                    Log.d("TEST", "4️⃣ Chat: ✅ OK. Reply: ${chatResp.reply.take(20)}...")
                    results.add("✅ Chat: OK")
                } catch (e: Exception) {
                    results.add("❌ Chat: Error")
                }

                // SUMMARY TOAST
                val pass = results.count { it.startsWith("✅") }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Cloud Tests: $pass/4 Passed", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("TEST", "❌ Critical connection failure: ${e.message}")
            }
        }
    }

    /** Entry: try login first (authorized accounts only), then fallback to full picker (signup allowed). */
    fun signInWithGoogle() = lifecycleScope.launch {
        val loggedIn = trySignIn(filterAuthorizedOnly = true)
        if (!loggedIn) {
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
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(filterAuthorizedOnly)
            .setAutoSelectEnabled(true)
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
            false
        } catch (e: NoCredentialException) {
            false
        } catch (e: GetCredentialException) {
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
                    val firebaseUser = FirebaseAuth.getInstance().currentUser

                    // Safety Check: Ensure user exists
                    if (firebaseUser != null) {
                        val uid = firebaseUser.uid
                        val db = FirebaseDatabase.getInstance().reference

                        // Parse names safely
                        val nameParts = firebaseUser.displayName?.split(" ")
                        val profile = UserProfile(
                            firstName = nameParts?.firstOrNull().orEmpty(),
                            lastName = nameParts?.getOrNull(1).orEmpty(),
                            email = firebaseUser.email.orEmpty(),
                            birthday = ""
                        )

                        // Save to Realtime Database
                        db.child("users").child(uid).setValue(profile)
                            .addOnSuccessListener {
                                Log.d("AUTH", "User profile synced to Firebase")
                            }

                        // Update local ViewModel state
                        userViewModel.setUserProfile(profile)

                        Toast.makeText(
                            this,
                            "Signed in as ${profile.firstName}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("AUTH", "Firebase Auth Failed", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
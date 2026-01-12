package com.example.healthguard

// Credential Manager + Google ID
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
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
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        credentialManager = CredentialManager.create(this)

        // ✅ TEST ALL ENDPOINTS
        testAllEndpoints()

        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value
            AppTheme(darkTheme = isDarkTheme) {
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
     * Tests all endpoints: health, meds, scan, chat
     */
    private fun testAllEndpoints() {
        lifecycleScope.launch {
            val results = mutableListOf<String>()

            try {
                Log.d("TEST", "═══════════════════════════════════════")
                Log.d("TEST", "🧪 TESTING ALL BACKEND ENDPOINTS")
                Log.d("TEST", "📍 Backend: http://10.0.2.2:8080/")
                Log.d("TEST", "═══════════════════════════════════════")

                // ========================================
                // TEST 1: HEALTH CHECK
                // ========================================
                Log.d("TEST", "")
                Log.d("TEST", "1️⃣ Testing Health Endpoint...")
                Log.d("TEST", "   GET /health")

                try {
                    val health = ApiClient.health.getHealth()
                    if (health.isSuccessful) {
                        val body = health.body() ?: "Empty"
                        Log.d("TEST", "   ✅ SUCCESS: $body")
                        Log.d("TEST", "   Status: ${health.code()}")
                        results.add("✅ Health: OK")
                    } else {
                        Log.e("TEST", "   ❌ FAILED: ${health.code()}")
                        results.add("❌ Health: ${health.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("TEST", "   ❌ ERROR: ${e.message}", e)
                    results.add("❌ Health: ${e.message}")
                }

                // ========================================
                // TEST 2: MEDICINE SEARCH
                // ========================================
                Log.d("TEST", "")
                Log.d("TEST", "2️⃣ Testing Medicine Search...")
                Log.d("TEST", "   GET /meds/search?q=aspirin")

                try {
                    val meds = ApiClient.med.search("aspirin")
                    Log.d("TEST", "   ✅ SUCCESS: Found ${meds.results.size} medicines")
                    Log.d("TEST", "   Query: ${meds.query}")
                    if (meds.results.isNotEmpty()) {
                        Log.d("TEST", "   First result: ${meds.results[0].brand}")
                    }
                    results.add("✅ Med Search: ${meds.results.size} results")
                } catch (e: Exception) {
                    Log.e("TEST", "   ❌ ERROR: ${e.message}", e)
                    results.add("❌ Med Search: ${e.message}")
                }

                // ========================================
                // TEST 3: SCAN SAVE
                // ========================================
                Log.d("TEST", "")
                Log.d("TEST", "3️⃣ Testing Scan Save...")
                Log.d("TEST", "   POST /api/vision/scan")

                try {
                    val scanReq = MedicineScanRequest(
                        ocrText = "Panadol 500mg tablets",
                        imageBase64 = null
                    )
                    val scanResult = ApiClient.scan.saveScan(scanReq)
                    Log.d("TEST", "   ✅ SUCCESS: Scan saved!")
                    Log.d("TEST", "   Scan ID: ${scanResult.id}")
                    Log.d("TEST", "   Saved: ${scanResult.saved}")
                    results.add("✅ Scan: ${scanResult.id}")
                } catch (e: Exception) {
                    Log.e("TEST", "   ❌ ERROR: ${e.message}", e)
                    results.add("❌ Scan: ${e.message}")
                }

                // ========================================
                // TEST 4: CHAT
                // ========================================
                Log.d("TEST", "")
                Log.d("TEST", "4️⃣ Testing Chat...")
                Log.d("TEST", "   POST /chat")

                try {
                    val chatReq = ChatRequest(text = "What is aspirin?")
                    val chatResp = ApiClient.chat.chat(chatReq)
                    Log.d("TEST", "   ✅ SUCCESS: Got chat response")
                    Log.d("TEST", "   Session ID: ${chatResp.sessionId}")
                    Log.d("TEST", "   Reply length: ${chatResp.reply.length} chars")
                    Log.d("TEST", "   Reply preview: ${chatResp.reply.take(100)}...")
                    results.add("✅ Chat: Working")
                } catch (e: Exception) {
                    Log.e("TEST", "   ❌ ERROR: ${e.message}", e)
                    results.add("❌ Chat: ${e.message}")
                }

                // ========================================
                // TEST 5: GALINOS (Optional - might fail)
                // ========================================

                // ========================================
                // SUMMARY
                // ========================================
                Log.d("TEST", "")
                Log.d("TEST", "═══════════════════════════════════════")
                Log.d("TEST", "📊 TEST RESULTS SUMMARY")
                Log.d("TEST", "═══════════════════════════════════════")
                results.forEach { result ->
                    Log.d("TEST", "   $result")
                }
                Log.d("TEST", "═══════════════════════════════════════")

                // Show toast with summary
                val successCount = results.count { it.startsWith("✅") }
                val totalTests = 4 // Health, Meds, Scan, Chat (excluding Galinos)

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Backend Tests: $successCount/$totalTests passed\nCheck Logcat for details",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("TEST", "❌ CRITICAL ERROR during testing", e)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Testing failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
                    FirebaseAuth.getInstance().currentUser?.let { user ->
                        val uid = user.uid
                        val db = FirebaseDatabase.getInstance().reference
                        val profile = UserProfile(
                            firstName = user.displayName?.split(" ")?.firstOrNull().orEmpty(),
                            lastName = user.displayName?.split(" ")?.getOrNull(1).orEmpty(),
                            email = user.email.orEmpty(),
                            birthday = ""
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
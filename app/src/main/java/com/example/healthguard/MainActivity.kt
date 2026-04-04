package com.example.healthguard

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.healthguard.data.network.appointments.AppointmentNotificationHelper
import com.example.healthguard.data.network.appointments.AppointmentRestoreManager
import com.example.healthguard.data.network.dto.ApiClient
import com.example.healthguard.data.network.dto.ChatRequest
import com.example.healthguard.data.network.steps.Injection
import com.example.healthguard.data.network.steps.StepSensorManager
import com.example.healthguard.data.network.steps.StepTrackingService
import com.example.healthguard.domain.model.pills.notifications.PillNotificationHelper
import com.example.healthguard.domain.model.pills.notifications.PillReminderRestoreManager
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.ChatViewModel
import com.example.healthguard.viewmodel.StepViewModel
import com.example.healthguard.viewmodel.StepViewModelFactory
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
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager
    private val userViewModel: UserViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    private val stepViewModel: StepViewModel by viewModels {
        // Get the current uid at ViewModel creation time.
        // If no user is logged in yet, we pass "" and the repo will use
        // an anonymous prefs file — it will be replaced on first login.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        StepViewModelFactory(Injection.provideStepRepository(this, uid))
    }

    private lateinit var stepSensorManager: StepSensorManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional: handle granted/denied */ }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            stepSensorManager.startListening()
        } else {
            Toast.makeText(this, "Step tracking requires permission", Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }
    }

    private fun ensureExactAlarmPermission() {
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun restorePillRemindersIfLoggedIn() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                PillReminderRestoreManager().restoreAll(this@MainActivity)
                Log.d("PILL_DEBUG", "Reminders restored for user: ${currentUser.uid}")
            } catch (e: Exception) {
                Log.e("PILL_DEBUG", "Failed to restore reminders", e)
            }
        }
    }

    private fun restoreAppointmentsIfLoggedIn() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                AppointmentRestoreManager().restoreAll(this@MainActivity)
                Log.d("APPOINTMENT_DEBUG", "Appointments restored for user: ${currentUser.uid}")
            } catch (e: Exception) {
                Log.e("APPOINTMENT_DEBUG", "Failed to restore appointments", e)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ensureExactAlarmPermission()
        ensureFullScreenIntentPermission()
        credentialManager = CredentialManager.create(this)

        PillNotificationHelper(this).createNotificationChannel()
        AppointmentNotificationHelper(this).createChannel()

        stepSensorManager = StepSensorManager(this) { totalSteps ->
            stepViewModel.onStepDetected(totalSteps)
        }
        StepTrackingService.start(this)
        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value
            AppTheme(darkTheme = isDarkTheme) {
                MyApp(
                    userViewModel = userViewModel,
                    themeViewModel = themeViewModel,
                    chatVm = chatViewModel,
                    stepViewModel = stepViewModel,
                    onGoogleSignIn = { signInWithGoogle() }
                )
            }
        }

        lifecycleScope.launch {
            requestNotificationPermissionIfNeeded()
            restorePillRemindersIfLoggedIn()
            restoreAppointmentsIfLoggedIn()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                stepSensorManager.startListening()
            }

            if (BuildConfig.DEBUG) {
                testAllEndpoints()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::stepSensorManager.isInitialized) {
            stepSensorManager.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::stepSensorManager.isInitialized) {
            stepSensorManager.stopListening()
        }
    }

    private fun testAllEndpoints() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            Log.d("TEST", "🧪 STARTING BACKEND TESTS")
            try {
                val response = ApiClient.health.getHealth()
                if (response.isSuccessful) Log.d("TEST", "✅ Health: ${response.body()?.status}")
            } catch (e: Exception) {
                Log.e("TEST", "❌ Health Connection Error: ${e.message}")
            }
            try {
                val response = ApiClient.chat.chat(ChatRequest(text = "Hello"))
                if (response.isSuccessful) Log.d("TEST", "✅ Chat Success: ${response.body()?.reply}")
            } catch (e: Exception) {
                Log.e("TEST", "❌ Chat Exception: ${e.message}")
            }
        }
    }

    // ── Sign in ───────────────────────────────────────────────────────────────

    fun signInWithGoogle() = lifecycleScope.launch {
        if (!trySignIn(filterAuthorizedOnly = true)) {
            trySignIn(filterAuthorizedOnly = false)
        }
    }

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
            } else false
        } catch (e: Exception) { false }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    firebaseUser?.let { user ->


                        Injection.reset()

                        val nameParts = user.displayName?.split(" ")
                        val profile = UserProfile(
                            firstName = nameParts?.firstOrNull().orEmpty(),
                            lastName = nameParts?.getOrNull(1).orEmpty(),
                            email = user.email.orEmpty(),
                            birthday = ""
                        )
                        FirebaseDatabase.getInstance().reference
                            .child("users")
                            .child(user.uid)
                            .child("profile")
                            .setValue(profile)

                        userViewModel.setUserProfile(profile)

                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            PillReminderRestoreManager().restoreAll(this@MainActivity)
                            AppointmentRestoreManager().restoreAll(this@MainActivity)
                        }


                        stepViewModel.loadDashboardData()

                        Toast.makeText(this, "Welcome ${profile.firstName}!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }



}
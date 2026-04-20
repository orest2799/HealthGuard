package com.example.healthguard.presentation.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.data.network.steps.Injection
import com.example.healthguard.data.network.steps.StepTrackingService
import com.example.healthguard.viewmodel.LanguageViewModel
import com.example.healthguard.viewmodel.ThemeViewModel
import com.example.healthguard.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    themeViewModel: ThemeViewModel,
    languageViewModel: LanguageViewModel
) {
    val user by userViewModel.userProfile.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current   // moved to top level so toggle can use it

    val snackbarHostState = remember { SnackbarHostState() }
    val backendStatus by userViewModel.backendStatus.collectAsState()

    // Pre-read for use in snackbar callbacks
    val strConnected = stringResource(R.string.home_connected)

    LaunchedEffect(Unit) {
        userViewModel.loadUserData()
        userViewModel.pingBackend()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    100
                )
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }


        StepTrackingService.start(context)
    }

    LaunchedEffect(backendStatus) {
        when {
            backendStatus.equals("OK", ignoreCase = true) ->
                snackbarHostState.showSnackbar(strConnected)
            backendStatus.startsWith("Error") ->
                snackbarHostState.showSnackbar(backendStatus)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.healthguard_icon2),
                            contentDescription = "Logo",
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_placeholder_user),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                colorFilter = ColorFilter.tint(
                                    if (isDarkTheme) Color.White else Color.Black
                                )
                            )
                        }

                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_placeholder_user),
                                    contentDescription = "User Photo",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.home_settings),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )

                            HorizontalDivider()


                            Text(
                                text = stringResource(R.string.home_appearance),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { themeViewModel.toggleTheme(false) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = !isDarkTheme,
                                    onClick = { themeViewModel.toggleTheme(false) }
                                )
                                Text(
                                    stringResource(R.string.home_light_theme),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { themeViewModel.toggleTheme(true) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = isDarkTheme,
                                    onClick = { themeViewModel.toggleTheme(true) }
                                )
                                Text(
                                    stringResource(R.string.home_dark_theme),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            HorizontalDivider()


                            Text(
                                text = stringResource(R.string.home_language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        languageViewModel.toggleLanguage(context as Activity)
                                        showSettings = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // No ic_language needed — the toggle label itself is the visual cue
                                Text(stringResource(R.string.lang_toggle))
                            }

                            HorizontalDivider()


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        StepTrackingService.stop(context)
                                        Injection.reset()
                                        FirebaseAuth.getInstance().signOut()
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_logout),
                                    contentDescription = stringResource(R.string.home_sign_out)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.home_sign_out))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var showWelcome by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(3000)
                    showWelcome = false
                }

                AnimatedVisibility(
                    visible = showWelcome,
                    exit = fadeOut(animationSpec = tween(600))
                ) {
                    Text(
                        text = stringResource(R.string.home_welcome, user?.firstName ?: ""),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HomeActionButton(
                            icon = R.drawable.ic_camera,
                            label = stringResource(R.string.home_scan_medicine),
                            onClick = { navController.navigate("camera") },
                            modifier = Modifier.weight(1f)
                        )
                        HomeActionButton(
                            icon = R.drawable.ic_pill,
                            label = stringResource(R.string.home_todays_pills),
                            onClick = { navController.navigate("pills") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HomeActionButton(
                            icon = R.drawable.ic_calendar,
                            label = stringResource(R.string.home_appointments),
                            onClick = { navController.navigate("appointment_list") },
                            modifier = Modifier.weight(1f)
                        )
                        HomeActionButton(
                            icon = R.drawable.ic_stats,
                            label = stringResource(R.string.home_health_stats),
                            onClick = { navController.navigate("stats") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.navigate("emergency") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE45745))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sos),
                        contentDescription = "SOS",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.home_emergency), color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { navController.navigate("gallery") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Gallery",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.home_gallery))
                }
            }
        }
    }
}
package com.example.healthguard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthguard.data.repo.MedicineRepository
import com.example.healthguard.presentation.appointments.AddAppointmentScreen
import com.example.healthguard.presentation.appointments.AppointmentListScreen
import com.example.healthguard.presentation.camera.CameraScreen
import com.example.healthguard.presentation.chat.ChatScreen
import com.example.healthguard.presentation.chat.MatchBubbleOverlay
import com.example.healthguard.presentation.emergency.EmergencyScreen
import com.example.healthguard.presentation.emergency.EmergencySetupScreen
import com.example.healthguard.presentation.gallery.GalleryScreen
import com.example.healthguard.presentation.home.HomeScreen
import com.example.healthguard.presentation.login.LoginScreen
import com.example.healthguard.presentation.pills.AddEditPillScreen
import com.example.healthguard.presentation.pills.PillListScreen
import com.example.healthguard.presentation.signup.SignUpScreen
import com.example.healthguard.presentation.splash.SplashScreen
import com.example.healthguard.presentation.stats.StatsScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.AddEditPillViewModel
import com.example.healthguard.viewmodel.AppointmentViewModel
import com.example.healthguard.viewmodel.ChatViewModel
import com.example.healthguard.viewmodel.EmergencyViewModel
import com.example.healthguard.viewmodel.GalleryViewModel
import com.example.healthguard.viewmodel.LanguageViewModel
import com.example.healthguard.viewmodel.MatchOverlayViewModel
import com.example.healthguard.viewmodel.PillListViewModel
import com.example.healthguard.viewmodel.StepViewModel
import com.example.healthguard.viewmodel.ThemeViewModel
import com.example.healthguard.viewmodel.UserViewModel

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MyApp(
    userViewModel: UserViewModel,
    themeViewModel: ThemeViewModel,
    chatVm: ChatViewModel,
    stepViewModel: StepViewModel,
    onGoogleSignIn: () -> Unit,
    languageViewModel: LanguageViewModel,
    galleryViewModel: GalleryViewModel
) {
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val navController = rememberNavController()


    LaunchedEffect(Unit) {
        languageViewModel.language
            .collect { lang ->
                chatVm.syncAppLanguage(lang)
            }
    }

    val emergencyViewModel: EmergencyViewModel = viewModel()
    val emergencyState by emergencyViewModel.uiState.collectAsState()

    LaunchedEffect(emergencyState.contacts, emergencyState.isLoading) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        if (!emergencyState.isLoading &&
            emergencyState.contacts.isEmpty() &&
            currentRoute != "splash" &&
            currentRoute != "login" &&
            currentRoute != "signup" &&
            currentRoute != "emergency_setup") {

            navController.navigate("emergency_setup") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    val medicineRepo = MedicineRepository()
    val overlayVm: MatchOverlayViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MatchOverlayViewModel(medicineRepo) as T
            }
        }
    )

    AppTheme(darkTheme = isDarkTheme) {
        NavHost(navController = navController, startDestination = "splash") {

            composable("splash") { SplashScreen(navController, userViewModel) }

            composable("login") {
                LoginScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    languageViewModel = languageViewModel,
                    onGoogleSignIn = onGoogleSignIn
                )
            }

            composable("signup") {
                SignUpScreen(navController, userViewModel, onGoogleSignIn, languageViewModel)
            }

            composable("home") {
                HomeScreen(navController, userViewModel, themeViewModel, languageViewModel)
            }

            composable("emergency") {
                EmergencyScreen(viewModel = emergencyViewModel, navController = navController)
            }

            composable("emergency_setup") {
                EmergencySetupScreen(viewModel = emergencyViewModel, navController = navController)
            }

            composable("pills") {
                val listViewModel: PillListViewModel = viewModel()
                PillListScreen(
                    viewModel = listViewModel,
                    onAddClick = { navController.navigate("pills/add") },
                    onEditClick = { id -> navController.navigate("pills/edit/$id") }
                )
            }

            composable("pills/add") {
                val pillViewModel: AddEditPillViewModel = viewModel()
                AddEditPillScreen(viewModel = pillViewModel) { navController.popBackStack() }
            }

            composable("pills/edit/{reminderId}") { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getString("reminderId").orEmpty()
                val pillViewModel: AddEditPillViewModel = viewModel()
                LaunchedEffect(reminderId) { pillViewModel.loadReminder(reminderId) }
                AddEditPillScreen(viewModel = pillViewModel) { navController.popBackStack() }
            }

            composable("camera") {
                val context = LocalContext.current
                CameraScreen(navController = navController) { file ->
                    chatVm.processScannedImageAndSave(file, context)
                    navController.navigate("chat/new")
                }
            }

            composable("gallery") {
                val context = LocalContext.current
                GalleryScreen(
                    navController = navController,
                    chatVm = chatVm,
                    onImageSelected = { file ->
                        chatVm.processScannedImageAndSave(file, context)
                        navController.navigate("chat/new?title=Ανάλυση Εικόνας")
                    },
                    galleryViewModel = galleryViewModel
                )
            }

            composable("stats") { StatsScreen(viewModel = stepViewModel) }

            composable("appointment_list") {
                val vm: AppointmentViewModel = viewModel()
                AppointmentListScreen(navController = navController, viewModel = vm)
            }

            composable("add_appointment") {
                val vm: AppointmentViewModel = viewModel()
                AddAppointmentScreen(viewModel = vm, navController = navController)
            }

            composable(
                route = "add_appointment/{appointmentId}",
                arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("appointmentId")
                val vm: AppointmentViewModel = viewModel()
                AddAppointmentScreen(viewModel = vm, navController = navController, appointmentId = id)
            }

            composable(
                route = "chat/{sessionId}?title={title}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = "Φαρμακευτικός Βοηθός"
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                val rawTitle = backStackEntry.arguments?.getString("title")
                val decodedTitle = remember(rawTitle) {
                    try { java.net.URLDecoder.decode(rawTitle ?: "Φαρμακευτικός Βοηθός", "UTF-8") }
                    catch (e: Exception) { rawTitle ?: "Φαρμακευτικός Βοηθός" }
                }

                LaunchedEffect(sessionId) {
                    if (sessionId != null && sessionId != "new") {
                        chatVm.startWithSession(sessionId, decodedTitle)
                    }
                }

                ChatScreen(
                    vm = chatVm,
                    startWithSessionId = if (sessionId == "new") null else sessionId,
                    startTitle = decodedTitle,
                    languageViewModel = languageViewModel
                )
            }
        }

        MatchBubbleOverlay(
            vm = overlayVm,
            onOpenChat = { sessionId, title ->
                chatVm.clearChat()
                val encoded = java.net.URLEncoder.encode(title, "UTF-8")
                navController.navigate("chat/$sessionId?title=$encoded")
            }
        )
    }
}
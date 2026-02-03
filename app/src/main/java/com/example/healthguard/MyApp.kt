package com.example.healthguard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthguard.data.repo.MedicineRepository
import com.example.healthguard.presentation.camera.CameraScreen
import com.example.healthguard.presentation.camera.GalleryScreen
import com.example.healthguard.presentation.chat.ChatScreen
import com.example.healthguard.presentation.chat.MatchBubbleOverlay
import com.example.healthguard.presentation.home.HomeScreen
import com.example.healthguard.presentation.login.LoginScreen
import com.example.healthguard.presentation.medication.PillsScreen
import com.example.healthguard.presentation.signup.SignUpScreen
import com.example.healthguard.presentation.splash.SplashScreen
import com.example.healthguard.presentation.stats.StatsScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.viewmodel.ChatViewModel
import com.example.healthguard.viewmodel.MatchOverlayViewModel
import com.example.healthguard.viewmodel.ThemeViewModel
import com.example.healthguard.viewmodel.UserViewModel
import java.io.File

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MyApp(
    userViewModel: UserViewModel,
    themeViewModel: ThemeViewModel,
    onGoogleSignIn: () -> Unit
) {
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val navController = rememberNavController()

    // 🔹 Create the chat & overlay VMs once at the root
    val chatVm: ChatViewModel = viewModel()
    val medicineRepo = MedicineRepository()
    val overlayVm: MatchOverlayViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatchOverlayViewModel(medicineRepo) as T
            }
        }
    )
    AppTheme(darkTheme = isDarkTheme) {

        // 🔹 Render your nav graph
        NavHost(navController = navController, startDestination = "splash") {

            composable("splash") {
                SplashScreen(navController, userViewModel)
            }

            composable("login") {
                LoginScreen(navController, userViewModel, onGoogleSignIn)
            }

            composable("signup") {
                SignUpScreen(navController, userViewModel, onGoogleSignIn)
            }

            composable("home") {

                HomeScreen(navController, userViewModel, themeViewModel)
            }

            // Μέσα στο MyApp.kt, στο NavHost
            composable("camera") {
                CameraScreen(
                    navController = navController,
                    onImageCaptured = { file ->
                        chatVm.processScannedImage(file) // Η συνάρτηση πλέον χρησιμοποιείται!
                        navController.navigate("chat/new")
                    }
                )
            }


            composable("gallery") {
                GalleryScreen(
                    navController = navController,
                    onImageSelected = { file: File -> // Τώρα το GalleryScreen επιστρέφει το αρχείο
                        chatVm.processScannedImage(file)
                        navController.navigate("chat/new?title=Ανάλυση Εικόνας")
                    }
                )
            }
            composable("calendar") { CalendarScreen() }
            composable("stats") { StatsScreen() }
            composable("emergency") { EmergencyScreen() }
            composable("pills") { PillsScreen() }

            // 🔹 NEW: Chat route (sessionId + optional title)
            composable(
                route = "chat/{sessionId}?title={title}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Φαρμακευτικός Βοηθός" }
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                val titleArg  = backStackEntry.arguments?.getString("title")

                // Καλούμε τη συνάρτηση μόνο αν δεν πρόκειται για τη λέξη-κλειδί "new"
                if (sessionId != null && sessionId != "new") {
                    chatVm.startWithSession(sessionId, titleArg)
                }

                ChatScreen(
                    vm = chatVm,
                    startWithSessionId = if (sessionId == "new") null else sessionId,
                    startTitle = titleArg
                )
            }
        }

        // 🔹 Always-on overlay (draggable bubble) rendered above your screens
        MatchBubbleOverlay(
            vm = overlayVm,
            onOpenChat = { sessionId, title ->
                chatVm.clearChat()
                navController.navigate("chat/$sessionId?title=${java.net.URLEncoder.encode(title, "UTF-8")}")
            }
        )
    }
}



@Composable
fun EmergencyScreen() {

}



@Composable
fun CalendarScreen() {

}
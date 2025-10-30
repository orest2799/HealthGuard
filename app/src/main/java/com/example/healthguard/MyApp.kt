package com.example.healthguard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthguard.presentation.camera.CameraScreen
import com.example.healthguard.presentation.camera.GalleryScreen
import com.example.healthguard.presentation.chat.ChatScreen
import com.example.healthguard.presentation.chat.ChatViewModel
import com.example.healthguard.presentation.chat.MatchBubbleOverlay
import com.example.healthguard.presentation.chat.MatchOverlayViewModel
import com.example.healthguard.presentation.home.HomeScreen
import com.example.healthguard.presentation.login.LoginScreen
import com.example.healthguard.presentation.medication.PillsScreen
import com.example.healthguard.presentation.signup.SignUpScreen
import com.example.healthguard.presentation.splash.SplashScreen
import com.example.healthguard.presentation.stats.StatsScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.ui.theme.ThemeViewModel

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
    val overlayVm: MatchOverlayViewModel = viewModel()

    AppTheme(darkTheme = isDarkTheme) {

        // 🔹 Render your nav graph
        NavHost(navController = navController, startDestination = "splash") {

            composable("splash") {
                SplashScreen(navController, userViewModel)
            }

            composable("login")  {
                LoginScreen(navController, userViewModel, onGoogleSignIn)
            }

            composable("signup") {
                SignUpScreen(navController, userViewModel, onGoogleSignIn)
            }

            composable("home") {
                HomeScreen(navController, userViewModel, themeViewModel)
            }

            composable("camera") {
                // You can call overlayVm.show(...) from inside CameraScreen when OCR match is found
                CameraScreen(navController = navController,overlayVm = overlayVm)
            }

            composable("gallery") { GalleryScreen(navController) }
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

                // Seed the VM so ChatScreen knows which session to use
                if (sessionId != null) {
                    chatVm.startWithSession(sessionId, titleArg)
                }

                ChatScreen(
                    vm = chatVm,
                    startWithSessionId = sessionId,
                    startTitle = titleArg
                )
            }
        }

        // 🔹 Always-on overlay (draggable bubble) rendered above your screens
        MatchBubbleOverlay(
            vm = overlayVm,
            onOpenChat = { sessionId, title ->
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
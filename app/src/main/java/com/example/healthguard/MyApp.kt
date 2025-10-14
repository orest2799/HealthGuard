package com.example.healthguard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthguard.presentation.camera.CameraScreen
import com.example.healthguard.presentation.camera.GalleryScreen
import com.example.healthguard.presentation.home.HomeScreen


import com.example.healthguard.presentation.login.LoginScreen
import com.example.healthguard.presentation.medication.PillsScreen
import com.example.healthguard.presentation.signup.SignUpScreen
import com.example.healthguard.presentation.splash.SplashScreen
import com.example.healthguard.presentation.stats.StatsScreen
import com.example.healthguard.ui.theme.AppTheme
import com.example.healthguard.ui.theme.ThemeViewModel

@Composable
fun MyApp(
    userViewModel: UserViewModel,
    themeViewModel: ThemeViewModel,
    onGoogleSignIn: () -> Unit
) {
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val navController = rememberNavController()

    AppTheme(darkTheme = isDarkTheme) {
        NavHost(navController = navController, startDestination = "splash") {

            composable("splash") {
                SplashScreen(navController, userViewModel)
            }

            composable("login")  { LoginScreen(navController, userViewModel, onGoogleSignIn) }

            composable("signup") { SignUpScreen(navController, userViewModel, onGoogleSignIn) }


            composable("home") {
                HomeScreen(navController, userViewModel, themeViewModel)
            }

            composable("camera") { CameraScreen(navController) }
            composable("gallery") { GalleryScreen(navController) }
            composable("calendar") { CalendarScreen() }
            composable("stats") { StatsScreen() }
            composable("emergency") { EmergencyScreen() }
            composable("pills") { PillsScreen() }
        }
    }
}






@Composable
fun EmergencyScreen() {

}



@Composable
fun CalendarScreen() {

}
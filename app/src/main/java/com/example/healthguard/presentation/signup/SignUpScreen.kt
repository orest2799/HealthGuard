package com.example.healthguard.presentation.signup

import android.app.DatePickerDialog

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle

import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


import com.example.healthguard.R
import com.example.healthguard.UserProfile
import com.example.healthguard.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar


@Composable
fun SignUpScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    onGoogleSignIn: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseDatabase.getInstance().reference }
    val blue = colorResource(id = R.color.blue)

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Inline errors
    var firstNameErr by remember { mutableStateOf<String?>(null) }
    var lastNameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var birthdayErr by remember { mutableStateOf<String?>(null) }
    var passwordErr by remember { mutableStateOf<String?>(null) }
    var confirmErr by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Navigate when signed in (covers Google flow too)
    val isSignedIn by userViewModel.isSignedIn.collectAsState()
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            navController.navigate("home") {
                popUpTo("signup") { inclusive = true }
            }
        }
    }

    // Date picker
    val calendar = remember { Calendar.getInstance() }
    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                birthday = "%02d/%02d/%04d".format(day, month + 1, year)
                birthdayErr = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun validate(): Boolean {
        var ok = true
        firstNameErr = null
        lastNameErr = null
        emailErr = null
        birthdayErr = null
        passwordErr = null
        confirmErr = null

        if (firstName.isBlank()) { firstNameErr = "First name required"; ok = false }
        if (lastName.isBlank()) { lastNameErr = "Last name required"; ok = false }

        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = "Enter a valid email"; ok = false
        }

        if (birthday.isBlank()) {
            // Optional: make birthday required; if not, remove this.
            birthdayErr = "Please select your birthday"; ok = false
        }

        val regex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+=<>?]).{8,}$")
        if (!regex.matches(password)) {
            passwordErr = "8+ chars, 1 capital, 1 number, 1 symbol"
            ok = false
        }
        if (confirmPassword != password) {
            confirmErr = "Passwords do not match"
            ok = false
        }
        return ok
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_background))
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.healthguard_icon1),
            contentDescription = "HealthGuard Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it; firstNameErr = null },
            label = { Text("First Name") },
            isError = firstNameErr != null,
            supportingText = { if (firstNameErr != null) Text(firstNameErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it; lastNameErr = null },
            label = { Text("Last Name") },
            isError = lastNameErr != null,
            supportingText = { if (lastNameErr != null) Text(lastNameErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailErr = null },
            label = { Text("Email") },
            isError = emailErr != null,
            supportingText = { if (emailErr != null) Text(emailErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Birthday: readOnly + clickable with a calendar icon
        OutlinedTextField(
            value = birthday,
            onValueChange = {},
            readOnly = true,
            label = { Text("Birthday") },
            trailingIcon = {
                IconButton(onClick = showDatePicker) { Icon(Icons.Default.DateRange, null) }
            },
            isError = birthdayErr != null,
            supportingText = { if (birthdayErr != null) Text(birthdayErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { showDatePicker() },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordErr = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordErr != null,
            supportingText = { if (passwordErr != null) Text(passwordErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmErr = null },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmErr != null,
            supportingText = { if (confirmErr != null) Text(confirmErr!!) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (!validate()) return@Button
                val cleanEmail = email.trim()

                auth.createUserWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            val profile = UserProfile(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                birthday = birthday,
                                email = cleanEmail
                            )
                            // Save to DB and update VM
                            db.child("users").child(uid).setValue(profile)
                                .addOnSuccessListener {
                                    userViewModel.setUserProfile(profile)
                                    userViewModel.markSignedIn()
                                    Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    // Even if DB write fails, user is created; you can retry later
                                    userViewModel.setUserProfile(profile)
                                    userViewModel.markSignedIn()
                                }
                        } else {
                            Toast.makeText(context, "User ID is null after signup", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        val msg = if (e is FirebaseAuthUserCollisionException)
                            "An account already exists with this email."
                        else e.localizedMessage ?: "Sign-up failed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = blue)
        ) {
            Text("Sign Up")
        }

        Spacer(Modifier.height(16.dp))

        // Google sign-up/sign-in uses the same flow; delegate to Activity
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Sign up with Google", color = Color.Black)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(buildAnnotatedString {
                append("Already have an account? ")
                withStyle(SpanStyle(color = blue)) { append("Login") }
            })
        }

        Spacer(Modifier.height(24.dp))
    }
}












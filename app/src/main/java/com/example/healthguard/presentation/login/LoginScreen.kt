package com.example.healthguard.presentation.login


import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    onGoogleSignIn: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passError by remember { mutableStateOf<String?>(null) }

    // Forgot password dialog state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailError by remember { mutableStateOf<String?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseDatabase.getInstance().reference }
    val scrollState = rememberScrollState()
    val lightBlue = colorResource(id = R.color.blue)


    val isSignedIn by userViewModel.isSignedIn.collectAsStateWithLifecycle()
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    fun validate(): Boolean {
        var ok = true
        emailError = null
        passError = null
        if (email.isBlank()) { emailError = "Email required"; ok = false }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email"; ok = false
        }
        if (password.isBlank()) { passError = "Password required"; ok = false }
        return ok
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetEmail = ""
                resetEmailError = null
            },
            title = {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetEmailError = null
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        isError = resetEmailError != null,
                        supportingText = { if (resetEmailError != null) Text(resetEmailError!!) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black,
                            focusedBorderColor = lightBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        resetEmailError = null

                        // Validate email
                        if (resetEmail.isBlank()) {
                            resetEmailError = "Email required"
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                            resetEmailError = "Enter a valid email"
                            return@Button
                        }

                        // Send password reset email
                        auth.sendPasswordResetEmail(resetEmail.trim())
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Password reset email sent! Check your inbox.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showForgotPasswordDialog = false
                                resetEmail = ""
                                resetEmailError = null
                            }
                            .addOnFailureListener { e ->
                                val errorMsg = when {
                                    e.message?.contains("no user record", ignoreCase = true) == true ->
                                        "No account found with this email"
                                    else -> e.localizedMessage ?: "Failed to send reset email"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lightBlue)
                ) {
                    Text("Send Reset Link", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetEmail = ""
                        resetEmailError = null
                    }
                ) {
                    Text("Cancel", color = lightBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.light_background))
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.healthguard_icon1),
                contentDescription = "HealthGuard Logo",
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
            )
        }

        Text(
            text = "Login to Your Account",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError!!) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = lightBlue,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passError = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            isError = passError != null,
            supportingText = { if (passError != null) Text(passError!!) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = lightBlue,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Forgot Password Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { showForgotPasswordDialog = true }) {
                Text(
                    "Forgot Password?",
                    color = lightBlue,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Button(
            onClick = {
                if (!validate()) return@Button
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener {
                        // Optionally warm the profile; navigation happens via isSignedIn observer.
                        auth.currentUser?.uid?.let { uid ->
                            db.child("users").child(uid).get()
                                .addOnSuccessListener { snap ->
                                    val first = snap.child("firstName").value?.toString().orEmpty()
                                    Toast.makeText(context, "Welcome $first!", Toast.LENGTH_SHORT).show()
                                    userViewModel.markSignedIn()
                                }
                                .addOnFailureListener {
                                    userViewModel.markSignedIn()
                                }
                        } ?: run { userViewModel.markSignedIn() }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Login failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = lightBlue)
        ) { Text("Login", color = Color.White) }

        Spacer(Modifier.height(20.dp))

        Text("or continue with", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Continue with Google", color = Color(0xFF3C4043), fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate("signup") }) {
            Text(buildAnnotatedString {
                append("Don't have an account? ")
                withStyle(SpanStyle(color = lightBlue)) { append("Sign Up") }
            })
        }
    }
}
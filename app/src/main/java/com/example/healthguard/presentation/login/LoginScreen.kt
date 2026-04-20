package com.example.healthguard.presentation.login

import android.app.Activity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.LanguageViewModel
import com.example.healthguard.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun LoginScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    languageViewModel: LanguageViewModel,
    onGoogleSignIn: () -> Unit
) {
    // context is only used inside non-Composable callbacks (Toast, Firebase listeners)
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passError by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailError by remember { mutableStateOf<String?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseDatabase.getInstance().reference }
    val scrollState = rememberScrollState()
    val lightBlue = colorResource(id = R.color.blue)

    // Read strings once for use inside non-Composable callbacks
    val strEmailRequired  = stringResource(R.string.login_email_required)
    val strEmailInvalid   = stringResource(R.string.login_email_invalid)
    val strPassRequired   = stringResource(R.string.login_password_required)
    val strLoginFailed    = stringResource(R.string.login_failed)
    val strWelcome        = stringResource(R.string.login_welcome)
    val strForgotNoAcct   = stringResource(R.string.forgot_no_account)
    val strForgotFailed   = stringResource(R.string.forgot_failed)
    val strForgotSuccess  = stringResource(R.string.forgot_success)

    val isSignedIn by userViewModel.isSignedIn.collectAsStateWithLifecycle()
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // validate() runs in a click callback — uses pre-read strings, not context.getString()
    fun validate(): Boolean {
        var ok = true
        emailError = null
        passError = null
        if (email.isBlank()) {
            emailError = strEmailRequired; ok = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = strEmailInvalid; ok = false
        }
        if (password.isBlank()) { passError = strPassRequired; ok = false }
        return ok
    }

    // ── Forgot Password Dialog ─────────────────────────────────────────────
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetEmail = ""
                resetEmailError = null
            },
            title = {
                Text(stringResource(R.string.forgot_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.forgot_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it; resetEmailError = null },
                        label = { Text(stringResource(R.string.login_email)) },
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
                        if (resetEmail.isBlank()) {
                            resetEmailError = strEmailRequired
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                            resetEmailError = strEmailInvalid
                            return@Button
                        }
                        // Firebase callback — context.getString() is correct here
                        auth.sendPasswordResetEmail(resetEmail.trim())
                            .addOnSuccessListener {
                                Toast.makeText(context, strForgotSuccess, Toast.LENGTH_LONG).show()
                                showForgotPasswordDialog = false
                                resetEmail = ""
                                resetEmailError = null
                            }
                            .addOnFailureListener { e ->
                                val msg = when {
                                    e.message?.contains("no user record", ignoreCase = true) == true ->
                                        strForgotNoAcct
                                    else -> e.localizedMessage ?: strForgotFailed
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lightBlue)
                ) {
                    Text(stringResource(R.string.forgot_send), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    resetEmail = ""
                    resetEmailError = null
                }) {
                    Text(stringResource(R.string.forgot_cancel), color = lightBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ── Screen ─────────────────────────────────────────────────────────────
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


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    languageViewModel.toggleLanguage(context as Activity)
                }
            ) {
                Text(
                    text = stringResource(R.string.lang_toggle),
                    color = lightBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // Logo
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.healthguard_icon1),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f)
            )
        }

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text(stringResource(R.string.login_email)) },
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
            label = { Text(stringResource(R.string.login_password)) },
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { showForgotPasswordDialog = true }) {
                Text(
                    stringResource(R.string.login_forgot_password),
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
                        auth.currentUser?.uid?.let { uid ->
                            db.child("users").child(uid).get()
                                .addOnSuccessListener { snap ->
                                    val first = snap.child("firstName").value?.toString().orEmpty()
                                    Toast.makeText(context, strWelcome.format(first), Toast.LENGTH_SHORT).show()
                                    userViewModel.markSignedIn()
                                }
                                .addOnFailureListener { userViewModel.markSignedIn() }
                        } ?: run { userViewModel.markSignedIn() }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, strLoginFailed + it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = lightBlue)
        ) { Text(stringResource(R.string.login_button), color = Color.White) }

        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.login_or_continue), style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.login_google),
                color = Color(0xFF3C4043),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate("signup") }) {
            Text(buildAnnotatedString {
                append(stringResource(R.string.login_no_account))
                withStyle(SpanStyle(color = lightBlue)) {
                    append(stringResource(R.string.login_sign_up))
                }
            })
        }
    }
}
package com.example.healthguard.presentation.signup

import android.app.Activity
import android.app.DatePickerDialog
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthguard.R
import com.example.healthguard.viewmodel.LanguageViewModel
import com.example.healthguard.viewmodel.UserProfile
import com.example.healthguard.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

@Composable
fun SignUpScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    onGoogleSignIn: () -> Unit,
    languageViewModel: LanguageViewModel
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

    var firstNameErr by remember { mutableStateOf<String?>(null) }
    var lastNameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var birthdayErr by remember { mutableStateOf<String?>(null) }
    var passwordErr by remember { mutableStateOf<String?>(null) }
    var confirmErr by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Pre-read strings for use inside non-Composable callbacks
    val strFirstNameRequired = stringResource(R.string.signup_first_name_required)
    val strLastNameRequired  = stringResource(R.string.signup_last_name_required)
    val strEmailInvalid      = stringResource(R.string.signup_email_invalid)
    val strBirthdayRequired  = stringResource(R.string.signup_birthday_required)
    val strPasswordWeak      = stringResource(R.string.signup_password_rules)
    val strPasswordMismatch  = stringResource(R.string.signup_passwords_no_match)
    val strAccountCreated    = stringResource(R.string.signup_success)
    val strEmailCollision    = stringResource(R.string.signup_email_collision)
    val strUidNull           = stringResource(R.string.signup_uid_null)

    val isSignedIn by userViewModel.isSignedIn.collectAsState()
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            navController.navigate("home") {
                popUpTo("signup") { inclusive = true }
            }
        }
    }

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
        firstNameErr = null; lastNameErr = null; emailErr = null
        birthdayErr = null; passwordErr = null; confirmErr = null

        if (firstName.isBlank()) { firstNameErr = strFirstNameRequired; ok = false }
        if (lastName.isBlank())  { lastNameErr  = strLastNameRequired;  ok = false }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = strEmailInvalid; ok = false
        }
        if (birthday.isBlank()) { birthdayErr = strBirthdayRequired; ok = false }
        val regex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+=<>?]).{8,}$")
        if (!regex.matches(password)) { passwordErr = strPasswordWeak; ok = false }
        if (confirmPassword != password) { confirmErr = strPasswordMismatch; ok = false }
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

        // ── Language toggle (top-right, matches LoginScreen exactly) ──────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { languageViewModel.toggleLanguage(context as Activity) }
            ) {
                Text(
                    text = stringResource(R.string.lang_toggle),
                    color = blue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.healthguard_icon1),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.signup_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it; firstNameErr = null },
            label = { Text(stringResource(R.string.signup_first_name)) },
            isError = firstNameErr != null,
            supportingText = { if (firstNameErr != null) Text(firstNameErr!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it; lastNameErr = null },
            label = { Text(stringResource(R.string.signup_last_name)) },
            isError = lastNameErr != null,
            supportingText = { if (lastNameErr != null) Text(lastNameErr!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailErr = null },
            label = { Text(stringResource(R.string.login_email)) },
            isError = emailErr != null,
            supportingText = { if (emailErr != null) Text(emailErr!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = birthday,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.signup_birthday)) },
            trailingIcon = {
                IconButton(onClick = showDatePicker) {
                    Icon(Icons.Default.DateRange, null, tint = blue)
                }
            },
            isError = birthdayErr != null,
            supportingText = { if (birthdayErr != null) Text(birthdayErr!!) },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker() },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordErr = null },
            label = { Text(stringResource(R.string.login_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordErr != null,
            supportingText = { if (passwordErr != null) Text(passwordErr!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmErr = null },
            label = { Text(stringResource(R.string.signup_confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmErr != null,
            supportingText = { if (confirmErr != null) Text(confirmErr!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Gray, focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                cursorColor = Color.Black, focusedBorderColor = blue, unfocusedBorderColor = Color.Gray
            )
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
                            db.child("users").child(uid).setValue(profile)
                                .addOnSuccessListener {
                                    userViewModel.setUserProfile(profile)
                                    userViewModel.markSignedIn()
                                    Toast.makeText(context, strAccountCreated, Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    userViewModel.setUserProfile(profile)
                                    userViewModel.markSignedIn()
                                }
                        } else {
                            Toast.makeText(context, strUidNull, Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        val msg = if (e is FirebaseAuthUserCollisionException)
                            strEmailCollision
                        else e.localizedMessage ?: strEmailCollision
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = blue)
        ) {
            Text(stringResource(R.string.signup_button), color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.signup_google), color = Color.Black)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(buildAnnotatedString {
                append(stringResource(R.string.signup_has_account))
                withStyle(SpanStyle(color = blue)) {
                    append(stringResource(R.string.signup_login))
                }
            })
        }

        Spacer(Modifier.height(24.dp))
    }
}
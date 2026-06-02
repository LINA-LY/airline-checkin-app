package com.airline.checkin.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airline.checkin.R
import com.airline.checkin.ui.AppColors
import com.airline.checkin.ui.AppDimens
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class AuthMode { SignIn, SignUp }

// ─── Helpers: parse Firebase error codes into human messages ───────────────

private fun mapAuthError(raw: String?): String {
    if (raw == null) return "Something went wrong. Please try again."
    return when {
        raw.contains("wrong-password", ignoreCase = true) ||
                raw.contains("INVALID_PASSWORD", ignoreCase = true) ||
                raw.contains("invalid-credential", ignoreCase = true) ->
            "Incorrect password. Please try again."

        raw.contains("user-not-found", ignoreCase = true) ||
                raw.contains("USER_NOT_FOUND", ignoreCase = true) ->
            "No account found with that email address."

        raw.contains("email-already-in-use", ignoreCase = true) ||
                raw.contains("EMAIL_EXISTS", ignoreCase = true) ->
            "An account with this email already exists. Try signing in."

        raw.contains("weak-password", ignoreCase = true) ->
            "Password must be at least 6 characters."

        raw.contains("invalid-email", ignoreCase = true) ->
            "Please enter a valid email address."

        raw.contains("too-many-requests", ignoreCase = true) ->
            "Too many attempts. Please wait a moment and try again."

        raw.contains("network", ignoreCase = true) ->
            "Network error. Check your connection and try again."

        else -> "Something went wrong. Please try again."
    }
}

// ─── Welcome / Auth Screen ───────────────────────────────────────────────────

@Composable
fun WelcomeScreen(
    onAuthSuccess: () -> Unit,
    onProfileRequired: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showGoogleError by remember { mutableStateOf(false) }
    val webClientId = remember { context.getString(R.string.default_web_client_id) }
    var authMode by remember { mutableStateOf(AuthMode.SignIn) }

    // Form fields
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Inline validation state (shown on submit attempt only)
    var submitted by remember { mutableStateOf(false) }

    val passwordMismatch = authMode == AuthMode.SignUp &&
            submitted && confirmPassword.isNotBlank() && password != confirmPassword

    val googleSignInClient = remember(webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) showGoogleError = true
            else viewModel.signInWithGoogle(idToken)
        } catch (e: ApiException) {
            showGoogleError = true
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) { onAuthSuccess(); viewModel.clearStatus() }
    }
    LaunchedEffect(uiState.requiresProfile) {
        if (uiState.requiresProfile) { onProfileRequired(); viewModel.clearStatus() }
    }

    // Reset submitted flag when switching modes
    LaunchedEffect(authMode) { submitted = false }

    val canSubmit = when (authMode) {
        AuthMode.SignIn -> email.isNotBlank() && password.isNotBlank()
        AuthMode.SignUp -> firstName.isNotBlank() && lastName.isNotBlank() &&
                email.isNotBlank() && phone.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(56.dp))

        // --- App Logo ---
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Title
        Text(
            text = if (authMode == AuthMode.SignIn) "Welcome back" else "Create account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Gray900
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (authMode == AuthMode.SignIn)
                "Sign in to continue your journey"
            else
                "Join us and start planning your trip",
            fontSize = 14.sp,
            color = AppColors.Gray500
        )

        Spacer(Modifier.height(28.dp))

        // Google button
        OutlinedButton(
            onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            enabled = !uiState.isLoading && webClientId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            shape = RoundedCornerShape(AppDimens.radiusLarge),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray900)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.Primary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Continue with Google", fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Gray300)
            Text("  or  ", color = AppColors.Gray500, fontSize = 13.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Gray300)
        }

        Spacer(Modifier.height(20.dp))

        // Form fields
        if (authMode == AuthMode.SignUp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "First name",
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.weight(1f),
                    error = if (submitted && firstName.isBlank()) "Required" else null
                )
                AppTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Last name",
                    modifier = Modifier.weight(1f),
                    error = if (submitted && lastName.isBlank()) "Required" else null
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        AppTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            error = if (submitted && email.isBlank()) "Required" else null
        )
        Spacer(Modifier.height(12.dp))

        if (authMode == AuthMode.SignUp) {
            AppTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone number",
                icon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone,
                error = if (submitted && phone.isBlank()) "Required" else null
            )
            Spacer(Modifier.height(12.dp))
        }

        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            icon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = AppColors.Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            error = if (submitted && password.isBlank()) "Required" else null
        )

        if (authMode == AuthMode.SignUp) {
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm password",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                error = if (passwordMismatch) "Passwords do not match" else null
            )
        }

        if (authMode == AuthMode.SignIn) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { }) {
                    Text("Forgot password?", color = AppColors.Primary, fontSize = 13.sp)
                }
            }
        }

        // Server-side error message
        uiState.error?.let { raw ->
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = mapAuthError(raw))
        }

        if (showGoogleError) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = "Google sign-in failed. Please try again.")
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                submitted = true
                if (!canSubmit) return@Button
                if (authMode == AuthMode.SignUp && password != confirmPassword) return@Button
                if (authMode == AuthMode.SignIn) {
                    viewModel.signIn(email.trim(), password)
                } else {
                    viewModel.register(email.trim(), password, firstName.trim(), lastName.trim(), phone.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(AppDimens.radiusFull),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    text = if (authMode == AuthMode.SignIn) "Sign In" else "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (authMode == AuthMode.SignIn) "Don't have an account? " else "Already have an account? ",
                color = AppColors.Gray500,
                fontSize = 14.sp
            )
            Text(
                text = if (authMode == AuthMode.SignIn) "Sign up" else "Sign in",
                color = AppColors.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    authMode = if (authMode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
                    submitted = false
                }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Complete Profile Screen ──────────────────────────────────────────────────

@Composable
fun CompleteProfileScreen(
    onProfileSaved: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) { onProfileSaved(); viewModel.clearStatus() }
    }

    val canSubmit = firstName.isNotBlank() && lastName.isNotBlank() && phone.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Text("Complete profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("Add your details to finish signing up.", fontSize = 14.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "First name",
                modifier = Modifier.weight(1f),
                error = if (submitted && firstName.isBlank()) "Required" else null
            )
            AppTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Last name",
                modifier = Modifier.weight(1f),
                error = if (submitted && lastName.isBlank()) "Required" else null
            )
        }
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone number",
            icon = Icons.Outlined.Phone,
            keyboardType = KeyboardType.Phone,
            error = if (submitted && phone.isBlank()) "Required" else null
        )

        uiState.error?.let {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = mapAuthError(it))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                submitted = true
                if (canSubmit) viewModel.saveProfile(firstName.trim(), lastName.trim(), phone.trim())
            },
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(AppDimens.radiusFull),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Save & Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    error: String? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 14.sp) },
            leadingIcon = icon?.let {
                { Icon(it, contentDescription = null, tint = if (error != null) AppColors.Error else AppColors.Gray500, modifier = Modifier.size(18.dp)) }
            },
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(AppDimens.radiusLarge),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Gray300,
                errorBorderColor = AppColors.Error,
                focusedLabelColor = AppColors.Primary,
                cursorColor = AppColors.Primary
            ),
            isError = error != null
        )
        if (error != null) {
            Spacer(Modifier.height(2.dp))
            Text(error, color = AppColors.Error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMedium))
            .background(AppColors.ErrorLight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(AppColors.Error, RoundedCornerShape(AppDimens.radiusFull)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(message, color = AppColors.Error, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SuccessBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMedium))
            .background(AppColors.SuccessLight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(AppColors.Success, RoundedCornerShape(AppDimens.radiusFull)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(message, color = AppColors.Success, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}
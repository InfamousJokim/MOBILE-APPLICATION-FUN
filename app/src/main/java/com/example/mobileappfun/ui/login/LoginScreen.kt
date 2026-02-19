package com.example.mobileappfun.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileappfun.data.db.UserEntity

/**
 * Root composable for the Login / Register screen.
 *
 * Collects [LoginUiState] from [LoginViewModel] and delegates all events
 * (field changes, button presses) back to the ViewModel — a strict application
 * of the **Unidirectional Data Flow** (UDF) pattern required by Compose MVVM.
 *
 * Navigation is handled by [LoginActivity], which observes [LoginUiState.authenticatedUser].
 * This composable never navigates directly; it only raises events.
 *
 * @param viewModel The [LoginViewModel] providing state and handling business logic.
 * @param onAuthSuccess Callback invoked when the user successfully authenticates.
 *                      The caller ([LoginActivity]) uses this to start [MainActivity].
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onAuthSuccess: (UserEntity) -> Unit
) {
    // Collect the StateFlow as Compose State — triggers recomposition on change
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when auth succeeds — LaunchedEffect ensures it fires exactly once
    LaunchedEffect(uiState.authenticatedUser) {
        uiState.authenticatedUser?.let { onAuthSuccess(it) }
    }

    // Full-screen gradient background matching the game's neon aesthetic
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D2B), Color(0xFF1A1A4E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        LoginCard(
            uiState = uiState,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = if (uiState.isRegisterMode) viewModel::register else viewModel::login,
            onToggleMode = viewModel::toggleMode
        )
    }
}

/**
 * The central card UI containing all login/register form elements.
 *
 * Extracted into its own composable for reusability and to keep [LoginScreen]
 * focused on state collection and navigation logic.
 *
 * @param uiState         Current snapshot of the login form state.
 * @param onUsernameChange Callback for username field changes.
 * @param onPasswordChange Callback for password field changes.
 * @param onSubmit         Callback for the primary CTA button press.
 * @param onToggleMode     Callback for switching between Login/Register modes.
 */
@Composable
private fun LoginCard(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "Brain Rot Knowledge",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7EB8F7),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (uiState.isRegisterMode) "Create Account" else "Welcome Back",
                fontSize = 16.sp,
                color = Color(0xFFB0B8D4),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // ── Username Field ────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = loginTextFieldColors(),
                isError = uiState.errorMessage != null
            )

            // ── Password Field ────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }
                ),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            if (passwordVisible) "Hide" else "Show",
                            color = Color(0xFF7EB8F7),
                            fontSize = 12.sp
                        )
                    }
                },
                colors = loginTextFieldColors(),
                isError = uiState.errorMessage != null
            )

            // ── Error Banner ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Primary CTA Button ────────────────────────────────────────────
            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = if (uiState.isRegisterMode) "Create Account" else "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Toggle Mode Link ──────────────────────────────────────────────
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (uiState.isRegisterMode)
                        "Already have an account? Log In"
                    else
                        "New player? Create Account",
                    color = Color(0xFF7EB8F7),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Centralised color scheme for all [OutlinedTextField]s on the login screen,
 * ensuring consistent neon-on-dark styling without repetition.
 */
@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFB0B8D4),
    focusedBorderColor = Color(0xFF3D5AFE),
    unfocusedBorderColor = Color(0xFF3D3D6B),
    focusedLabelColor = Color(0xFF7EB8F7),
    unfocusedLabelColor = Color(0xFF6B6B9A),
    cursorColor = Color(0xFF7EB8F7)
)

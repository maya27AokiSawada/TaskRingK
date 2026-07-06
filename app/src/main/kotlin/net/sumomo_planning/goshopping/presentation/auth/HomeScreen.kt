package net.sumomo_planning.goshopping.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Auth home screen: sign-in / sign-up toggle with validation. */
@Composable
fun HomeScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate away when user is authenticated
    LaunchedEffect(uiState.currentUser) {
        if (uiState.currentUser != null) onAuthSuccess()
    }

    // Show error / success messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        HomeScreenContent(
            uiState = uiState,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onSendPasswordReset = viewModel::sendPasswordResetEmail,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun HomeScreenContent(
    uiState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, displayName: String) -> Unit,
    onSendPasswordReset: (email: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Inline validation errors
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    fun validate(): Boolean {
        emailError = when {
            email.isBlank() -> "メールアドレスを入力してください"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "メールアドレスの形式が正しくありません"
            else -> null
        }
        passwordError = when {
            password.isBlank() -> "パスワードを入力してください"
            password.length < 6 -> "パスワードは6文字以上にしてください"
            else -> null
        }
        if (isSignUp) {
            displayNameError = when {
                displayName.isBlank() -> "表示名を入力してください"
                displayName.length > 30 -> "表示名は30文字以内にしてください"
                else -> null
            }
        }
        return emailError == null && passwordError == null &&
            (!isSignUp || displayNameError == null)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.88f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GoShopping",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (isSignUp) "アカウントを作成" else "サインイン",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(8.dp))

            // Display name field — sign-up only
            AnimatedVisibility(visible = isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; displayNameError = null },
                    label = { Text("表示名") },
                    isError = displayNameError != null,
                    supportingText = displayNameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("メールアドレス") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = { Text("パスワード") },
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "パスワードを隠す" else "パスワードを表示",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // Submit button
            Button(
                onClick = {
                    if (validate()) {
                        focusManager.clearFocus()
                        if (isSignUp) onSignUp(email, password, displayName)
                        else onSignIn(email, password)
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .height(20.dp)
                            .padding(end = 8.dp),
                    )
                }
                Text(if (isSignUp) "アカウントを作成" else "サインイン")
            }

            // Toggle sign-in / sign-up
            TextButton(
                onClick = {
                    isSignUp = !isSignUp
                    emailError = null
                    passwordError = null
                    displayNameError = null
                },
            ) {
                Text(
                    text = if (isSignUp) "すでにアカウントをお持ちですか？ サインイン"
                    else "アカウントをお持ちでないですか？ 新規登録",
                    textAlign = TextAlign.Center,
                )
            }

            // Password reset — sign-in mode only
            AnimatedVisibility(visible = !isSignUp) {
                TextButton(onClick = { showResetDialog = true }) {
                    Text("パスワードを忘れた場合")
                }
            }
        }

        if (showResetDialog) {
            PasswordResetDialog(
                onConfirm = { resetEmail ->
                    onSendPasswordReset(resetEmail)
                    showResetDialog = false
                },
                onDismiss = { showResetDialog = false },
            )
        }
    }
}

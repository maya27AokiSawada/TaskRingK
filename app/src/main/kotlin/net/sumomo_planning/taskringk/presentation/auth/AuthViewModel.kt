package net.sumomo_planning.taskringk.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SendPasswordResetEmailUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignInUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignOutUseCase
import net.sumomo_planning.taskringk.domain.usecase.auth.SignUpUseCase

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: AuthUser? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthStateUseCase()
            .onEach { user -> _uiState.update { it.copy(currentUser = user) } }
            .launchIn(viewModelScope)
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            signUpUseCase(email, password, displayName)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "サインアップに失敗しました")
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            signInUseCase(email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "サインインに失敗しました")
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signOutUseCase()
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "サインアウトに失敗しました")
                    }
                }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            sendPasswordResetEmailUseCase(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "パスワードリセットメールを送信しました")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "メール送信に失敗しました")
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

package net.sumomo_planning.taskringk.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.Notification
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.notification.MarkAllNotificationsAsReadUseCase
import net.sumomo_planning.taskringk.domain.usecase.notification.MarkNotificationAsReadUseCase
import net.sumomo_planning.taskringk.domain.usecase.notification.ObserveUnreadNotificationsUseCase

data class NotificationUiState(
    val currentUser: AuthUser? = null,
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeUnreadNotificationsUseCase: ObserveUnreadNotificationsUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val markAllNotificationsAsReadUseCase: MarkAllNotificationsAsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var notificationsJob: Job? = null

    init {
        observeAuthStateUseCase()
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    startObservingNotifications(user.uid)
                } else {
                    notificationsJob?.cancel()
                    _uiState.update { it.copy(notifications = emptyList(), isLoading = false) }
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun startObservingNotifications(userId: String) {
        notificationsJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        notificationsJob = observeUnreadNotificationsUseCase(userId)
            .onEach { notifications ->
                _uiState.update {
                    it.copy(
                        notifications = notifications.sortedByDescending { item -> item.createdAt },
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
            .catch {
                _uiState.update {
                    it.copy(
                        notifications = emptyList(),
                        isLoading = false,
                        errorMessage = "通知の取得に失敗しました",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            markNotificationAsReadUseCase(notificationId)
        }
    }

    fun markAllAsRead() {
        val userId = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            markAllNotificationsAsReadUseCase(userId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
package net.sumomo_planning.taskringk.presentation.group

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
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.CreateGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.DeleteGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.LeaveGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase

data class SharedGroupUiState(
    val groups: List<SharedGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: AuthUser? = null,
)

@HiltViewModel
class SharedGroupViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeGroupsUseCase: ObserveGroupsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedGroupUiState())
    val uiState: StateFlow<SharedGroupUiState> = _uiState.asStateFlow()

    private var groupsJob: Job? = null

    init {
        observeAuthStateUseCase()
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    startObservingGroups(user.uid)
                } else {
                    groupsJob?.cancel()
                    _uiState.update { it.copy(groups = emptyList()) }
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun startObservingGroups(uid: String) {
        groupsJob?.cancel()
        groupsJob = observeGroupsUseCase(uid)
            .onEach { groups ->
                _uiState.update { it.copy(groups = groups, isLoading = false) }
            }
            .catch {
                _uiState.update { it.copy(errorMessage = "グループ取得に失敗しました", isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun createGroup(groupName: String) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            createGroupUseCase(
                groupName = groupName,
                ownerUid = user.uid,
                ownerDisplayName = user.displayName ?: user.uid,
                ownerEmail = user.email ?: "",
            ).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "グループ作成に失敗しました",
                        )
                    }
                },
            )
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteGroupUseCase(groupId).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "グループ削除に失敗しました",
                        )
                    }
                },
            )
        }
    }

    fun leaveGroup(groupId: String, memberId: String) {
        val uid = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            leaveGroupUseCase(groupId, uid, memberId).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "グループ離脱に失敗しました",
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

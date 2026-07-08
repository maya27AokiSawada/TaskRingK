package net.sumomo_planning.taskringk.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
import net.sumomo_planning.taskringk.core.common.DeviceIdService
import net.sumomo_planning.taskringk.core.network.NetworkMonitor
import net.sumomo_planning.taskringk.domain.model.AuthUser
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.domain.usecase.auth.ObserveAuthStateUseCase
import net.sumomo_planning.taskringk.domain.usecase.group.ObserveGroupsUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.AddItemUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.CreateListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.DeleteListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.ObserveListUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.ObserveListsByGroupUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.RemoveItemUseCase
import net.sumomo_planning.taskringk.domain.usecase.list.UpdateItemUseCase

data class SharedListUiState(
    val currentUser: AuthUser? = null,
    val groups: List<SharedGroup> = emptyList(),
    val selectedGroupId: String? = null,
    val lists: List<SharedList> = emptyList(),
    val selectedListId: String? = null,
    val currentList: SharedList? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOnline: Boolean = true,
)

@HiltViewModel
class SharedListViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeGroupsUseCase: ObserveGroupsUseCase,
    private val observeListsByGroupUseCase: ObserveListsByGroupUseCase,
    private val observeListUseCase: ObserveListUseCase,
    private val createListUseCase: CreateListUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val removeItemUseCase: RemoveItemUseCase,
    private val deviceIdService: DeviceIdService,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedListUiState())
    val uiState: StateFlow<SharedListUiState> = _uiState.asStateFlow()

    private var groupsJob: Job? = null
    private var listsJob: Job? = null
    private var listDetailJob: Job? = null

    init {
        observeAuthStateUseCase()
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    startObservingGroups(user.uid)
                } else {
                    stopAllObservations()
                    _uiState.update {
                        it.copy(
                            groups = emptyList(),
                            selectedGroupId = null,
                            lists = emptyList(),
                            selectedListId = null,
                            currentList = null,
                        )
                    }
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)

        networkMonitor.isOnlineFlow
            .onEach { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
            .catch { /* non-fatal */ }
            .launchIn(viewModelScope)
    }

    private fun startObservingGroups(uid: String) {
        groupsJob?.cancel()
        groupsJob = observeGroupsUseCase(uid)
            .onEach { groups ->
                val selectedId = _uiState.value.selectedGroupId
                    ?.let { id -> groups.firstOrNull { it.groupId == id }?.groupId }
                    ?: groups.firstOrNull()?.groupId
                _uiState.update { it.copy(groups = groups) }
                if (selectedId != _uiState.value.selectedGroupId) {
                    selectGroup(selectedId)
                } else if (selectedId != null && listsJob == null) {
                    startObservingLists(selectedId)
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    fun selectGroup(groupId: String?) {
        _uiState.update {
            it.copy(
                selectedGroupId = groupId,
                lists = emptyList(),
                selectedListId = null,
                currentList = null,
            )
        }
        listsJob?.cancel()
        listsJob = null
        listDetailJob?.cancel()
        listDetailJob = null

        if (groupId != null) startObservingLists(groupId)
    }

    private fun startObservingLists(groupId: String) {
        listsJob?.cancel()
        listsJob = observeListsByGroupUseCase(groupId)
            .onEach { lists ->
                val selectedId = _uiState.value.selectedListId
                    ?.let { id -> lists.firstOrNull { it.listId == id }?.listId }
                    ?: lists.firstOrNull()?.listId
                _uiState.update { it.copy(lists = lists) }
                if (selectedId != _uiState.value.selectedListId) {
                    selectList(selectedId)
                }
            }
            .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .launchIn(viewModelScope)
    }

    fun selectList(listId: String?) {
        _uiState.update { it.copy(selectedListId = listId, currentList = null) }
        listDetailJob?.cancel()
        listDetailJob = null

        val groupId = _uiState.value.selectedGroupId ?: return
        if (listId != null) {
            listDetailJob = observeListUseCase(groupId, listId)
                .onEach { list -> _uiState.update { it.copy(currentList = list) } }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .launchIn(viewModelScope)
        }
    }

    // ---- List CRUD ----

    fun createList(listName: String) {
        val user = _uiState.value.currentUser ?: return
        val group = _uiState.value.groups
            .firstOrNull { it.groupId == _uiState.value.selectedGroupId } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            createListUseCase(
                groupId = group.groupId,
                groupName = group.groupName,
                listName = listName,
                ownerUid = user.uid,
            ).onSuccess { list ->
                _uiState.update { it.copy(isLoading = false) }
                selectList(list.listId)
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun deleteList(listId: String) {
        val groupId = _uiState.value.selectedGroupId ?: return
        viewModelScope.launch {
            deleteListUseCase(groupId, listId).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // ---- Item CRUD ----

    fun addItem(name: String, quantity: Int = 1, deadline: Instant? = null) {
        val user = _uiState.value.currentUser ?: return
        val groupId = _uiState.value.selectedGroupId ?: return
        val listId = _uiState.value.selectedListId ?: return

        val memberId = user.uid // use uid as memberId for signed-in user
        val item = SharedItem(
            itemId = deviceIdService.generateItemId(),
            name = name,
            quantity = quantity,
            memberId = memberId,
            registeredDate = Instant.now(),
            deadline = deadline,
        )
        viewModelScope.launch {
            addItemUseCase(groupId, listId, item).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun togglePurchased(item: SharedItem) {
        val groupId = _uiState.value.selectedGroupId ?: return
        val listId = _uiState.value.selectedListId ?: return
        val updated = item.copy(
            isPurchased = !item.isPurchased,
            purchaseDate = if (!item.isPurchased) Instant.now() else null,
        )
        viewModelScope.launch {
            updateItemUseCase(groupId, listId, updated).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateItem(item: SharedItem) {
        val groupId = _uiState.value.selectedGroupId ?: return
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            updateItemUseCase(groupId, listId, item).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun removeItem(itemId: String) {
        val groupId = _uiState.value.selectedGroupId ?: return
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            removeItemUseCase(groupId, listId, itemId).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun stopAllObservations() {
        groupsJob?.cancel()
        groupsJob = null
        listsJob?.cancel()
        listsJob = null
        listDetailJob?.cancel()
        listDetailJob = null
    }
}

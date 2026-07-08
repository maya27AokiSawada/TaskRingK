package net.sumomo_planning.taskringk.presentation.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.sumomo_planning.taskringk.domain.model.GroupType
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedGroupRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedGroupScreen(
    modifier: Modifier = Modifier,
    viewModel: SharedGroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<SharedGroup?>(null) }
    var groupToLeave by remember { mutableStateOf<SharedGroup?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("グループ管理") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "グループを作成")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.groups.isEmpty() -> {
                    EmptyGroupsPlaceholder(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    ) {
                        items(
                            items = uiState.groups,
                            key = { it.groupId },
                        ) { group ->
                            GroupCard(
                                group = group,
                                currentUid = uiState.currentUser?.uid ?: "",
                                currentUserEmail = uiState.currentUser?.email ?: "",
                                onDeleteRequested = { groupToDelete = group },
                                onLeaveRequested = { groupToLeave = group },
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createGroup(name)
                showCreateDialog = false
            },
        )
    }

    groupToDelete?.let { group ->
        ConfirmDeleteDialog(
            groupName = group.groupName,
            onDismiss = { groupToDelete = null },
            onConfirm = {
                viewModel.deleteGroup(group.groupId)
                groupToDelete = null
            },
        )
    }

    groupToLeave?.let { group ->
        val currentEmail = uiState.currentUser?.email ?: ""
        val memberId = group.members.find { it.contact == currentEmail }?.memberId
        ConfirmLeaveDialog(
            groupName = group.groupName,
            onDismiss = { groupToLeave = null },
            onConfirm = {
                if (memberId != null) {
                    viewModel.leaveGroup(group.groupId, memberId)
                }
                groupToLeave = null
            },
        )
    }
}

// ---- Private composables ----

@Composable
private fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "グループがありません",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "+ ボタンからグループを作成してください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun GroupCard(
    group: SharedGroup,
    currentUid: String,
    currentUserEmail: String,
    onDeleteRequested: () -> Unit,
    onLeaveRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOwner = group.ownerUid == currentUid
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (group.groupType == GroupType.SHOPPING)
                    Icons.Default.ShoppingCart else Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.groupName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${group.members.size} 人のメンバー",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (isOwner) {
                        // Owner can delete the group (§3-4)
                        DropdownMenuItem(
                            text = { Text("削除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteRequested()
                            },
                        )
                    } else {
                        // Non-owner member can leave the group (§3-4)
                        DropdownMenuItem(
                            text = { Text("離脱") },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onLeaveRequested()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    val isNameValid = groupName.isNotBlank()

    fun submit() {
        if (isNameValid) onConfirm(groupName.trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを作成") },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("グループ名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = groupName.isNotEmpty() && !isNameValid,
            )
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = isNameValid) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを削除") },
        text = { Text("「$groupName」を削除してもよろしいですか？\nメンバー全員がアクセスできなくなります。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun ConfirmLeaveDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを離脱") },
        text = { Text("「$groupName」から離脱してもよろしいですか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("離脱") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

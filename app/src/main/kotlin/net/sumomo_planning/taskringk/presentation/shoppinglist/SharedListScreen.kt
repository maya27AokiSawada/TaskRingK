package net.sumomo_planning.taskringk.presentation.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import net.sumomo_planning.taskringk.core.ui.OfflineBanner
import net.sumomo_planning.taskringk.domain.model.ListKind
import net.sumomo_planning.taskringk.domain.model.SharedGroup
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.domain.model.SharedList
import net.sumomo_planning.taskringk.presentation.todo.TodoScreen

/**
 * リスト画面のルートコンポーザブル（MainScreen Tab 0）。
 *
 * Phase 4: ViewModel に接続し、グループ選択 → リスト選択 → アイテム CRUD を実装。
 *
 * UI 構成 (list_views_spec.md §1):
 *  ┌─────────────────────────────────┐
 *  │  TopAppBar（グループ/リスト選択）  │
 *  ├─────────────────────────────────┤
 *  │  [ ショッピング ] [  ToDo  ]    │  ← ListKind TabRow
 *  ├─────────────────────────────────┤
 *  │  各ビュー                       │
 *  └─────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedListScreen(
    modifier: Modifier = Modifier,
    initialGroupId: String? = null,
    onInitialGroupConsumed: () -> Unit = {},
    viewModel: SharedListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateListDialog by rememberSaveable { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<SharedList?>(null) }
    var itemToEdit by remember { mutableStateOf<SharedItem?>(null) }

    // listKind タブはリスト選択後に currentList.listKind から初期値を取得
    var listKind by rememberSaveable(uiState.selectedListId) {
        mutableStateOf(uiState.currentList?.listKind ?: ListKind.SHOPPING_LIST)
    }
    // currentList が変わったら listKind を同期
    LaunchedEffect(uiState.currentList?.listKind) {
        uiState.currentList?.listKind?.let { listKind = it }
    }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    LaunchedEffect(initialGroupId, uiState.groups) {
        val targetGroupId = initialGroupId ?: return@LaunchedEffect
        if (uiState.groups.any { it.groupId == targetGroupId }) {
            viewModel.selectGroup(targetGroupId)
            onInitialGroupConsumed()
        }
    }

    val items = uiState.currentList?.activeItems ?: emptyList()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("リスト") },
                actions = {
                    // リスト削除ボタン
                    if (uiState.selectedListId != null) {
                        IconButton(onClick = {
                            uiState.currentList?.let { listToDelete = it }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "リストを削除")
                        }
                    }
                    // リスト追加ボタン
                    if (uiState.selectedGroupId != null) {
                        IconButton(onClick = { showCreateListDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "リストを追加")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedListId != null) {
                FloatingActionButton(onClick = { showAddItemDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "アイテムを追加")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OfflineBanner(isOnline = uiState.isOnline)

            // ---- グループ・リスト選択行 ----
            if (uiState.groups.isNotEmpty()) {
                SelectorRow(
                    groups = uiState.groups,
                    selectedGroupId = uiState.selectedGroupId,
                    lists = uiState.lists,
                    selectedListId = uiState.selectedListId,
                    onGroupSelected = viewModel::selectGroup,
                    onListSelected = viewModel::selectList,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            when {
                uiState.currentUser == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("サインインしてください", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                uiState.groups.isEmpty() && uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.groups.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("グループがありません", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                uiState.selectedListId == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("リストがありません", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showCreateListDialog = true }) {
                                Text("リストを作成する")
                            }
                        }
                    }
                }
                else -> {
                    // ---- ListKind switcher ----
                    TabRow(selectedTabIndex = if (listKind == ListKind.SHOPPING_LIST) 0 else 1) {
                        Tab(
                            selected = listKind == ListKind.SHOPPING_LIST,
                            onClick = { listKind = ListKind.SHOPPING_LIST },
                            text = { Text("ショッピング") },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                        )
                        Tab(
                            selected = listKind == ListKind.TO_DO_LIST,
                            onClick = { listKind = ListKind.TO_DO_LIST },
                            text = { Text("ToDo") },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        )
                    }

                    // ---- Content ----
                    when (listKind) {
                        ListKind.SHOPPING_LIST -> ShoppingListView(
                            items = items,
                            onTogglePurchased = viewModel::togglePurchased,
                            onEditItem = { itemToEdit = it },
                            onRemoveItem = viewModel::removeItem,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ListKind.TO_DO_LIST -> TodoScreen(
                            items = items,
                            onTogglePurchased = viewModel::togglePurchased,
                            onEditItem = { itemToEdit = it },
                            onRemoveItem = viewModel::removeItem,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    // ---- Dialogs ----

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, quantity ->
                viewModel.addItem(name, quantity)
                showAddItemDialog = false
            },
        )
    }

    if (showCreateListDialog) {
        CreateListDialog(
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name ->
                viewModel.createList(name)
                showCreateListDialog = false
            },
        )
    }

    itemToEdit?.let { item ->
        EditItemDialog(
            initialName = item.name,
            initialQuantity = item.quantity,
            onDismiss = { itemToEdit = null },
            onConfirm = { name, quantity ->
                viewModel.updateItem(item.copy(name = name, quantity = quantity))
                itemToEdit = null
            },
        )
    }

    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("リストを削除") },
            text = { Text("「${list.listName}」を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list.listId)
                    listToDelete = null
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) { Text("キャンセル") }
            },
        )
    }
}

// ---- グループ・リスト選択行 ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRow(
    groups: List<SharedGroup>,
    selectedGroupId: String?,
    lists: List<SharedList>,
    selectedListId: String?,
    onGroupSelected: (String?) -> Unit,
    onListSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ---- Group dropdown ----
        var groupExpanded by remember { mutableStateOf(false) }
        val selectedGroup = groups.firstOrNull { it.groupId == selectedGroupId }

        ExposedDropdownMenuBox(
            expanded = groupExpanded,
            onExpandedChange = { groupExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedGroup?.groupName ?: "グループ選択",
                onValueChange = {},
                readOnly = true,
                label = { Text("グループ") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            DropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false },
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.groupName) },
                        onClick = {
                            onGroupSelected(group.groupId)
                            groupExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // ---- List dropdown ----
        var listExpanded by remember { mutableStateOf(false) }
        val selectedList = lists.firstOrNull { it.listId == selectedListId }

        ExposedDropdownMenuBox(
            expanded = listExpanded,
            onExpandedChange = { listExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedList?.listName ?: "リスト選択",
                onValueChange = {},
                readOnly = true,
                label = { Text("リスト") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = listExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                enabled = lists.isNotEmpty(),
            )
            if (lists.isNotEmpty()) {
                DropdownMenu(
                    expanded = listExpanded,
                    onDismissRequest = { listExpanded = false },
                ) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.listName) },
                            onClick = {
                                onListSelected(list.listId)
                                listExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---- ダイアログ ----

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("アイテムを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("アイテム名") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("数量") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        if (name.isNotBlank()) onConfirm(name.trim(), qty)
                    }),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (name.isNotBlank()) onConfirm(name.trim(), qty)
                },
                enabled = name.isNotBlank(),
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun CreateListDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("リストを作成") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("リスト名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("作成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun EditItemDialog(
    initialName: String,
    initialQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var quantityText by rememberSaveable { mutableStateOf(initialQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("アイテムを編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("アイテム名") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("数量") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        if (name.isNotBlank()) onConfirm(name.trim(), qty)
                    }),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (name.isNotBlank()) onConfirm(name.trim(), qty)
                },
                enabled = name.isNotBlank(),
            ) { Text("更新") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

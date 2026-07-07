package net.sumomo_planning.goshopping.presentation.shoppinglist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.sumomo_planning.goshopping.domain.model.ListKind
import net.sumomo_planning.goshopping.domain.model.SharedItem
import net.sumomo_planning.goshopping.presentation.todo.TodoScreen

/**
 * リスト画面のルートコンポーザブル（MainScreen Tab 0）。
 *
 * [listKind] に応じて [ShoppingListView] または [TodoScreen] を表示する。
 * Phase 4 でリスト選択・グループ選択を ViewModel から注入する。
 *
 * UI 構成 (list_views_spec.md §1):
 *  ┌─────────────────────────────────┐
 *  │  [ ショッピング ] [  ToDo  ]    │  ← ListKind TabRow
 *  ├─────────────────────────────────┤
 *  │  各ビュー                       │
 *  └─────────────────────────────────┘
 */
@Composable
fun SharedListScreen(
    modifier: Modifier = Modifier,
    // Phase 4: inject via ViewModel
    items: List<SharedItem> = emptyList(),
    initialListKind: ListKind = ListKind.SHOPPING_LIST,
    onTogglePurchased: (SharedItem) -> Unit = {},
) {
    var listKind by rememberSaveable { mutableStateOf(initialListKind) }

    Column(modifier = modifier.fillMaxSize()) {
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
                onTogglePurchased = onTogglePurchased,
                modifier = Modifier.fillMaxSize(),
            )
            ListKind.TO_DO_LIST -> TodoScreen(
                items = items,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

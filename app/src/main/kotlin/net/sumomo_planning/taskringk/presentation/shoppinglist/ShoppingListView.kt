package net.sumomo_planning.taskringk.presentation.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.sumomo_planning.taskringk.domain.model.SharedItem

/**
 * 従来のチェックリスト形式の ShoppingList ビュー（list_views_spec.md §2）。
 *
 * [items] は `SharedList.activeItems`（isDeleted=false のみ）を渡すこと。
 * Phase 4 で ViewModel から実データを受け取る。
 */
@Composable
fun ShoppingListView(
    items: List<SharedItem>,
    onTogglePurchased: (SharedItem) -> Unit = {},
    onEditItem: (SharedItem) -> Unit = {},
    onRemoveItem: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyShoppingState(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        items(items = items, key = { it.itemId }) { item ->
            ShoppingItemRow(
                item = item,
                onTogglePurchased = { onTogglePurchased(item) },
                onEditItem = { onEditItem(item) },
                onRemoveItem = { onRemoveItem(item.itemId) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

// ---- Private composables ----

@Composable
private fun ShoppingItemRow(
    item: SharedItem,
    onTogglePurchased: () -> Unit,
    onEditItem: () -> Unit,
    onRemoveItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTogglePurchased,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isPurchased)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                item.deadline?.let { dl ->
                    Text(
                        text = "期限: ${dl.toDeadlineLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.quantity > 1) {
                Text(
                    text = "×${item.quantity}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            IconButton(onClick = onEditItem) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "アイテムを編集",
                )
            }
            IconButton(onClick = onRemoveItem) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "アイテムを削除",
                )
            }
        }
    }
}

@Composable
private fun EmptyShoppingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            androidx.compose.foundation.layout.Spacer(
                Modifier.size(16.dp)
            )
            Text(
                text = "アイテムがありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "+ ボタンからアイテムを追加してください",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * 期限の表示ラベル（list_views_spec.md §4-2）。
 * 時刻が 00:00:00 ならば日付のみ、それ以外は日付 + 時刻を返す。
 */
internal fun Instant.toDeadlineLabel(zone: ZoneId = ZoneId.systemDefault()): String {
    val ldt = atZone(zone).toLocalDateTime()
    return if (ldt.hour == 0 && ldt.minute == 0 && ldt.second == 0) {
        ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    } else {
        ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
    }
}

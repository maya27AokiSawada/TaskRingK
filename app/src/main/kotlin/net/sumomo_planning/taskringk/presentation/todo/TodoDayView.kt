package net.sumomo_planning.taskringk.presentation.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import net.sumomo_planning.taskringk.domain.model.SharedItem
import net.sumomo_planning.taskringk.presentation.shoppinglist.toDeadlineLabel

/**
 * ToDo 日ビュー（list_views_spec.md §3-2）。
 *
 * セクション構成:
 *  - 🔴 期限切れ : deadline.date < today（selectedDate == today のときのみ表示）
 *  - 📋 今日     : deadline.date == selectedDate（時刻昇順）
 *  - 📝 期限なし : deadline == null
 */
@Composable
fun TodoDayView(
    items: List<SharedItem>,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()

    val overdueItems = if (selectedDate == today) {
        items.filter { it.deadline?.atZone(zone)?.toLocalDate()?.isBefore(today) == true }
            .sortedBy { it.deadline }
    } else emptyList()

    val dueTodayItems = items
        .filter { it.deadline?.atZone(zone)?.toLocalDate() == selectedDate }
        .sortedBy { it.deadline }

    val noDeadlineItems = items
        .filter { it.deadline == null }
        .sortedBy { it.registeredDate }

    val allEmpty = overdueItems.isEmpty() && dueTodayItems.isEmpty() && noDeadlineItems.isEmpty()
    if (allEmpty) {
        EmptyTodoState(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        if (overdueItems.isNotEmpty()) {
            item {
                SectionHeader(
                    label = "期限切れ",
                    labelColor = MaterialTheme.colorScheme.error,
                )
            }
            items(overdueItems, key = { it.itemId }) { item ->
                TodoItemRow(item = item, accentColor = MaterialTheme.colorScheme.error)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (dueTodayItems.isNotEmpty()) {
            item { SectionHeader(label = if (selectedDate == today) "今日" else selectedDate.toString()) }
            items(dueTodayItems, key = { it.itemId }) { item ->
                TodoItemRow(item = item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (noDeadlineItems.isNotEmpty()) {
            item { SectionHeader(label = "期限なし") }
            items(noDeadlineItems, key = { it.itemId }) { item ->
                TodoItemRow(item = item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// ---- Internal shared composables (used by other Todo views too) ----

@Composable
internal fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = labelColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
internal fun TodoItemRow(
    item: SharedItem,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor,
            )
            item.deadline?.let { dl ->
                Text(
                    text = dl.toDeadlineLabel(),
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
            )
        }
    }
}

@Composable
internal fun EmptyTodoState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
            Text(
                text = "このビューにアイテムはありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

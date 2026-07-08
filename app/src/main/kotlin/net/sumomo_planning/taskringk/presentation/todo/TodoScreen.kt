package net.sumomo_planning.taskringk.presentation.todo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import net.sumomo_planning.taskringk.domain.model.SharedItem

/**
 * ToDo ビューのコンテナ。
 *
 * 上部に Day / 週 / 月 の `TabRow` を表示し、選択された [TodoViewMode] に
 * 応じた子コンポーザブルへ委譲する（list_views_spec.md §3）。
 *
 * 日付ナビゲーションは各モードで意味が変わる:
 *  - DAY   : 前日 / 翌日
 *  - WEEK  : 前週 / 翌週
 *  - MONTH : 前月 / 翌月
 */
@Composable
fun TodoScreen(
    items: List<SharedItem>,
    onTogglePurchased: (SharedItem) -> Unit,
    onEditItem: (SharedItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by rememberSaveable { mutableStateOf(TodoViewMode.DAY) }

    // current date encoded as epochDay (Long) so rememberSaveable works
    var currentEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val currentDate = LocalDate.ofEpochDay(currentEpochDay)

    Column(modifier = modifier.fillMaxSize()) {
        // ---- View mode tab row (日 / 週 / 月) ----
        TabRow(selectedTabIndex = viewMode.ordinal) {
            TodoViewMode.entries.forEachIndexed { index, mode ->
                Tab(
                    selected = viewMode.ordinal == index,
                    onClick = { viewMode = mode },
                    text = { Text(mode.label) },
                )
            }
        }

        // ---- Date navigation header ----
        DateNavigationHeader(
            label = currentDate.toNavLabel(viewMode),
            onPrevious = {
                currentEpochDay = when (viewMode) {
                    TodoViewMode.DAY -> currentDate.minusDays(1).toEpochDay()
                    TodoViewMode.WEEK -> currentDate.minusWeeks(1).toEpochDay()
                    TodoViewMode.MONTH -> currentDate.minusMonths(1).toEpochDay()
                }
            },
            onNext = {
                currentEpochDay = when (viewMode) {
                    TodoViewMode.DAY -> currentDate.plusDays(1).toEpochDay()
                    TodoViewMode.WEEK -> currentDate.plusWeeks(1).toEpochDay()
                    TodoViewMode.MONTH -> currentDate.plusMonths(1).toEpochDay()
                }
            },
        )

        // ---- Content ----
        when (viewMode) {
            TodoViewMode.DAY -> TodoDayView(
                items = items,
                selectedDate = currentDate,
                onTogglePurchased = onTogglePurchased,
                onEditItem = onEditItem,
                onRemoveItem = onRemoveItem,
                modifier = Modifier.fillMaxSize(),
            )
            TodoViewMode.WEEK -> TodoWeekView(
                items = items,
                weekStart = currentDate.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                ),
                onTogglePurchased = onTogglePurchased,
                onEditItem = onEditItem,
                onRemoveItem = onRemoveItem,
                modifier = Modifier.fillMaxSize(),
            )
            TodoViewMode.MONTH -> TodoMonthView(
                items = items,
                yearMonth = YearMonth.from(currentDate),
                onTogglePurchased = onTogglePurchased,
                onEditItem = onEditItem,
                onRemoveItem = onRemoveItem,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DateNavigationHeader(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "前へ",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "次へ",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LocalDate.toNavLabel(viewMode: TodoViewMode): String = when (viewMode) {
    TodoViewMode.DAY ->
        format(DateTimeFormatter.ofPattern("yyyy年M月d日（E）"))
    TodoViewMode.WEEK -> {
        val weekStart = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        "${weekStart.format(DateTimeFormatter.ofPattern("M/d"))}〜${weekEnd.format(DateTimeFormatter.ofPattern("M/d"))}"
    }
    TodoViewMode.MONTH ->
        format(DateTimeFormatter.ofPattern("yyyy年M月"))
}

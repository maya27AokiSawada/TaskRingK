package net.sumomo_planning.taskringk.presentation.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import net.sumomo_planning.taskringk.domain.model.SharedItem

/**
 * ToDo 週ビュー（list_views_spec.md §3-3）。
 *
 * [weekStart] は月曜日の日付。`currentDate.with(previousOrSame(MONDAY))` で算出する。
 * 各曜日セクションに deadline がその日のアイテムを表示する。
 */
@Composable
fun TodoWeekView(
    items: List<SharedItem>,
    weekStart: LocalDate,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val weekDays = (0 until 7).map { weekStart.plusDays(it.toLong()) }

    // Items by date (only those with deadline)
    val itemsByDate = items
        .filter { it.deadline != null }
        .groupBy { it.deadline!!.atZone(zone).toLocalDate() }

    val noDeadlineItems = items.filter { it.deadline == null }.sortedBy { it.registeredDate }

    val allEmpty = items.isEmpty()
    if (allEmpty) {
        EmptyTodoState(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        weekDays.forEach { date ->
            val dayItems = (itemsByDate[date] ?: emptyList()).sortedBy { it.deadline }
            item {
                WeekDayHeader(date = date, today = today, itemCount = dayItems.size)
            }
            if (dayItems.isNotEmpty()) {
                items(dayItems, key = { "${date}_${it.itemId}" }) { item ->
                    TodoItemRow(
                        item = item,
                        accentColor = if (date.isBefore(today))
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        if (noDeadlineItems.isNotEmpty()) {
            item {
                SectionHeader(label = "期限なし")
            }
            items(noDeadlineItems, key = { "none_${it.itemId}" }) { item ->
                TodoItemRow(item = item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun WeekDayHeader(
    date: LocalDate,
    today: LocalDate,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val isToday = date == today
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE)
    val label = date.format(DateTimeFormatter.ofPattern("M/d")) + "（$dayName）"

    HorizontalDivider()
    Text(
        text = if (itemCount > 0) "$label  $itemCount 件" else label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        color = when {
            isToday -> MaterialTheme.colorScheme.primary
            date.isBefore(today) -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** 指定日を含む週（月曜日起点）の月曜日を返す */
fun LocalDate.weekStart(): LocalDate =
    this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

package net.sumomo_planning.goshopping.presentation.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import net.sumomo_planning.goshopping.domain.model.SharedItem

/**
 * ToDo 月ビュー — カレンダーグリッド（list_views_spec.md §3-4）。
 *
 * - 月曜日起点 7 列 × 最大 6 行のグリッド
 * - 各セルにアイテム数バッジ表示
 * - セル選択 → 下部に選択日のアイテム一覧
 */
@Composable
fun TodoMonthView(
    items: List<SharedItem>,
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()

    val itemsByDate: Map<LocalDate, List<SharedItem>> = items
        .filter { it.deadline != null }
        .groupBy { it.deadline!!.atZone(zone).toLocalDate() }

    // Generate calendar grid starting from Monday on or before the 1st
    val firstDay = yearMonth.atDay(1)
    val gridStart = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridDays = (0 until 42).map { gridStart.plusDays(it.toLong()) }

    var selectedDate by remember(yearMonth) { mutableStateOf(today.takeIf { YearMonth.from(it) == yearMonth } ?: firstDay) }

    Column(modifier = modifier.fillMaxSize()) {
        // Day-of-week header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            DayOfWeek.values().let { dows ->
                // Start from Monday
                val reordered = listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY,
                )
                reordered.forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.NARROW, Locale.JAPANESE),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = when (dow) {
                            DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                            DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                }
            }
        }

        HorizontalDivider()

        // Calendar grid (7×6)
        gridDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        isCurrentMonth = date.month == yearMonth.month,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        itemCount = itemsByDate[date]?.size ?: 0,
                        onClick = { selectedDate = date },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        HorizontalDivider()

        // Selected date's items
        val selectedItems = itemsByDate[selectedDate]?.sortedBy { it.deadline } ?: emptyList()
        SelectedDateItemList(
            date = selectedDate,
            items = selectedItems,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val isPast = date.isBefore(LocalDate.now())

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    !isCurrentMonth -> MaterialTheme.colorScheme.outline
                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                    date.dayOfWeek == DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (itemCount > 0 && isCurrentMonth) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    ),
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SelectedDateItemList(
    date: LocalDate,
    items: List<SharedItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(label = "${date.monthValue}月${date.dayOfMonth}日のタスク")
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "タスクがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn {
                items(items, key = { it.itemId }) { item ->
                    TodoItemRow(item = item)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

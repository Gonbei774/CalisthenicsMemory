package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    items: List<RecordItem>,
    exercises: List<Exercise>,
    selectedExerciseFilter: Exercise?,
    selectedPeriod: Period?,
    onExerciseClick: (Exercise) -> Unit
) {
    val appColors = LocalAppColors.current

    // 各日の記録タイプを判定（ドット色用）
    val dayInfoMap = remember(items) {
        items.groupBy { it.date }.mapValues { (_, dayItems) ->
            DayInfo(
                hasSession = dayItems.any { it is RecordItem.Session },
                hasInterval = dayItems.any { it is RecordItem.Interval },
                items = dayItems
            )
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember { LocalDate.now() }
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }

    if (selectedPeriod == Period.OneWeek) {
        // 週間表示（1週間フィルター時）
        val weekDays = remember(today) {
            val startOfWeek = today.minusDays(6)
            (0L..6L).map { startOfWeek.plusDays(it) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 曜日 + 日付の大きなセル行
            item(key = "week-grid") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekDays.forEach { date ->
                        val dateKey = date.toString()
                        val dayInfo = dayInfoMap[dateKey]
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        WeekDayCell(
                            date = date,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasSession = dayInfo?.hasSession == true,
                            hasInterval = dayInfo?.hasInterval == true,
                            onClick = {
                                selectedDate = if (selectedDate == date) null else date
                            },
                            appColors = appColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 選択日の記録サマリー
            val selected = selectedDate
            if (selected != null) {
                val dayItems = dayInfoMap[selected.toString()]?.items
                if (!dayItems.isNullOrEmpty()) {
                    item(key = "week-summary") {
                        DayRecordSummary(
                            date = selected,
                            items = dayItems,
                            exerciseMap = exerciseMap,
                            appColors = appColors,
                            onExerciseClick = onExerciseClick
                        )
                    }
                }
            }
        }
    } else {
        // 月間表示（通常・1ヶ月・3ヶ月）
        val months = remember(items) {
            val now = YearMonth.now()
            if (items.isEmpty()) {
                listOf(now)
            } else {
                val dates = items.mapNotNull { item ->
                    try {
                        LocalDate.parse(item.date)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (dates.isEmpty()) {
                    listOf(now)
                } else {
                    val earliest = YearMonth.from(dates.min())
                    val latest = maxOf(YearMonth.from(dates.max()), now)
                    generateSequence(earliest) { it.plusMonths(1) }
                        .takeWhile { it <= latest }
                        .toList()
                }
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(months.size) {
            if (months.isNotEmpty()) {
                listState.scrollToItem(months.size - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(months, key = { it.toString() }) { yearMonth ->
                MonthGrid(
                    yearMonth = yearMonth,
                    today = today,
                    selectedDate = selectedDate,
                    dayInfoMap = dayInfoMap,
                    onDateClick = { date ->
                        selectedDate = if (selectedDate == date) null else date
                    },
                    appColors = appColors
                )

                val selected = selectedDate
                if (selected != null && YearMonth.from(selected) == yearMonth) {
                    val dateKey = selected.toString()
                    val dayItems = dayInfoMap[dateKey]?.items
                    if (!dayItems.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DayRecordSummary(
                            date = selected,
                            items = dayItems,
                            exerciseMap = exerciseMap,
                            appColors = appColors,
                            onExerciseClick = onExerciseClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    dayInfoMap: Map<String, DayInfo>,
    onDateClick: (LocalDate) -> Unit,
    appColors: AppColors
) {
    Column {
        // 月ヘッダー
        Text(
            text = formatYearMonth(yearMonth),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 曜日ヘッダー
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            )
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 12.sp,
                    color = appColors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日付グリッド
        val firstDayOfMonth = yearMonth.atDay(1)
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday=0
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val dateKey = date.toString()
                        val dayInfo = dayInfoMap[dateKey]
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        DayCell(
                            dayOfMonth = dayOfMonth,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasSession = dayInfo?.hasSession == true,
                            hasInterval = dayInfo?.hasInterval == true,
                            onClick = { onDateClick(date) },
                            appColors = appColors,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(0.5.dp, appColors.textTertiary.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

private data class DayInfo(
    val hasSession: Boolean,
    val hasInterval: Boolean,
    val items: List<RecordItem>
)

@Composable
private fun DayCell(
    dayOfMonth: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasSession: Boolean,
    hasInterval: Boolean,
    onClick: () -> Unit,
    appColors: AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .border(0.5.dp, appColors.textTertiary.copy(alpha = 0.2f))
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(Purple600.copy(alpha = 0.15f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) Purple600 else appColors.textPrimary,
            textAlign = TextAlign.Center
        )

        // ドット表示
        if (hasSession || hasInterval) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                if (hasSession) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Purple600, CircleShape)
                    )
                }
                if (hasInterval) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Orange600, CircleShape)
                    )
                }
            }
        } else {
            // ドットがないときもスペース確保で高さを揃える
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasSession: Boolean,
    hasInterval: Boolean,
    onClick: () -> Unit,
    appColors: AppColors,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val dayOfWeekText = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    Column(
        modifier = modifier
            .border(0.5.dp, appColors.textTertiary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.background(Purple600.copy(alpha = 0.15f))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 曜日
        Text(
            text = dayOfWeekText,
            fontSize = 11.sp,
            color = appColors.textTertiary,
            textAlign = TextAlign.Center
        )
        // 日付（大きめ）
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 20.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) Purple600 else appColors.textPrimary,
            textAlign = TextAlign.Center
        )
        // ドット
        if (hasSession || hasInterval) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasSession) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Purple600, CircleShape)
                    )
                }
                if (hasInterval) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Orange600, CircleShape)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayRecordSummary(
    date: LocalDate,
    items: List<RecordItem>,
    exerciseMap: Map<Long, Exercise>,
    appColors: AppColors,
    onExerciseClick: (Exercise) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 日付ヘッダー
            Text(
                text = date.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textSecondary
            )

            items.forEach { item ->
                when (item) {
                    is RecordItem.Session -> {
                        SessionSummaryRow(
                            session = item.session,
                            exerciseMap = exerciseMap,
                            appColors = appColors,
                            onExerciseClick = onExerciseClick
                        )
                    }
                    is RecordItem.Interval -> {
                        IntervalSummaryRow(
                            record = item.record,
                            appColors = appColors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryRow(
    session: SessionInfo,
    exerciseMap: Map<Long, Exercise>,
    appColors: AppColors,
    onExerciseClick: (Exercise) -> Unit
) {
    val exercise = exerciseMap[session.exerciseId]
    val exerciseName = exercise?.name ?: "?"
    val setCount = session.records.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(appColors.cardBackgroundSecondary)
            .then(
                if (exercise != null) {
                    Modifier.clickable { onExerciseClick(exercise) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Purple600, CircleShape)
            )
            Text(
                text = exerciseName,
                fontSize = 14.sp,
                color = appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${setCount}set",
            fontSize = 13.sp,
            color = appColors.textTertiary
        )
    }
}

@Composable
private fun IntervalSummaryRow(
    record: IntervalRecord,
    appColors: AppColors
) {
    val isFullCompletion = record.completedRounds == record.rounds

    val exerciseNames = remember(record.exercisesJson) {
        try {
            val arr = JSONArray(record.exercisesJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(appColors.cardBackgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Orange600, CircleShape)
            )
            Column {
                Text(
                    text = record.programName,
                    fontSize = 14.sp,
                    color = Orange600,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (exerciseNames.isNotEmpty()) {
                    Text(
                        text = exerciseNames.joinToString(", "),
                        fontSize = 12.sp,
                        color = appColors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = "${record.completedRounds}/${record.rounds}",
            fontSize = 13.sp,
            color = if (isFullCompletion) Orange600 else appColors.textTertiary,
            fontWeight = if (isFullCompletion) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatYearMonth(yearMonth: YearMonth): String {
    val locale = Locale.getDefault()
    return if (locale.language == "ja") {
        "${yearMonth.year}年${yearMonth.monthValue}月"
    } else {
        "${yearMonth.month.getDisplayName(TextStyle.FULL, locale)} ${yearMonth.year}"
    }
}

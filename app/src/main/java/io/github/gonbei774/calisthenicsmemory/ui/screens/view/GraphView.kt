package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// データクラス
data class GraphDataPoint(
    val date: String,
    val value: Float,
    val label: String = "",
    val valueLeft: Float? = null  // ← 追加: Unilateral用の左側の値
)

enum class Period(val days: Int, val displayNameResId: Int) {
    OneWeek(7, R.string.period_one_week),
    OneMonth(30, R.string.period_one_month),
    ThreeMonths(90, R.string.period_three_months)
}

enum class GraphType(val displayNameResId: Int) {
    Average(R.string.graph_type_average),
    Max(R.string.graph_type_max),
    Total(R.string.graph_type_total)
}

data class Statistics(
    val totalSets: Int,
    val average: Float,
    val max: Int,
    val min: Int,
    val weeklyChange: Int,
    // ← 追加: Unilateral用
    val averageLeft: Float? = null,
    val maxLeft: Int? = null,
    val minLeft: Int? = null
)

// グラフ表示コンポーネント
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphView(
    exercises: List<Exercise>,
    records: List<TrainingRecord>,
    selectedExerciseFilter: Exercise?,
    selectedPeriod: Period?
) {
    var selectedGraphType by remember { mutableStateOf(GraphType.Average) }

    if (exercises.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.no_exercises_registered),
                    fontSize = 18.sp,
                    color = Slate400
                )
                Text(
                    text = stringResource(R.string.add_exercises_in_settings),
                    fontSize = 14.sp,
                    color = Slate400
                )
            }
        }
        return
    }

    if (selectedExerciseFilter == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_exercise_please),
                    fontSize = 18.sp,
                    color = Slate400
                )
                Text(
                    text = stringResource(R.string.select_from_top),
                    fontSize = 14.sp,
                    color = Slate400
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // グラフタイプ選択
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Slate800
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.display_data),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphType.values().forEach { type ->
                            FilterChip(
                                selected = selectedGraphType == type,
                                onClick = { selectedGraphType = type },
                                label = { Text(stringResource(type.displayNameResId)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Purple600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate700,
                                    labelColor = Slate300
                                )
                            )
                        }
                    }
                }
            }
        }

        // グラフ
        item {
            val unit = stringResource(if (selectedExerciseFilter!!.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
            val graphData = remember(selectedExerciseFilter, records, selectedPeriod, selectedGraphType, unit) {
                prepareGraphData(
                    exercise = selectedExerciseFilter!!,
                    records = records,
                    period = selectedPeriod,
                    graphType = selectedGraphType,
                    unit = unit
                )
            }

            LineChart(
                data = graphData,
                exercise = selectedExerciseFilter!!,
                period = selectedPeriod
            )
        }

        // 統計サマリー
        item {
            val stats = remember(selectedExerciseFilter, records, selectedPeriod) {
                calculateStatistics(
                    exercise = selectedExerciseFilter!!,
                    records = records,
                    period = selectedPeriod
                )
            }

            StatisticsSummary(
                statistics = stats,
                exercise = selectedExerciseFilter!!
            )
        }
    }
}

// グラフデータ準備関数
fun prepareGraphData(
    exercise: Exercise,
    records: List<TrainingRecord>,
    period: Period?,
    graphType: GraphType,
    unit: String
): List<GraphDataPoint> {
    // 対象種目の記録を抽出
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }

    if (exerciseRecords.isEmpty()) {
        return emptyList()
    }

    // 期間でフィルター（nullの場合は全期間）
    val today = LocalDate.now()
    val filteredRecords = if (period == null) {
        exerciseRecords
    } else {
        val cutoffDate = today.minusDays(period.days.toLong() - 1)
        exerciseRecords.filter { record ->
            try {
                val recordDate = LocalDate.parse(record.date)
                recordDate >= cutoffDate && recordDate <= today
            } catch (e: Exception) {
                false
            }
        }
    }

    if (filteredRecords.isEmpty()) {
        return emptyList()
    }

    // 日付ごとにグループ化して値を計算
    val sessionsByDate = filteredRecords
        .groupBy { it.date }
        .mapValues { (_, sessionRecords) ->
            // Bilateral/Unilateral で分岐
            val isUnilateral = sessionRecords.firstOrNull()?.valueLeft != null

            if (isUnilateral) {
                // Unilateral: 左右別々に計算
                val valuesRight = sessionRecords.map { it.valueRight.toDouble() }
                val valuesLeft = sessionRecords.mapNotNull { it.valueLeft?.toDouble() }

                val valueRight = when (graphType) {
                    GraphType.Average -> valuesRight.average().toFloat()
                    GraphType.Max -> valuesRight.maxOrNull()?.toFloat() ?: 0f
                    GraphType.Total -> valuesRight.sum().toFloat()
                }

                val valueLeft = if (valuesLeft.isNotEmpty()) {
                    when (graphType) {
                        GraphType.Average -> valuesLeft.average().toFloat()
                        GraphType.Max -> valuesLeft.maxOrNull()?.toFloat() ?: 0f
                        GraphType.Total -> valuesLeft.sum().toFloat()
                    }
                } else {
                    0f
                }

                Pair(valueRight, valueLeft)
            } else {
                // Bilateral: 右側のみ
                val values = sessionRecords.map { it.valueRight.toDouble() }
                val value = when (graphType) {
                    GraphType.Average -> values.average().toFloat()
                    GraphType.Max -> values.maxOrNull()?.toFloat() ?: 0f
                    GraphType.Total -> values.sum().toFloat()
                }
                Pair(value, null)
            }
        }

    // データポイントを作成
    return sessionsByDate.map { (date, values) ->
        val (valueRight, valueLeft) = values

        val label = if (valueLeft != null) {
            String.format("$date: R%d$unit / L%d$unit", valueRight.roundToInt(), valueLeft.roundToInt())
        } else {
            String.format("$date: %d$unit", valueRight.roundToInt())
        }

        GraphDataPoint(
            date = date,
            value = valueRight,
            valueLeft = valueLeft,
            label = label
        )
    }.sortedBy { it.date }
}

// 折れ線グラフコンポーネント
@Composable
fun LineChart(
    data: List<GraphDataPoint>,
    exercise: Exercise,
    period: Period?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = Slate800
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier) {
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_data),
                            fontSize = 16.sp,
                            color = Slate400
                        )
                        Text(
                            text = stringResource(R.string.record_training_for_exercise),
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            } else {
                SimpleLineChart(
                    data = data,
                    exercise = exercise,
                    period = period
                )
            }
        }
    }
}

@Composable
fun SimpleLineChart(
    data: List<GraphDataPoint>,
    exercise: Exercise,
    period: Period?
) {
    // Unilateral判定
    val isUnilateral = data.any { it.valueLeft != null }

    // 単位文字列を取得（@Composable関数内でのみ取得可能）
    val unit = stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (data.isEmpty()) return@Canvas

        // maxValue: 左右両方を考慮
        val maxValue = if (isUnilateral) {
            maxOf(
                data.maxOfOrNull { it.value } ?: 1f,
                data.mapNotNull { it.valueLeft }.maxOrNull() ?: 1f
            )
        } else {
            data.maxOfOrNull { it.value } ?: 1f
        }

        val minValue = 0f
        val range = (maxValue - minValue).coerceAtLeast(1f)

        val leftPadding = 50.dp.toPx()
        val bottomPadding = 40.dp.toPx()
        val topPadding = 20.dp.toPx()
        val rightPadding = 30.dp.toPx()

        val graphWidth = size.width - leftPadding - rightPadding
        val graphHeight = size.height - topPadding - bottomPadding

        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }

        val today = LocalDate.now()
        val startDate = if (period == null) {
            try {
                LocalDate.parse(data.minOf { it.date })
            } catch (e: Exception) {
                today.minusDays(30)
            }
        } else {
            today.minusDays(period.days.toLong() - 1)
        }
        val endDate = today

        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val yLabels = calculateYAxisLabels(0f, maxValue)

        // Y軸グリッド線とラベル
        yLabels.forEach { labelValue ->
            val y = topPadding + graphHeight - ((labelValue - minValue) / range * graphHeight)

            drawLine(
                color = Slate600.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx()
            )

            val labelText = "${labelValue.toInt()}$unit"

            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                leftPadding - 8.dp.toPx(),
                y + 4.dp.toPx(),
                textPaint.apply {
                    textAlign = Paint.Align.RIGHT
                }
            )
        }

        // 右側の線（緑）
        val pathRight = Path()
        var isFirstPoint = true

        data.forEach { point ->
            try {
                val pointDate = LocalDate.parse(point.date)
                val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                val y = topPadding + graphHeight - ((point.value - minValue) / range * graphHeight)

                if (isFirstPoint) {
                    pathRight.moveTo(x, y)
                    isFirstPoint = false
                } else {
                    pathRight.lineTo(x, y)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        // 右側の線を描画（緑）
        drawPath(
            path = pathRight,
            color = Green400,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Unilateralの場合、左側の線も描画
        if (isUnilateral) {
            val pathLeft = Path()
            isFirstPoint = true

            data.forEach { point ->
                point.valueLeft?.let { leftValue ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val y = topPadding + graphHeight - ((leftValue - minValue) / range * graphHeight)

                        if (isFirstPoint) {
                            pathLeft.moveTo(x, y)
                            isFirstPoint = false
                        } else {
                            pathLeft.lineTo(x, y)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            // 左側の線を描画（紫）
            drawPath(
                path = pathLeft,
                color = Purple600,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ポイント描画（右側）
        data.forEach { point ->
            try {
                val pointDate = LocalDate.parse(point.date)
                val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                val y = topPadding + graphHeight - ((point.value - minValue) / range * graphHeight)

                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Green400,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            } catch (e: Exception) {
                // ignore
            }
        }

        // ポイント描画（左側）- Unilateralのみ
        if (isUnilateral) {
            data.forEach { point ->
                point.valueLeft?.let { leftValue ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val y = topPadding + graphHeight - ((leftValue - minValue) / range * graphHeight)

                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Purple600,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        // X軸ラベル
        val displayDates = calculateXAxisDisplayDates(startDate, endDate, period)

        displayDates.forEach { date ->
            val daysFromStart = ChronoUnit.DAYS.between(startDate, date).toInt()
            val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
            val y = size.height - bottomPadding + 20.dp.toPx()

            val dateText = "${date.monthValue}/${date.dayOfMonth}"

            drawContext.canvas.nativeCanvas.drawText(
                dateText,
                x,
                y,
                textPaint.apply {
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    }
}

fun calculateYAxisLabels(min: Float, max: Float): List<Float> {
    val range = max
    val interval = when {
        range < 10 -> 2f
        range < 50 -> 10f
        range < 100 -> 20f
        else -> 50f
    }

    val adjustedMax = ((max / interval).toInt() + 1) * interval

    return (0..4).map { i ->
        (adjustedMax * i / 4).toFloat()
    }
}

fun calculateXAxisDisplayDates(startDate: LocalDate, endDate: LocalDate, period: Period?): List<LocalDate> {
    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    return when {
        totalDays <= 7 -> {
            (0 until totalDays).map { startDate.plusDays(it.toLong()) }
        }
        totalDays <= 15 -> {
            listOf(
                startDate,
                startDate.plusDays(totalDays / 3L),
                startDate.plusDays(totalDays * 2L / 3),
                endDate
            )
        }
        totalDays <= 30 -> {
            listOf(
                startDate,
                startDate.plusDays(totalDays / 4L),
                startDate.plusDays(totalDays / 2L),
                startDate.plusDays(totalDays * 3L / 4),
                endDate
            )
        }
        else -> {
            listOf(
                startDate,
                startDate.plusDays(totalDays / 3L),
                startDate.plusDays(totalDays * 2L / 3),
                endDate
            )
        }
    }
}

@Composable
fun StatisticsSummary(
    statistics: Statistics,
    exercise: Exercise
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Slate800
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.statistics_summary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (statistics.totalSets == 0) {
                Text(
                    text = stringResource(R.string.no_data),
                    fontSize = 14.sp,
                    color = Slate400
                )
            } else {
                val unit = stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                val isUnilateral = statistics.averageLeft != null

                // 総セット数
                val totalSetsText = stringResource(R.string.total_sets)
                val setsSuffix = stringResource(R.string.sets_suffix)
                StatItem(totalSetsText, "${statistics.totalSets}$setsSuffix")

                if (isUnilateral) {
                    // Unilateral: 左右並べて表示
                    StatItemDual(
                        label = stringResource(R.string.average),
                        valueRight = "${statistics.average.roundToInt()}$unit",
                        valueLeft = "${statistics.averageLeft?.roundToInt() ?: 0}$unit"
                    )
                    StatItemDual(
                        label = stringResource(R.string.best_record),
                        valueRight = "${statistics.max}$unit",
                        valueLeft = "${statistics.maxLeft ?: 0}$unit"
                    )
                    StatItemDual(
                        label = stringResource(R.string.worst_record),
                        valueRight = "${statistics.min}$unit",
                        valueLeft = "${statistics.minLeft ?: 0}$unit"
                    )
                } else {
                    // Bilateral: 従来通り
                    StatItem(stringResource(R.string.average), "${statistics.average.roundToInt()}$unit")
                    StatItem(stringResource(R.string.best_record), "${statistics.max}$unit")
                    StatItem(stringResource(R.string.worst_record), "${statistics.min}$unit")
                }

                if (statistics.weeklyChange != 0) {
                    val changeText = if (statistics.weeklyChange > 0) {
                        "+${statistics.weeklyChange}$setsSuffix"
                    } else {
                        "${statistics.weeklyChange}$setsSuffix"
                    }
                    val color = if (statistics.weeklyChange > 0) Green400 else Red600
                    StatItem(stringResource(R.string.weekly_change), changeText, valueColor = color)
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    valueColor: Color = Green400
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Slate400
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// ← 追加: Unilateral用の左右並べて表示
@Composable
fun StatItemDual(
    label: String,
    valueRight: String,
    valueLeft: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Slate400
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.right_short),
                fontSize = 12.sp,
                color = Green400,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueRight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Green400
            )
            Text(
                text = "|",
                fontSize = 14.sp,
                color = Slate600
            )
            Text(
                text = stringResource(R.string.left_short),
                fontSize = 12.sp,
                color = Purple600,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueLeft,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Purple600
            )
        }
    }
}

// 統計計算関数
fun calculateStatistics(
    exercise: Exercise,
    records: List<TrainingRecord>,
    period: Period?
): Statistics {
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }

    if (exerciseRecords.isEmpty()) {
        return Statistics(0, 0f, 0, 0, 0, null, null, null)
    }

    // 期間フィルター（nullの場合は全期間）
    val filteredRecords = if (period == null) {
        exerciseRecords
    } else {
        val cutoffDate = LocalDate.now().minusDays(period.days.toLong())
        exerciseRecords.filter {
            try {
                LocalDate.parse(it.date) >= cutoffDate
            } catch (e: Exception) {
                false
            }
        }
    }

    if (filteredRecords.isEmpty()) {
        return Statistics(0, 0f, 0, 0, 0, null, null, null)
    }

    // Unilateral判定
    val isUnilateral = filteredRecords.any { it.valueLeft != null }

    // 右側の値
    val valuesRight = filteredRecords.map { it.valueRight }

    // 今週のデータ
    val weekStart = LocalDate.now().minusDays(7)
    val thisWeekRecords = filteredRecords.filter {
        try {
            LocalDate.parse(it.date) >= weekStart
        } catch (e: Exception) {
            false
        }
    }
    val lastWeekRecords = filteredRecords.filter {
        try {
            val date = LocalDate.parse(it.date)
            date >= weekStart.minusDays(7) && date < weekStart
        } catch (e: Exception) {
            false
        }
    }

    val weeklyChange = thisWeekRecords.size - lastWeekRecords.size

    if (isUnilateral) {
        // Unilateral: 左右別々に計算
        val valuesLeft = filteredRecords.mapNotNull { it.valueLeft }

        return Statistics(
            totalSets = filteredRecords.size,
            average = valuesRight.average().toFloat(),
            max = valuesRight.maxOrNull() ?: 0,
            min = valuesRight.minOrNull() ?: 0,
            weeklyChange = weeklyChange,
            averageLeft = if (valuesLeft.isNotEmpty()) valuesLeft.average().toFloat() else null,
            maxLeft = valuesLeft.maxOrNull(),
            minLeft = valuesLeft.minOrNull()
        )
    } else {
        // Bilateral: 右側のみ
        return Statistics(
            totalSets = filteredRecords.size,
            average = valuesRight.average().toFloat(),
            max = valuesRight.maxOrNull() ?: 0,
            min = valuesRight.minOrNull() ?: 0,
            weeklyChange = weeklyChange,
            averageLeft = null,
            maxLeft = null,
            minLeft = null
        )
    }
}
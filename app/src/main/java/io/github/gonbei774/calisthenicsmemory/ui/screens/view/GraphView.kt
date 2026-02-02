package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
    val valueLeft: Float? = null,  // Unilateral用の左側の値
    val distanceCm: Float? = null  // 距離トラッキング用
)

// ボリュームグラフ用データクラス
data class VolumeDataPoint(
    val date: String,
    val volumeRight: Float,        // kg（Bilateral時はこれのみ使用）
    val volumeLeft: Float? = null  // Unilateral用
)

// アシストグラフ用データクラス
data class AssistanceDataPoint(
    val date: String,
    val assistanceKg: Float        // アシスト量（kg）
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
    // Unilateral用
    val averageLeft: Float? = null,
    val maxLeft: Int? = null,
    val minLeft: Int? = null,
    // 荷重統計（weightTrackingEnabled時のみ有効）
    val maxDailyVolume: Float? = null,    // 最大日ボリューム (kg)
    val avgDailyVolume: Float? = null,    // 平均日ボリューム (kg)
    val maxDailyVolumeLeft: Float? = null,    // 最大日ボリューム左 (kg) - Unilateral用
    val avgDailyVolumeLeft: Float? = null,    // 平均日ボリューム左 (kg) - Unilateral用
    val maxWeight: Float? = null,         // 最大荷重 (kg)
    val avgWeight: Float? = null          // 平均荷重 (kg)
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
    val appColors = LocalAppColors.current
    var selectedGraphType by remember { mutableStateOf(GraphType.Average) }
    var isDistanceInverted by remember { mutableStateOf(false) }

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
                    color = appColors.textSecondary
                )
                Text(
                    text = stringResource(R.string.add_exercises_in_settings),
                    fontSize = 14.sp,
                    color = appColors.textSecondary
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
                    color = appColors.textSecondary
                )
                Text(
                    text = stringResource(R.string.select_from_top),
                    fontSize = 14.sp,
                    color = appColors.textSecondary
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // グラフタイプ選択
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = appColors.cardBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphType.values().forEach { type ->
                            FilterChip(
                                selected = selectedGraphType == type,
                                onClick = { selectedGraphType = type },
                                label = { Text(stringResource(type.displayNameResId)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Purple600,
                                    selectedLabelColor = appColors.textPrimary,
                                    containerColor = appColors.cardBackgroundSecondary,
                                    labelColor = appColors.textTertiary
                                )
                            )
                        }
                    }

                    // 距離トラッキング有効時のみ反転トグルを表示
                    if (selectedExerciseFilter.distanceTrackingEnabled) {
                        FilterChip(
                            selected = isDistanceInverted,
                            onClick = { isDistanceInverted = !isDistanceInverted },
                            label = {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.invert),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue600,
                                selectedLabelColor = appColors.textPrimary,
                                containerColor = appColors.cardBackgroundSecondary,
                                labelColor = appColors.textTertiary
                            )
                        )
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

            // 全期間の距離min/maxを計算（フィルター期間に関係なくスケールを固定）
            val allTimeDistanceRange = remember(selectedExerciseFilter, records) {
                if (selectedExerciseFilter!!.distanceTrackingEnabled) {
                    val allDistances = records
                        .filter { it.exerciseId == selectedExerciseFilter!!.id }
                        .mapNotNull { it.distanceCm?.toFloat() }
                    if (allDistances.isNotEmpty()) {
                        Pair(allDistances.minOrNull() ?: 0f, allDistances.maxOrNull() ?: 0f)
                    } else null
                } else null
            }

            LineChart(
                data = graphData,
                exercise = selectedExerciseFilter!!,
                period = selectedPeriod,
                isDistanceInverted = isDistanceInverted,
                allTimeDistanceRange = allTimeDistanceRange
            )
        }

        // ボリュームグラフ（荷重トラッキング有効時のみ）
        if (selectedExerciseFilter.weightTrackingEnabled) {
            item {
                val volumeData = remember(selectedExerciseFilter, records, selectedPeriod) {
                    prepareVolumeData(
                        exercise = selectedExerciseFilter,
                        records = records,
                        period = selectedPeriod
                    )
                }

                val allTimeVolumeMax = remember(selectedExerciseFilter, records) {
                    val allVolumeData = prepareVolumeData(
                        exercise = selectedExerciseFilter,
                        records = records,
                        period = null
                    )
                    val maxRight = allVolumeData.maxOfOrNull { it.volumeRight } ?: 0f
                    val maxLeft = allVolumeData.mapNotNull { it.volumeLeft }.maxOrNull() ?: 0f
                    maxOf(maxRight, maxLeft)
                }

                VolumeChart(
                    data = volumeData,
                    period = selectedPeriod,
                    allTimeVolumeMax = allTimeVolumeMax
                )
            }
        }

        // アシストグラフ（アシストトラッキング有効時のみ）
        if (selectedExerciseFilter.assistanceTrackingEnabled) {
            item {
                val assistanceData = remember(selectedExerciseFilter, records, selectedPeriod) {
                    prepareAssistanceData(
                        exercise = selectedExerciseFilter,
                        records = records,
                        period = selectedPeriod
                    )
                }

                val allTimeAssistanceRange = remember(selectedExerciseFilter, records) {
                    val allAssistanceData = prepareAssistanceData(
                        exercise = selectedExerciseFilter,
                        records = records,
                        period = null
                    )
                    if (allAssistanceData.isNotEmpty()) {
                        Pair(
                            allAssistanceData.minOfOrNull { it.assistanceKg } ?: 0f,
                            allAssistanceData.maxOfOrNull { it.assistanceKg } ?: 0f
                        )
                    } else {
                        Pair(0f, 0f)
                    }
                }

                // 全記録の日付範囲を計算（メインチャートと同じX軸範囲にするため）
                val allRecordsDateRange = remember(selectedExerciseFilter, records) {
                    val exerciseRecords = records.filter { it.exerciseId == selectedExerciseFilter.id }
                    if (exerciseRecords.isNotEmpty()) {
                        val dates = exerciseRecords.mapNotNull {
                            try { LocalDate.parse(it.date) } catch (e: Exception) { null }
                        }
                        if (dates.isNotEmpty()) {
                            Pair(dates.minOrNull()!!, dates.maxOrNull()!!)
                        } else null
                    } else null
                }

                AssistanceChart(
                    data = assistanceData,
                    period = selectedPeriod,
                    allTimeAssistanceRange = allTimeAssistanceRange,
                    allRecordsDateRange = allRecordsDateRange
                )
            }
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

            // 距離の加重平均を計算（距離トラッキング有効時のみ）
            val weightedDistance: Float? = if (exercise.distanceTrackingEnabled) {
                val distanceRecords = sessionRecords.filter { it.distanceCm != null }
                if (distanceRecords.isNotEmpty()) {
                    val totalReps = distanceRecords.sumOf { it.valueRight }
                    if (totalReps > 0) {
                        distanceRecords.sumOf { (it.distanceCm ?: 0) * it.valueRight } / totalReps.toFloat()
                    } else {
                        distanceRecords.mapNotNull { it.distanceCm }.average().toFloat()
                    }
                } else {
                    null
                }
            } else {
                null
            }

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

                Triple(valueRight, valueLeft, weightedDistance)
            } else {
                // Bilateral: 右側のみ
                val values = sessionRecords.map { it.valueRight.toDouble() }
                val value = when (graphType) {
                    GraphType.Average -> values.average().toFloat()
                    GraphType.Max -> values.maxOrNull()?.toFloat() ?: 0f
                    GraphType.Total -> values.sum().toFloat()
                }
                Triple(value, null, weightedDistance)
            }
        }

    // データポイントを作成
    return sessionsByDate.map { (date, values) ->
        val (valueRight, valueLeft, distanceCm) = values

        val label = if (valueLeft != null) {
            String.format("$date: R%d$unit / L%d$unit", valueRight.roundToInt(), valueLeft.roundToInt())
        } else {
            String.format("$date: %d$unit", valueRight.roundToInt())
        }

        GraphDataPoint(
            date = date,
            value = valueRight,
            valueLeft = valueLeft,
            label = label,
            distanceCm = distanceCm
        )
    }.sortedBy { it.date }
}

// ボリュームデータ準備関数
fun prepareVolumeData(
    exercise: Exercise,
    records: List<TrainingRecord>,
    period: Period?
): List<VolumeDataPoint> {
    // 荷重トラッキングが無効な場合は空リスト
    if (!exercise.weightTrackingEnabled) {
        return emptyList()
    }

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

    // 荷重データがあるレコードのみ抽出
    val recordsWithWeight = filteredRecords.filter { it.weightG != null && it.weightG > 0 }
    if (recordsWithWeight.isEmpty()) {
        return emptyList()
    }

    // 日付ごとにグループ化してボリュームを計算
    val volumeByDate = recordsWithWeight
        .groupBy { it.date }
        .mapValues { (_, sessionRecords) ->
            val isUnilateral = sessionRecords.firstOrNull()?.valueLeft != null

            if (isUnilateral) {
                // Unilateral: 左右別々にボリューム計算
                val volumeRight = sessionRecords.sumOf { record ->
                    record.valueRight * (record.weightG ?: 0) / 1000.0
                }.toFloat()
                val volumeLeft = sessionRecords.sumOf { record ->
                    (record.valueLeft ?: 0) * (record.weightG ?: 0) / 1000.0
                }.toFloat()
                Pair(volumeRight, volumeLeft)
            } else {
                // Bilateral: 右側のみ
                val volume = sessionRecords.sumOf { record ->
                    record.valueRight * (record.weightG ?: 0) / 1000.0
                }.toFloat()
                Pair(volume, null)
            }
        }

    // データポイントを作成
    return volumeByDate.map { (date, volumes) ->
        val (volumeRight, volumeLeft) = volumes
        VolumeDataPoint(
            date = date,
            volumeRight = volumeRight,
            volumeLeft = volumeLeft
        )
    }.sortedBy { it.date }
}

// アシストデータ準備関数
fun prepareAssistanceData(
    exercise: Exercise,
    records: List<TrainingRecord>,
    period: Period?
): List<AssistanceDataPoint> {
    // アシストトラッキングが無効な場合は空リスト
    if (!exercise.assistanceTrackingEnabled) {
        return emptyList()
    }

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

    // アシストデータがあるレコードのみ抽出
    val recordsWithAssistance = filteredRecords.filter { it.assistanceG != null && it.assistanceG > 0 }
    if (recordsWithAssistance.isEmpty()) {
        return emptyList()
    }

    // 日付ごとにグループ化してアシスト量の加重平均を計算（レップ数で重み付け）
    val assistanceByDate = recordsWithAssistance
        .groupBy { it.date }
        .mapValues { (_, sessionRecords) ->
            val totalReps = sessionRecords.sumOf { it.valueRight }
            if (totalReps > 0) {
                sessionRecords.sumOf { (it.assistanceG ?: 0) * it.valueRight } / totalReps.toFloat() / 1000f
            } else {
                sessionRecords.mapNotNull { it.assistanceG }.average().toFloat() / 1000f
            }
        }

    // データポイントを作成
    return assistanceByDate.map { (date, assistanceKg) ->
        AssistanceDataPoint(
            date = date,
            assistanceKg = assistanceKg
        )
    }.sortedBy { it.date }
}

// 折れ線グラフコンポーネント
@Composable
fun LineChart(
    data: List<GraphDataPoint>,
    exercise: Exercise,
    period: Period?,
    isDistanceInverted: Boolean = false,
    allTimeDistanceRange: Pair<Float, Float>? = null
) {
    val appColors = LocalAppColors.current
    val hasDistanceData = data.any { it.distanceCm != null }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (hasDistanceData) 340.dp else 300.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
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
                                color = appColors.textSecondary
                            )
                            Text(
                                text = stringResource(R.string.record_training_for_exercise),
                                fontSize = 14.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                } else {
                    SimpleLineChart(
                        data = data,
                        exercise = exercise,
                        period = period,
                        isDistanceInverted = isDistanceInverted,
                        allTimeDistanceRange = allTimeDistanceRange
                    )
                }
            }

            // 凡例（距離データがある場合のみ）
            if (hasDistanceData && data.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 回数
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Green400, CircleShape)
                    )
                    Text(
                        text = " ${stringResource(R.string.legend_reps)}",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // 距離
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Blue600, CircleShape)
                    )
                    Text(
                        text = " ${stringResource(if (isDistanceInverted) R.string.legend_distance_inverted else R.string.legend_distance)}",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleLineChart(
    data: List<GraphDataPoint>,
    exercise: Exercise,
    period: Period?,
    isDistanceInverted: Boolean = false,
    allTimeDistanceRange: Pair<Float, Float>? = null
) {
    val appColors = LocalAppColors.current
    // Unilateral判定
    val isUnilateral = data.any { it.valueLeft != null }

    // 距離データの有無を判定
    val distanceValues = data.mapNotNull { it.distanceCm }
    val hasDistanceData = distanceValues.isNotEmpty()

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

        // 距離データがある場合は右Y軸用にパディングを拡大
        val leftPadding = 50.dp.toPx()
        val bottomPadding = 16.dp.toPx()
        val topPadding = 32.dp.toPx()
        val rightPadding = if (hasDistanceData) 55.dp.toPx() else 30.dp.toPx()

        // 距離の最小/最大値とレンジ（全期間のデータを使用してスケールを固定）
        val minDistance = allTimeDistanceRange?.first ?: 0f
        val maxDistance = allTimeDistanceRange?.second ?: (if (hasDistanceData) distanceValues.maxOrNull() ?: 0f else 0f)
        // ±7%のマージンを追加
        val distanceMargin = (maxDistance - minDistance) * 0.07f
        val adjustedMinDistance = (minDistance - distanceMargin).coerceAtLeast(0f)
        val adjustedMaxDistance = maxDistance + distanceMargin
        val distanceRange = (adjustedMaxDistance - adjustedMinDistance).coerceAtLeast(1f)

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
                color = appColors.border.copy(alpha = 0.3f),
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

        // 右Y軸ラベル描画（距離）- 底部のラベルはスキップしてX軸との干渉を回避
        if (hasDistanceData) {
            val distanceLabels = calculateDistanceYAxisLabels(adjustedMinDistance, adjustedMaxDistance)
            val bottomThreshold = topPadding + graphHeight - 15.dp.toPx() // 底部から15dpは描画しない

            distanceLabels.forEach { labelValue ->
                val normalizedValue = (labelValue - adjustedMinDistance) / distanceRange
                val y = if (isDistanceInverted) {
                    // 反転: 上が大きい値
                    topPadding + normalizedValue * graphHeight
                } else {
                    // 通常: 下が大きい値（距離増加=壁から離れる=難易度up）
                    topPadding + graphHeight - normalizedValue * graphHeight
                }

                // 底部に近いラベルはスキップ（X軸ラベルとの干渉回避）
                if (y < bottomThreshold) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${labelValue.toInt()}cm",
                        size.width - rightPadding + 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        textPaint.apply {
                            textAlign = Paint.Align.LEFT
                        }
                    )
                }
            }
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
                    color = appColors.textPrimary.copy(alpha = 0.4f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Green400.copy(alpha = 0.6f),
                    radius = 4.dp.toPx(),
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
                            color = appColors.textPrimary.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Purple600.copy(alpha = 0.6f),
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        // 距離線の描画（Blue600）
        if (hasDistanceData) {
            val pathDistance = Path()
            var isFirstDistancePoint = true

            data.forEach { point ->
                point.distanceCm?.let { distance ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val normalizedDistance = (distance - adjustedMinDistance) / distanceRange
                        val y = if (isDistanceInverted) {
                            // 反転: 上が大きい値
                            topPadding + normalizedDistance * graphHeight
                        } else {
                            // 通常: 下が大きい値
                            topPadding + graphHeight - normalizedDistance * graphHeight
                        }

                        if (isFirstDistancePoint) {
                            pathDistance.moveTo(x, y)
                            isFirstDistancePoint = false
                        } else {
                            pathDistance.lineTo(x, y)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            drawPath(
                path = pathDistance,
                color = Blue600,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 距離ポイントの描画
            data.forEach { point ->
                point.distanceCm?.let { distance ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val normalizedDistance = (distance - adjustedMinDistance) / distanceRange
                        val y = if (isDistanceInverted) {
                            topPadding + normalizedDistance * graphHeight
                        } else {
                            topPadding + graphHeight - normalizedDistance * graphHeight
                        }

                        drawCircle(
                            color = appColors.textPrimary.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Blue600.copy(alpha = 0.6f),
                            radius = 4.dp.toPx(),
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

// ボリュームグラフコンポーネント
@Composable
fun VolumeChart(
    data: List<VolumeDataPoint>,
    period: Period?,
    allTimeVolumeMax: Float
) {
    val appColors = LocalAppColors.current
    val isUnilateral = data.any { it.volumeLeft != null }
    val volumeLabel = stringResource(R.string.legend_volume)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
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
                                color = appColors.textSecondary
                            )
                            Text(
                                text = stringResource(R.string.record_weight_for_graph),
                                fontSize = 14.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                } else {
                    SimpleVolumeChart(
                        data = data,
                        period = period,
                        allTimeVolumeMax = allTimeVolumeMax
                    )
                }
            }

            // 凡例
            if (data.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnilateral) {
                        // Unilateral: 右/左
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Green400, CircleShape)
                        )
                        Text(
                            text = " ${stringResource(R.string.right_short)}",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Purple600, CircleShape)
                        )
                        Text(
                            text = " ${stringResource(R.string.left_short)}",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    } else {
                        // Bilateral: ボリューム
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Green400, CircleShape)
                        )
                        Text(
                            text = " $volumeLabel",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleVolumeChart(
    data: List<VolumeDataPoint>,
    period: Period?,
    allTimeVolumeMax: Float
) {
    val appColors = LocalAppColors.current
    val isUnilateral = data.any { it.volumeLeft != null }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (data.isEmpty()) return@Canvas

        // Y軸スケール: 0〜max
        val maxValue = allTimeVolumeMax.coerceAtLeast(1f)
        val minValue = 0f
        val range = maxValue

        val leftPadding = 50.dp.toPx()
        val bottomPadding = 16.dp.toPx()
        val topPadding = 32.dp.toPx()
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
        val yLabels = calculateVolumeYAxisLabels(0f, maxValue)

        // Y軸グリッド線とラベル
        yLabels.forEach { labelValue ->
            val y = topPadding + graphHeight - (labelValue / range * graphHeight)

            drawLine(
                color = appColors.border.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx()
            )

            val labelText = if (labelValue >= 1000) {
                String.format("%.1ft", labelValue / 1000f)
            } else {
                "${labelValue.toInt()}kg"
            }

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
                val y = topPadding + graphHeight - (point.volumeRight / range * graphHeight)

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
                point.volumeLeft?.let { leftValue ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val y = topPadding + graphHeight - (leftValue / range * graphHeight)

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
                val y = topPadding + graphHeight - (point.volumeRight / range * graphHeight)

                drawCircle(
                    color = appColors.textPrimary.copy(alpha = 0.4f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Green400.copy(alpha = 0.6f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            } catch (e: Exception) {
                // ignore
            }
        }

        // ポイント描画（左側）- Unilateralのみ
        if (isUnilateral) {
            data.forEach { point ->
                point.volumeLeft?.let { leftValue ->
                    try {
                        val pointDate = LocalDate.parse(point.date)
                        val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                        val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                        val y = topPadding + graphHeight - (leftValue / range * graphHeight)

                        drawCircle(
                            color = appColors.textPrimary.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Purple600.copy(alpha = 0.6f),
                            radius = 4.dp.toPx(),
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

// アシストグラフコンポーネント
@Composable
fun AssistanceChart(
    data: List<AssistanceDataPoint>,
    period: Period?,
    allTimeAssistanceRange: Pair<Float, Float>,
    allRecordsDateRange: Pair<LocalDate, LocalDate>? = null
) {
    val appColors = LocalAppColors.current
    val assistanceLabel = stringResource(R.string.legend_assistance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
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
                                color = appColors.textSecondary
                            )
                            Text(
                                text = stringResource(R.string.record_assistance_for_graph),
                                fontSize = 14.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                } else {
                    SimpleAssistanceChart(
                        data = data,
                        period = period,
                        allTimeAssistanceRange = allTimeAssistanceRange,
                        allRecordsDateRange = allRecordsDateRange
                    )
                }
            }

            // 凡例
            if (data.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Amber500, CircleShape)
                    )
                    Text(
                        text = " $assistanceLabel",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleAssistanceChart(
    data: List<AssistanceDataPoint>,
    period: Period?,
    allTimeAssistanceRange: Pair<Float, Float>,
    allRecordsDateRange: Pair<LocalDate, LocalDate>? = null
) {
    val appColors = LocalAppColors.current
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (data.isEmpty()) return@Canvas

        // Y軸スケール: 全期間のデータを使用してスケールを固定
        val minValue = allTimeAssistanceRange.first.coerceAtLeast(0f)
        val maxValue = allTimeAssistanceRange.second.coerceAtLeast(minValue + 1f)
        // ±10%のマージンを追加
        val margin = (maxValue - minValue) * 0.1f
        val adjustedMin = (minValue - margin).coerceAtLeast(0f)
        val adjustedMax = maxValue + margin
        val range = (adjustedMax - adjustedMin).coerceAtLeast(1f)

        val leftPadding = 50.dp.toPx()
        val bottomPadding = 16.dp.toPx()
        val topPadding = 32.dp.toPx()
        val rightPadding = 30.dp.toPx()

        val graphWidth = size.width - leftPadding - rightPadding
        val graphHeight = size.height - topPadding - bottomPadding

        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }

        val today = LocalDate.now()
        // X軸の日付範囲: 全記録の範囲を使用（メインチャートと同じ）
        val startDate = if (period == null) {
            allRecordsDateRange?.first ?: try {
                LocalDate.parse(data.minOf { it.date })
            } catch (e: Exception) {
                today.minusDays(30)
            }
        } else {
            today.minusDays(period.days.toLong() - 1)
        }
        val endDate = today

        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val yLabels = calculateAssistanceYAxisLabels(adjustedMin, adjustedMax)

        // Y軸グリッド線とラベル（下がるほど進歩なので、上が大きい値）
        yLabels.forEach { labelValue ->
            val y = topPadding + graphHeight - ((labelValue - adjustedMin) / range * graphHeight)

            drawLine(
                color = appColors.border.copy(alpha = 0.3f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx()
            )

            val labelText = String.format("%.1fkg", labelValue)

            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                leftPadding - 8.dp.toPx(),
                y + 4.dp.toPx(),
                textPaint.apply {
                    textAlign = Paint.Align.RIGHT
                }
            )
        }

        // 線の描画（Amber500）
        val path = Path()
        var isFirstPoint = true

        data.forEach { point ->
            try {
                val pointDate = LocalDate.parse(point.date)
                val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                val y = topPadding + graphHeight - ((point.assistanceKg - adjustedMin) / range * graphHeight)

                if (isFirstPoint) {
                    path.moveTo(x, y)
                    isFirstPoint = false
                } else {
                    path.lineTo(x, y)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        drawPath(
            path = path,
            color = Amber500,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // ポイント描画
        data.forEach { point ->
            try {
                val pointDate = LocalDate.parse(point.date)
                val daysFromStart = ChronoUnit.DAYS.between(startDate, pointDate).toInt()

                val x = leftPadding + (daysFromStart.toFloat() / (totalDays - 1).coerceAtLeast(1)) * graphWidth
                val y = topPadding + graphHeight - ((point.assistanceKg - adjustedMin) / range * graphHeight)

                drawCircle(
                    color = appColors.textPrimary.copy(alpha = 0.4f),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Amber500.copy(alpha = 0.6f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            } catch (e: Exception) {
                // ignore
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

// アシストY軸用のラベル計算（最下部はスキップしてX軸との干渉を回避）
fun calculateAssistanceYAxisLabels(min: Float, max: Float): List<Float> {
    val range = max - min
    val interval = when {
        range < 5 -> 1f
        range < 10 -> 2f
        range < 25 -> 5f
        range < 50 -> 10f
        else -> 25f
    }

    val adjustedMin = (min / interval).toInt() * interval
    val adjustedMax = ((max / interval).toInt() + 1) * interval

    // 最下部（index 0）をスキップ
    return (1..5).map { i ->
        adjustedMin + (adjustedMax - adjustedMin) * i / 5f
    }
}

fun calculateYAxisLabels(min: Float, max: Float): List<Float> {
    val range = max
    val interval = when {
        range < 10 -> 1f
        range < 50 -> 5f
        range < 100 -> 10f
        else -> 25f
    }

    val adjustedMax = ((max / interval).toInt() + 1) * interval

    return (0..9).map { i ->
        (adjustedMax * i / 9).toFloat()
    }
}

// 距離Y軸用のラベル計算（6個程度、最下部はスキップしてX軸との干渉を回避）
fun calculateDistanceYAxisLabels(min: Float, max: Float): List<Float> {
    val range = max - min
    val interval = when {
        range < 5 -> 1f
        range < 10 -> 2f
        range < 25 -> 5f
        range < 50 -> 10f
        else -> 25f
    }

    val adjustedMin = (min / interval).toInt() * interval
    val adjustedMax = ((max / interval).toInt() + 1) * interval

    // 6個のラベルを生成し、最下部（index 0）をスキップ
    return (1..5).map { i ->
        adjustedMin + (adjustedMax - adjustedMin) * i / 5f
    }
}

// ボリュームY軸用のラベル計算
fun calculateVolumeYAxisLabels(min: Float, max: Float): List<Float> {
    val range = max - min
    val interval = when {
        range < 50 -> 10f
        range < 100 -> 20f
        range < 250 -> 50f
        range < 500 -> 100f
        range < 1000 -> 200f
        else -> 500f
    }

    val adjustedMax = ((max / interval).toInt() + 1) * interval

    return (0..5).map { i ->
        (adjustedMax * i / 5).toFloat()
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
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.statistics_summary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (statistics.totalSets == 0) {
                Text(
                    text = stringResource(R.string.no_data),
                    fontSize = 14.sp,
                    color = appColors.textSecondary
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
                    StatItem(stringResource(R.string.weekly_change), changeText)
                }

                // 荷重統計（weightTrackingEnabled時のみ）
                if (exercise.weightTrackingEnabled && (statistics.maxDailyVolume != null || statistics.maxWeight != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = appColors.divider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // ボリューム統計（左右別表示 or 単一表示）
                    if (isUnilateral && statistics.maxDailyVolumeLeft != null) {
                        // Unilateral: 左右並べて表示
                        statistics.maxDailyVolume?.let { maxVolRight ->
                            StatItemDual(
                                label = stringResource(R.string.max_daily_volume),
                                valueRight = String.format("%.1f kg", maxVolRight),
                                valueLeft = String.format("%.1f kg", statistics.maxDailyVolumeLeft ?: 0f)
                            )
                        }
                        statistics.avgDailyVolume?.let { avgVolRight ->
                            StatItemDual(
                                label = stringResource(R.string.avg_daily_volume),
                                valueRight = String.format("%.1f kg", avgVolRight),
                                valueLeft = String.format("%.1f kg", statistics.avgDailyVolumeLeft ?: 0f)
                            )
                        }
                    } else {
                        // Bilateral: 従来通り
                        statistics.maxDailyVolume?.let { maxVol ->
                            StatItem(stringResource(R.string.max_daily_volume), String.format("%.1f kg", maxVol))
                        }
                        statistics.avgDailyVolume?.let { avgVol ->
                            StatItem(stringResource(R.string.avg_daily_volume), String.format("%.1f kg", avgVol))
                        }
                    }

                    // 荷重統計（左右共通）
                    statistics.maxWeight?.let { maxW ->
                        StatItem(stringResource(R.string.max_weight), String.format("%.1f kg", maxW))
                    }
                    statistics.avgWeight?.let { avgW ->
                        StatItem(stringResource(R.string.avg_weight), String.format("%.1f kg", avgW))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = appColors.textSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: appColors.textTertiary
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
    val appColors = LocalAppColors.current
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
            color = appColors.textSecondary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueRight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Green400
            )
            Text(
                text = "|",
                fontSize = 14.sp,
                color = appColors.border
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

    // 荷重統計（weightTrackingEnabled時のみ）
    val weightStats = if (exercise.weightTrackingEnabled) {
        val recordsWithWeight = filteredRecords.filter { it.weightG != null && it.weightG > 0 }
        if (recordsWithWeight.isNotEmpty()) {
            // 日ごとにボリュームを集計（左右別々）
            val volumeByDateRight = recordsWithWeight
                .groupBy { it.date }
                .mapValues { (_, dayRecords) ->
                    dayRecords.sumOf { record ->
                        record.valueRight * (record.weightG ?: 0) / 1000.0
                    }.toFloat()
                }
                .values
                .toList()

            val volumeByDateLeft = if (isUnilateral) {
                recordsWithWeight
                    .groupBy { it.date }
                    .mapValues { (_, dayRecords) ->
                        dayRecords.sumOf { record ->
                            (record.valueLeft ?: 0) * (record.weightG ?: 0) / 1000.0
                        }.toFloat()
                    }
                    .values
                    .toList()
            } else null

            val weights = recordsWithWeight.map { (it.weightG ?: 0) / 1000f }

            object {
                val maxDailyVolume = volumeByDateRight.maxOrNull()
                val avgDailyVolume = if (volumeByDateRight.isNotEmpty()) volumeByDateRight.average().toFloat() else null
                val maxDailyVolumeLeft = volumeByDateLeft?.maxOrNull()
                val avgDailyVolumeLeft = if (volumeByDateLeft != null && volumeByDateLeft.isNotEmpty()) volumeByDateLeft.average().toFloat() else null
                val maxWeight = weights.maxOrNull()
                val avgWeight = if (weights.isNotEmpty()) weights.average().toFloat() else null
            }
        } else null
    } else null

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
            minLeft = valuesLeft.minOrNull(),
            maxDailyVolume = weightStats?.maxDailyVolume,
            avgDailyVolume = weightStats?.avgDailyVolume,
            maxDailyVolumeLeft = weightStats?.maxDailyVolumeLeft,
            avgDailyVolumeLeft = weightStats?.avgDailyVolumeLeft,
            maxWeight = weightStats?.maxWeight,
            avgWeight = weightStats?.avgWeight
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
            minLeft = null,
            maxDailyVolume = weightStats?.maxDailyVolume,
            avgDailyVolume = weightStats?.avgDailyVolume,
            maxDailyVolumeLeft = null,
            avgDailyVolumeLeft = null,
            maxWeight = weightStats?.maxWeight,
            avgWeight = weightStats?.avgWeight
        )
    }
}
package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.util.ProgramTimeEstimator

// Sealed class to represent items in the confirm list (for grouping loops)
private sealed class ConfirmListItem {
    abstract val sortOrder: Int

    data class StandaloneExercise(
        val exerciseIndex: Int,
        val pe: ProgramExercise,
        val exercise: Exercise
    ) : ConfirmListItem() {
        override val sortOrder: Int get() = pe.sortOrder
    }

    data class Loop(
        val loop: ProgramLoop,
        val exercises: List<Triple<Int, ProgramExercise, Exercise>>  // exerciseIndex, pe, exercise
    ) : ConfirmListItem() {
        override val sortOrder: Int get() = loop.sortOrder
    }
}

@Composable
internal fun ProgramConfirmStep(
    session: ProgramExecutionSession,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateInterval: (Int, Int) -> Unit,  // exerciseIndex, newInterval
    onUpdateSetCount: (Int, Int) -> Unit,  // exerciseIndex, newSetCount
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    onUseAllProgramValues: () -> Unit,
    onUseAllChallengeValues: () -> Unit,
    onUseAllPreviousRecordValues: () -> Unit,
    // 音声設定
    isAutoMode: Boolean,
    startCountdownSeconds: Int,
    isDynamicCountSoundEnabled: Boolean,
    isIsometricIntervalSoundEnabled: Boolean,
    isometricIntervalSeconds: Int,
    onAutoModeChange: (Boolean) -> Unit,
    onStartCountdownChange: (Int) -> Unit,
    onDynamicCountSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSecondsChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    val appColors = LocalAppColors.current
    var refreshKey by remember { mutableIntStateOf(0) }
    // 各種目の展開状態を親で管理（スクロール時も状態を保持）
    var expandedExercises by remember { mutableStateOf(session.exercises.indices.toSet()) }
    // 課題設定がある種目が1つでもあるか
    val hasChallengeExercise = session.exercises.any { (_, exercise) -> exercise.targetValue != null }
    // 推定時間を計算
    val estimatedMinutes = remember(session, session.exercises, session.sets, startCountdownSeconds) {
        val seconds = ProgramTimeEstimator.estimateSeconds(session, startCountdownSeconds)
        ProgramTimeEstimator.formatMinutes(seconds)
    }

    // ループの展開状態を管理
    var expandedLoopIds by remember { mutableStateOf(session.loops.map { it.id }.toSet()) }

    // ループとスタンドアロン種目をグループ化
    val confirmListItems = remember(session.exercises, session.loops, refreshKey) {
        val standaloneExercises = session.exercises.mapIndexedNotNull { index, (pe, exercise) ->
            if (pe.loopId == null) {
                ConfirmListItem.StandaloneExercise(index, pe, exercise)
            } else null
        }
        val loops = session.loops.map { loop ->
            val loopExercises = session.exercises.mapIndexedNotNull { index, (pe, exercise) ->
                if (pe.loopId == loop.id) Triple(index, pe, exercise) else null
            }
            ConfirmListItem.Loop(loop, loopExercises)
        }
        (standaloneExercises + loops).sortedBy { it.sortOrder }
    }

    // exerciseIndex → 表示番号のマップを作成（ソート順に基づく）
    val exerciseDisplayNumbers = remember(confirmListItems) {
        var displayNum = 0
        val map = mutableMapOf<Int, Int>()
        confirmListItems.forEach { item ->
            when (item) {
                is ConfirmListItem.StandaloneExercise -> {
                    displayNum++
                    map[item.exerciseIndex] = displayNum
                }
                is ConfirmListItem.Loop -> {
                    item.exercises.forEach { (exerciseIndex, _, _) ->
                        displayNum++
                        map[exerciseIndex] = displayNum
                    }
                }
            }
        }
        map
    }

    // 一括適用タブの選択状態
    var selectedBulkTab by remember { mutableIntStateOf(0) } // 0=Program, 1=Challenge, 2=Previous

    // 全てスクロール可能なリストとして表示
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ヘッダー: 種目数 + 推定時間
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.program_exercise_count, session.exercises.size),
                    fontSize = 14.sp,
                    color = appColors.textSecondary
                )
                Text(
                    text = stringResource(R.string.program_estimated_time, estimatedMinutes),
                    fontSize = 14.sp,
                    color = appColors.textSecondary
                )
            }
        }

        // 設定セクション
        item {
            SettingsSection(
                isAutoMode = isAutoMode,
                startCountdownSeconds = startCountdownSeconds,
                isDynamicCountSoundEnabled = isDynamicCountSoundEnabled,
                isIsometricIntervalSoundEnabled = isIsometricIntervalSoundEnabled,
                isometricIntervalSeconds = isometricIntervalSeconds,
                onAutoModeChange = onAutoModeChange,
                onStartCountdownChange = onStartCountdownChange,
                onDynamicCountSoundChange = onDynamicCountSoundChange,
                onIsometricIntervalSoundChange = onIsometricIntervalSoundChange,
                onIsometricIntervalSecondsChange = onIsometricIntervalSecondsChange
            )
        }

        // 一括適用タブ
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.auto_fill_target_label),
                    fontSize = 12.sp,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BulkSettingTab(
                        text = stringResource(R.string.program_use_program),
                        isSelected = selectedBulkTab == 0,
                        onClick = {
                            selectedBulkTab = 0
                            onUseAllProgramValues()
                            refreshKey++
                        }
                    )
                    if (hasChallengeExercise) {
                        BulkSettingTab(
                            text = stringResource(R.string.program_use_challenge),
                            isSelected = selectedBulkTab == 1,
                            onClick = {
                                selectedBulkTab = 1
                                onUseAllChallengeValues()
                                refreshKey++
                            }
                        )
                    }
                    BulkSettingTab(
                        text = stringResource(R.string.program_use_previous),
                        isSelected = selectedBulkTab == if (hasChallengeExercise) 2 else 1,
                        onClick = {
                            selectedBulkTab = if (hasChallengeExercise) 2 else 1
                            onUseAllPreviousRecordValues()
                            refreshKey++
                        }
                    )
                }
            }
        }

        // 種目リスト
        items(
                items = confirmListItems,
                key = { item ->
                    when (item) {
                        is ConfirmListItem.StandaloneExercise -> "exercise-${item.exerciseIndex}-$refreshKey"
                        is ConfirmListItem.Loop -> "loop-${item.loop.id}-$refreshKey"
                    }
                }
            ) { item ->
                when (item) {
                    is ConfirmListItem.StandaloneExercise -> {
                        val (exerciseIndex, pe, exercise) = Triple(item.exerciseIndex, item.pe, item.exercise)
                        val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex }
                        val firstRoundSets = setsForExercise.filter { it.roundNumber == 1 }
                        val displaySets = if (exercise.laterality == "Unilateral") {
                            firstRoundSets.filter { it.side == "Right" }
                        } else {
                            firstRoundSets
                        }

                        ProgramConfirmExerciseCard(
                            exerciseIndex = exerciseIndex,
                            displayNumber = exerciseDisplayNumbers[exerciseIndex] ?: (exerciseIndex + 1),
                            exercise = exercise,
                            programExercise = pe,
                            sets = displaySets,
                            allSets = session.sets,
                            isExpanded = exerciseIndex in expandedExercises,
                            loopRounds = null,
                            onToggleExpanded = {
                                expandedExercises = if (exerciseIndex in expandedExercises) {
                                    expandedExercises - exerciseIndex
                                } else {
                                    expandedExercises + exerciseIndex
                                }
                            },
                            onUpdateValue = onUpdateTargetValue,
                            onUpdateInterval = { newInterval ->
                                onUpdateInterval(exerciseIndex, newInterval)
                            },
                            onUpdateSetCount = { newSetCount ->
                                onUpdateSetCount(exerciseIndex, newSetCount)
                            },
                            onUpdateWeightG = onUpdateSetWeightG,
                            onUpdateDistanceCm = onUpdateSetDistanceCm,
                            onUpdateAssistanceG = onUpdateSetAssistanceG
                        )
                    }
                    is ConfirmListItem.Loop -> {
                        val loop = item.loop
                        val isLoopExpanded = loop.id in expandedLoopIds

                        ProgramConfirmLoopBlock(
                            loop = loop,
                            exercises = item.exercises,
                            session = session,
                            exerciseDisplayNumbers = exerciseDisplayNumbers,
                            isExpanded = isLoopExpanded,
                            expandedExercises = expandedExercises,
                            onToggleLoopExpanded = {
                                expandedLoopIds = if (loop.id in expandedLoopIds) {
                                    expandedLoopIds - loop.id
                                } else {
                                    expandedLoopIds + loop.id
                                }
                            },
                            onToggleExerciseExpanded = { exerciseIndex ->
                                expandedExercises = if (exerciseIndex in expandedExercises) {
                                    expandedExercises - exerciseIndex
                                } else {
                                    expandedExercises + exerciseIndex
                                }
                            },
                            onUpdateTargetValue = onUpdateTargetValue,
                            onUpdateInterval = onUpdateInterval,
                            onUpdateSetCount = onUpdateSetCount,
                            onUpdateSetWeightG = onUpdateSetWeightG,
                            onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                            onUpdateSetAssistanceG = onUpdateSetAssistanceG
                        )
                    }
                }
            }

            // 開始ボタン（リストの最後、スクロール対応）
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green600),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.program_start),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
        }
    }
}

// 設定セクション（ProgramConfirmStep内）折りたたみ式
@Composable
internal fun SettingsSection(
    isAutoMode: Boolean,
    startCountdownSeconds: Int,
    isDynamicCountSoundEnabled: Boolean,
    isIsometricIntervalSoundEnabled: Boolean,
    isometricIntervalSeconds: Int,
    onAutoModeChange: (Boolean) -> Unit,
    onStartCountdownChange: (Int) -> Unit,
    onDynamicCountSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSecondsChange: (Int) -> Unit
) {
    val appColors = LocalAppColors.current
    // 折りたたみ状態（デフォルトは展開状態）
    var isExpanded by remember { mutableStateOf(true) }
    // シェブロンの回転アニメーション
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron"
    )
    // ローカル状態（間隔秒数入力用）
    var intervalText by remember(isometricIntervalSeconds) { mutableStateOf(isometricIntervalSeconds.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 折りたたみヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙",
                        fontSize = 14.sp,
                        color = appColors.textTertiary
                    )
                    Text(
                        text = stringResource(R.string.settings),
                        fontSize = 14.sp,
                        color = appColors.textTertiary
                    )
                }
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // 折りたたみコンテンツ
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // タイマーモード
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.auto_mode),
                        fontSize = 14.sp,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = if (isAutoMode) {
                            stringResource(R.string.timer_mode_on_description)
                        } else {
                            stringResource(R.string.timer_mode_off_description)
                        },
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }
                Switch(
                    checked = isAutoMode,
                    onCheckedChange = onAutoModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = appColors.switchThumb,
                        checkedTrackColor = Orange600,
                        uncheckedThumbColor = appColors.switchThumb,
                        uncheckedTrackColor = Slate500
                    )
                )
            }

            // 開始カウントダウン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.start_countdown),
                    fontSize = 14.sp,
                    color = appColors.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (startCountdownSeconds > 0) onStartCountdownChange(startCountdownSeconds - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                    }
                    Text(
                        text = startCountdownSeconds.toString(),
                        fontSize = 14.sp,
                        color = appColors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(24.dp)
                    )
                    IconButton(
                        onClick = { onStartCountdownChange(startCountdownSeconds + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                    }
                    Text(
                        text = stringResource(R.string.unit_seconds_short),
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            // 数え上げ音（ダイナミック種目）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dynamic_count_sound_label),
                        fontSize = 14.sp,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.dynamic_count_sound_description),
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }
                Switch(
                    checked = isDynamicCountSoundEnabled,
                    onCheckedChange = onDynamicCountSoundChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = appColors.switchThumb,
                        checkedTrackColor = Orange600,
                        uncheckedThumbColor = appColors.switchThumb,
                        uncheckedTrackColor = Slate500
                    )
                )
            }

            // 間隔通知（アイソメトリック種目）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.isometric_interval_sound_label),
                        fontSize = 14.sp,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.isometric_interval_sound_description),
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = intervalText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    intervalText = newValue
                                    newValue.toIntOrNull()?.let { onIsometricIntervalSecondsChange(it) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = appColors.textPrimary
                            ),
                            decorationBox = { innerTextField ->
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        innerTextField()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(horizontal = 2.dp)
                                            .then(Modifier.drawBehind {
                                                drawLine(
                                                    color = appColors.textSecondary,
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            })
                                    )
                                }
                            }
                        )
                    }
                    Text(
                        text = stringResource(R.string.unit_seconds_short),
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isIsometricIntervalSoundEnabled,
                        onCheckedChange = onIsometricIntervalSoundChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = appColors.switchThumb,
                            checkedTrackColor = Orange600,
                            uncheckedThumbColor = appColors.switchThumb,
                            uncheckedTrackColor = Slate500
                        )
                    )
                }
            }
                }
            }
        }
    }
}

@Composable
internal fun ProgramConfirmExerciseCard(
    exerciseIndex: Int,
    displayNumber: Int,  // 表示用番号（ソート順）
    exercise: Exercise,
    programExercise: ProgramExercise,
    sets: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    isExpanded: Boolean,
    loopRounds: Int? = null,  // ループ内種目の場合はラウンド数
    onToggleExpanded: () -> Unit,
    onUpdateValue: (Int, Int) -> Unit,
    onUpdateInterval: (Int) -> Unit,
    onUpdateSetCount: (Int) -> Unit,
    onUpdateWeightG: (Int, Int?) -> Unit,
    onUpdateDistanceCm: (Int, Int?) -> Unit,
    onUpdateAssistanceG: (Int, Int?) -> Unit
) {
    val appColors = LocalAppColors.current
    // 現在のセット数とインターバルを取得
    val currentSetCount = sets.maxOfOrNull { it.setNumber } ?: programExercise.sets
    val currentInterval = sets.firstOrNull()?.intervalSeconds ?: programExercise.intervalSeconds

    // ローカル状態（UIの編集用）
    var intervalText by remember(exerciseIndex, currentInterval) { mutableStateOf(currentInterval.toString()) }

    // シェブロン回転アニメーション（状態は親から渡される）
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // ヘッダー: 番号バッジ + 種目名 + セット数バッジ + シェブロン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 番号バッジ
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Amber600, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayNumber.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                }

                // 種目名
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                // セット数バッジ
                Box(
                    modifier = Modifier
                        .background(appColors.cardBackgroundSecondary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sets_format, currentSetCount),
                        fontSize = 12.sp,
                        color = appColors.textTertiary
                    )
                }

                // ループ内種目の場合はラウンド数バッジを表示
                if (loopRounds != null) {
                    Box(
                        modifier = Modifier
                            .background(Purple600.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loop_round_format, loopRounds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple400
                        )
                    }
                }

                // シェブロン
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // コンテンツ（折りたたみ可能）
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    // セット数変更（ステッパー）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sets_label_short),
                            fontSize = 13.sp,
                            color = appColors.textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .background(appColors.cardBackgroundSecondary, RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentSetCount > 1) onUpdateSetCount(currentSetCount - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("−", fontSize = 16.sp, color = appColors.textSecondary)
                            }
                            Text(
                                text = currentSetCount.toString(),
                                fontSize = 14.sp,
                                color = appColors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(28.dp)
                            )
                            IconButton(
                                onClick = { onUpdateSetCount(currentSetCount + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp, color = appColors.textSecondary)
                            }
                        }
                    }

                    // セットごとの値（統一行レイアウト）
                    sets.forEachIndexed { index, set ->
                        // オブジェクト参照ではなくセマンティックに検索（copy()で参照が変わるため）
                        // roundNumberも含めて正確にマッチング（ループ内種目の重複防止）
                        val setIndex = allSets.indexOfFirst {
                            it.exerciseIndex == set.exerciseIndex &&
                            it.setNumber == set.setNumber &&
                            it.side == set.side &&
                            it.roundNumber == set.roundNumber
                        }
                        if (setIndex < 0) return@forEachIndexed
                        val currentSet = allSets[setIndex]

                        if (index > 0) Spacer(modifier = Modifier.height(10.dp))

                        // セット見出し（1セット目だけ右に「前回値」カラムラベル）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.set_number_format, set.setNumber),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (index == 0) {
                                Text(
                                    text = stringResource(R.string.program_use_previous),
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // 回数 / 時間
                        var repsText by remember(setIndex, currentSet.targetValue) {
                            mutableStateOf(currentSet.targetValue.toString())
                        }
                        UnifiedStepperRow(
                            label = stringResource(
                                if (exercise.type == "Isometric") R.string.time_label
                                else R.string.reps_label
                            ),
                            valueText = repsText,
                            onValueTextChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    repsText = newValue
                                    newValue.toIntOrNull()?.let { onUpdateValue(setIndex, it) }
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            previousText = set.previousValue?.toString() ?: "-",
                            onMinus = {
                                val newValue = (currentSet.targetValue - 1).coerceAtLeast(0)
                                onUpdateValue(setIndex, newValue)
                            },
                            onPlus = {
                                onUpdateValue(setIndex, currentSet.targetValue + 1)
                            },
                            minusEnabled = currentSet.targetValue > 0
                        )

                        // 距離（cm）
                        if (exercise.distanceTrackingEnabled) {
                            var distanceStr by remember(setIndex, currentSet.distanceCm) {
                                mutableStateOf(currentSet.distanceCm?.toString() ?: "")
                            }
                            UnifiedStepperRow(
                                label = stringResource(R.string.distance_input_label),
                                valueText = distanceStr,
                                onValueTextChange = { value ->
                                    val normalized = value
                                        .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                        .replace("．", ".").replace("－", "-")
                                    if (normalized.isEmpty() || normalized == "-" || normalized.toIntOrNull() != null) {
                                        distanceStr = normalized
                                        onUpdateDistanceCm(setIndex, parseProgramDistanceCmValue(normalized))
                                    }
                                },
                                keyboardType = KeyboardType.Number,
                                previousText = currentSet.previousDistanceCm?.toString() ?: "-",
                                onMinus = {
                                    val newCm = ((currentSet.distanceCm ?: 0) - 1).coerceAtLeast(0)
                                    onUpdateDistanceCm(setIndex, newCm)
                                },
                                onPlus = {
                                    val newCm = ((currentSet.distanceCm ?: 0) + 1).coerceAtLeast(0)
                                    onUpdateDistanceCm(setIndex, newCm)
                                },
                                minusEnabled = (currentSet.distanceCm ?: 0) > 0
                            )
                        }

                        // 荷重（kg）
                        if (exercise.weightTrackingEnabled) {
                            var weightStr by remember(setIndex, currentSet.weightG) {
                                mutableStateOf(currentSet.weightG?.let { "%.1f".format(it / 1000.0) } ?: "")
                            }
                            UnifiedStepperRow(
                                label = stringResource(R.string.weight_input_label),
                                valueText = weightStr,
                                onValueTextChange = { value ->
                                    val normalized = value
                                        .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                        .replace("．", ".")
                                    val isValid = normalized.isEmpty() || normalized == "." ||
                                        normalized.matches(Regex("^\\d*\\.?\\d?$"))
                                    if (isValid) {
                                        weightStr = normalized
                                        onUpdateWeightG(setIndex, parseProgramWeightGValue(normalized))
                                    }
                                },
                                keyboardType = KeyboardType.Decimal,
                                previousText = currentSet.previousWeightG?.let { "%.1f".format(it / 1000.0) } ?: "-",
                                onMinus = {
                                    val newG = ((currentSet.weightG ?: 0) - 1000).coerceAtLeast(0)
                                    onUpdateWeightG(setIndex, newG)
                                },
                                onPlus = {
                                    val newG = ((currentSet.weightG ?: 0) + 1000).coerceAtLeast(0)
                                    onUpdateWeightG(setIndex, newG)
                                },
                                minusEnabled = (currentSet.weightG ?: 0) > 0
                            )
                        }

                        // アシスト（kg）
                        if (exercise.assistanceTrackingEnabled) {
                            var assistanceStr by remember(setIndex, currentSet.assistanceG) {
                                mutableStateOf(currentSet.assistanceG?.let { "%.1f".format(it / 1000.0) } ?: "")
                            }
                            UnifiedStepperRow(
                                label = stringResource(R.string.assistance_input_label),
                                valueText = assistanceStr,
                                onValueTextChange = { value ->
                                    val normalized = value
                                        .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                        .replace("．", ".")
                                    val isValid = normalized.isEmpty() || normalized == "." ||
                                        normalized.matches(Regex("^\\d*\\.?\\d?$"))
                                    if (isValid) {
                                        assistanceStr = normalized
                                        onUpdateAssistanceG(setIndex, parseProgramWeightGValue(normalized))
                                    }
                                },
                                keyboardType = KeyboardType.Decimal,
                                previousText = currentSet.previousAssistanceG?.let { "%.1f".format(it / 1000.0) } ?: "-",
                                onMinus = {
                                    val newG = ((currentSet.assistanceG ?: 0) - 1000).coerceAtLeast(0)
                                    onUpdateAssistanceG(setIndex, newG)
                                },
                                onPlus = {
                                    val newG = ((currentSet.assistanceG ?: 0) + 1000).coerceAtLeast(0)
                                    onUpdateAssistanceG(setIndex, newG)
                                },
                                minusEnabled = (currentSet.assistanceG ?: 0) > 0
                            )
                        }
                    }

                    // 休憩時間
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.interval_short),
                            fontSize = 12.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(28.dp)
                                .background(appColors.cardBackgroundSecondary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = intervalText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        intervalText = newValue
                                        newValue.toIntOrNull()?.let { onUpdateInterval(it) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = appColors.textPrimary
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.unit_seconds_short),
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}

/**
 * 一括設定タブ（pill型ボタン）
 */
@Composable
private fun BulkSettingTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) Amber600 else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isSelected) appColors.textPrimary else appColors.textSecondary
        )
    }
}


/**
 * ループブロック（オレンジ枠で囲まれたグループ）
 */
@Composable
private fun ProgramConfirmLoopBlock(
    loop: ProgramLoop,
    exercises: List<Triple<Int, ProgramExercise, Exercise>>,  // exerciseIndex, pe, exercise
    session: ProgramExecutionSession,
    exerciseDisplayNumbers: Map<Int, Int>,
    isExpanded: Boolean,
    expandedExercises: Set<Int>,
    onToggleLoopExpanded: () -> Unit,
    onToggleExerciseExpanded: (Int) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateInterval: (Int, Int) -> Unit,
    onUpdateSetCount: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit
) {
    val appColors = LocalAppColors.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "loopChevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Orange600, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // ループヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLoopExpanded() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ループアイコン
                Text(
                    text = "🔁",
                    fontSize = 16.sp
                )

                // ループ情報
                Text(
                    text = stringResource(R.string.loop_label),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange600
                )

                Spacer(modifier = Modifier.weight(1f))

                // ラウンド数バッジ
                Box(
                    modifier = Modifier
                        .background(Purple600.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loop_round_format, loop.rounds),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple400
                    )
                }

                // ラウンド間休憩バッジ
                if (loop.restBetweenRounds > 0) {
                    Box(
                        modifier = Modifier
                            .background(appColors.cardBackgroundSecondary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loop_rest_format, loop.restBetweenRounds),
                            fontSize = 12.sp,
                            color = appColors.textTertiary
                        )
                    }
                }

                // シェブロン
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // ループ内種目（折りたたみ可能）
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercises.forEach { (exerciseIndex, pe, exercise) ->
                        val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex }
                        val firstRoundSets = setsForExercise.filter { it.roundNumber == 1 }
                        val displaySets = if (exercise.laterality == "Unilateral") {
                            firstRoundSets.filter { it.side == "Right" }
                        } else {
                            firstRoundSets
                        }

                        // ループ内種目カード（左ボーダー付き）
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 左のオレンジボーダー
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(Amber500)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // 種目カード
                            ProgramConfirmExerciseCard(
                                exerciseIndex = exerciseIndex,
                                displayNumber = exerciseDisplayNumbers[exerciseIndex] ?: (exerciseIndex + 1),
                                exercise = exercise,
                                programExercise = pe,
                                sets = displaySets,
                                allSets = session.sets,
                                isExpanded = exerciseIndex in expandedExercises,
                                loopRounds = null,  // ループヘッダーで表示するのでここでは不要
                                onToggleExpanded = { onToggleExerciseExpanded(exerciseIndex) },
                                onUpdateValue = onUpdateTargetValue,
                                onUpdateInterval = { newInterval ->
                                    onUpdateInterval(exerciseIndex, newInterval)
                                },
                                onUpdateSetCount = { newSetCount ->
                                    onUpdateSetCount(exerciseIndex, newSetCount)
                                },
                                onUpdateWeightG = onUpdateSetWeightG,
                                onUpdateDistanceCm = onUpdateSetDistanceCm,
                                onUpdateAssistanceG = onUpdateSetAssistanceG
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun parseProgramDistanceCmValue(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed == "-") return null
    return trimmed.toIntOrNull()
}

private fun parseProgramWeightGValue(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed == ".") return null
    val kg = trimmed.toDoubleOrNull() ?: return null
    return (kg * 1000).toInt()
}

@Composable
private fun OrangeCircleStepButton(
    label: String,
    enabled: Boolean = true,
    onStep: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnStep by rememberUpdatedState(onStep)
    val bgColor = if (enabled) Orange600 else Color(0xFF4A5B70)
    val labelColor = if (enabled) Color.White else Color(0xFF8696AA)
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(bgColor, CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            currentOnStep()
                            var repeatJob: Job? = null
                            try {
                                repeatJob = scope.launch {
                                    delay(350)
                                    while (isActive) {
                                        currentOnStep()
                                        delay(80)
                                    }
                                }
                                waitForUpOrCancellation()
                            } finally {
                                repeatJob?.cancel()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

@Composable
private fun UnifiedStepperRow(
    label: String,
    valueText: String,
    onValueTextChange: (String) -> Unit,
    keyboardType: KeyboardType,
    previousText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = appColors.textSecondary,
            modifier = Modifier.width(96.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrangeCircleStepButton(
                label = "−",
                enabled = minusEnabled,
                onStep = onMinus
            )
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = valueText,
                onValueChange = onValueTextChange,
                modifier = Modifier.width(60.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = appColors.textPrimary
                )
            )
            Spacer(modifier = Modifier.width(14.dp))
            OrangeCircleStepButton(
                label = "+",
                onStep = onPlus
            )
        }
        Text(
            text = previousText,
            fontSize = 13.sp,
            color = Slate500,
            textAlign = TextAlign.End,
            modifier = Modifier.width(60.dp)
        )
    }
}
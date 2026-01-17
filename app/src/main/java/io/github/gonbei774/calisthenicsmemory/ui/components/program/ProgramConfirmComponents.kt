package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import kotlin.math.roundToInt

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
    onUpdateExerciseSetsValue: (Int, Int) -> Unit,  // exerciseIndex, delta - 種目内の全セット一括更新
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
    var refreshKey by remember { mutableIntStateOf(0) }
    // 各種目の展開状態を親で管理（スクロール時も状態を保持）
    var expandedExercises by remember { mutableStateOf(session.exercises.indices.toSet()) }
    // 課題設定がある種目が1つでもあるか
    val hasChallengeExercise = session.exercises.any { (_, exercise) -> exercise.targetValue != null }
    // 推定時間を計算
    val estimatedMinutes = calculateEstimatedMinutes(session)

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
                    color = Slate400
                )
                Text(
                    text = stringResource(R.string.program_estimated_time, estimatedMinutes),
                    fontSize = 14.sp,
                    color = Slate400
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
                            onUpdateValue = { setIndex, newValue ->
                                onUpdateTargetValue(setIndex, newValue)
                                if (exercise.laterality == "Unilateral") {
                                    val rightSet = session.sets[setIndex]
                                    val leftSetIndex = session.sets.indexOfFirst {
                                        it.exerciseIndex == exerciseIndex &&
                                        it.setNumber == rightSet.setNumber &&
                                        it.side == "Left"
                                    }
                                    if (leftSetIndex >= 0) {
                                        onUpdateTargetValue(leftSetIndex, newValue)
                                    }
                                }
                            },
                            onUpdateInterval = { newInterval ->
                                onUpdateInterval(exerciseIndex, newInterval)
                            },
                            onUpdateSetCount = { newSetCount ->
                                onUpdateSetCount(exerciseIndex, newSetCount)
                            },
                            onUpdateAllSetsValue = { delta ->
                                onUpdateExerciseSetsValue(exerciseIndex, delta)
                            }
                        )
                    }
                    is ConfirmListItem.Loop -> {
                        val loop = item.loop
                        val isLoopExpanded = loop.id in expandedLoopIds

                        ProgramConfirmLoopBlock(
                            loop = loop,
                            exercises = item.exercises,
                            session = session,
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
                            onUpdateExerciseSetsValue = onUpdateExerciseSetsValue
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
        colors = CardDefaults.cardColors(containerColor = Slate800),
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
                        color = Slate300
                    )
                    Text(
                        text = stringResource(R.string.settings),
                        fontSize = 14.sp,
                        color = Slate300
                    )
                }
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Slate400,
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
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.auto_mode_description),
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
                Switch(
                    checked = isAutoMode,
                    onCheckedChange = onAutoModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Orange600,
                        uncheckedThumbColor = Color.White,
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
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (startCountdownSeconds > 0) onStartCountdownChange(startCountdownSeconds - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    }
                    Text(
                        text = startCountdownSeconds.toString(),
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(24.dp)
                    )
                    IconButton(
                        onClick = { onStartCountdownChange(startCountdownSeconds + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    }
                    Text(
                        text = stringResource(R.string.unit_seconds_short),
                        fontSize = 12.sp,
                        color = Slate400
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
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.dynamic_count_sound_description),
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
                Switch(
                    checked = isDynamicCountSoundEnabled,
                    onCheckedChange = onDynamicCountSoundChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Orange600,
                        uncheckedThumbColor = Color.White,
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
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.isometric_interval_sound_description),
                        fontSize = 11.sp,
                        color = Slate400
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
                                color = Color.White
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
                                                    color = Slate400,
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
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isIsometricIntervalSoundEnabled,
                        onCheckedChange = onIsometricIntervalSoundChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Orange600,
                            uncheckedThumbColor = Color.White,
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
    onUpdateAllSetsValue: (Int) -> Unit
) {
    val unit = stringResource(if (exercise.type == "Isometric") R.string.unit_seconds else R.string.unit_reps)

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
        colors = CardDefaults.cardColors(containerColor = Slate800),
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
                        text = (exerciseIndex + 1).toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 種目名
                Text(
                    text = exercise.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // セット数バッジ
                Box(
                    modifier = Modifier
                        .background(Slate700, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sets_format, currentSetCount),
                        fontSize = 12.sp,
                        color = Slate300
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
                    color = Slate400,
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
                            color = Slate400
                        )
                        Row(
                            modifier = Modifier
                                .background(Slate700, RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentSetCount > 1) onUpdateSetCount(currentSetCount - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("−", fontSize = 16.sp, color = Slate400)
                            }
                            Text(
                                text = currentSetCount.toString(),
                                fontSize = 14.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(28.dp)
                            )
                            IconButton(
                                onClick = { onUpdateSetCount(currentSetCount + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp, color = Slate400)
                            }
                        }
                    }

                    // カラムヘッダー（目標±ボタン付き / 前回）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 空（セットラベル用スペース）- 固定幅でコンパクトに
                        Spacer(modifier = Modifier.width(72.dp))

                        // 目標（±ボタン付き）
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // −ボタン
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onUpdateAllSetsValue(-1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", fontSize = 14.sp, color = Slate400)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.target_value_label),
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // +ボタン
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onUpdateAllSetsValue(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 14.sp, color = Slate400)
                            }
                        }

                        // 前回
                        Text(
                            text = stringResource(R.string.program_use_previous),
                            fontSize = 12.sp,
                            color = Slate500,
                            modifier = Modifier.width(70.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // セットごとの値
                    sets.forEach { set ->
                // オブジェクト参照ではなくセマンティックに検索（copy()で参照が変わるため）
                // roundNumberも含めて正確にマッチング（ループ内種目の重複防止）
                val setIndex = allSets.indexOfFirst {
                    it.exerciseIndex == set.exerciseIndex &&
                    it.setNumber == set.setNumber &&
                    it.side == set.side &&
                    it.roundNumber == set.roundNumber
                }
                if (setIndex < 0) return@forEach

                val currentSet = allSets[setIndex]
                var textValue by remember(setIndex, currentSet.targetValue) {
                    mutableStateOf(currentSet.targetValue.toString())
                }

                // 現在の種目の実際のセット数（Program/Challenge切り替え後も正しい値）
                val actualTotalSets = sets.maxOfOrNull { it.setNumber } ?: programExercise.sets

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // セットラベル（固定幅）
                    Text(
                        text = stringResource(R.string.set_format, set.setNumber, actualTotalSets),
                        fontSize = 14.sp,
                        color = Slate300,
                        modifier = Modifier.width(72.dp)
                    )

                    // 目標値入力（中央配置）
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        textValue = newValue
                                        newValue.toIntOrNull()?.let { onUpdateValue(setIndex, it) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
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
                                        // 下線
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .padding(horizontal = 4.dp)
                                                .then(Modifier.drawBehind {
                                                    drawLine(
                                                        color = Slate400,
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
                    }

                    // 前回値（70dp）
                    Text(
                        text = set.previousValue?.toString() ?: "-",
                        fontSize = 13.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(70.dp)
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
                                .background(Slate700, RoundedCornerShape(6.dp)),
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
                                    color = Color.White
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
            color = if (isSelected) Color.White else Slate400
        )
    }
}

/**
 * 推定時間を計算
 * - Dynamic: 1レップ2秒として概算
 * - Isometric: 目標秒数をそのまま使用
 * - 各セット後のインターバルを加算
 */
internal fun calculateEstimatedMinutes(session: ProgramExecutionSession): Int {
    var totalSeconds = 0
    session.sets.forEach { set ->
        val exercise = session.exercises[set.exerciseIndex].second
        val repSeconds = if (exercise.type == "Isometric") {
            set.targetValue
        } else {
            set.targetValue * 2  // 1レップ2秒
        }
        totalSeconds += repSeconds + set.intervalSeconds
    }
    return (totalSeconds / 60.0).roundToInt().coerceAtLeast(1)
}

/**
 * ループブロック（オレンジ枠で囲まれたグループ）
 */
@Composable
private fun ProgramConfirmLoopBlock(
    loop: ProgramLoop,
    exercises: List<Triple<Int, ProgramExercise, Exercise>>,  // exerciseIndex, pe, exercise
    session: ProgramExecutionSession,
    isExpanded: Boolean,
    expandedExercises: Set<Int>,
    onToggleLoopExpanded: () -> Unit,
    onToggleExerciseExpanded: (Int) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateInterval: (Int, Int) -> Unit,
    onUpdateSetCount: (Int, Int) -> Unit,
    onUpdateExerciseSetsValue: (Int, Int) -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "loopChevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Orange600, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800),
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
                            .background(Slate700, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loop_rest_format, loop.restBetweenRounds),
                            fontSize = 12.sp,
                            color = Slate300
                        )
                    }
                }

                // シェブロン
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Slate400,
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
                                exercise = exercise,
                                programExercise = pe,
                                sets = displaySets,
                                allSets = session.sets,
                                isExpanded = exerciseIndex in expandedExercises,
                                loopRounds = null,  // ループヘッダーで表示するのでここでは不要
                                onToggleExpanded = { onToggleExerciseExpanded(exerciseIndex) },
                                onUpdateValue = { setIndex, newValue ->
                                    onUpdateTargetValue(setIndex, newValue)
                                    if (exercise.laterality == "Unilateral") {
                                        val rightSet = session.sets[setIndex]
                                        val leftSetIndex = session.sets.indexOfFirst {
                                            it.exerciseIndex == exerciseIndex &&
                                            it.setNumber == rightSet.setNumber &&
                                            it.side == "Left"
                                        }
                                        if (leftSetIndex >= 0) {
                                            onUpdateTargetValue(leftSetIndex, newValue)
                                        }
                                    }
                                },
                                onUpdateInterval = { newInterval ->
                                    onUpdateInterval(exerciseIndex, newInterval)
                                },
                                onUpdateSetCount = { newSetCount ->
                                    onUpdateSetCount(exerciseIndex, newSetCount)
                                },
                                onUpdateAllSetsValue = { delta ->
                                    onUpdateExerciseSetsValue(exerciseIndex, delta)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
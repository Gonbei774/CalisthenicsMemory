package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import kotlin.math.roundToInt

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
    onUpdateAllExercisesValue: (Int) -> Unit,  // delta (+1 or -1)
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
    // 課題設定がある種目が1つでもあるか
    val hasChallengeExercise = session.exercises.any { (_, exercise) -> exercise.targetValue != null }
    // 推定時間を計算
    val estimatedMinutes = calculateEstimatedMinutes(session)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ヘッダー: プログラム名 + 種目数 + 推定時間
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = session.program.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
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

        Spacer(modifier = Modifier.height(12.dp))

        // 一括適用ボタン（一列横並び）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onUseAllProgramValues()
                    refreshKey++
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.program_use_program),
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
            if (hasChallengeExercise) {
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        onUseAllChallengeValues()
                        refreshKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green600),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.program_use_challenge),
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = {
                    onUseAllPreviousRecordValues()
                    refreshKey++
                },
                colors = ButtonDefaults.buttonColors(containerColor = Purple600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.program_use_previous),
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 全種目± ボタン行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate700, RoundedCornerShape(6.dp))
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onUpdateAllExercisesValue(-1)
                    refreshKey++
                },
                modifier = Modifier.size(28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.all_exercises_label),
                fontSize = 12.sp,
                color = Slate400
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onUpdateAllExercisesValue(1)
                    refreshKey++
                },
                modifier = Modifier.size(28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 種目リスト
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // key = refreshKey ensures recomposition
            items(
                items = session.exercises.mapIndexed { index, pair -> index to pair },
                key = { (index, _) -> "$index-$refreshKey" }
            ) { (exerciseIndex, pair) ->
                val (pe, exercise) = pair
                val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex }
                val displaySets = if (exercise.laterality == "Unilateral") {
                    // 片側種目: 右側のセットのみ表示（代表値として）
                    setsForExercise.filter { it.side == "Right" }
                } else {
                    setsForExercise
                }

                ProgramConfirmExerciseCard(
                    exerciseIndex = exerciseIndex,
                    exercise = exercise,
                    programExercise = pe,
                    sets = displaySets,
                    allSets = session.sets,
                    onUpdateValue = { setIndex, newValue ->
                        onUpdateTargetValue(setIndex, newValue)
                        // 片側種目の場合、左側も同じ値に更新
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
                        // この種目の全セットの値を一括更新
                        onUpdateExerciseSetsValue(exerciseIndex, delta)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 開始ボタン
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
    // 折りたたみ状態（デフォルトは閉じた状態）
    var isExpanded by remember { mutableStateOf(false) }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // ヘッダー: 種目名
            Text(
                text = exercise.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // セット数・インターバル編集行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // セット数（+/-ボタン）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sets_label_short),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                    IconButton(
                        onClick = { if (currentSetCount > 1) onUpdateSetCount(currentSetCount - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "−",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    }
                    Text(
                        text = currentSetCount.toString(),
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(20.dp)
                    )
                    IconButton(
                        onClick = { onUpdateSetCount(currentSetCount + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    }
                }

                // インターバル
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_short),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(28.dp),
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 全セット± ボタン行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate700, RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onUpdateAllSetsValue(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.all_sets_label),
                    fontSize = 12.sp,
                    color = Slate400
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onUpdateAllSetsValue(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // セットごとの値（コンパクト）
            sets.forEach { set ->
                // オブジェクト参照ではなくセマンティックに検索（copy()で参照が変わるため）
                val setIndex = allSets.indexOfFirst {
                    it.exerciseIndex == set.exerciseIndex &&
                    it.setNumber == set.setNumber &&
                    it.side == set.side
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
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.set_format, set.setNumber, actualTotalSets),
                        fontSize = 14.sp,
                        color = Slate300
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        Text(
                            text = unit,
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }
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
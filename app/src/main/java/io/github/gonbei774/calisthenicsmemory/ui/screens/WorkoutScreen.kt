package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ワークアウトセットのデータ
data class WorkoutSet(
    val setNumber: Int,
    val side: String?, // "Right" or "Left" or null (Bilateral)
    val targetValue: Int, // 目標値
    var actualValue: Int = 0, // 実際の値
    var isCompleted: Boolean = false,
    var isSkipped: Boolean = false
)

// ワークアウトセッションのデータ
data class WorkoutSession(
    val exercise: Exercise,
    val totalSets: Int,
    val targetValue: Int, // 目標値
    val repDuration: Int?, // Dynamic用: 1レップ時間（秒）
    val startInterval: Int, // 開始前インターバル（秒）
    var intervalDuration: Int, // セット間インターバル（秒）
    val sets: MutableList<WorkoutSet>,
    var comment: String = ""
)

// ワークアウト画面の状態
sealed class WorkoutStep {
    object ExerciseSelection : WorkoutStep()
    object Settings : WorkoutStep()
    data class StartInterval(val session: WorkoutSession, val currentSetIndex: Int) : WorkoutStep()
    data class Executing(val session: WorkoutSession, val currentSetIndex: Int) : WorkoutStep()
    data class Interval(val session: WorkoutSession, val currentSetIndex: Int) : WorkoutStep()
    data class Confirmation(val session: WorkoutSession) : WorkoutStep()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()

    var currentStep by remember { mutableStateOf<WorkoutStep>(WorkoutStep.ExerciseSelection) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    // ビープ音用
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    // ワークアウトモードのコメント文字列
    val workoutModeComment = stringResource(R.string.workout_mode_comment)

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Orange600
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.workout_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val step = currentStep) {
                is WorkoutStep.ExerciseSelection -> {
                    ExerciseSelectionStep(
                        viewModel = viewModel,
                        onExerciseSelected = { exercise ->
                            selectedExercise = exercise
                            currentStep = WorkoutStep.Settings
                        }
                    )
                }
                is WorkoutStep.Settings -> {
                    selectedExercise?.let { exercise ->
                        SettingsStep(
                            exercise = exercise,
                            onStartWorkout = { session ->
                                currentStep = if (session.startInterval > 0) {
                                    WorkoutStep.StartInterval(session, 0)
                                } else {
                                    WorkoutStep.Executing(session, 0)
                                }
                            },
                            onBack = { currentStep = WorkoutStep.ExerciseSelection }
                        )
                    }
                }
                is WorkoutStep.StartInterval -> {
                    StartIntervalStep(
                        session = step.session,
                        toneGenerator = toneGenerator,
                        onIntervalComplete = {
                            currentStep = WorkoutStep.Executing(step.session, step.currentSetIndex)
                        },
                        onSkip = {
                            currentStep = WorkoutStep.Executing(step.session, step.currentSetIndex)
                        }
                    )
                }
                is WorkoutStep.Executing -> {
                    ExecutingStep(
                        session = step.session,
                        currentSetIndex = step.currentSetIndex,
                        toneGenerator = toneGenerator,
                        onSetComplete = { updatedSession ->
                            val nextIndex = step.currentSetIndex + 1
                            currentStep = if (nextIndex < updatedSession.sets.size) {
                                WorkoutStep.Interval(updatedSession, nextIndex)
                            } else {
                                WorkoutStep.Confirmation(updatedSession)
                            }
                        },
                        onSkip = { updatedSession ->
                            // actualValueとisSkippedは既にExecutingStep内で設定済み
                            val nextIndex = step.currentSetIndex + 1
                            currentStep = if (nextIndex < updatedSession.sets.size) {
                                WorkoutStep.Interval(updatedSession, nextIndex)
                            } else {
                                WorkoutStep.Confirmation(updatedSession)
                            }
                        },
                        onAbort = { updatedSession ->
                            // 現在のセットは既にExecutingStep内で途中までの記録を保存済み
                            // 次のセット以降を全てスキップ扱いにする
                            for (i in step.currentSetIndex + 1 until updatedSession.sets.size) {
                                updatedSession.sets[i].isSkipped = true
                                updatedSession.sets[i].actualValue = 0
                            }
                            currentStep = WorkoutStep.Confirmation(updatedSession)
                        }
                    )
                }
                is WorkoutStep.Interval -> {
                    IntervalStep(
                        session = step.session,
                        nextSetIndex = step.currentSetIndex,
                        toneGenerator = toneGenerator,
                        onIntervalComplete = {
                            // インターバル完了後、準備（StartInterval）を挟む
                            currentStep = if (step.session.startInterval > 0) {
                                WorkoutStep.StartInterval(step.session, step.currentSetIndex)
                            } else {
                                WorkoutStep.Executing(step.session, step.currentSetIndex)
                            }
                        },
                        onSkip = {
                            // スキップ時も準備を挟む
                            currentStep = if (step.session.startInterval > 0) {
                                WorkoutStep.StartInterval(step.session, step.currentSetIndex)
                            } else {
                                WorkoutStep.Executing(step.session, step.currentSetIndex)
                            }
                        },
                        onUpdateInterval = { newInterval ->
                            step.session.intervalDuration = newInterval
                        }
                    )
                }
                is WorkoutStep.Confirmation -> {
                    ConfirmationStep(
                        session = step.session,
                        onConfirm = { finalSession ->
                            saveWorkoutRecords(viewModel, finalSession, workoutModeComment)
                            onNavigateBack()
                        },
                        onCancel = { currentStep = WorkoutStep.ExerciseSelection }
                    )
                }
            }
        }
    }
}

// Step 1: 種目選択（階層表示）
@Composable
fun ExerciseSelectionStep(
    viewModel: TrainingViewModel,
    onExerciseSelected: (Exercise) -> Unit
) {
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    if (hierarchicalData.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_exercises_yet_workout),
                color = Slate400,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = hierarchicalData.size,
                key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
            ) { index ->
                val group = hierarchicalData[index]
                WorkoutHierarchicalGroup(
                    group = group,
                    isExpanded = if (group.groupName != null) {
                        group.groupName in expandedGroups
                    } else {
                        "ungrouped" in expandedGroups
                    },
                    onExpandToggle = {
                        val key = group.groupName ?: "ungrouped"
                        viewModel.toggleGroupExpansion(key)
                    },
                    onExerciseSelected = onExerciseSelected
                )
            }
        }
    }
}

// ワークアウト用階層グループ
@Composable
fun WorkoutHierarchicalGroup(
    group: TrainingViewModel.GroupWithExercises,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // グループヘッダー
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                onClick = onExpandToggle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = when (group.groupName) {
                                TrainingViewModel.FAVORITE_GROUP_KEY -> stringResource(R.string.favorite)
                                null -> stringResource(R.string.no_group_workout)
                                else -> group.groupName
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.exercises_count, group.exercises.size),
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            }

            // 種目リスト
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.exercises.forEach { exercise ->
                        WorkoutExerciseItem(
                            exercise = exercise,
                            onClick = { onExerciseSelected(exercise) }
                        )
                    }
                }
            }
        }
    }
}

// ワークアウト用種目アイテム
@Composable
fun WorkoutExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate700),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // お気に入り
                    if (exercise.isFavorite) {
                        Text(
                            text = "★",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // レベル
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(
                            text = "Lv.${exercise.sortOrder}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                    }

                    // タイプ
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )

                    // Unilateral
                    if (exercise.laterality == "Unilateral") {
                        Text(
                            text = stringResource(R.string.one_sided_workout),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Purple600
                        )
                    }
                }

                // 課題情報
                if (exercise.targetSets != null && exercise.targetValue != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val unit = if (exercise.type == "Dynamic") stringResource(R.string.unit_reps) else stringResource(R.string.unit_seconds)
                        Text(
                            text = stringResource(
                                if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                exercise.targetSets ?: 0,
                                exercise.targetValue ?: 0,
                                unit
                            ),
                            fontSize = 12.sp,
                            color = Green400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.select),
                tint = Orange600,
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

// Step 2: 設定画面
@Composable
fun SettingsStep(
    exercise: Exercise,
    onStartWorkout: (WorkoutSession) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val workoutPrefs = remember { WorkoutPreferences(context) }

    var sets by remember { mutableStateOf(exercise.targetSets?.toString() ?: "") }
    var targetValue by remember { mutableStateOf(exercise.targetValue?.toString() ?: "") }
    var repDuration by remember {
        mutableStateOf(
            if (workoutPrefs.isRepDurationEnabled()) {
                workoutPrefs.getRepDuration().toString()
            } else {
                ""
            }
        )
    }
    var startInterval by remember {
        mutableStateOf(
            if (workoutPrefs.isStartCountdownEnabled()) {
                workoutPrefs.getStartCountdown().toString()
            } else {
                ""
            }
        )
    }
    var interval by remember {
        mutableStateOf(
            if (workoutPrefs.isSetIntervalEnabled()) {
                workoutPrefs.getSetInterval().toString()
            } else {
                ""
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
            fontSize = 14.sp,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sets,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) sets = it },
            label = { Text(stringResource(R.string.target_sets_label)) },
            placeholder = { Text("3", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        OutlinedTextField(
            value = targetValue,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) targetValue = it },
            label = { Text(stringResource(if (exercise.type == "Dynamic") R.string.target_reps_label else R.string.target_duration_label)) },
            placeholder = { Text("10", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        if (exercise.type == "Dynamic") {
            OutlinedTextField(
                value = repDuration,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) repDuration = it },
                label = { Text(stringResource(R.string.rep_duration_label)) },
                placeholder = { Text("5", color = Slate400) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Orange600,
                    focusedLabelColor = Orange600
                )
            )
        }

        OutlinedTextField(
            value = startInterval,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) startInterval = it },
            label = { Text(stringResource(R.string.start_countdown_label)) },
            placeholder = { Text("5", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        OutlinedTextField(
            value = interval,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) interval = it },
            label = { Text(stringResource(R.string.interval_duration_label)) },
            placeholder = { Text("240", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        val isValid = sets.isNotEmpty() && targetValue.isNotEmpty() &&
                (exercise.type != "Dynamic" || repDuration.isNotEmpty())

        Button(
            onClick = {
                val totalSets = sets.toIntOrNull() ?: 3
                val target = targetValue.toIntOrNull() ?: 10
                val repDur = if (exercise.type == "Dynamic") repDuration.toIntOrNull() ?: 5 else null
                val start = startInterval.toIntOrNull() ?: 5
                val inter = interval.toIntOrNull() ?: 240

                val workoutSets = mutableListOf<WorkoutSet>()
                if (exercise.laterality == "Unilateral") {
                    for (i in 1..totalSets) {
                        workoutSets.add(WorkoutSet(i, "Right", target))
                        workoutSets.add(WorkoutSet(i, "Left", target))
                    }
                } else {
                    for (i in 1..totalSets) {
                        workoutSets.add(WorkoutSet(i, null, target))
                    }
                }

                val session = WorkoutSession(
                    exercise = exercise,
                    totalSets = totalSets,
                    targetValue = target,
                    repDuration = repDur,
                    startInterval = start,
                    intervalDuration = inter,
                    sets = workoutSets
                )
                onStartWorkout(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = Orange600)
        ) {
            Text(stringResource(R.string.start_workout), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back_button))
        }
    }
}

// 開始前インターバル
@Composable
fun StartIntervalStep(
    session: WorkoutSession,
    toneGenerator: ToneGenerator,
    onIntervalComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var remainingTime by remember { mutableIntStateOf(session.startInterval) }
    val progress = if (session.startInterval > 0) remainingTime.toFloat() / session.startInterval else 0f

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000L)
            remainingTime--
            if (remainingTime <= 3 && remainingTime > 0) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        }
        // カウントダウン完了
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        delay(300L)
        onIntervalComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 種目名（上部）
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 中央固定エリア
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 状態表示
            Text(
                text = stringResource(R.string.preparing),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Orange600
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressTimer(
                progress = progress,
                remainingTime = remainingTime,
                color = Orange600
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ボタン
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = Slate600),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.skip_button))
        }
    }
}

// 円形プログレスタイマー
@Composable
fun CircularProgressTimer(
    progress: Float,
    remainingTime: Int,
    color: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawArc(
                color = Slate600,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$remainingTime",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                    blurRadius = 8f
                )
            )
        )
    }
}

// Step 3: 実行画面
@Composable
fun ExecutingStep(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit
) {
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isRunning by remember(currentSetIndex) { mutableStateOf(true) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }

    // Dynamic: レップ内の経過時間を計算（カウントアップ）
    val repTimeElapsed = if (session.exercise.type == "Dynamic") {
        val repDur = session.repDuration ?: 5
        elapsedTime % repDur
    } else {
        0
    }

    val progress = if (session.exercise.type == "Isometric") {
        (currentSet.targetValue - elapsedTime).toFloat() / currentSet.targetValue
    } else {
        // Dynamic: レップ内の進捗（カウントダウン用に反転）
        val repDur = session.repDuration ?: 5
        (elapsedTime % repDur).toFloat() / repDur
    }

    LaunchedEffect(currentSetIndex) {
        while (true) {
            if (isRunning) {
                delay(1000L)
                elapsedTime++

                // Dynamic: レップカウント
                session.repDuration?.let { repDur ->
                    if (elapsedTime % repDur == 0) {
                        currentCount++
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)

                        // Dynamic: 目標達成時に自動遷移
                        if (currentCount >= currentSet.targetValue) {
                            playTripleBeepTwice(toneGenerator)
                            currentSet.actualValue = currentCount
                            currentSet.isCompleted = true
                            onSetComplete(session)
                            return@LaunchedEffect
                        }
                    }
                }

                // Isometric: 目標達成時に自動遷移
                if (session.exercise.type == "Isometric" && elapsedTime >= currentSet.targetValue) {
                    playTripleBeepTwice(toneGenerator)
                    currentSet.actualValue = elapsedTime
                    currentSet.isCompleted = true
                    onSetComplete(session)
                    return@LaunchedEffect
                }
            } else {
                delay(100L)  // 一時停止中は短い間隔でチェック
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 種目名（上部）
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 中央固定エリア
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 状態表示
            Text(
                text = stringResource(R.string.workout_in_progress),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Orange600
            )

            // セット表示
            val sideText = when (currentSet.side) {
                "Right" -> stringResource(R.string.side_right)
                "Left" -> stringResource(R.string.side_left)
                else -> null
            }
            Text(
                text = if (sideText != null) {
                    stringResource(R.string.set_format_with_side, currentSet.setNumber, session.totalSets, sideText)
                } else {
                    stringResource(R.string.set_format, currentSet.setNumber, session.totalSets)
                },
                fontSize = 20.sp,
                color = Slate300,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressTimer(
                progress = progress.coerceIn(0f, 1f),
                remainingTime = if (session.exercise.type == "Isometric") {
                    (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
                } else {
                    // Dynamic: レップ内の経過時間を表示（カウントアップ）
                    repTimeElapsed
                },
                color = Orange600
            )

            if (session.exercise.type == "Dynamic") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.reps_count, currentCount),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { isRunning = !isRunning },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Red600 else Green600
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                stringResource(if (isRunning) R.string.pause_button else R.string.resume_button),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // 途中までの記録を保存してからスキップ
                    currentSet.actualValue = if (session.exercise.type == "Dynamic") currentCount else elapsedTime
                    currentSet.isSkipped = true
                    onSkip(session)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.skip_button))
            }

            Button(
                onClick = {
                    // 現在のセットは途中までの記録を保存
                    currentSet.actualValue = if (session.exercise.type == "Dynamic") currentCount else elapsedTime
                    currentSet.isSkipped = true
                    onAbort(session)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.stop_button))
            }
        }
    }
}

// セット間インターバル
@Composable
fun IntervalStep(
    session: WorkoutSession,
    nextSetIndex: Int,
    toneGenerator: ToneGenerator,
    onIntervalComplete: () -> Unit,
    onSkip: () -> Unit,
    onUpdateInterval: (Int) -> Unit
) {
    var isRunning by remember { mutableStateOf(true) }
    var remainingTime by remember { mutableIntStateOf(session.intervalDuration) }
    val progress = if (session.intervalDuration > 0) remainingTime.toFloat() / session.intervalDuration else 0f

    LaunchedEffect(Unit) {
        while (remainingTime > 0 && isRunning) {
            delay(1000L)
            remainingTime--
            if (remainingTime <= 3 && remainingTime > 0) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        }
        if (remainingTime == 0) {
            // インターバル完了
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            delay(300L)
            onIntervalComplete()
        }
    }

    val nextSet = session.sets.getOrNull(nextSetIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 種目名（上部）
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 中央固定エリア
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 状態表示
            Text(
                text = stringResource(R.string.interval_label),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Cyan600
            )

            // 次のセット表示
            nextSet?.let {
                val nextSideText = when (it.side) {
                    "Right" -> stringResource(R.string.side_right)
                    "Left" -> stringResource(R.string.side_left)
                    else -> null
                }
                Text(
                    text = if (nextSideText != null) {
                        stringResource(R.string.next_set_format_with_side, it.setNumber, session.totalSets, nextSideText)
                    } else {
                        stringResource(R.string.next_set_format, it.setNumber, session.totalSets)
                    },
                    fontSize = 20.sp,
                    color = Slate300,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressTimer(
                progress = progress,
                remainingTime = remainingTime,
                color = Cyan600
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ボタンエリア
        Button(
            onClick = { isRunning = !isRunning },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Red600 else Green600
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                stringResource(if (isRunning) R.string.pause_button else R.string.resume_button),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    // 今回のインターバルのみ短縮（次回以降は影響しない）
                    remainingTime = (remainingTime - 10).coerceAtLeast(0)
                }
            ) {
                Text(
                    text = "-",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.ten_seconds),
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    // 今回のインターバルのみ延長（次回以降は影響しない）
                    remainingTime += 10
                }
            ) {
                Text(
                    text = "+",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = Slate600),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.skip_button))
        }
    }
}

// 確認画面（片側種目は1行表示）
@Composable
fun ConfirmationStep(
    session: WorkoutSession,
    onConfirm: (WorkoutSession) -> Unit,
    onCancel: () -> Unit
) {
    var comment by remember { mutableStateOf(session.comment) }

    // Unilateralの場合、セット番号でグループ化
    val displaySets = remember(session) {
        if (session.exercise.laterality == "Unilateral") {
            session.sets.groupBy { it.setNumber }.map { (setNumber, sets) ->
                val rightSet = sets.firstOrNull { it.side == "Right" }
                val leftSet = sets.firstOrNull { it.side == "Left" }
                Triple(setNumber, rightSet, leftSet)
            }
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.workout_complete),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text(stringResource(R.string.comment_label)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (session.exercise.laterality == "Unilateral") {
                // Unilateral: 1行表示
                items(displaySets.size) { index ->
                    val (setNumber, rightSet, leftSet) = displaySets[index]
                    UnilateralSetItem(
                        setNumber = setNumber,
                        rightSet = rightSet,
                        leftSet = leftSet,
                        exerciseType = session.exercise.type,
                        onRightValueChange = { newValue ->
                            rightSet?.actualValue = newValue
                        },
                        onLeftValueChange = { newValue ->
                            leftSet?.actualValue = newValue
                        }
                    )
                }
            } else {
                // Bilateral: 通常表示
                items(session.sets.size) { index ->
                    val set = session.sets[index]
                    BilateralSetItem(
                        set = set,
                        exerciseType = session.exercise.type,
                        onValueChange = { newValue ->
                            set.actualValue = newValue
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                session.comment = comment
                onConfirm(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600)
        ) {
            Text(stringResource(R.string.record_workout), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

// Unilateral用セットアイテム（1行表示）
@Composable
fun UnilateralSetItem(
    setNumber: Int,
    rightSet: WorkoutSet?,
    leftSet: WorkoutSet?,
    exerciseType: String,
    onRightValueChange: (Int) -> Unit,
    onLeftValueChange: (Int) -> Unit
) {
    // 編集可能な状態として管理
    var rightValue by remember(rightSet) { mutableStateOf(rightSet?.actualValue?.toString() ?: "0") }
    var leftValue by remember(leftSet) { mutableStateOf(leftSet?.actualValue?.toString() ?: "0") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rightSet?.isSkipped == true && leftSet?.isSkipped == true) Slate700 else Slate800
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.set_label, setNumber),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (rightSet?.isSkipped == true && leftSet?.isSkipped == true) {
                Text(
                    text = stringResource(R.string.skipped_label),
                    fontSize = 12.sp,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 右側
                Text(
                    text = stringResource(R.string.right_colon),
                    fontSize = 14.sp,
                    color = Slate400,
                    modifier = Modifier.width(30.dp)
                )
                OutlinedTextField(
                    value = rightValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            rightValue = newValue
                            newValue.toIntOrNull()?.let { onRightValueChange(it) }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 左側
                Text(
                    text = stringResource(R.string.left_colon),
                    fontSize = 14.sp,
                    color = Slate400,
                    modifier = Modifier.width(30.dp)
                )
                OutlinedTextField(
                    value = leftValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            leftValue = newValue
                            newValue.toIntOrNull()?.let { onLeftValueChange(it) }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

// Bilateral用セットアイテム
@Composable
fun BilateralSetItem(
    set: WorkoutSet,
    exerciseType: String,
    onValueChange: (Int) -> Unit
) {
    // 編集可能な状態として管理
    var value by remember(set) { mutableStateOf(set.actualValue.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (set.isSkipped) Slate700 else Slate800
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.set_label, set.setNumber),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (set.isSkipped) {
                    Text(
                        text = stringResource(R.string.skipped_label),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        value = newValue
                        newValue.toIntOrNull()?.let { onValueChange(it) }
                    }
                },
                label = {
                    Text(
                        stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.width(100.dp),
                singleLine = true
            )
        }
    }
}

// 記録保存関数
fun saveWorkoutRecords(
    viewModel: TrainingViewModel,
    session: WorkoutSession,
    workoutModeComment: String
) {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    if (session.exercise.laterality == "Unilateral") {
        val valuesRight = session.sets
            .filter { it.side == "Right" && it.actualValue > 0 }
            .map { it.actualValue }
        val valuesLeft = session.sets
            .filter { it.side == "Left" && it.actualValue > 0 }
            .map { it.actualValue }

        if (valuesRight.isNotEmpty()) {
            viewModel.addTrainingRecordsUnilateral(
                exerciseId = session.exercise.id,
                valuesRight = valuesRight,
                valuesLeft = valuesLeft,
                date = today,
                time = now,
                comment = session.comment.ifEmpty { workoutModeComment }
            )
        }
    } else {
        val values = session.sets
            .filter { it.actualValue > 0 }
            .map { it.actualValue }

        if (values.isNotEmpty()) {
            viewModel.addTrainingRecords(
                exerciseId = session.exercise.id,
                values = values,
                date = today,
                time = now,
                comment = session.comment.ifEmpty { workoutModeComment }
            )
        }
    }
}

// ピピピ、ピピピ、ピピピ（3連×3セット）のビープ音を再生
suspend fun playTripleBeepTwice(toneGenerator: ToneGenerator) {
    // 3セット繰り返す
    repeat(3) { setIndex ->
        repeat(3) {
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 150)
            delay(150L)
            delay(100L) // ビープ間の間隔
        }
        // 最後のセット以外は間隔を入れる
        if (setIndex < 2) {
            delay(150L) // セット間の間隔
        }
    }
}
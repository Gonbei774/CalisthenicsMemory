package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.components.single.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var isSkipped: Boolean = false,
    val previousValue: Int? = null // 前回値（参考用）
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
    var comment: String = "",
    val distanceCm: Int? = null, // 距離（cm）
    val weightG: Int? = null, // 追加ウエイト（g）
    val isAutoMode: Boolean = true, // 自動モード（目標達成時に自動遷移）
    val isDynamicCountSoundEnabled: Boolean = true // レップカウント音有効
)

// ワークアウト画面の状態
sealed class WorkoutStep {
    object ModeSelection : WorkoutStep()  // モード選択（単発/プログラム）
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
    onNavigateBack: () -> Unit,
    onNavigateToProgramList: () -> Unit = {},
    initialExerciseId: Long? = null,
    fromToDo: Boolean = false
) {
    val exercises by viewModel.exercises.collectAsState()
    val context = LocalContext.current

    // Find initial exercise if provided
    val initialExercise = remember(initialExerciseId, exercises) {
        if (initialExerciseId != null) {
            exercises.find { it.id == initialExerciseId }
        } else {
            null
        }
    }

    // 初期ステップの決定：
    // - initialExerciseが指定されている場合 → Settings
    // - fromToDoの場合 → ExerciseSelection（単発モード）
    // - それ以外 → ModeSelection（モード選択）
    var currentStep by remember(initialExercise, fromToDo) {
        mutableStateOf<WorkoutStep>(
            when {
                initialExercise != null -> WorkoutStep.Settings
                fromToDo -> WorkoutStep.ExerciseSelection
                else -> WorkoutStep.ModeSelection
            }
        )
    }
    var selectedExercise by remember(initialExercise) { mutableStateOf<Exercise?>(initialExercise) }

    // ビープ音用
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    // LEDフラッシュ用
    val flashController = remember { FlashController(context) }
    val workoutPreferences = remember { WorkoutPreferences(context) }
    val isFlashEnabled = remember { workoutPreferences.isFlashNotificationEnabled() }
    val isKeepScreenOnEnabled = remember { workoutPreferences.isKeepScreenOnEnabled() }

    // ワークアウトモードのコメント文字列
    val workoutModeComment = stringResource(R.string.workout_mode_comment)

    // 中断確認ダイアログ
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // やり直しボタン用のキー（インクリメントで実行ステップをリセット）
    var retryKey by remember { mutableIntStateOf(0) }

    // 戻るボタンのハンドリング
    BackHandler {
        when (currentStep) {
            is WorkoutStep.ModeSelection,
            is WorkoutStep.ExerciseSelection,
            is WorkoutStep.Settings -> onNavigateBack()
            else -> {
                // 実行中・完了画面は確認ダイアログを表示
                showExitConfirmDialog = true
            }
        }
    }

    // ワークアウト実行中（タイマーが動いている間）のみForeground Serviceを起動
    LaunchedEffect(currentStep) {
        when (currentStep) {
            is WorkoutStep.StartInterval,
            is WorkoutStep.Executing,
            is WorkoutStep.Interval -> WorkoutTimerService.startService(context)
            else -> WorkoutTimerService.stopService(context)
        }
    }

    // 画面オン維持の制御
    val view = LocalView.current
    LaunchedEffect(isKeepScreenOnEnabled, currentStep) {
        val window = (view.context as? android.app.Activity)?.window

        if (isKeepScreenOnEnabled) {
            when (currentStep) {
                is WorkoutStep.StartInterval,
                is WorkoutStep.Executing,
                is WorkoutStep.Interval -> {
                    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                else -> {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? android.app.Activity)?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            toneGenerator.release()
            flashController.turnOff()
            WorkoutTimerService.stopService(context)
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
                    IconButton(onClick = {
                        when (currentStep) {
                            is WorkoutStep.ModeSelection,
                            is WorkoutStep.ExerciseSelection,
                            is WorkoutStep.Settings -> onNavigateBack()
                            else -> showExitConfirmDialog = true
                        }
                    }) {
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
                is WorkoutStep.ModeSelection -> {
                    ModeSelectionStep(
                        onSingleModeSelected = {
                            currentStep = WorkoutStep.ExerciseSelection
                        },
                        onProgramModeSelected = onNavigateToProgramList
                    )
                }
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
                            viewModel = viewModel,
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
                        currentSetIndex = step.currentSetIndex,
                        toneGenerator = toneGenerator,
                        flashController = flashController,
                        isFlashEnabled = isFlashEnabled,
                        onIntervalComplete = {
                            currentStep = WorkoutStep.Executing(step.session, step.currentSetIndex)
                        },
                        onSkip = {
                            currentStep = WorkoutStep.Executing(step.session, step.currentSetIndex)
                        }
                    )
                }
                is WorkoutStep.Executing -> {
                    // 設定に基づいて適切なExecutingコンポーネントを選択
                    val exercise = step.session.exercise
                    val onSetComplete: (WorkoutSession) -> Unit = { updatedSession ->
                        val nextIndex = step.currentSetIndex + 1
                        currentStep = if (nextIndex < updatedSession.sets.size) {
                            WorkoutStep.Interval(updatedSession, nextIndex)
                        } else {
                            WorkoutStep.Confirmation(updatedSession)
                        }
                    }
                    val onSkip: (WorkoutSession) -> Unit = { updatedSession ->
                        val nextIndex = step.currentSetIndex + 1
                        currentStep = if (nextIndex < updatedSession.sets.size) {
                            WorkoutStep.Interval(updatedSession, nextIndex)
                        } else {
                            WorkoutStep.Confirmation(updatedSession)
                        }
                    }
                    val onAbort: (WorkoutSession) -> Unit = { updatedSession ->
                        for (i in step.currentSetIndex + 1 until updatedSession.sets.size) {
                            updatedSession.sets[i].isSkipped = true
                            updatedSession.sets[i].actualValue = 0
                        }
                        currentStep = WorkoutStep.Confirmation(updatedSession)
                    }
                    val onRetry: () -> Unit = { retryKey++ }

                    // key()でラップしてretryKeyの変更でリセット可能に
                    key(retryKey) {
                        when {
                            // Isometric + AutoMode
                            exercise.type == "Isometric" && step.session.isAutoMode -> {
                                SingleExecutingStepIsometricAuto(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isIntervalSoundEnabled = workoutPreferences.isIsometricIntervalSoundEnabled(),
                                    intervalSeconds = workoutPreferences.getIsometricIntervalSeconds(),
                                    onSetComplete = onSetComplete,
                                    onSkip = onSkip,
                                    onAbort = onAbort,
                                    onRetry = onRetry
                                )
                            }
                            // Isometric + ManualMode
                            exercise.type == "Isometric" && !step.session.isAutoMode -> {
                                SingleExecutingStepIsometricManual(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isIntervalSoundEnabled = workoutPreferences.isIsometricIntervalSoundEnabled(),
                                    intervalSeconds = workoutPreferences.getIsometricIntervalSeconds(),
                                    onSetComplete = onSetComplete,
                                    onSkip = onSkip,
                                    onAbort = onAbort,
                                    onRetry = onRetry
                                )
                            }
                            // Dynamic + CountSound OFF → Simple
                            exercise.type == "Dynamic" && !step.session.isDynamicCountSoundEnabled -> {
                                SingleExecutingStepDynamicSimple(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    onSetComplete = onSetComplete,
                                    onSkip = onSkip,
                                    onAbort = onAbort,
                                    onRetry = onRetry
                                )
                            }
                            // Dynamic + AutoMode
                            exercise.type == "Dynamic" && step.session.isAutoMode -> {
                                SingleExecutingStepDynamicAuto(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isCountSoundEnabled = step.session.isDynamicCountSoundEnabled,
                                    onSetComplete = onSetComplete,
                                    onSkip = onSkip,
                                    onAbort = onAbort,
                                    onRetry = onRetry
                                )
                            }
                            // Dynamic + ManualMode (default)
                            else -> {
                                SingleExecutingStepDynamicManual(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isCountSoundEnabled = step.session.isDynamicCountSoundEnabled,
                                    onSetComplete = onSetComplete,
                                    onSkip = onSkip,
                                    onAbort = onAbort,
                                    onRetry = onRetry
                                )
                            }
                        }
                    }
                }
                is WorkoutStep.Interval -> {
                    IntervalStep(
                        session = step.session,
                        nextSetIndex = step.currentSetIndex,
                        toneGenerator = toneGenerator,
                        flashController = flashController,
                        isFlashEnabled = isFlashEnabled,
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
                            // Delete todo task if from ToDo
                            if (fromToDo) {
                                viewModel.deleteTodoTaskByExerciseId(finalSession.exercise.id)
                            }
                            onNavigateBack()
                        },
                        onCancel = {
                            // If from ToDo, go back to ToDo; otherwise go to exercise selection
                            if (fromToDo) {
                                onNavigateBack()
                            } else {
                                currentStep = WorkoutStep.ExerciseSelection
                            }
                        }
                    )
                }
            }
        }
    }

    // 中断確認ダイアログ
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(stringResource(R.string.exit_workout_title)) },
            text = { Text(stringResource(R.string.exit_workout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.exit_workout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
    viewModel: TrainingViewModel,
    onStartWorkout: (WorkoutSession) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val workoutPrefs = remember { WorkoutPreferences(context) }

    var sets by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var repDuration by remember { mutableStateOf("") }
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
            when {
                // 1. 種目設定が最優先
                exercise.restInterval != null -> exercise.restInterval.toString()
                // 2. スイッチONなら設定画面の秒数
                workoutPrefs.isSetIntervalEnabled() -> workoutPrefs.getSetInterval().toString()
                // 3. それ以外は空欄
                else -> ""
            }
        )
    }
    var distanceInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    // 実行設定（WorkoutPreferencesと連動）
    // Isometricはデフォルトでタイマーオフ（手動完了）、Dynamicはオン（既存ユーザー体験維持）
    var isAutoMode by remember {
        mutableStateOf(
            if (exercise.type == "Isometric") false else workoutPrefs.isAutoMode()
        )
    }
    var isDynamicCountSoundEnabled by remember { mutableStateOf(workoutPrefs.isDynamicCountSoundEnabled()) }
    var isIsometricIntervalSoundEnabled by remember { mutableStateOf(workoutPrefs.isIsometricIntervalSoundEnabled()) }
    var isometricIntervalSeconds by remember { mutableIntStateOf(workoutPrefs.getIsometricIntervalSeconds()) }

    // 前回セッションデータ（前回値表示用）
    var previousSessionRecords by remember { mutableStateOf<List<io.github.gonbei774.calisthenicsmemory.data.TrainingRecord>>(emptyList()) }

    // プリフィル：前回セッションのデータを取得
    LaunchedEffect(exercise.id) {
        val prevSession = viewModel.getLatestSession(exercise.id)
        previousSessionRecords = prevSession
        if (workoutPrefs.isPrefillPreviousRecordEnabled() && prevSession.isNotEmpty()) {
            // セット数をプリフィル
            sets = prevSession.size.toString()
            // 目標値（前回値の最大値）をプリフィル
            val maxValue = prevSession.maxOf { it.valueRight }
            targetValue = maxValue.toString()
            // 距離をプリフィル（トラッキング有効時）
            if (exercise.distanceTrackingEnabled) {
                prevSession.firstOrNull()?.distanceCm?.let {
                    distanceInput = it.toString()
                }
            }
            // 荷重をプリフィル（トラッキング有効時）
            if (exercise.weightTrackingEnabled) {
                prevSession.firstOrNull()?.weightG?.let {
                    weightInput = (it / 1000.0).toString()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

        // 実行設定セクション（上部に配置）
        SingleWorkoutSettingsSection(
            isAutoMode = isAutoMode,
            isDynamicCountSoundEnabled = isDynamicCountSoundEnabled,
            isIsometricIntervalSoundEnabled = isIsometricIntervalSoundEnabled,
            isometricIntervalSeconds = isometricIntervalSeconds,
            isDynamicExercise = exercise.type == "Dynamic",
            onAutoModeChange = { value ->
                isAutoMode = value
                workoutPrefs.setAutoMode(value)
            },
            onDynamicCountSoundChange = { value ->
                isDynamicCountSoundEnabled = value
                workoutPrefs.setDynamicCountSoundEnabled(value)
            },
            onIsometricIntervalSoundChange = { value ->
                isIsometricIntervalSoundEnabled = value
                workoutPrefs.setIsometricIntervalSoundEnabled(value)
            },
            onIsometricIntervalSecondsChange = { value ->
                isometricIntervalSeconds = value
                workoutPrefs.setIsometricIntervalSeconds(value)
            }
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
                onValueChange = {
                    if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.toIntOrNull()?.let { num -> num in 1..60 } == true)) {
                        repDuration = it
                    }
                },
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

        // 距離入力（有効な場合のみ表示）
        if (exercise.distanceTrackingEnabled) {
            OutlinedTextField(
                value = distanceInput,
                onValueChange = { value ->
                    // 空、"-"、または整数（負を含む）を許可
                    if (value.isEmpty() || value == "-" || value.toIntOrNull() != null) {
                        distanceInput = value
                    }
                },
                label = { Text(stringResource(R.string.distance_input_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue600,
                    unfocusedBorderColor = Slate600,
                    focusedLabelColor = Blue600,
                    unfocusedLabelColor = Slate400,
                    cursorColor = Blue600,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // 荷重入力（有効な場合のみ表示）
        if (exercise.weightTrackingEnabled) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { value ->
                    // 空、または小数（小数点1つまで、小数第1位まで）を許可
                    val isValidDecimal = value.isEmpty() ||
                        value == "." ||
                        value.matches(Regex("^\\d*\\.?\\d?\$"))
                    if (isValidDecimal) {
                        weightInput = value
                    }
                },
                label = { Text(stringResource(R.string.weight_input_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Orange600,
                    unfocusedBorderColor = Slate600,
                    focusedLabelColor = Orange600,
                    unfocusedLabelColor = Slate400,
                    cursorColor = Orange600,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 種目設定を適用ボタン
        Button(
            onClick = {
                // セット数を反映
                if (exercise.targetSets != null) {
                    sets = exercise.targetSets.toString()
                }
                // 目標値を反映
                if (exercise.targetValue != null) {
                    targetValue = exercise.targetValue.toString()
                }
                // 1レップ時間を反映（Dynamic種目のみ）
                if (exercise.type == "Dynamic" && exercise.repDuration != null) {
                    repDuration = exercise.repDuration.toString()
                }
                // 休憩時間を反映（種目設定がある場合のみ）
                if (exercise.restInterval != null) {
                    interval = exercise.restInterval.toString()
                }
                // 種目設定がない場合はユーザー入力を保持
                // 開始カウントダウンは種目設定がないため、ここでは何もしない
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange600
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.apply_exercise_settings),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isValid = sets.isNotEmpty() && targetValue.isNotEmpty() &&
                (exercise.type != "Dynamic" || repDuration.toIntOrNull()?.let { it >= 1 } == true)

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
                        // 前回セッションからこのセット番号のレコードを探す
                        val prevRecord = previousSessionRecords.find { it.setNumber == i }
                        workoutSets.add(WorkoutSet(i, "Right", target, previousValue = prevRecord?.valueRight))
                        workoutSets.add(WorkoutSet(i, "Left", target, previousValue = prevRecord?.valueLeft))
                    }
                } else {
                    for (i in 1..totalSets) {
                        // 前回セッションからこのセット番号のレコードを探す
                        val prevRecord = previousSessionRecords.find { it.setNumber == i }
                        workoutSets.add(WorkoutSet(i, null, target, previousValue = prevRecord?.valueRight))
                    }
                }

                // 距離・荷重の値を取得（空の場合はnull）
                val distanceCm = distanceInput.ifEmpty { null }?.toIntOrNull()
                // 荷重はkgで入力、gに変換（例: 1.5kg → 1500g）
                val weightG = weightInput.ifEmpty { null }?.toDoubleOrNull()?.let { (it * 1000).toInt() }

                val session = WorkoutSession(
                    exercise = exercise,
                    totalSets = totalSets,
                    targetValue = target,
                    repDuration = repDur,
                    startInterval = start,
                    intervalDuration = inter,
                    sets = workoutSets,
                    distanceCm = distanceCm,
                    weightG = weightG,
                    isAutoMode = isAutoMode,
                    isDynamicCountSoundEnabled = isDynamicCountSoundEnabled
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
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
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
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        // カウントダウン完了
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        if (isFlashEnabled) {
            launch { flashController.flashComplete() }
        }
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

        // 次のセット情報
        NextSetInfo(session = session, currentSetIndex = currentSetIndex)

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
    flashController: FlashController,
    isFlashEnabled: Boolean,
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

                        // Dynamic: 目標達成時に自動遷移
                        if (currentCount >= currentSet.targetValue) {
                            // 音とフラッシュを同時に開始
                            if (isFlashEnabled) {
                                launch { flashController.flashSetComplete() }
                            }
                            playTripleBeepTwice(toneGenerator)
                            currentSet.actualValue = currentCount
                            currentSet.isCompleted = true
                            onSetComplete(session)
                            return@LaunchedEffect
                        } else {
                            // 途中のレップは短いフラッシュ
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                            if (isFlashEnabled) {
                                launch { flashController.flashShort() }
                            }
                        }
                    }
                }

                // Isometric: 目標達成時に自動遷移
                if (session.exercise.type == "Isometric" && elapsedTime >= currentSet.targetValue) {
                    // 音とフラッシュを同時に開始
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
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

        // 次のセット情報
        NextSetInfo(session = session, currentSetIndex = currentSetIndex)

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
    flashController: FlashController,
    isFlashEnabled: Boolean,
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
                if (isFlashEnabled) {
                    launch { flashController.flashShort() }
                }
            }
        }
        if (remainingTime == 0) {
            // インターバル完了
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            if (isFlashEnabled) {
                launch { flashController.flashComplete() }
            }
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

        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip_button),
                color = Slate400
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.set_label, setNumber),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 前回値表示（右/左）
                val prevRight = rightSet?.previousValue
                val prevLeft = leftSet?.previousValue
                if (prevRight != null || prevLeft != null) {
                    Text(
                        text = stringResource(
                            R.string.previous_value_format,
                            "${prevRight ?: "-"}/${prevLeft ?: "-"}"
                        ),
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

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
                // 前回値表示
                set.previousValue?.let { prev ->
                    Text(
                        text = stringResource(R.string.previous_value_format, prev),
                        fontSize = 12.sp,
                        color = Slate500
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
                comment = session.comment.ifEmpty { workoutModeComment },
                distanceCm = session.distanceCm,
                weightG = session.weightG
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
                comment = session.comment.ifEmpty { workoutModeComment },
                distanceCm = session.distanceCm,
                weightG = session.weightG
            )
        }
    }
}

// 次のセット情報を表示するコンポーザブル（シングルモード用）
@Composable
fun NextSetInfo(
    session: WorkoutSession,
    currentSetIndex: Int
) {
    val nextSetIndex = currentSetIndex + 1
    val nextSet = session.sets.getOrNull(nextSetIndex) ?: return

    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // 次のセット情報
        Text(
            text = if (nextSideText != null) {
                stringResource(R.string.next_set_format_with_side, nextSet.setNumber, session.totalSets, nextSideText)
            } else {
                stringResource(R.string.next_set_format, nextSet.setNumber, session.totalSets)
            },
            fontSize = 16.sp,
            color = Slate400
        )
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

// シングルワークアウト設定セクション
@Composable
fun SingleWorkoutSettingsSection(
    isAutoMode: Boolean,
    isDynamicCountSoundEnabled: Boolean,
    isIsometricIntervalSoundEnabled: Boolean,
    isometricIntervalSeconds: Int,
    isDynamicExercise: Boolean,
    onAutoModeChange: (Boolean) -> Unit,
    onDynamicCountSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSoundChange: (Boolean) -> Unit,
    onIsometricIntervalSecondsChange: (Int) -> Unit
) {
    // ローカル状態（間隔秒数入力用）
    var intervalText by remember(isometricIntervalSeconds) { mutableStateOf(isometricIntervalSeconds.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Slate300
        )

        if (isDynamicExercise) {
            // ダイナミック種目: 数え上げ音を先に表示
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

            // タイマーモード（Count Sound OFFの時は無効化）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.auto_mode),
                        fontSize = 14.sp,
                        color = if (isDynamicCountSoundEnabled) Color.White else Slate500
                    )
                    Text(
                        text = if (isDynamicCountSoundEnabled) {
                            stringResource(R.string.auto_mode_description)
                        } else {
                            stringResource(R.string.auto_mode_disabled_hint)
                        },
                        fontSize = 11.sp,
                        color = if (isDynamicCountSoundEnabled) Slate400 else Slate600
                    )
                }
                Switch(
                    checked = isAutoMode && isDynamicCountSoundEnabled,
                    onCheckedChange = onAutoModeChange,
                    enabled = isDynamicCountSoundEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Orange600,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Slate500,
                        disabledCheckedThumbColor = Slate400,
                        disabledCheckedTrackColor = Slate600,
                        disabledUncheckedThumbColor = Slate400,
                        disabledUncheckedTrackColor = Slate600
                    )
                )
            }
        } else {
            // アイソメトリック種目: タイマーモード
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

            // アイソメトリック種目: 間隔通知音（秒数入力付き）
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

// モード選択画面
@Composable
fun ModeSelectionStep(
    onSingleModeSelected: () -> Unit,
    onProgramModeSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // タイトル
        Text(
            text = stringResource(R.string.workout_mode_selection),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 単発モード
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp),
            onClick = onSingleModeSelected
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.single_mode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.single_mode_description),
                        fontSize = 14.sp,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Orange600,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // プログラムモード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp),
            onClick = onProgramModeSelected
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.program_mode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.program_mode_description),
                        fontSize = 14.sp,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Orange600,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
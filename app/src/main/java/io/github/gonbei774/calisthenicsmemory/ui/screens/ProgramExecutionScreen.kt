package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionStep
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramConfirmExerciseCard
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramConfirmStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.SettingsSection
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramExecutionScreen(
    viewModel: TrainingViewModel,
    programId: Long,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val exercises by viewModel.exercises.collectAsState()
    val scope = rememberCoroutineScope()

    // プログラムと種目データをロード
    var program by remember { mutableStateOf<Program?>(null) }
    var programExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(programId) {
        program = viewModel.getProgramById(programId)
        programExercises = viewModel.getProgramExercisesSync(programId)
        isLoading = false
    }

    // セッションの初期化
    var session by remember { mutableStateOf<ProgramExecutionSession?>(null) }
    var currentStep by remember { mutableStateOf<ProgramExecutionStep?>(null) }

    // 設定（LaunchedEffect内で使用するため先に宣言）
    val workoutPreferences = remember { WorkoutPreferences(context) }

    // プログラムと種目がロードされたらセッションを構築
    LaunchedEffect(program, programExercises, exercises) {
        val prog = program ?: return@LaunchedEffect
        if (programExercises.isEmpty() || exercises.isEmpty()) return@LaunchedEffect
        if (session != null) return@LaunchedEffect // 既に初期化済み

        // 種目情報をマッピング
        val exercisePairs = programExercises.mapNotNull { pe ->
            val exercise = exercises.find { it.id == pe.exerciseId }
            if (exercise != null) pe to exercise else null
        }

        if (exercisePairs.isEmpty()) return@LaunchedEffect

        // 実行順のセットリストを構築
        val allSets = mutableListOf<ProgramWorkoutSet>()
        exercisePairs.forEachIndexed { index, (pe, exercise) ->
            for (setNum in 1..pe.sets) {
                if (exercise.laterality == "Unilateral") {
                    // 片側種目: 右→左（両方にインターバルを設定）
                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = index,
                            setNumber = setNum,
                            side = "Right",
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds
                        )
                    )
                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = index,
                            setNumber = setNum,
                            side = "Left",
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds
                        )
                    )
                } else {
                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = index,
                            setNumber = setNum,
                            side = null,
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds
                        )
                    )
                }
            }
        }

        // 前回値をプリフィル（設定がONの場合のみ）
        if (workoutPreferences.isPrefillPreviousRecordEnabled()) {
            exercisePairs.forEachIndexed { index, (_, exercise) ->
                val latestRecords = viewModel.getLatestSession(exercise.id)
                if (latestRecords.isNotEmpty()) {
                    // セットごとに前回値を適用
                    val setsForExercise = allSets.filter { it.exerciseIndex == index }
                    setsForExercise.forEach { set ->
                        val matchingRecord = latestRecords.find { r ->
                            r.setNumber == set.setNumber
                        }
                        if (matchingRecord != null) {
                            when (set.side) {
                                "Right" -> set.targetValue.let {
                                    val newValue = matchingRecord.valueRight
                                    allSets[allSets.indexOf(set)] = set.copy(targetValue = newValue)
                                }
                                "Left" -> set.targetValue.let {
                                    val newValue = matchingRecord.valueLeft ?: matchingRecord.valueRight
                                    allSets[allSets.indexOf(set)] = set.copy(targetValue = newValue)
                                }
                                else -> set.targetValue.let {
                                    val newValue = matchingRecord.valueRight
                                    allSets[allSets.indexOf(set)] = set.copy(targetValue = newValue)
                                }
                            }
                        }
                    }
                }
            }
        }

        val newSession = ProgramExecutionSession(
            program = prog,
            exercises = exercisePairs,
            sets = allSets,
            comment = "【Program】${prog.name}"
        )
        session = newSession
        currentStep = ProgramExecutionStep.Confirm(newSession)
    }

    // ビープ音・フラッシュ
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val flashController = remember { FlashController(context) }
    val isFlashEnabled = remember { workoutPreferences.isFlashNotificationEnabled() }
    val isKeepScreenOnEnabled = remember { workoutPreferences.isKeepScreenOnEnabled() }

    // 音声設定（ProgramConfirmStepで変更可能）
    var isAutoMode by remember { mutableStateOf(workoutPreferences.isAutoMode()) }
    var startCountdownSeconds by remember { mutableIntStateOf(workoutPreferences.getStartCountdown()) }
    var isDynamicCountSoundEnabled by remember { mutableStateOf(workoutPreferences.isDynamicCountSoundEnabled()) }
    var isIsometricIntervalSoundEnabled by remember { mutableStateOf(workoutPreferences.isIsometricIntervalSoundEnabled()) }
    var isometricIntervalSeconds by remember { mutableIntStateOf(workoutPreferences.getIsometricIntervalSeconds()) }

    // Foreground Service制御
    LaunchedEffect(currentStep) {
        when (currentStep) {
            is ProgramExecutionStep.StartInterval,
            is ProgramExecutionStep.Executing,
            is ProgramExecutionStep.Interval -> WorkoutTimerService.startService(context)
            else -> WorkoutTimerService.stopService(context)
        }
    }

    // 画面オン維持
    val view = LocalView.current
    LaunchedEffect(isKeepScreenOnEnabled, currentStep) {
        val window = (view.context as? android.app.Activity)?.window
        if (isKeepScreenOnEnabled) {
            when (currentStep) {
                is ProgramExecutionStep.StartInterval,
                is ProgramExecutionStep.Executing,
                is ProgramExecutionStep.Interval -> {
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

    // 中断確認ダイアログ
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // 戻るボタンのハンドリング
    BackHandler {
        when (currentStep) {
            is ProgramExecutionStep.Confirm -> onNavigateBack()
            else -> {
                // 実行中・完了画面は確認ダイアログを表示
                showExitConfirmDialog = true
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
                            is ProgramExecutionStep.Confirm -> onNavigateBack()
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
                        text = program?.name ?: stringResource(R.string.program_list_title),
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
            if (isLoading || currentStep == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Orange600
                )
            } else {
                when (val step = currentStep) {
                    is ProgramExecutionStep.Confirm -> {
                        ProgramConfirmStep(
                            session = step.session,
                            onUpdateTargetValue = { setIndex, newValue ->
                                val newSets = step.session.sets.toMutableList()
                                newSets[setIndex] = newSets[setIndex].copy(targetValue = newValue)
                                currentStep = ProgramExecutionStep.Confirm(step.session.copy(sets = newSets))
                            },
                            onUpdateInterval = { exerciseIndex, newInterval ->
                                // この種目の全セットのインターバルを更新
                                val sets = step.session.sets
                                sets.forEachIndexed { index, set ->
                                    if (set.exerciseIndex == exerciseIndex) {
                                        sets[index] = set.copy(intervalSeconds = newInterval)
                                    }
                                }
                                // 再構成をトリガー（新しいセッションオブジェクトを作成）
                                currentStep = ProgramExecutionStep.Confirm(step.session.copy())
                            },
                            onUpdateExerciseSetsValue = { exerciseIndex, delta ->
                                // この種目の全セットの値を一括更新
                                val newSets = step.session.sets.toMutableList()
                                newSets.forEachIndexed { index, set ->
                                    if (set.exerciseIndex == exerciseIndex) {
                                        val newValue = (set.targetValue + delta).coerceAtLeast(0)
                                        newSets[index] = set.copy(targetValue = newValue)
                                    }
                                }
                                currentStep = ProgramExecutionStep.Confirm(step.session.copy(sets = newSets))
                            },
                            onUpdateSetCount = { exerciseIndex, newSetCount ->
                                // セット数を変更: セットリストを再構築
                                val (pe, exercise) = step.session.exercises[exerciseIndex]
                                val currentSets = step.session.sets.filter { it.exerciseIndex == exerciseIndex }
                                val currentSetCount = if (exercise.laterality == "Unilateral") {
                                    currentSets.filter { it.side == "Right" }.size
                                } else {
                                    currentSets.size
                                }

                                if (newSetCount == currentSetCount || newSetCount < 1) return@ProgramConfirmStep

                                // 現在のインターバルと目標値を取得（最後のセットから）
                                val lastSet = currentSets.lastOrNull()
                                val interval = lastSet?.intervalSeconds ?: pe.intervalSeconds
                                val targetValue = lastSet?.targetValue ?: pe.targetValue

                                // 他の種目のセットはそのまま、この種目のセットのみ再構築
                                val newSets = mutableListOf<ProgramWorkoutSet>()
                                step.session.exercises.forEachIndexed { idx, (pex, ex) ->
                                    if (idx == exerciseIndex) {
                                        // この種目のセットを再構築
                                        for (setNum in 1..newSetCount) {
                                            // 既存セットから値を取得（あれば）
                                            val existingRight = currentSets.find { it.setNumber == setNum && it.side == "Right" }
                                            val existingLeft = currentSets.find { it.setNumber == setNum && it.side == "Left" }
                                            val existingBilateral = currentSets.find { it.setNumber == setNum && it.side == null }

                                            if (ex.laterality == "Unilateral") {
                                                newSets.add(ProgramWorkoutSet(
                                                    exerciseIndex = idx,
                                                    setNumber = setNum,
                                                    side = "Right",
                                                    targetValue = existingRight?.targetValue ?: targetValue,
                                                    intervalSeconds = interval
                                                ))
                                                newSets.add(ProgramWorkoutSet(
                                                    exerciseIndex = idx,
                                                    setNumber = setNum,
                                                    side = "Left",
                                                    targetValue = existingLeft?.targetValue ?: targetValue,
                                                    intervalSeconds = interval
                                                ))
                                            } else {
                                                newSets.add(ProgramWorkoutSet(
                                                    exerciseIndex = idx,
                                                    setNumber = setNum,
                                                    side = null,
                                                    targetValue = existingBilateral?.targetValue ?: targetValue,
                                                    intervalSeconds = interval
                                                ))
                                            }
                                        }
                                    } else {
                                        // 他の種目はそのまま
                                        newSets.addAll(step.session.sets.filter { it.exerciseIndex == idx })
                                    }
                                }
                                // 新しいsessionオブジェクトを作成して強制的に再コンポーズ
                                val newSession = step.session.copy(sets = newSets.toMutableList())
                                currentStep = ProgramExecutionStep.Confirm(newSession)
                            },
                            onUseAllProgramValues = {
                                // セット一覧を再構築（プログラム設定のセット数・目標値を使用）
                                val newSets = mutableListOf<ProgramWorkoutSet>()
                                step.session.exercises.forEachIndexed { index, (pe, exercise) ->
                                    for (setNum in 1..pe.sets) {
                                        if (exercise.laterality == "Unilateral") {
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = "Right",
                                                targetValue = pe.targetValue,
                                                intervalSeconds = pe.intervalSeconds
                                            ))
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = "Left",
                                                targetValue = pe.targetValue,
                                                intervalSeconds = pe.intervalSeconds
                                            ))
                                        } else {
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = null,
                                                targetValue = pe.targetValue,
                                                intervalSeconds = pe.intervalSeconds
                                            ))
                                        }
                                    }
                                }
                                val newSession = step.session.copy(sets = newSets.toMutableList())
                                currentStep = ProgramExecutionStep.Confirm(newSession)
                            },
                            onUseAllChallengeValues = {
                                // セット一覧を再構築（種目設定のセット数・目標値・インターバルを使用、なければプログラム設定）
                                val newSets = mutableListOf<ProgramWorkoutSet>()
                                step.session.exercises.forEachIndexed { index, (pe, exercise) ->
                                    val sets = exercise.targetSets ?: pe.sets
                                    val targetValue = exercise.targetValue ?: pe.targetValue
                                    val interval = exercise.restInterval ?: pe.intervalSeconds
                                    for (setNum in 1..sets) {
                                        if (exercise.laterality == "Unilateral") {
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = "Right",
                                                targetValue = targetValue,
                                                intervalSeconds = interval
                                            ))
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = "Left",
                                                targetValue = targetValue,
                                                intervalSeconds = interval
                                            ))
                                        } else {
                                            newSets.add(ProgramWorkoutSet(
                                                exerciseIndex = index,
                                                setNumber = setNum,
                                                side = null,
                                                targetValue = targetValue,
                                                intervalSeconds = interval
                                            ))
                                        }
                                    }
                                }
                                val newSession = step.session.copy(sets = newSets.toMutableList())
                                currentStep = ProgramExecutionStep.Confirm(newSession)
                            },
                            onUseAllPreviousRecordValues = {
                                // 前回記録を取得してセット一覧を再構築（非同期）
                                scope.launch {
                                    val newSets = mutableListOf<ProgramWorkoutSet>()
                                    step.session.exercises.forEachIndexed { index, (pe, exercise) ->
                                        val latestRecords = viewModel.getLatestSession(exercise.id)
                                        if (latestRecords.isNotEmpty()) {
                                            // 前回記録のセット数を使用
                                            latestRecords.forEach { record ->
                                                if (exercise.laterality == "Unilateral") {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = "Right",
                                                        targetValue = record.valueRight,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = "Left",
                                                        targetValue = record.valueLeft ?: record.valueRight,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                } else {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = null,
                                                        targetValue = record.valueRight,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                }
                                            }
                                        } else {
                                            // 前回記録がない場合はプログラム設定を使用
                                            for (setNum in 1..pe.sets) {
                                                if (exercise.laterality == "Unilateral") {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = "Right",
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = "Left",
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                } else {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = null,
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds
                                                    ))
                                                }
                                            }
                                        }
                                    }
                                    val newSession = step.session.copy(sets = newSets.toMutableList())
                                    currentStep = ProgramExecutionStep.Confirm(newSession)
                                }
                            },
                            onUpdateAllExercisesValue = { delta ->
                                // 全種目の全セットの値を更新（最小値0）
                                val sets = step.session.sets
                                sets.forEachIndexed { index, set ->
                                    val newValue = (set.targetValue + delta).coerceAtLeast(0)
                                    sets[index] = set.copy(targetValue = newValue)
                                }
                                // 再構成をトリガー（新しいセッションオブジェクトを作成）
                                currentStep = ProgramExecutionStep.Confirm(step.session.copy())
                            },
                            // 音声設定
                            isAutoMode = isAutoMode,
                            startCountdownSeconds = startCountdownSeconds,
                            isDynamicCountSoundEnabled = isDynamicCountSoundEnabled,
                            isIsometricIntervalSoundEnabled = isIsometricIntervalSoundEnabled,
                            isometricIntervalSeconds = isometricIntervalSeconds,
                            onAutoModeChange = { value ->
                                isAutoMode = value
                                workoutPreferences.setAutoMode(value)
                            },
                            onStartCountdownChange = { value ->
                                startCountdownSeconds = value
                                workoutPreferences.setStartCountdown(value)
                            },
                            onDynamicCountSoundChange = { value ->
                                isDynamicCountSoundEnabled = value
                                workoutPreferences.setDynamicCountSoundEnabled(value)
                            },
                            onIsometricIntervalSoundChange = { value ->
                                isIsometricIntervalSoundEnabled = value
                                workoutPreferences.setIsometricIntervalSoundEnabled(value)
                            },
                            onIsometricIntervalSecondsChange = { value ->
                                isometricIntervalSeconds = value
                                workoutPreferences.setIsometricIntervalSeconds(value)
                            },
                            onStart = {
                                // プログラムの開始カウントダウンが0より大きい場合のみ表示
                                if (startCountdownSeconds > 0) {
                                    currentStep = ProgramExecutionStep.StartInterval(step.session, 0)
                                } else {
                                    currentStep = ProgramExecutionStep.Executing(step.session, 0)
                                }
                            }
                        )
                    }

                    is ProgramExecutionStep.StartInterval -> {
                        ProgramStartIntervalStep(
                            session = step.session,
                            currentSetIndex = step.currentSetIndex,
                            startCountdownSeconds = startCountdownSeconds,
                            toneGenerator = toneGenerator,
                            flashController = flashController,
                            isFlashEnabled = isFlashEnabled,
                            onComplete = {
                                currentStep = ProgramExecutionStep.Executing(step.session, step.currentSetIndex)
                            }
                        )
                    }

                    is ProgramExecutionStep.Executing -> {
                        // タイマーモードに応じて分岐
                        val currentSet = step.session.sets[step.currentSetIndex]
                        val (_, currentExercise) = step.session.exercises[currentSet.exerciseIndex]

                        if (isAutoMode) {
                            // タイマーON: 自動カウント
                            if (currentExercise.type == "Isometric") {
                                // Isometric種目: 新UIで自動遷移
                                ProgramExecutingStepIsometricAuto(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isIntervalSoundEnabled = isIsometricIntervalSoundEnabled,
                                    intervalSeconds = isometricIntervalSeconds,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]
                                        val nextIndex = step.currentSetIndex + 1

                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0) {
                                                currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                            } else if (startCountdownSeconds > 0) {
                                                currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                            } else {
                                                currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                            }
                                        } else {
                                            currentStep = ProgramExecutionStep.Result(step.session)
                                        }
                                    },
                                    onAbort = {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                )
                            } else {
                                // Dynamic種目: 新UI（Isometricと統一）
                                ProgramExecutingStepDynamicAuto(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isCountSoundEnabled = isDynamicCountSoundEnabled,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]

                                        val nextIndex = step.currentSetIndex + 1
                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0) {
                                                currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                            } else if (startCountdownSeconds > 0) {
                                                currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                            } else {
                                                currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                            }
                                        } else {
                                            currentStep = ProgramExecutionStep.Result(step.session)
                                        }
                                    },
                                    onAbort = {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                )
                            }
                        } else {
                            // タイマーOFF: 手動完了
                            if (currentExercise.type == "Isometric") {
                                // Isometric種目: タイマー付き手動完了
                                ProgramExecutingStepIsometricManual(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isIntervalSoundEnabled = isIsometricIntervalSoundEnabled,
                                    intervalSeconds = isometricIntervalSeconds,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]
                                        val nextIndex = step.currentSetIndex + 1

                                        // 次のセットへ
                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0) {
                                                currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                            } else if (startCountdownSeconds > 0) {
                                                currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                            } else {
                                                currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                            }
                                        } else {
                                            currentStep = ProgramExecutionStep.Result(step.session)
                                        }
                                    },
                                    onAbort = {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                )
                            } else {
                                // Dynamic種目: 新UI（Timer ONと統一、自動遷移なし）
                                ProgramExecutingStepDynamicManual(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isCountSoundEnabled = isDynamicCountSoundEnabled,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]
                                        val nextIndex = step.currentSetIndex + 1

                                        // 次のセットへ
                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0) {
                                                currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                            } else if (startCountdownSeconds > 0) {
                                                currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                            } else {
                                                currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                            }
                                        } else {
                                            currentStep = ProgramExecutionStep.Result(step.session)
                                        }
                                    },
                                    onAbort = {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                )
                            }
                        }
                    }

                    is ProgramExecutionStep.Interval -> {
                        ProgramIntervalStep(
                            session = step.session,
                            currentSetIndex = step.currentSetIndex,
                            toneGenerator = toneGenerator,
                            flashController = flashController,
                            isFlashEnabled = isFlashEnabled,
                            onComplete = {
                                val nextIndex = step.currentSetIndex + 1
                                if (nextIndex < step.session.sets.size) {
                                    // プログラムの開始カウントダウンが0より大きい場合のみ表示
                                    if (startCountdownSeconds > 0) {
                                        currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                    } else {
                                        currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                    }
                                } else {
                                    currentStep = ProgramExecutionStep.Result(step.session)
                                }
                            },
                            onSkip = {
                                val nextIndex = step.currentSetIndex + 1
                                if (nextIndex < step.session.sets.size) {
                                    // プログラムの開始カウントダウンが0より大きい場合のみ表示
                                    if (startCountdownSeconds > 0) {
                                        currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                    } else {
                                        currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                    }
                                } else {
                                    currentStep = ProgramExecutionStep.Result(step.session)
                                }
                            }
                        )
                    }

                    is ProgramExecutionStep.Result -> {
                        ProgramResultStep(
                            session = step.session,
                            onSave = {
                                scope.launch {
                                    saveProgramResults(viewModel, step.session)
                                    onComplete()
                                }
                            },
                            onCancel = onNavigateBack
                        )
                    }

                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun ProgramStartIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    startCountdownSeconds: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    onComplete: () -> Unit
) {
    val currentSet = session.sets[currentSetIndex]
    val (_, exercise) = session.exercises[currentSet.exerciseIndex]

    var remainingTime by remember { mutableIntStateOf(startCountdownSeconds) }
    val progress = remainingTime.toFloat() / startCountdownSeconds

    LaunchedEffect(currentSetIndex) {
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
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        if (isFlashEnabled) {
            launch { flashController.flashComplete() }
        }
        delay(300L)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 種目名
        Text(
            text = exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        // セット情報
        val sideText = when (currentSet.side) {
            "Right" -> stringResource(R.string.side_right)
            "Left" -> stringResource(R.string.side_left)
            else -> null
        }
        val (pe, _) = session.exercises[currentSet.exerciseIndex]
        Text(
            text = if (sideText != null) {
                stringResource(R.string.set_format_with_side, currentSet.setNumber, pe.sets, sideText)
            } else {
                stringResource(R.string.set_format, currentSet.setNumber, pe.sets)
            },
            fontSize = 18.sp,
            color = Slate300,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 準備中表示
        Text(
            text = stringResource(R.string.get_ready),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Orange600
        )

        Spacer(modifier = Modifier.height(48.dp))

        // タイマー
        ProgramCircularTimer(
            progress = progress,
            remainingTime = remainingTime,
            color = Orange600
        )

        Spacer(modifier = Modifier.weight(1f))

        // 次の種目/セット情報
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)
    }
}

@Composable
private fun ProgramIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val currentSet = session.sets[currentSetIndex]
    val nextSetIndex = currentSetIndex + 1
    val nextSet = session.sets.getOrNull(nextSetIndex)

    var remainingTime by remember { mutableIntStateOf(currentSet.intervalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    val progress = if (currentSet.intervalSeconds > 0) {
        remainingTime.toFloat() / currentSet.intervalSeconds
    } else 0f

    LaunchedEffect(currentSetIndex) {
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
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            if (isFlashEnabled) {
                launch { flashController.flashComplete() }
            }
            delay(300L)
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 現在の種目名
        val (_, currentExercise) = session.exercises[currentSet.exerciseIndex]
        Text(
            text = currentExercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 中央エリア
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 休憩表示
            Text(
                text = stringResource(R.string.interval_label),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Cyan600
            )

            // 次のセット/種目表示
            nextSet?.let { next ->
                val (nextPe, nextExercise) = session.exercises[next.exerciseIndex]
                val nextSideText = when (next.side) {
                    "Right" -> stringResource(R.string.side_right)
                    "Left" -> stringResource(R.string.side_left)
                    else -> null
                }

                // 次の種目名（常に表示）
                Text(
                    text = stringResource(R.string.next_exercise_label, nextExercise.name),
                    fontSize = 18.sp,
                    color = Slate300,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // 次のセット情報
                Text(
                    text = if (nextSideText != null) {
                        stringResource(R.string.set_format_with_side, next.setNumber, nextPe.sets, nextSideText)
                    } else {
                        stringResource(R.string.set_format, next.setNumber, nextPe.sets)
                    },
                    fontSize = 18.sp,
                    color = Slate300,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // タイマー
            ProgramCircularTimer(
                progress = progress,
                remainingTime = remainingTime,
                color = Cyan600
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 一時停止/再開ボタン
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

        // 調整ボタン
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { remainingTime = (remainingTime - 10).coerceAtLeast(0) }
            ) {
                Text("-", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.ten_seconds),
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { remainingTime += 10 }
            ) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // スキップボタン
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip_button),
                color = Slate400
            )
        }
    }
}

@Composable
private fun ProgramResultStep(
    session: ProgramExecutionSession,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var comment by remember { mutableStateOf(session.comment) }

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

        // コメント入力
        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
                session.comment = it
            },
            label = { Text(stringResource(R.string.comment_label)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 結果一覧
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            session.exercises.forEachIndexed { exerciseIndex, (_, exercise) ->
                val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex }

                // 種目名ヘッダー
                item(key = "header-$exerciseIndex") {
                    Text(
                        text = exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = if (exerciseIndex > 0) 8.dp else 0.dp)
                    )
                }

                if (exercise.laterality == "Unilateral") {
                    // 片側種目: セット番号でグループ化
                    val groupedSets = setsForExercise.groupBy { it.setNumber }
                    groupedSets.forEach { (setNumber, sets) ->
                        val rightSet = sets.firstOrNull { it.side == "Right" }
                        val leftSet = sets.firstOrNull { it.side == "Left" }

                        item(key = "exercise-$exerciseIndex-set-$setNumber") {
                            ProgramUnilateralSetItem(
                                setNumber = setNumber,
                                rightSet = rightSet,
                                leftSet = leftSet,
                                exerciseType = exercise.type
                            )
                        }
                    }
                } else {
                    // 両側種目
                    setsForExercise.forEach { set ->
                        item(key = "exercise-$exerciseIndex-set-${set.setNumber}") {
                            ProgramBilateralSetItem(
                                set = set,
                                exerciseType = exercise.type
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 保存ボタン
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600)
        ) {
            Text(
                text = stringResource(R.string.record_workout),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

// Program用 Unilateralセットアイテム（1行表示）
@Composable
private fun ProgramUnilateralSetItem(
    setNumber: Int,
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    exerciseType: String
) {
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
                            newValue.toIntOrNull()?.let { rightSet?.actualValue = it }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                            newValue.toIntOrNull()?.let { leftSet?.actualValue = it }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

// Program用 Bilateralセットアイテム
@Composable
private fun ProgramBilateralSetItem(
    set: ProgramWorkoutSet,
    exerciseType: String
) {
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
                        newValue.toIntOrNull()?.let { set.actualValue = it }
                    }
                },
                label = {
                    Text(
                        stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

// 次の種目/セット情報を表示するコンポーザブル
@Composable
internal fun NextExerciseInfo(
    session: ProgramExecutionSession,
    currentSetIndex: Int
) {
    val nextSetIndex = currentSetIndex + 1
    val nextSet = session.sets.getOrNull(nextSetIndex) ?: return

    val (nextPe, nextExercise) = session.exercises[nextSet.exerciseIndex]
    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // 次の種目名（常に表示）
        Text(
            text = stringResource(R.string.next_exercise_label, nextExercise.name),
            fontSize = 16.sp,
            color = Slate400
        )
        // 次のセット情報
        Text(
            text = if (nextSideText != null) {
                stringResource(R.string.set_format_with_side, nextSet.setNumber, nextPe.sets, nextSideText)
            } else {
                stringResource(R.string.set_format, nextSet.setNumber, nextPe.sets)
            },
            fontSize = 14.sp,
            color = Slate400
        )
    }
}

@Composable
private fun ProgramCircularTimer(
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
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
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

// 記録保存
private fun saveProgramResults(
    viewModel: TrainingViewModel,
    session: ProgramExecutionSession
) {
    val date = LocalDate.now().toString()
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    // 同じ種目がプログラム内で複数回出てくる場合、全セットをまとめて1回で保存
    // 種目IDでグループ化
    val groupedByExerciseId = session.exercises
        .mapIndexed { index, pair -> index to pair }
        .groupBy { (_, pair) -> pair.second.id }

    groupedByExerciseId.forEach { (exerciseId, exerciseInfoList) ->
        val exercise = exerciseInfoList.first().second.second

        // この種目の全セット（プログラム内で複数回出てきても全て収集）
        // isCompleted または isSkipped のセットを記録対象とする
        val allSetsForExercise = exerciseInfoList.flatMap { (exerciseIndex, _) ->
            session.sets.filter {
                it.exerciseIndex == exerciseIndex && (it.isCompleted || it.isSkipped)
            }
        }

        if (allSetsForExercise.isEmpty()) return@forEach

        if (exercise.laterality == "Unilateral") {
            // 片側種目: 右・左をまとめて記録
            val rightSets = allSetsForExercise.filter { it.side == "Right" }
            val leftSets = allSetsForExercise.filter { it.side == "Left" }

            val valuesRight = rightSets.map { it.actualValue }
            val valuesLeft = leftSets.map { it.actualValue }

            if (valuesRight.isNotEmpty()) {
                viewModel.addTrainingRecordsUnilateral(
                    exerciseId = exerciseId,
                    valuesRight = valuesRight,
                    valuesLeft = valuesLeft,
                    date = date,
                    time = time,
                    comment = session.comment
                )
            }
        } else {
            // 両側種目
            val values = allSetsForExercise.map { it.actualValue }

            if (values.isNotEmpty()) {
                viewModel.addTrainingRecords(
                    exerciseId = exerciseId,
                    values = values,
                    date = date,
                    time = time,
                    comment = session.comment
                )
            }
        }
    }
}
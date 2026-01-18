package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.gonbei774.calisthenicsmemory.data.SavedWorkoutState
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramConfirmExerciseCard
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramConfirmStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepDynamicSimple
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricAuto
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExecutingStepIsometricManual
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramIntervalStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramNavigationSheet
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramResultStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramStartIntervalStep
import io.github.gonbei774.calisthenicsmemory.ui.components.program.SettingsSection
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.buildChallengeValueSets
import io.github.gonbei774.calisthenicsmemory.util.buildProgramValueSets
import io.github.gonbei774.calisthenicsmemory.util.saveProgramResults
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop

// ループ実行時に使用するsealed class
private sealed class ExecutionItem {
    data class StandaloneExercise(
        val exerciseIndex: Int,
        val pe: ProgramExercise,
        val exercise: Exercise,
        val sortOrder: Int
    ) : ExecutionItem()

    data class Loop(
        val loop: ProgramLoop,
        val exercises: List<Triple<Int, ProgramExercise, Exercise>>
    ) : ExecutionItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramExecutionScreen(
    viewModel: TrainingViewModel,
    programId: Long,
    resumeSavedState: Boolean = false,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val exercises by viewModel.exercises.collectAsState()
    val scope = rememberCoroutineScope()

    // プログラムと種目データをロード
    var program by remember { mutableStateOf<Program?>(null) }
    var programExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }
    var programLoops by remember { mutableStateOf<List<io.github.gonbei774.calisthenicsmemory.data.ProgramLoop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(programId) {
        program = viewModel.getProgramById(programId)
        programExercises = viewModel.getProgramExercisesSync(programId)
        programLoops = viewModel.getProgramLoopsSync(programId)
        isLoading = false
    }

    // セッションの初期化
    var session by remember { mutableStateOf<ProgramExecutionSession?>(null) }
    var currentStep by remember { mutableStateOf<ProgramExecutionStep?>(null) }

    // 設定（LaunchedEffect内で使用するため先に宣言）
    val workoutPreferences = remember { WorkoutPreferences(context) }

    // プログラムと種目がロードされたらセッションを構築
    LaunchedEffect(program, programExercises, programLoops, exercises) {
        val prog = program ?: return@LaunchedEffect
        if (programExercises.isEmpty() || exercises.isEmpty()) return@LaunchedEffect
        if (session != null) return@LaunchedEffect // 既に初期化済み

        // 種目情報をマッピング
        val exercisePairs = programExercises.mapNotNull { pe ->
            val exercise = exercises.find { it.id == pe.exerciseId }
            if (exercise != null) pe to exercise else null
        }

        if (exercisePairs.isEmpty()) return@LaunchedEffect

        // 前回値を取得（表示用に常に取得）
        val previousRecordsMap = mutableMapOf<Long, List<io.github.gonbei774.calisthenicsmemory.data.TrainingRecord>>()
        exercisePairs.forEach { (_, exercise) ->
            previousRecordsMap[exercise.id] = viewModel.getLatestSession(exercise.id)
        }

        val isPrefillEnabled = workoutPreferences.isPrefillPreviousRecordEnabled()

        // ループ情報をマップ化
        val loopMap = programLoops.associateBy { it.id }

        // スタンドアロン種目とループを sortOrder でソートして実行順に並べる
        val executionItems = mutableListOf<ExecutionItem>()

        // スタンドアロン種目を追加
        exercisePairs.forEachIndexed { index, (pe, exercise) ->
            if (pe.loopId == null) {
                executionItems.add(ExecutionItem.StandaloneExercise(index, pe, exercise, pe.sortOrder))
            }
        }

        // ループを追加（ループ内の種目も含む）
        programLoops.forEach { loop ->
            val loopExercises = exercisePairs
                .mapIndexedNotNull { index, (pe, exercise) ->
                    if (pe.loopId == loop.id) Triple(index, pe, exercise) else null
                }
                .sortedBy { it.second.sortOrder }
            if (loopExercises.isNotEmpty()) {
                executionItems.add(ExecutionItem.Loop(loop, loopExercises))
            }
        }

        // sortOrder でソート（スタンドアロンはpe.sortOrder、ループはloop.sortOrder）
        val sortedItems = executionItems.sortedBy { item ->
            when (item) {
                is ExecutionItem.StandaloneExercise -> item.sortOrder
                is ExecutionItem.Loop -> item.loop.sortOrder
            }
        }

        // 実行順のセットリストを構築（前回値を含む）
        val allSets = mutableListOf<ProgramWorkoutSet>()

        // ヘルパー関数: 種目のセットを追加
        fun addExerciseSets(
            exerciseIndex: Int,
            pe: ProgramExercise,
            exercise: Exercise,
            loopId: Long? = null,
            roundNumber: Int = 1,
            totalRounds: Int = 1,
            loopRestAfterSeconds: Int = 0,
            isLastExerciseInRound: Boolean = false
        ) {
            val latestRecords = previousRecordsMap[exercise.id] ?: emptyList()

            for (setNum in 1..pe.sets) {
                val matchingRecord = latestRecords.find { it.setNumber == setNum }
                val isLastSetOfExercise = setNum == pe.sets

                // ラウンド間休憩は最後のラウンド以外で、ループの最後の種目の最後のセットに追加
                val actualLoopRestAfter = if (isLastExerciseInRound && isLastSetOfExercise) loopRestAfterSeconds else 0

                if (exercise.laterality == "Unilateral") {
                    val prevRight = matchingRecord?.valueRight
                    val prevLeft = matchingRecord?.valueLeft ?: matchingRecord?.valueRight
                    val prevAverage = when {
                        prevRight != null && prevLeft != null -> (prevRight + prevLeft) / 2
                        prevRight != null -> prevRight
                        prevLeft != null -> prevLeft
                        else -> null
                    }

                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = "Right",
                            targetValue = if (isPrefillEnabled && prevRight != null) prevRight else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevAverage,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = 0  // Rightの後はLeftが来るので0
                        )
                    )
                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = "Left",
                            targetValue = if (isPrefillEnabled && prevLeft != null) prevLeft else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevAverage,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = actualLoopRestAfter
                        )
                    )
                } else {
                    val prevValue = matchingRecord?.valueRight

                    allSets.add(
                        ProgramWorkoutSet(
                            exerciseIndex = exerciseIndex,
                            setNumber = setNum,
                            side = null,
                            targetValue = if (isPrefillEnabled && prevValue != null) prevValue else pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            previousValue = prevValue,
                            loopId = loopId,
                            roundNumber = roundNumber,
                            totalRounds = totalRounds,
                            loopRestAfterSeconds = actualLoopRestAfter
                        )
                    )
                }
            }
        }

        // 実行アイテムを順に処理
        sortedItems.forEach { item ->
            when (item) {
                is ExecutionItem.StandaloneExercise -> {
                    addExerciseSets(item.exerciseIndex, item.pe, item.exercise)
                }
                is ExecutionItem.Loop -> {
                    val loop = item.loop
                    val loopExercises = item.exercises

                    // 各ラウンドを処理
                    for (round in 1..loop.rounds) {
                        // 全ラウンド終了後も休憩を入れる（次の種目/ループへの準備時間として）
                        val loopRestAfter = loop.restBetweenRounds

                        loopExercises.forEachIndexed { loopExIdx, (exerciseIndex, pe, exercise) ->
                            val isLastExerciseInRound = loopExIdx == loopExercises.size - 1
                            addExerciseSets(
                                exerciseIndex = exerciseIndex,
                                pe = pe,
                                exercise = exercise,
                                loopId = loop.id,
                                roundNumber = round,
                                totalRounds = loop.rounds,
                                loopRestAfterSeconds = loopRestAfter,
                                isLastExerciseInRound = isLastExerciseInRound
                            )
                        }
                    }
                }
            }
        }

        // 保存状態からの復元チェック
        val savedState = SavedWorkoutState(context)
        if (resumeSavedState && savedState.getSavedProgramId() == programId) {
            // 保存された状態を復元
            val savedSets = savedState.getSets()
            val savedComment = savedState.getComment()
            val savedSetIndex = savedState.getCurrentSetIndex()

            if (savedSets != null && savedSets.size == allSets.size) {
                // 保存されたセット状態を適用
                val newSession = ProgramExecutionSession(
                    program = prog,
                    exercises = exercisePairs,
                    sets = savedSets.toMutableList(),
                    comment = savedComment,
                    loops = programLoops
                )
                session = newSession

                // 保存状態をクリア
                savedState.clear()

                // 保存されたセットインデックスから再開（準備カウントダウンありの場合）
                val resumeIndex = savedSetIndex.coerceIn(0, savedSets.size - 1)
                if (workoutPreferences.getStartCountdown() > 0) {
                    currentStep = ProgramExecutionStep.StartInterval(newSession, resumeIndex)
                } else {
                    currentStep = ProgramExecutionStep.Executing(newSession, resumeIndex)
                }
                return@LaunchedEffect
            }
        }

        val newSession = ProgramExecutionSession(
            program = prog,
            exercises = exercisePairs,
            sets = allSets,
            comment = "【Program】${prog.name}",
            loops = programLoops
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

    // Result画面の破棄確認ダイアログ
    var showDiscardResultDialog by remember { mutableStateOf(false) }

    // Save & Exit上書き確認ダイアログ
    var showSaveOverwriteDialog by remember { mutableStateOf(false) }
    var pendingSaveSession by remember { mutableStateOf<ProgramExecutionSession?>(null) }
    var pendingSaveSetIndex by remember { mutableIntStateOf(0) }

    // ナビゲーションシート表示状態
    var showNavigationSheet by remember { mutableStateOf(false) }

    // やり直し用キー（startCountdownSeconds == 0 のときにコンポーネントをリセットするため）
    var retryKey by remember { mutableIntStateOf(0) }

    // Redoモード（やり直し後は次の未完了セットへ自動ジャンプ）
    var isRedoMode by remember { mutableStateOf(false) }

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

    // Result画面の破棄確認ダイアログ
    if (showDiscardResultDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardResultDialog = false },
            title = { Text(stringResource(R.string.discard_result_title)) },
            text = { Text(stringResource(R.string.discard_result_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardResultDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard_result_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardResultDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Save & Exit上書き確認ダイアログ
    if (showSaveOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showSaveOverwriteDialog = false },
            title = { Text(stringResource(R.string.nav_save_overwrite_title)) },
            text = { Text(stringResource(R.string.nav_save_overwrite_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveOverwriteDialog = false
                        // 保存を実行
                        pendingSaveSession?.let { saveSession ->
                            val savedState = SavedWorkoutState(context)
                            savedState.save(
                                programId = programId,
                                currentSetIndex = pendingSaveSetIndex,
                                sets = saveSession.sets.toList(),
                                comment = saveSession.comment
                            )
                        }
                        pendingSaveSession = null
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.nav_save_and_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveOverwriteDialog = false
                    pendingSaveSession = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ナビゲーションボタンを表示するかどうか
    val showNavigationButton = when (currentStep) {
        is ProgramExecutionStep.StartInterval,
        is ProgramExecutionStep.Executing,
        is ProgramExecutionStep.Interval,
        is ProgramExecutionStep.Result -> true
        else -> false
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
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    // 開始ボタン（Confirm画面のみ表示）
                    if (currentStep is ProgramExecutionStep.Confirm) {
                        val confirmStep = currentStep as ProgramExecutionStep.Confirm
                        TextButton(
                            onClick = {
                                if (startCountdownSeconds > 0) {
                                    currentStep = ProgramExecutionStep.StartInterval(confirmStep.session, 0)
                                } else {
                                    currentStep = ProgramExecutionStep.Executing(confirmStep.session, 0)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.program_start),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // ナビゲーションボタン（実行中/インターバル時のみ表示）
                    if (showNavigationButton) {
                        IconButton(onClick = { showNavigationSheet = true }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_program_overview),
                                tint = Color.White
                            )
                        }
                    }
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

                                // ループ内種目の場合、1ラウンド分のセット数を計算
                                val firstRoundSets = currentSets.filter { it.roundNumber == 1 }
                                val currentSetCount = if (exercise.laterality == "Unilateral") {
                                    firstRoundSets.filter { it.side == "Right" }.size
                                } else {
                                    firstRoundSets.size
                                }

                                if (newSetCount == currentSetCount || newSetCount < 1) return@ProgramConfirmStep

                                // ループ情報を取得（最初のセットから）
                                val firstSet = currentSets.firstOrNull()
                                val loopId = firstSet?.loopId
                                val totalRounds = firstSet?.totalRounds ?: 1

                                // 現在のインターバルと目標値を取得（1ラウンド目の最後のセットから）
                                val lastSetOfFirstRound = firstRoundSets.lastOrNull()
                                val interval = lastSetOfFirstRound?.intervalSeconds ?: pe.intervalSeconds
                                val targetValue = lastSetOfFirstRound?.targetValue ?: pe.targetValue

                                // 他の種目のセットはそのまま、この種目のセットのみ再構築
                                val newSets = mutableListOf<ProgramWorkoutSet>()
                                step.session.exercises.forEachIndexed { idx, (pex, ex) ->
                                    if (idx == exerciseIndex) {
                                        // この種目のセットを再構築（各ラウンドごと）
                                        for (round in 1..totalRounds) {
                                            val isLastRound = round == totalRounds
                                            // このラウンドの既存セットからloopRestAfterSecondsを取得
                                            val existingRoundSets = currentSets.filter { it.roundNumber == round }
                                            val loopRestAfter = existingRoundSets.lastOrNull()?.loopRestAfterSeconds ?: 0

                                            for (setNum in 1..newSetCount) {
                                                val isLastSetOfRound = setNum == newSetCount
                                                // 既存セットから値を取得（あれば、同じラウンドのもの）
                                                val existingRight = existingRoundSets.find { it.setNumber == setNum && it.side == "Right" }
                                                val existingLeft = existingRoundSets.find { it.setNumber == setNum && it.side == "Left" }
                                                val existingBilateral = existingRoundSets.find { it.setNumber == setNum && it.side == null }

                                                if (ex.laterality == "Unilateral") {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = idx,
                                                        setNumber = setNum,
                                                        side = "Right",
                                                        targetValue = existingRight?.targetValue ?: targetValue,
                                                        intervalSeconds = interval,
                                                        previousValue = existingRight?.previousValue,
                                                        loopId = loopId,
                                                        roundNumber = round,
                                                        totalRounds = totalRounds,
                                                        loopRestAfterSeconds = 0  // Rightの後はLeftが来る
                                                    ))
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = idx,
                                                        setNumber = setNum,
                                                        side = "Left",
                                                        targetValue = existingLeft?.targetValue ?: targetValue,
                                                        intervalSeconds = interval,
                                                        previousValue = existingLeft?.previousValue,
                                                        loopId = loopId,
                                                        roundNumber = round,
                                                        totalRounds = totalRounds,
                                                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0
                                                    ))
                                                } else {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = idx,
                                                        setNumber = setNum,
                                                        side = null,
                                                        targetValue = existingBilateral?.targetValue ?: targetValue,
                                                        intervalSeconds = interval,
                                                        previousValue = existingBilateral?.previousValue,
                                                        loopId = loopId,
                                                        roundNumber = round,
                                                        totalRounds = totalRounds,
                                                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0
                                                    ))
                                                }
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
                                val newSets = buildProgramValueSets(step.session.exercises, step.session.sets)
                                val newSession = step.session.copy(sets = newSets)
                                currentStep = ProgramExecutionStep.Confirm(newSession)
                            },
                            onUseAllChallengeValues = {
                                // セット一覧を再構築（種目設定のセット数・目標値・インターバルを使用、なければプログラム設定）
                                val newSets = buildChallengeValueSets(step.session.exercises, step.session.sets)
                                val newSession = step.session.copy(sets = newSets)
                                currentStep = ProgramExecutionStep.Confirm(newSession)
                            },
                            onUseAllPreviousRecordValues = {
                                // 前回記録を取得してセット一覧を再構築（非同期）
                                scope.launch {
                                    val originalSets = step.session.sets
                                    val newSets = mutableListOf<ProgramWorkoutSet>()
                                    step.session.exercises.forEachIndexed { index, (pe, exercise) ->
                                        val latestRecords = viewModel.getLatestSession(exercise.id)
                                        if (latestRecords.isNotEmpty()) {
                                            // 前回記録のセット数を使用
                                            latestRecords.forEach { record ->
                                                if (exercise.laterality == "Unilateral") {
                                                    val valueRight = record.valueRight
                                                    val valueLeft = record.valueLeft ?: record.valueRight
                                                    // 前回値は左右の平均値を使用（目標値も平均値に設定）
                                                    val prevAverage = (valueRight + valueLeft) / 2
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = "Right",
                                                        targetValue = prevAverage,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = prevAverage
                                                    ))
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = "Left",
                                                        targetValue = prevAverage,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = prevAverage
                                                    ))
                                                } else {
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = record.setNumber,
                                                        side = null,
                                                        targetValue = record.valueRight,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = record.valueRight
                                                    ))
                                                }
                                            }
                                        } else {
                                            // 前回記録がない場合はプログラム設定を使用（元のpreviousValueを引き継ぐ）
                                            for (setNum in 1..pe.sets) {
                                                if (exercise.laterality == "Unilateral") {
                                                    val prevRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" }?.previousValue
                                                    val prevLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" }?.previousValue
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = "Right",
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = prevRight
                                                    ))
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = "Left",
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = prevLeft
                                                    ))
                                                } else {
                                                    val prevValue = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null }?.previousValue
                                                    newSets.add(ProgramWorkoutSet(
                                                        exerciseIndex = index,
                                                        setNumber = setNum,
                                                        side = null,
                                                        targetValue = pe.targetValue,
                                                        intervalSeconds = pe.intervalSeconds,
                                                        previousValue = prevValue
                                                    ))
                                                }
                                            }
                                        }
                                    }
                                    val newSession = step.session.copy(sets = newSets.toMutableList())
                                    currentStep = ProgramExecutionStep.Confirm(newSession)
                                }
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
                            isNavigationOpen = showNavigationSheet,
                            onComplete = {
                                currentStep = ProgramExecutionStep.Executing(step.session, step.currentSetIndex)
                            },
                            onSkip = {
                                currentStep = ProgramExecutionStep.Executing(step.session, step.currentSetIndex)
                            }
                        )
                    }

                    is ProgramExecutionStep.Executing -> {
                        // タイマーモードに応じて分岐
                        val currentSet = step.session.sets[step.currentSetIndex]
                        val (_, currentExercise) = step.session.exercises[currentSet.exerciseIndex]

                        // やり直し処理（準備カウントダウンに戻る or 内部リセット）
                        val handleRetry: () -> Unit = {
                            if (startCountdownSeconds > 0) {
                                currentStep = ProgramExecutionStep.StartInterval(step.session, step.currentSetIndex)
                            } else {
                                retryKey++
                            }
                        }

                        // key() でラップすることで、retryKey が変わったときにコンポーネントが再生成される
                        key(step.currentSetIndex, retryKey) {
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
                                        isNavigationOpen = showNavigationSheet,
                                        onSetComplete = { actualValue ->
                                            val sets = step.session.sets
                                            sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                                actualValue = actualValue,
                                                isCompleted = true
                                            )
                                            val completedSet = sets[step.currentSetIndex]
                                            val nextIndex = step.currentSetIndex + 1

                                            if (nextIndex < sets.size) {
                                                if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                        },
                                        onRetry = handleRetry,
                                        onOpenNavigation = { showNavigationSheet = true }
                                    )
                                } else if (!isDynamicCountSoundEnabled) {
                                // Dynamic種目 + レップ数数え上げOFF: シンプルカウンター
                                ProgramExecutingStepDynamicSimple(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]

                                        val nextIndex = step.currentSetIndex + 1
                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                    },
                                    onOpenNavigation = { showNavigationSheet = true }
                                )
                            } else {
                                // Dynamic種目: 自動カウント（タイマー付き）
                                ProgramExecutingStepDynamicAuto(
                                    session = step.session,
                                    currentSetIndex = step.currentSetIndex,
                                    toneGenerator = toneGenerator,
                                    flashController = flashController,
                                    isFlashEnabled = isFlashEnabled,
                                    isCountSoundEnabled = isDynamicCountSoundEnabled,
                                    isNavigationOpen = showNavigationSheet,
                                    onSetComplete = { actualValue ->
                                        val sets = step.session.sets
                                        sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                            actualValue = actualValue,
                                            isCompleted = true
                                        )
                                        val completedSet = sets[step.currentSetIndex]

                                        val nextIndex = step.currentSetIndex + 1
                                        if (nextIndex < sets.size) {
                                            if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                    },
                                    onRetry = handleRetry,
                                    onOpenNavigation = { showNavigationSheet = true }
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
                                        isNavigationOpen = showNavigationSheet,
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
                                                if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                        },
                                        onRetry = handleRetry,
                                        onOpenNavigation = { showNavigationSheet = true }
                                    )
                                } else if (!isDynamicCountSoundEnabled) {
                                    // Dynamic種目 + レップ数数え上げOFF: シンプルカウンター
                                    ProgramExecutingStepDynamicSimple(
                                        session = step.session,
                                        currentSetIndex = step.currentSetIndex,
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
                                                if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                        },
                                        onOpenNavigation = { showNavigationSheet = true }
                                    )
                                } else {
                                    // Dynamic種目: タイマー付き手動完了
                                    ProgramExecutingStepDynamicManual(
                                        session = step.session,
                                        currentSetIndex = step.currentSetIndex,
                                        toneGenerator = toneGenerator,
                                        flashController = flashController,
                                        isFlashEnabled = isFlashEnabled,
                                        isCountSoundEnabled = isDynamicCountSoundEnabled,
                                        isNavigationOpen = showNavigationSheet,
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
                                                if (completedSet.intervalSeconds > 0 || completedSet.loopRestAfterSeconds > 0) {
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
                                        },
                                        onRetry = handleRetry,
                                        onOpenNavigation = { showNavigationSheet = true }
                                    )
                                }
                            }
                        }
                    }

                    is ProgramExecutionStep.Interval -> {
                        // Redoモード時は次の未完了セットを探す処理
                        val findNextIncompleteSetIndex: () -> Int = {
                            val sets = step.session.sets
                            var nextIndex = -1
                            for (i in (step.currentSetIndex + 1) until sets.size) {
                                if (!sets[i].isCompleted) {
                                    nextIndex = i
                                    break
                                }
                            }
                            nextIndex
                        }

                        // Redoモード時は次の未完了セットのインデックスを計算
                        val nextSetIndexForDisplay = if (isRedoMode) {
                            findNextIncompleteSetIndex().takeIf { it >= 0 }
                        } else {
                            null
                        }

                        ProgramIntervalStep(
                            session = step.session,
                            currentSetIndex = step.currentSetIndex,
                            toneGenerator = toneGenerator,
                            flashController = flashController,
                            isFlashEnabled = isFlashEnabled,
                            isNavigationOpen = showNavigationSheet,
                            nextSetIndexOverride = nextSetIndexForDisplay,
                            onComplete = {
                                if (isRedoMode) {
                                    // Redoモード: 次の未完了セットを探す
                                    val nextIncompleteIndex = findNextIncompleteSetIndex()
                                    isRedoMode = false // Redoモード終了
                                    if (nextIncompleteIndex >= 0) {
                                        if (startCountdownSeconds > 0) {
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIncompleteIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIncompleteIndex)
                                        }
                                    } else {
                                        // 未完了セットなし → Result画面へ
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                } else {
                                    // 通常モード: 順番に進む
                                    val nextIndex = step.currentSetIndex + 1
                                    if (nextIndex < step.session.sets.size) {
                                        if (startCountdownSeconds > 0) {
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                }
                            },
                            onSkip = {
                                if (isRedoMode) {
                                    // Redoモード: 次の未完了セットを探す
                                    val nextIncompleteIndex = findNextIncompleteSetIndex()
                                    isRedoMode = false // Redoモード終了
                                    if (nextIncompleteIndex >= 0) {
                                        if (startCountdownSeconds > 0) {
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIncompleteIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIncompleteIndex)
                                        }
                                    } else {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                } else {
                                    // 通常モード: 順番に進む
                                    val nextIndex = step.currentSetIndex + 1
                                    if (nextIndex < step.session.sets.size) {
                                        if (startCountdownSeconds > 0) {
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
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
                            onCancel = { showDiscardResultDialog = true }
                        )
                    }

                    null -> {}
                }
            }
        }
    }

    // ナビゲーションシート（Executing, Interval, StartInterval, Result で表示可能）
    val step = currentStep
    val navSession = when (step) {
        is ProgramExecutionStep.Executing -> step.session
        is ProgramExecutionStep.Interval -> step.session
        is ProgramExecutionStep.StartInterval -> step.session
        is ProgramExecutionStep.Result -> step.session
        else -> null
    }
    val navCurrentSetIndex = when (step) {
        is ProgramExecutionStep.Executing -> step.currentSetIndex
        is ProgramExecutionStep.Interval -> step.currentSetIndex + 1  // 次のセット
        is ProgramExecutionStep.StartInterval -> step.currentSetIndex
        is ProgramExecutionStep.Result -> -1  // Result画面では「現在セット」なし
        else -> 0
    }
    val isFromResult = step is ProgramExecutionStep.Result
    if (showNavigationSheet && navSession != null) {
        ProgramNavigationSheet(
            session = navSession,
            currentSetIndex = if (navCurrentSetIndex >= 0) navCurrentSetIndex.coerceIn(0, navSession.sets.size - 1) else -1,
            isFromResult = isFromResult,
            onDismiss = { showNavigationSheet = false },
            onJumpToSet = { targetIndex ->
                // Jumpでも次の未完了セットを探すようにする（完了済みセットをスキップ）
                isRedoMode = true
                // 現在のセットからジャンプ先までのセットをスキップ済みにする（Result画面からの場合はスキップしない）
                if (!isFromResult) {
                    for (i in navCurrentSetIndex until targetIndex) {
                        if (!navSession.sets[i].isCompleted) {
                            navSession.sets[i] = navSession.sets[i].copy(isSkipped = true)
                        }
                    }
                }
                // ジャンプ先に遷移
                if (startCountdownSeconds > 0) {
                    currentStep = ProgramExecutionStep.StartInterval(navSession, targetIndex)
                } else {
                    currentStep = ProgramExecutionStep.Executing(navSession, targetIndex)
                }
                showNavigationSheet = false
            },
            onRedoSet = { targetIndex ->
                // Redoモードを有効化
                isRedoMode = true
                // 対象セットのみリセット
                navSession.sets[targetIndex] = navSession.sets[targetIndex].copy(
                    isCompleted = false,
                    isSkipped = false,
                    actualValue = 0
                )
                // やり直し対象セットに遷移
                if (startCountdownSeconds > 0) {
                    currentStep = ProgramExecutionStep.StartInterval(navSession, targetIndex)
                } else {
                    currentStep = ProgramExecutionStep.Executing(navSession, targetIndex)
                }
                showNavigationSheet = false
            },
            onFinish = {
                // 結果画面に遷移（完了したセットのみ保存される）
                isRedoMode = false
                showNavigationSheet = false
                currentStep = ProgramExecutionStep.Result(navSession)
            },
            onSaveAndExit = {
                // 途中状態を保存してホームへ戻る
                val savedState = SavedWorkoutState(context)
                val currentIndex = when (val s = currentStep) {
                    is ProgramExecutionStep.StartInterval -> s.currentSetIndex
                    is ProgramExecutionStep.Executing -> s.currentSetIndex
                    is ProgramExecutionStep.Interval -> s.currentSetIndex
                    else -> 0
                }

                // 既に保存された状態があるかチェック
                if (savedState.hasSavedState()) {
                    // 確認ダイアログを表示
                    pendingSaveSession = navSession
                    pendingSaveSetIndex = currentIndex
                    showNavigationSheet = false
                    showSaveOverwriteDialog = true
                } else {
                    // 直接保存
                    savedState.save(
                        programId = programId,
                        currentSetIndex = currentIndex,
                        sets = navSession.sets.toList(),
                        comment = navSession.comment
                    )
                    showNavigationSheet = false
                    onNavigateBack()
                }
            },
            onDiscard = {
                // 確認ダイアログを表示（既存のダイアログを流用）
                showNavigationSheet = false
                showExitConfirmDialog = true
            }
        )
    }
}
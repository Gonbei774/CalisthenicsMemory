package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// プログラム実行用のセットデータ
data class ProgramWorkoutSet(
    val exerciseIndex: Int,       // プログラム内の種目インデックス
    val setNumber: Int,           // セット番号（1始まり）
    val side: String?,            // "Right" or "Left" or null
    val targetValue: Int,         // 目標値
    var actualValue: Int = 0,     // 実際の値
    var isCompleted: Boolean = false,
    var isSkipped: Boolean = false,
    val intervalSeconds: Int      // このセット後のインターバル
)

// プログラム実行用のセッションデータ
data class ProgramExecutionSession(
    val program: Program,
    val exercises: List<Pair<ProgramExercise, Exercise>>, // ProgramExercise + Exercise情報
    val sets: MutableList<ProgramWorkoutSet>,             // 実行順の全セット
    var comment: String = ""
)

// プログラム実行画面のステップ
sealed class ProgramExecutionStep {
    data class Confirm(val session: ProgramExecutionSession) : ProgramExecutionStep()
    data class StartInterval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Executing(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Interval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Result(val session: ProgramExecutionSession) : ProgramExecutionStep()
}

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

        // 前回値をプリフィル
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
    val workoutPreferences = remember { WorkoutPreferences(context) }
    val isFlashEnabled = remember { workoutPreferences.isFlashNotificationEnabled() }
    val isKeepScreenOnEnabled = remember { workoutPreferences.isKeepScreenOnEnabled() }

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

    // 戻るボタンのハンドリング
    BackHandler {
        when (currentStep) {
            is ProgramExecutionStep.Confirm -> onNavigateBack()
            is ProgramExecutionStep.Result -> onNavigateBack()
            else -> {
                // 実行中は確認ダイアログを表示（簡略化のため直接戻る）
                onNavigateBack()
            }
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
                                val sets = step.session.sets
                                sets[setIndex] = sets[setIndex].copy(targetValue = newValue)
                            },
                            onUseAllProgramValues = {
                                // 全種目をプログラム設定値に戻す
                                step.session.sets.forEachIndexed { i, set ->
                                    val (pe, _) = step.session.exercises[set.exerciseIndex]
                                    step.session.sets[i] = set.copy(targetValue = pe.targetValue)
                                }
                            },
                            onUseAllChallengeValues = {
                                // 全種目を課題設定値に変更（設定がある種目のみ）
                                step.session.sets.forEachIndexed { i, set ->
                                    val (_, exercise) = step.session.exercises[set.exerciseIndex]
                                    val challengeValue = exercise.targetValue
                                    if (challengeValue != null) {
                                        step.session.sets[i] = set.copy(targetValue = challengeValue)
                                    }
                                }
                            },
                            onStart = {
                                // 開始カウントダウンが0ならスキップ
                                if (step.session.program.startInterval > 0) {
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
                        if (step.session.program.timerMode) {
                            // タイマーON: 自動カウント
                            ProgramExecutingStepTimerOn(
                                session = step.session,
                                currentSetIndex = step.currentSetIndex,
                                toneGenerator = toneGenerator,
                                flashController = flashController,
                                isFlashEnabled = isFlashEnabled,
                                onSetComplete = { actualValue ->
                                    val sets = step.session.sets
                                    sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                        actualValue = actualValue,
                                        isCompleted = true
                                    )
                                    val currentSet = sets[step.currentSetIndex]

                                    // 次のセットへ
                                    val nextIndex = step.currentSetIndex + 1
                                    if (nextIndex < sets.size) {
                                        if (currentSet.intervalSeconds > 0) {
                                            currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                        } else {
                                            // インターバルなしで次へ（右→左など）
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        // 全セット完了
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                },
                                onSkip = { actualValue ->
                                    val sets = step.session.sets
                                    sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                        actualValue = actualValue,
                                        isSkipped = true
                                    )
                                    val currentSet = sets[step.currentSetIndex]

                                    val nextIndex = step.currentSetIndex + 1
                                    val nextSet = sets.getOrNull(nextIndex)
                                    if (nextIndex < sets.size) {
                                        if (currentSet.intervalSeconds > 0) {
                                            currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                        } else if (nextSet?.side == "Left" && currentSet.side == "Right") {
                                            // 右→左の遷移: 準備カウントダウンを表示
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                },
                                onAbort = { actualValue ->
                                    val sets = step.session.sets
                                    sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                        actualValue = actualValue,
                                        isSkipped = true
                                    )
                                    currentStep = ProgramExecutionStep.Result(step.session)
                                }
                            )
                        } else {
                            // タイマーOFF: 手動完了
                            ProgramExecutingStepTimerOff(
                                session = step.session,
                                currentSetIndex = step.currentSetIndex,
                                onSetComplete = { actualValue ->
                                    val sets = step.session.sets
                                    sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                        actualValue = actualValue,
                                        isCompleted = true
                                    )
                                    val currentSet = sets[step.currentSetIndex]
                                    val nextIndex = step.currentSetIndex + 1
                                    val nextSet = sets.getOrNull(nextIndex)

                                    // 次のセットへ
                                    if (nextIndex < sets.size) {
                                        if (currentSet.intervalSeconds > 0) {
                                            currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                        } else if (nextSet?.side == "Left" && currentSet.side == "Right") {
                                            // 右→左の遷移: 準備カウントダウンを表示
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                        } else {
                                            // その他のインターバルなしケース
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        // 全セット完了
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                },
                                onSkip = { actualValue ->
                                    val sets = step.session.sets
                                    sets[step.currentSetIndex] = sets[step.currentSetIndex].copy(
                                        actualValue = actualValue,
                                        isSkipped = true
                                    )
                                    val currentSet = sets[step.currentSetIndex]
                                    val nextIndex = step.currentSetIndex + 1
                                    val nextSet = sets.getOrNull(nextIndex)

                                    if (nextIndex < sets.size) {
                                        if (currentSet.intervalSeconds > 0) {
                                            currentStep = ProgramExecutionStep.Interval(step.session, step.currentSetIndex)
                                        } else if (nextSet?.side == "Left" && currentSet.side == "Right") {
                                            // 右→左の遷移: 準備カウントダウンを表示
                                            currentStep = ProgramExecutionStep.StartInterval(step.session, nextIndex)
                                        } else {
                                            currentStep = ProgramExecutionStep.Executing(step.session, nextIndex)
                                        }
                                    } else {
                                        currentStep = ProgramExecutionStep.Result(step.session)
                                    }
                                },
                                onAbort = {
                                    // タイマーOFF版: 中断したセットは記録しない（何も設定せずにResult画面へ）
                                    currentStep = ProgramExecutionStep.Result(step.session)
                                }
                            )
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
                                    // 開始カウントダウンが0ならスキップ
                                    if (step.session.program.startInterval > 0) {
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
                                    // 開始カウントダウンが0ならスキップ
                                    if (step.session.program.startInterval > 0) {
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
private fun ProgramConfirmStep(
    session: ProgramExecutionSession,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUseAllProgramValues: () -> Unit,
    onUseAllChallengeValues: () -> Unit,
    onStart: () -> Unit
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    // 課題設定がある種目が1つでもあるか
    val hasChallengeExercise = session.exercises.any { (_, exercise) -> exercise.targetValue != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 右上に一括適用ボタン
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.program_use_program),
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
            if (hasChallengeExercise) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onUseAllChallengeValues()
                        refreshKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green600),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.program_use_challenge),
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

@Composable
private fun ProgramConfirmExerciseCard(
    exercise: Exercise,
    programExercise: ProgramExercise,
    sets: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    onUpdateValue: (Int, Int) -> Unit
) {
    val unit = stringResource(if (exercise.type == "Isometric") R.string.unit_seconds else R.string.unit_reps)

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.set_format, set.setNumber, programExercise.sets),
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

@Composable
private fun ProgramStartIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    onComplete: () -> Unit
) {
    val currentSet = session.sets[currentSetIndex]
    val (_, exercise) = session.exercises[currentSet.exerciseIndex]

    var remainingTime by remember { mutableIntStateOf(session.program.startInterval) }
    val progress = remainingTime.toFloat() / session.program.startInterval

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
    }
}

// タイマーOFF版: 手動で完了ボタンを押す
@Composable
private fun ProgramExecutingStepTimerOff(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    onSetComplete: (Int) -> Unit,
    onSkip: (Int) -> Unit,
    onAbort: () -> Unit
) {
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    var currentValue by remember(currentSetIndex) { mutableIntStateOf(currentSet.targetValue) }

    val unit = if (exercise.type == "Isometric") {
        stringResource(R.string.unit_seconds)
    } else {
        stringResource(R.string.unit_reps)
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

        // 実行中表示
        Text(
            text = stringResource(R.string.executing_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Green600
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 値調整
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentValue > 1) currentValue-- },
                modifier = Modifier.size(64.dp)
            ) {
                Text(
                    text = "-",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentValue",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = unit,
                    fontSize = 20.sp,
                    color = Slate300
                )
            }

            IconButton(
                onClick = { currentValue++ },
                modifier = Modifier.size(64.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン
        Button(
            onClick = { onSetComplete(currentValue) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.complete_button),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // スキップ・中止ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // スキップボタン（実行していないので0）
            OutlinedButton(
                onClick = { onSkip(0) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.skip_button))
            }

            // 中止ボタン（中断したセットは記録しない）
            Button(
                onClick = { onAbort() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.stop_button))
            }
        }
    }
}

// タイマーON版: 自動カウント（Dynamic=レップカウント、Isometric=カウントダウン）
@Composable
private fun ProgramExecutingStepTimerOn(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    onSetComplete: (Int) -> Unit,
    onSkip: (Int) -> Unit,
    onAbort: (Int) -> Unit
) {
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isRunning by remember(currentSetIndex) { mutableStateOf(true) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = exercise.repDuration ?: 5

    // Dynamic: レップ内の経過時間を計算（カウントアップ）
    val repTimeElapsed = if (exercise.type == "Dynamic") {
        elapsedTime % repDuration
    } else {
        0
    }

    val progress = if (exercise.type == "Isometric") {
        (currentSet.targetValue - elapsedTime).toFloat() / currentSet.targetValue
    } else {
        // Dynamic: レップ内の進捗
        (elapsedTime % repDuration).toFloat() / repDuration
    }

    LaunchedEffect(currentSetIndex) {
        while (true) {
            if (isRunning) {
                delay(1000L)
                elapsedTime++

                // Dynamic: レップカウント
                if (exercise.type == "Dynamic") {
                    if (elapsedTime % repDuration == 0) {
                        currentCount++

                        // Dynamic: 目標達成時に自動遷移
                        if (currentCount >= currentSet.targetValue) {
                            // 音とフラッシュを同時に開始
                            if (isFlashEnabled) {
                                launch { flashController.flashSetComplete() }
                            }
                            playTripleBeepTwice(toneGenerator)
                            onSetComplete(currentCount)
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
                if (exercise.type == "Isometric" && elapsedTime >= currentSet.targetValue) {
                    // 音とフラッシュを同時に開始
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    playTripleBeepTwice(toneGenerator)
                    onSetComplete(elapsedTime)
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

            Spacer(modifier = Modifier.height(48.dp))

            ProgramCircularTimer(
                progress = progress.coerceIn(0f, 1f),
                remainingTime = if (exercise.type == "Isometric") {
                    (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
                } else {
                    // Dynamic: レップ内の経過時間を表示（カウントアップ）
                    repTimeElapsed
                },
                color = Orange600
            )

            if (exercise.type == "Dynamic") {
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

        // 一時停止/再開ボタン
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
            // スキップボタン
            OutlinedButton(
                onClick = {
                    // 途中までの記録を保存してからスキップ
                    val actualValue = if (exercise.type == "Dynamic") currentCount else elapsedTime
                    onSkip(actualValue)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.skip_button))
            }

            // 中止ボタン
            Button(
                onClick = {
                    // 現在のセットは途中までの記録を保存
                    val actualValue = if (exercise.type == "Dynamic") currentCount else elapsedTime
                    onAbort(actualValue)
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

                val isNextDifferentExercise = next.exerciseIndex != currentSet.exerciseIndex
                if (isNextDifferentExercise) {
                    Text(
                        text = stringResource(R.string.next_exercise_label, nextExercise.name),
                        fontSize = 18.sp,
                        color = Slate300,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

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
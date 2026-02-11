package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.BackHandler
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.service.WorkoutTimerService
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Exercise info resolved from IntervalProgramExercise + Exercise
 */
private data class IntervalExerciseInfo(
    val exerciseId: Long,
    val name: String,
    val description: String?
)

/**
 * Execution phase state machine
 */
private sealed class IntervalPhase {
    object Loading : IntervalPhase()

    data class Confirm(
        val program: IntervalProgram,
        val exercises: List<IntervalExerciseInfo>
    ) : IntervalPhase()

    object Prepare : IntervalPhase()

    data class Work(
        val round: Int,           // 1-based
        val exerciseIndex: Int    // 0-based
    ) : IntervalPhase()

    data class Rest(
        val round: Int,
        val exerciseIndex: Int    // the exercise that just finished
    ) : IntervalPhase()

    data class RoundRest(
        val completedRound: Int   // 1-based, the round that just finished
    ) : IntervalPhase()

    data class Complete(
        val completedRounds: Int,
        val completedExercisesInLastRound: Int,
        val isFullCompletion: Boolean
    ) : IntervalPhase()
}

@Composable
fun IntervalExecutionScreen(
    viewModel: TrainingViewModel,
    programId: Long,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    // Preferences
    val workoutPrefs = remember { WorkoutPreferences(context) }
    val isKeepScreenOnEnabled = remember { workoutPrefs.isKeepScreenOnEnabled() }
    val isFlashEnabled = remember { workoutPrefs.isFlashNotificationEnabled() }

    // Audio & Flash
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    val flashController = remember { FlashController(context) }

    // State
    var phase by remember { mutableStateOf<IntervalPhase>(IntervalPhase.Loading) }
    var program by remember { mutableStateOf<IntervalProgram?>(null) }
    var exercises by remember { mutableStateOf<List<IntervalExerciseInfo>>(emptyList()) }
    var isPaused by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Load program data
    LaunchedEffect(programId) {
        val loadedProgram = viewModel.getIntervalProgramById(programId)
        if (loadedProgram == null) {
            onNavigateBack()
            return@LaunchedEffect
        }
        program = loadedProgram

        val programExercises = viewModel.getIntervalProgramExercisesSync(programId)
        val allExercises = viewModel.exercises.value
        val exerciseMap = allExercises.associateBy { it.id }

        val resolved = programExercises
            .sortedBy { it.sortOrder }
            .mapNotNull { pe ->
                exerciseMap[pe.exerciseId]?.let { ex ->
                    IntervalExerciseInfo(
                        exerciseId = ex.id,
                        name = ex.name,
                        description = ex.description
                    )
                }
            }
        exercises = resolved
        phase = IntervalPhase.Confirm(loadedProgram, resolved)
    }

    // Keep screen on
    LaunchedEffect(phase, isKeepScreenOnEnabled) {
        val window = (view.context as? android.app.Activity)?.window
        if (isKeepScreenOnEnabled) {
            when (phase) {
                is IntervalPhase.Prepare,
                is IntervalPhase.Work,
                is IntervalPhase.Rest,
                is IntervalPhase.RoundRest -> {
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

    // Foreground service
    LaunchedEffect(phase) {
        when (phase) {
            is IntervalPhase.Prepare,
            is IntervalPhase.Work,
            is IntervalPhase.Rest,
            is IntervalPhase.RoundRest -> WorkoutTimerService.startService(context)
            else -> WorkoutTimerService.stopService(context)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? android.app.Activity)?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            toneGenerator.release()
            flashController.turnOff()
            WorkoutTimerService.stopService(context)
        }
    }

    // Back gesture: show exit dialog during active phases
    val isActivePhase = phase is IntervalPhase.Prepare ||
            phase is IntervalPhase.Work ||
            phase is IntervalPhase.Rest ||
            phase is IntervalPhase.RoundRest
    BackHandler(enabled = isActivePhase) {
        showExitDialog = true
    }

    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = appColors.cardBackground,
            title = {
                Text(
                    stringResource(R.string.interval_exit_confirm_title),
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.interval_exit_confirm_message),
                    color = appColors.textTertiary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    val p = program ?: return@TextButton
                    val currentPhase = phase

                    val (completedRounds, completedExInLast) = when (currentPhase) {
                        is IntervalPhase.Work -> Pair(
                            currentPhase.round - 1,
                            currentPhase.exerciseIndex
                        )
                        is IntervalPhase.Rest -> Pair(
                            currentPhase.round - 1,
                            currentPhase.exerciseIndex + 1
                        )
                        is IntervalPhase.RoundRest -> Pair(
                            currentPhase.completedRound,
                            exercises.size
                        )
                        else -> Pair(0, 0)
                    }

                    phase = IntervalPhase.Complete(
                        completedRounds = completedRounds,
                        completedExercisesInLastRound = completedExInLast,
                        isFullCompletion = false
                    )
                }) {
                    Text(stringResource(R.string.interval_stop), color = Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appColors.textSecondary)
                }
            }
        )
    }

    // Advance to next phase
    fun advanceFromWork(round: Int, exerciseIndex: Int) {
        val p = program ?: return
        val isLastExercise = exerciseIndex == exercises.size - 1
        val isLastRound = round == p.rounds

        when {
            isLastExercise && isLastRound -> {
                phase = IntervalPhase.Complete(
                    completedRounds = p.rounds,
                    completedExercisesInLastRound = exercises.size,
                    isFullCompletion = true
                )
            }
            isLastExercise -> {
                if (p.roundRestSeconds > 0) {
                    phase = IntervalPhase.RoundRest(completedRound = round)
                } else {
                    phase = IntervalPhase.Work(round = round + 1, exerciseIndex = 0)
                }
            }
            else -> {
                if (p.restSeconds > 0) {
                    phase = IntervalPhase.Rest(round = round, exerciseIndex = exerciseIndex)
                } else {
                    phase = IntervalPhase.Work(round = round, exerciseIndex = exerciseIndex + 1)
                }
            }
        }
    }

    fun advanceFromRest(round: Int, exerciseIndex: Int) {
        phase = IntervalPhase.Work(round = round, exerciseIndex = exerciseIndex + 1)
    }

    fun advanceFromRoundRest(completedRound: Int) {
        phase = IntervalPhase.Work(round = completedRound + 1, exerciseIndex = 0)
    }

    // Main content
    when (val currentPhase = phase) {
        is IntervalPhase.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Orange600)
            }
        }

        is IntervalPhase.Confirm -> {
            IntervalConfirmContent(
                program = currentPhase.program,
                exercises = currentPhase.exercises,
                appColors = appColors,
                onStart = {
                    isPaused = false
                    phase = IntervalPhase.Prepare
                },
                onBack = onNavigateBack
            )
        }

        is IntervalPhase.Prepare -> {
            IntervalPrepareContent(
                exercises = exercises,
                toneGenerator = toneGenerator,
                flashController = flashController,
                isFlashEnabled = isFlashEnabled,
                appColors = appColors,
                onFinish = {
                    phase = IntervalPhase.Work(round = 1, exerciseIndex = 0)
                }
            )
        }

        is IntervalPhase.Work -> {
            IntervalTimerContent(
                program = program!!,
                exercises = exercises,
                round = currentPhase.round,
                exerciseIndex = currentPhase.exerciseIndex,
                totalSeconds = program!!.workSeconds,
                phaseColor = Orange600,
                phaseLabel = stringResource(R.string.interval_work_label),
                exerciseName = exercises[currentPhase.exerciseIndex].name,
                nextPreview = null,
                isPaused = isPaused,
                onPauseToggle = { isPaused = !isPaused },
                onStop = { showExitDialog = true },
                onSkip = null,
                onTimerFinish = {
                    advanceFromWork(currentPhase.round, currentPhase.exerciseIndex)
                },
                toneGenerator = toneGenerator,
                flashController = flashController,
                isFlashEnabled = isFlashEnabled,
                appColors = appColors,
                isWorkPhase = true
            )
        }

        is IntervalPhase.Rest -> {
            val nextExercise = exercises.getOrNull(currentPhase.exerciseIndex + 1)
            IntervalTimerContent(
                program = program!!,
                exercises = exercises,
                round = currentPhase.round,
                exerciseIndex = currentPhase.exerciseIndex,
                totalSeconds = program!!.restSeconds,
                phaseColor = Cyan600,
                phaseLabel = stringResource(R.string.interval_rest_label),
                exerciseName = null,
                nextPreview = nextExercise?.let {
                    NextPreviewInfo(
                        label = stringResource(R.string.interval_next),
                        exerciseName = it.name,
                        description = it.description
                    )
                },
                isPaused = isPaused,
                onPauseToggle = { isPaused = !isPaused },
                onStop = null,
                onSkip = { advanceFromRest(currentPhase.round, currentPhase.exerciseIndex) },
                onTimerFinish = {
                    advanceFromRest(currentPhase.round, currentPhase.exerciseIndex)
                },
                toneGenerator = toneGenerator,
                flashController = flashController,
                isFlashEnabled = isFlashEnabled,
                appColors = appColors
            )
        }

        is IntervalPhase.RoundRest -> {
            val firstExercise = exercises.firstOrNull()
            IntervalTimerContent(
                program = program!!,
                exercises = exercises,
                round = currentPhase.completedRound,
                exerciseIndex = exercises.size - 1,
                totalSeconds = program!!.roundRestSeconds,
                phaseColor = Purple600,
                phaseLabel = stringResource(R.string.interval_round_rest_label),
                exerciseName = null,
                nextPreview = firstExercise?.let {
                    NextPreviewInfo(
                        label = stringResource(R.string.interval_next_round),
                        exerciseName = it.name,
                        description = it.description
                    )
                },
                roundCompleteMessage = stringResource(
                    R.string.interval_round_complete_format,
                    currentPhase.completedRound
                ),
                isPaused = isPaused,
                onPauseToggle = { isPaused = !isPaused },
                onStop = null,
                onSkip = { advanceFromRoundRest(currentPhase.completedRound) },
                onTimerFinish = {
                    advanceFromRoundRest(currentPhase.completedRound)
                },
                toneGenerator = toneGenerator,
                flashController = flashController,
                isFlashEnabled = isFlashEnabled,
                appColors = appColors
            )
        }

        is IntervalPhase.Complete -> {
            IntervalCompleteContent(
                program = program!!,
                exercises = exercises,
                completedRounds = currentPhase.completedRounds,
                completedExercisesInLastRound = currentPhase.completedExercisesInLastRound,
                isFullCompletion = currentPhase.isFullCompletion,
                appColors = appColors,
                onSave = {
                    scope.launch {
                        val p = program!!
                        val exercisesJson = JSONArray().apply {
                            exercises.forEach { put(it.name) }
                        }.toString()

                        val now = LocalDate.now()
                        val time = LocalTime.now()

                        val record = IntervalRecord(
                            programName = p.name,
                            date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            time = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                            workSeconds = p.workSeconds,
                            restSeconds = p.restSeconds,
                            rounds = p.rounds,
                            roundRestSeconds = p.roundRestSeconds,
                            completedRounds = currentPhase.completedRounds,
                            completedExercisesInLastRound = currentPhase.completedExercisesInLastRound,
                            exercisesJson = exercisesJson,
                            comment = null
                        )
                        viewModel.saveIntervalRecord(record)
                        onComplete()
                    }
                },
                onDiscard = onComplete
            )
        }
    }
}

// ========================================
// Confirm Screen
// ========================================

@Composable
private fun IntervalConfirmContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    appColors: AppColors,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val exerciseCount = exercises.size
    val perRoundSeconds = exerciseCount * program.workSeconds +
            (exerciseCount - 1).coerceAtLeast(0) * program.restSeconds
    val totalSeconds = perRoundSeconds * program.rounds +
            program.roundRestSeconds * (program.rounds - 1).coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val totalRemainSeconds = totalSeconds % 60

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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.interval_confirm_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Program name
            item {
                Text(
                    text = program.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }

            // Settings summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_work_seconds),
                            value = "${program.workSeconds}${stringResource(R.string.interval_seconds_suffix)}",
                            appColors = appColors
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_rest_seconds),
                            value = "${program.restSeconds}${stringResource(R.string.interval_seconds_suffix)}",
                            appColors = appColors
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_rounds),
                            value = "${program.rounds}${stringResource(R.string.interval_rounds_suffix)}",
                            appColors = appColors
                        )
                        if (program.roundRestSeconds > 0) {
                            ConfirmSettingRow(
                                label = stringResource(R.string.interval_round_rest_seconds),
                                value = "${program.roundRestSeconds}${stringResource(R.string.interval_seconds_suffix)}",
                                appColors = appColors
                            )
                        }
                        HorizontalDivider(
                            color = appColors.textTertiary.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        ConfirmSettingRow(
                            label = stringResource(R.string.interval_total_time),
                            value = stringResource(
                                R.string.interval_total_time_format,
                                totalMinutes,
                                totalRemainSeconds
                            ),
                            appColors = appColors,
                            isBold = true
                        )
                    }
                }
            }

            // Exercise list
            item {
                Text(
                    text = stringResource(R.string.interval_exercises),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(exercises) { index, exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Orange600,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                fontSize = 15.sp,
                                color = appColors.textPrimary
                            )
                            if (!exercise.description.isNullOrBlank()) {
                                Text(
                                    text = exercise.description,
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Start button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_start_workout),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmSettingRow(
    label: String,
    value: String,
    appColors: AppColors,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = appColors.textSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = appColors.textPrimary
        )
    }
}

// ========================================
// Prepare Countdown Screen
// ========================================

@Composable
private fun IntervalPrepareContent(
    exercises: List<IntervalExerciseInfo>,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    appColors: AppColors,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalSeconds = 5
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    val progress = remainingSeconds.toFloat() / totalSeconds
    val firstExercise = exercises.firstOrNull()

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds in 1..3) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                if (isFlashEnabled) {
                    scope.launch { flashController.flashShort() }
                }
            }
        }
        // Prepare complete: single beep like rest completion
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        if (isFlashEnabled) {
            scope.launch { flashController.flashComplete() }
        }
        delay(300L)
        onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: "Get Ready!"
        Text(
            text = stringResource(R.string.interval_get_ready),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Center: timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(240.dp)) {
                drawArc(
                    color = Orange600.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Orange600,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Text(
                text = "$remainingSeconds",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Orange600
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom: first exercise preview
        if (firstExercise != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${stringResource(R.string.interval_next)} ▶",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange600
                    )
                    Text(
                        text = firstExercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!firstExercise.description.isNullOrBlank()) {
                        Text(
                            text = firstExercise.description,
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========================================
// Timer Screen (Work / Rest / RoundRest)
// ========================================

private data class NextPreviewInfo(
    val label: String,
    val exerciseName: String,
    val description: String?
)

@Composable
private fun IntervalTimerContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    round: Int,
    exerciseIndex: Int,
    totalSeconds: Int,
    phaseColor: Color,
    phaseLabel: String,
    exerciseName: String?,
    nextPreview: NextPreviewInfo?,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onStop: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    onTimerFinish: () -> Unit,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    appColors: AppColors,
    isWorkPhase: Boolean = false,
    roundCompleteMessage: String? = null
) {
    var remainingSeconds by remember(round, exerciseIndex, phaseLabel) {
        mutableIntStateOf(totalSeconds)
    }
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f

    // Timer countdown
    LaunchedEffect(round, exerciseIndex, phaseLabel, isPaused) {
        while (remainingSeconds > 0 && !isPaused) {
            delay(1000L)
            if (!isPaused) {
                remainingSeconds--
                // Beep at 3, 2, 1
                if (remainingSeconds in 1..3) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }
            }
        }
        if (remainingSeconds <= 0) {
            if (isWorkPhase) {
                // Work complete: triple beep pattern (ピピピ×3) + long flash
                if (isFlashEnabled) {
                    launch { flashController.flashSetComplete() }
                }
                playTripleBeepTwice(toneGenerator)
            } else {
                // Rest/RoundRest complete: single beep + short flash
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                if (isFlashEnabled) {
                    launch { flashController.flashComplete() }
                }
                delay(300L)
            }
            onTimerFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: exercise name or phase label
        if (exerciseName != null) {
            Text(
                text = exerciseName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = phaseLabel,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Sub info
        Text(
            text = if (exerciseName != null) {
                stringResource(
                    R.string.interval_exercise_format,
                    exerciseIndex + 1,
                    exercises.size
                ) + " · " + stringResource(R.string.interval_round_format, round, program.rounds)
            } else {
                if (roundCompleteMessage != null) roundCompleteMessage
                else stringResource(R.string.interval_round_format, round, program.rounds)
            },
            fontSize = 16.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Circular timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(240.dp)) {
                drawArc(
                    color = phaseColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Text(
                text = "$remainingSeconds",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = phaseColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next exercise info
        val nextExIndex = exerciseIndex + 1
        val nextEx = if (exerciseName != null) {
            if (nextExIndex < exercises.size) exercises[nextExIndex]
            else if (round < program.rounds) exercises.firstOrNull()
            else null
        } else null

        if (nextPreview != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "${nextPreview.label} ▶",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                    Text(
                        text = nextPreview.exerciseName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!nextPreview.description.isNullOrBlank()) {
                        Text(
                            text = nextPreview.description,
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else if (nextEx != null) {
            Text(
                text = "${stringResource(R.string.interval_next)}: ${nextEx.name}",
                fontSize = 14.sp,
                color = appColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons - 2 rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pause
            OutlinedButton(
                onClick = onPauseToggle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = appColors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isPaused) stringResource(R.string.interval_resume)
                    else stringResource(R.string.interval_pause),
                    fontSize = 15.sp
                )
            }

            // Stop
            if (onStop != null) {
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red600),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_stop),
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            // Row 2: Skip
            if (onSkip != null) {
                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_skip),
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ========================================
// Complete Screen
// ========================================

@Composable
private fun IntervalCompleteContent(
    program: IntervalProgram,
    exercises: List<IntervalExerciseInfo>,
    completedRounds: Int,
    completedExercisesInLastRound: Int,
    isFullCompletion: Boolean,
    appColors: AppColors,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val statusColor = Orange600

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = statusColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFullCompletion) stringResource(R.string.interval_complete_title)
                        else stringResource(R.string.interval_ended_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = if (isFullCompletion) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Program name
            item {
                Text(
                    text = program.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Completion stats
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isFullCompletion) {
                            Text(
                                text = stringResource(
                                    R.string.interval_full_complete_format,
                                    completedRounds,
                                    program.rounds
                                ),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.interval_partial_format,
                                    completedRounds,
                                    program.rounds,
                                    completedExercisesInLastRound,
                                    exercises.size
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                text = stringResource(R.string.interval_partial_note),
                                fontSize = 14.sp,
                                color = appColors.textSecondary
                            )
                        }

                        HorizontalDivider(
                            color = appColors.textTertiary.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = stringResource(
                                R.string.interval_settings_format,
                                program.workSeconds,
                                exercises.size,
                                program.rounds
                            ),
                            fontSize = 13.sp,
                            color = appColors.textSecondary
                        )

                    }
                }
            }

            // Exercise list
            item {
                Text(
                    text = stringResource(R.string.interval_exercises_done),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            // Show exercises grouped by round
            val totalRoundsToShow = if (isFullCompletion) completedRounds
            else completedRounds + if (completedExercisesInLastRound > 0) 1 else 0

            for (round in 1..totalRoundsToShow) {
                val isLastPartialRound = !isFullCompletion && round == totalRoundsToShow && round > completedRounds

                item {
                    Text(
                        text = stringResource(R.string.interval_round_header_format, round),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange600,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (round == 1) 0.dp else 8.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(exercises) { index, exercise ->
                    val isExecuted = if (isLastPartialRound) index < completedExercisesInLastRound else true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exercise.name,
                            fontSize = 14.sp,
                            color = if (!isExecuted) appColors.textTertiary.copy(alpha = 0.5f)
                            else appColors.textPrimary
                        )
                        if (!isExecuted) {
                            Text(
                                text = " ${stringResource(R.string.interval_not_executed)}",
                                fontSize = 12.sp,
                                color = appColors.textTertiary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_save_and_finish),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (!isFullCompletion) {
                item {
                    OutlinedButton(
                        onClick = onDiscard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = appColors.textSecondary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.interval_discard_result),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

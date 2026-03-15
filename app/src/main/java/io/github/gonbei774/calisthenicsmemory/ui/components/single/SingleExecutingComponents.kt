package io.github.gonbei774.calisthenicsmemory.ui.components.single

import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.NextSetText
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSession
import io.github.gonbei774.calisthenicsmemory.ui.screens.playTripleBeepTwice
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dynamic + タイマーOFF + レップカウントON: 手動完了ボタンで遷移
 */
@Composable
fun SingleExecutingStepDynamicManual(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return
    val repDuration = session.repDuration ?: 5

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var adjustedReps by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    val recordValue = (currentCount + adjustedReps).coerceAtLeast(0)
    val repTimeElapsed = elapsedTime % repDuration
    val progress = repTimeElapsed.toFloat() / repDuration
    val isTimerComplete = currentCount >= currentSet.targetValue

    val activeColor = if (isTimerComplete) Green600 else Orange600
    val statusColor = if (effectivelyPaused) Slate400 else activeColor

    LaunchedEffect(currentSetIndex, effectivelyPaused) {
        while (true) {
            if (!effectivelyPaused && currentCount < currentSet.targetValue) {
                delay(1000L)
                elapsedTime++

                if (elapsedTime % repDuration == 0) {
                    currentCount++

                    if (currentCount >= currentSet.targetValue) {
                        if (isFlashEnabled) {
                            launch { flashController.flashSetComplete() }
                        }
                        playTripleBeepTwice(toneGenerator)
                    } else {
                        if (isCountSoundEnabled) {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                            if (isFlashEnabled) {
                                launch { flashController.flashShort() }
                            }
                        }
                    }
                }
            } else {
                delay(100L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 種目名
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
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
                stringResource(R.string.set_format_with_side, currentSet.setNumber, session.totalSets, sideText)
            } else {
                stringResource(R.string.set_format, currentSet.setNumber, session.totalSets)
            },
            fontSize = 18.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(24.dp))

        // 円形タイマー（タップで一時停止/再開）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isPaused = !isPaused }
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                drawArc(
                    color = appColors.timerTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = activeColor.copy(alpha = if (effectivelyPaused) 0.3f else 1f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$repTimeElapsed",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.alpha(if (effectivelyPaused) 0.2f else 1f)
            )
            if (effectivelyPaused) {
                val iconColor = appColors.textPrimary
                Canvas(modifier = Modifier.size(56.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.15f)
                        lineTo(size.width * 0.85f, size.height * 0.5f)
                        lineTo(size.width * 0.25f, size.height * 0.85f)
                        close()
                    }
                    drawPath(path, color = iconColor.copy(alpha = 0.9f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // レップカウント表示（+/- ボタン付き）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { adjustedReps-- },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }

            Box(
                modifier = Modifier.width(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.reps_count, recordValue),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = { adjustedReps++ },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue) +
                (currentSet.previousValue?.let { " " + stringResource(R.string.previous_reps_format, it) } ?: ""),
            fontSize = 14.sp,
            color = appColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン
        Button(
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_reps, recordValue),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Dynamic + タイマーON: 目標達成時に自動遷移（早期完了ボタンあり）
 */
@Composable
fun SingleExecutingStepDynamicAuto(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return
    val repDuration = session.repDuration ?: 5

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var adjustedReps by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    val recordValue = (currentCount + adjustedReps).coerceAtLeast(0)
    val repTimeElapsed = elapsedTime % repDuration
    val progress = repTimeElapsed.toFloat() / repDuration

    val activeColor = Orange600
    val statusColor = if (effectivelyPaused) Slate400 else activeColor

    LaunchedEffect(currentSetIndex, effectivelyPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                if (elapsedTime % repDuration == 0) {
                    currentCount++

                    if (currentCount >= currentSet.targetValue) {
                        if (isFlashEnabled) {
                            launch { flashController.flashSetComplete() }
                        }
                        playTripleBeepTwice(toneGenerator)
                        currentSet.actualValue = currentCount + adjustedReps
                        currentSet.isCompleted = true
                        onSetComplete(session)
                        return@LaunchedEffect
                    } else {
                        if (isCountSoundEnabled) {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                            if (isFlashEnabled) {
                                launch { flashController.flashShort() }
                            }
                        }
                    }
                }
            } else {
                delay(100L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
            fontSize = 18.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(24.dp))

        // 円形タイマー（タップで一時停止/再開）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isPaused = !isPaused }
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                drawArc(
                    color = appColors.timerTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = activeColor.copy(alpha = if (effectivelyPaused) 0.3f else 1f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$repTimeElapsed",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.alpha(if (effectivelyPaused) 0.2f else 1f)
            )
            if (effectivelyPaused) {
                val iconColor = appColors.textPrimary
                Canvas(modifier = Modifier.size(56.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.15f)
                        lineTo(size.width * 0.85f, size.height * 0.5f)
                        lineTo(size.width * 0.25f, size.height * 0.85f)
                        close()
                    }
                    drawPath(path, color = iconColor.copy(alpha = 0.9f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { adjustedReps-- },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }

            Box(
                modifier = Modifier.width(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.reps_count, recordValue),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = { adjustedReps++ },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue) +
                (currentSet.previousValue?.let { " " + stringResource(R.string.previous_reps_format, it) } ?: ""),
            fontSize = 14.sp,
            color = appColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン（早期完了用）
        Button(
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_reps, recordValue),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Dynamic + レップカウントOFF: シンプルなカウンターUI
 */
@Composable
fun SingleExecutingStepDynamicSimple(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var reps by remember(currentSetIndex) { mutableIntStateOf(currentSet.targetValue) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
            fontSize = 18.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(32.dp))

        // 大きなレップ数表示
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (reps > 0) reps-- },
                modifier = Modifier.size(64.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }

            Text(
                text = "$reps",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = Green400
            )

            IconButton(
                onClick = { reps++ },
                modifier = Modifier.size(64.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue) +
                (currentSet.previousValue?.let { " " + stringResource(R.string.previous_reps_format, it) } ?: ""),
            fontSize = 14.sp,
            color = appColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (isFlashEnabled) {
                    scope.launch { flashController.flashSetComplete() }
                }
                currentSet.actualValue = reps
                currentSet.isCompleted = true
                onSetComplete(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_reps, reps),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Isometric + タイマーOFF: カウントダウン + 手動完了
 */
@Composable
fun SingleExecutingStepIsometricManual(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var adjustedSeconds by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    val recordValue = (elapsedTime + adjustedSeconds).coerceAtLeast(0)
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
    val progress = if (currentSet.targetValue > 0) remainingTime.toFloat() / currentSet.targetValue else 0f
    val isTimerComplete = elapsedTime >= currentSet.targetValue

    val activeColor = if (isTimerComplete) Green600 else Orange600
    val statusColor = if (effectivelyPaused) Slate400 else activeColor

    LaunchedEffect(currentSetIndex, effectivelyPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }

                if (elapsedTime >= currentSet.targetValue) {
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    playTripleBeepTwice(toneGenerator)
                }
            } else {
                delay(100L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
            fontSize = 18.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(24.dp))

        // タイマーと+/-ボタン（プログラムモードと同じレイアウト）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { adjustedSeconds-- },
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-20).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isPaused = !isPaused }
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
                        color = activeColor.copy(alpha = if (effectivelyPaused) 0.3f else 1f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$remainingTime",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    modifier = Modifier.alpha(if (effectivelyPaused) 0.2f else 1f)
                )
                if (effectivelyPaused) {
                    val iconColor = appColors.textPrimary
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.25f, size.height * 0.15f)
                            lineTo(size.width * 0.85f, size.height * 0.5f)
                            lineTo(size.width * 0.25f, size.height * 0.85f)
                            close()
                        }
                        drawPath(path, color = iconColor.copy(alpha = 0.9f))
                    }
                }
            }

            IconButton(
                onClick = { adjustedSeconds++ },
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-20).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.elapsed_target_format, recordValue, currentSet.targetValue) +
                (currentSet.previousValue?.let { " " + stringResource(R.string.previous_time_format, it) } ?: ""),
            fontSize = 14.sp,
            color = appColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_time, recordValue),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}

/**
 * Isometric + タイマーON: 目標達成時に自動遷移（早期完了ボタンあり）
 */
@Composable
fun SingleExecutingStepIsometricAuto(
    session: WorkoutSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (WorkoutSession) -> Unit,
    onSkip: (WorkoutSession) -> Unit,
    onAbort: (WorkoutSession) -> Unit,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets.getOrNull(currentSetIndex) ?: return

    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var adjustedSeconds by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    val recordValue = (elapsedTime + adjustedSeconds).coerceAtLeast(0)
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)
    val progress = if (currentSet.targetValue > 0) remainingTime.toFloat() / currentSet.targetValue else 0f

    val activeColor = Orange600
    val statusColor = if (effectivelyPaused) Slate400 else activeColor

    LaunchedEffect(currentSetIndex, effectivelyPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }

                if (elapsedTime >= currentSet.targetValue) {
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    playTripleBeepTwice(toneGenerator)
                    currentSet.actualValue = elapsedTime + adjustedSeconds
                    currentSet.isCompleted = true
                    onSetComplete(session)
                    return@LaunchedEffect
                }
            } else {
                delay(100L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = session.exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

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
            fontSize = 18.sp,
            color = appColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(24.dp))

        // タイマーと+/-ボタン（プログラムモードと同じレイアウト）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { adjustedSeconds-- },
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-20).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isPaused = !isPaused }
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
                        color = activeColor.copy(alpha = if (effectivelyPaused) 0.3f else 1f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$remainingTime",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    modifier = Modifier.alpha(if (effectivelyPaused) 0.2f else 1f)
                )
                if (effectivelyPaused) {
                    val iconColor = appColors.textPrimary
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.25f, size.height * 0.15f)
                            lineTo(size.width * 0.85f, size.height * 0.5f)
                            lineTo(size.width * 0.25f, size.height * 0.85f)
                            close()
                        }
                        drawPath(path, color = iconColor.copy(alpha = 0.9f))
                    }
                }
            }

            IconButton(
                onClick = { adjustedSeconds++ },
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-20).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.elapsed_target_format, recordValue, currentSet.targetValue) +
                (currentSet.previousValue?.let { " " + stringResource(R.string.previous_time_format, it) } ?: ""),
            fontSize = 14.sp,
            color = appColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン（早期完了用）
        Button(
            onClick = {
                currentSet.actualValue = recordValue
                currentSet.isCompleted = true
                onSetComplete(session)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_time, recordValue),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextSetText(session = session, currentSetIndex = currentSetIndex)
    }
}
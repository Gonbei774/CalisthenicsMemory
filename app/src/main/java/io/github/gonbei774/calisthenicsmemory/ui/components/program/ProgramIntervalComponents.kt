package io.github.gonbei774.calisthenicsmemory.ui.components.program

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ProgramCircularTimer
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ProgramStartIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    startCountdownSeconds: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    nextSetIndexOverride: Int? = null,  // Redoモード時など、次のセットが+1でない場合に使用
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (_, exercise) = session.exercises[currentSet.exerciseIndex]

    var remainingTime by remember { mutableIntStateOf(startCountdownSeconds) }
    var isPaused by remember { mutableStateOf(false) }
    val progress = remainingTime.toFloat() / startCountdownSeconds
    val effectivelyPaused = isPaused || isNavigationOpen

    LaunchedEffect(currentSetIndex, isNavigationOpen, isPaused) {
        while (remainingTime > 0) {
            if (effectivelyPaused) {
                delay(100L)
                continue
            }
            delay(1000L)
            if (effectivelyPaused) continue
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
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 全体の進捗表示
        val globalSetIndex = currentSetIndex + 1
        val totalSets = session.sets.size
        val sideText = when (currentSet.side) {
            "Right" -> stringResource(R.string.side_right)
            "Left" -> stringResource(R.string.side_left)
            else -> null
        }
        Text(
            text = if (sideText != null) {
                stringResource(R.string.set_progress_with_side, globalSetIndex, totalSets, sideText)
            } else {
                stringResource(R.string.set_progress, globalSetIndex, totalSets)
            },
            fontSize = 18.sp,
            color = appColors.textTertiary,
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

        // タイマー（タップで一時停止/再開）
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
                    color = Orange600.copy(alpha = if (effectivelyPaused) 0.3f else 1f),
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

        Spacer(modifier = Modifier.weight(1f))

        // スキップボタン
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip_button),
                color = appColors.textSecondary
            )
        }
    }
}

@Composable
internal fun ProgramIntervalStep(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    nextSetIndexOverride: Int? = null,  // Redoモード時など、次のセットが+1でない場合に使用
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    // 次のセット：オーバーライドがあればそれを使用、なければ+1
    val nextSetIndex = nextSetIndexOverride ?: (currentSetIndex + 1)
    val nextSet = session.sets.getOrNull(nextSetIndex)

    // ループ間休憩がある場合は追加
    val totalInterval = currentSet.intervalSeconds + currentSet.loopRestAfterSeconds
    val hasLoopRest = currentSet.loopRestAfterSeconds > 0

    var remainingTime by remember { mutableIntStateOf(totalInterval) }
    var isRunning by remember { mutableStateOf(true) }
    val progress = if (totalInterval > 0) {
        remainingTime.toFloat() / totalInterval
    } else 0f

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyRunning = isRunning && !isNavigationOpen

    LaunchedEffect(currentSetIndex, isNavigationOpen, isRunning) {
        while (remainingTime > 0 && effectivelyRunning) {
            delay(1000L)
            if (!effectivelyRunning) break
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
        // 休憩表示（ヘッダー直下）
        if (hasLoopRest) {
            // ラウンド間休憩表示
            Text(
                text = stringResource(R.string.loop_round_rest),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Purple600,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 20.sp,
                color = appColors.textTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            // 通常の休憩表示
            Text(
                text = stringResource(R.string.interval_label),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Cyan600,
                modifier = Modifier.padding(top = 8.dp)
            )
            // ループ内セットならラウンド情報を表示
            if (currentSet.loopId != null && currentSet.totalRounds > 1) {
                Text(
                    text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                    fontSize = 16.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // タイマー + ±ボタン
        Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = { remainingTime = (remainingTime - 10).coerceAtLeast(0) },
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

                // タイマー - タップで一時停止/再開
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isRunning = !isRunning }
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
                            color = Cyan600.copy(alpha = if (!effectivelyRunning) 0.3f else 1f),
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
                        color = appColors.textPrimary,
                        modifier = Modifier.alpha(if (!effectivelyRunning) 0.2f else 1f)
                    )
                    if (!effectivelyRunning) {
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
                    onClick = { remainingTime += 10 },
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

        Spacer(modifier = Modifier.weight(1f))

        // 次のセット/種目カード
        nextSet?.let { next ->
            val (_, nextExercise) = session.exercises[next.exerciseIndex]
            val nextSideText = when (next.side) {
                "Right" -> stringResource(R.string.side_right)
                "Left" -> stringResource(R.string.side_left)
                else -> null
            }
            val nextGlobalIndex = nextSetIndex + 1
            val totalSets = session.sets.size
            val setProgressText = if (nextSideText != null) {
                stringResource(R.string.set_progress_with_side, nextGlobalIndex, totalSets, nextSideText)
            } else {
                stringResource(R.string.set_progress, nextGlobalIndex, totalSets)
            }

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
                        color = Cyan600
                    )
                    Text(
                        text = "${nextExercise.name}  $setProgressText",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!nextExercise.description.isNullOrBlank()) {
                        Text(
                            text = nextExercise.description,
                            fontSize = 13.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // スキップボタン
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip_button),
                color = appColors.textSecondary
            )
        }
    }
}
package io.github.gonbei774.calisthenicsmemory.ui.components.program

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
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.components.common.PreviousTargetRow
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RecordAdjustDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RepPaceIndicator
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.util.SoundPlayer
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// タイマーOFF版 Dynamic専用: UIはTimer ONと統一、自動遷移なし
@Composable
internal fun ProgramExecutingStepDynamicManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = (exercise.repDuration ?: 5).coerceAtLeast(1)

    // 記録するレップ数（自動カウント）
    val recordValue = currentCount

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // タイマー完了フラグ
    val isTimerComplete = currentCount >= currentSet.targetValue

    // タイマー処理（自動遷移なし、目標到達後も計数継続）
    LaunchedEffect(currentSetIndex, isNavigationOpen, isPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                // レップカウント
                if (elapsedTime % repDuration == 0) {
                    currentCount++

                    // 音とフラッシュ
                    if (currentCount == currentSet.targetValue) {
                        // 目標達成の音（1回だけ）
                        if (isFlashEnabled) {
                            launch { flashController.flashSetComplete() }
                        }
                        soundPlayer.playSetComplete()
                    } else {
                        // 途中のレップは短いビープ（設定ONの場合のみ）
                        if (isCountSoundEnabled) {
                            soundPlayer.playBeep()
                            if (isFlashEnabled) {
                                launch { flashController.flashShort() }
                            }
                        }
                    }
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

        // ループ内セットならラウンド情報を表示
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 16.sp,
                color = Purple400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 大きなレップ数（タップで一時停止/再開）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isPaused = !isPaused }
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$recordValue",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerComplete) Green400 else appColors.textPrimary,
                    modifier = Modifier.alpha(if (effectivelyPaused) 0.3f else 1f)
                )
                Text(
                    text = " " + stringResource(R.string.unit_reps),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // レップ内ペース表示（ドット or 横バー）
        RepPaceIndicator(
            repDuration = repDuration,
            secondsInRep = repTimeElapsed,
            paused = effectivelyPaused,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 完了ボタン（押下で記録確認シートを表示）
        var showConfirm by remember(currentSetIndex) { mutableStateOf(false) }
        Button(
            onClick = {
                isPaused = true
                showConfirm = true
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

        if (showConfirm) {
            RecordAdjustDialog(
                initialValue = recordValue,
                unit = stringResource(R.string.unit_reps),
                onConfirm = { adjusted ->
                    showConfirm = false
                    onSetComplete(adjusted)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーOFF版 Isometric専用: カウントダウンタイマー + 手動完了
@Composable
internal fun ProgramExecutingStepIsometricManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 記録する値（経過時間）
    val recordValue = elapsedTime

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // タイマー完了フラグ
    val isTimerComplete = remainingTime <= 0

    // 色（一時停止中=グレー、完了=緑、実行中=オレンジ）
    val activeColor = if (isTimerComplete) Green600 else Orange600

    // 目標達成時のビープを一度だけ鳴らすためのフラグ
    var hasPlayedCompletionBeep by remember(currentSetIndex) { mutableStateOf(false) }

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen, isPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    soundPlayer.playBeep()
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }

                // 目標時間達成時に終了アラーム
                if (elapsedTime >= currentSet.targetValue && !hasPlayedCompletionBeep) {
                    hasPlayedCompletionBeep = true
                    // 目標達成時のビープ（3回連続を2セット）
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    soundPlayer.playSetComplete()
                }
            } else {
                delay(100L)  // 一時停止中または目標達成後は短い間隔でチェック
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

        // ループ内セットならラウンド情報を表示
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 16.sp,
                color = Purple400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
                fontSize = 80.sp,
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

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_seconds_short)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 完了ボタン（押下で記録確認シートを表示）
        var showConfirm by remember(currentSetIndex) { mutableStateOf(false) }
        Button(
            onClick = {
                isPaused = true
                showConfirm = true
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

        if (showConfirm) {
            RecordAdjustDialog(
                initialValue = recordValue,
                unit = stringResource(R.string.unit_seconds_short),
                onConfirm = { adjusted ->
                    showConfirm = false
                    onSetComplete(adjusted)
                },
                onDismiss = {
                    showConfirm = false
                    isPaused = false
                }
            )
        }

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーON版 Isometric専用: カウントダウンタイマー + 自動遷移
@Composable
internal fun ProgramExecutingStepIsometricAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isIntervalSoundEnabled: Boolean,
    intervalSeconds: Int,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 記録する値（経過時間）
    val recordValue = elapsedTime

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // 色（一時停止中=グレー、実行中=オレンジ）
    val activeColor = Orange600

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen, isPaused) {
        while (true) {
            if (!effectivelyPaused && elapsedTime < currentSet.targetValue) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    soundPlayer.playBeep()
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }

                // 目標時間達成時に終了アラーム + 自動遷移
                if (elapsedTime >= currentSet.targetValue) {
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    soundPlayer.playSetComplete()
                    onSetComplete(elapsedTime)
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

        // ループ内セットならラウンド情報を表示
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 16.sp,
                color = Purple400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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
                fontSize = 80.sp,
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

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_seconds_short)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 完了ボタン（早期完了用）
        Button(
            onClick = { onSetComplete(recordValue) },
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

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// タイマーON版 Dynamic専用: 自動レップカウント + UI統一
@Composable
internal fun ProgramExecutingStepDynamicAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    soundPlayer: SoundPlayer,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = (exercise.repDuration ?: 5).coerceAtLeast(1)

    // 記録するレップ数（自動カウント）
    val recordValue = currentCount

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // タイマー完了フラグ
    val isTimerComplete = currentCount >= currentSet.targetValue

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen, isPaused) {
        while (true) {
            if (!effectivelyPaused) {
                delay(1000L)
                elapsedTime++

                // レップカウント
                if (elapsedTime % repDuration == 0) {
                    currentCount++

                    // 目標達成時に自動遷移
                    if (currentCount >= currentSet.targetValue) {
                        if (isFlashEnabled) {
                            launch { flashController.flashSetComplete() }
                        }
                        soundPlayer.playSetComplete()
                        onSetComplete(currentCount)
                        return@LaunchedEffect
                    } else {
                        // 途中のレップは短いビープ（設定ONの場合のみ）
                        if (isCountSoundEnabled) {
                            soundPlayer.playBeep()
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

        // ループ内セットならラウンド情報を表示
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 16.sp,
                color = Purple400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 大きなレップ数（タップで一時停止/再開）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isPaused = !isPaused }
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$recordValue",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerComplete) Green400 else appColors.textPrimary,
                    modifier = Modifier.alpha(if (effectivelyPaused) 0.3f else 1f)
                )
                Text(
                    text = " " + stringResource(R.string.unit_reps),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // レップ内ペース表示（ドット or 横バー）
        RepPaceIndicator(
            repDuration = repDuration,
            secondsInRep = repTimeElapsed,
            paused = effectivelyPaused,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 完了ボタン（早期完了用）
        Button(
            onClick = { onSetComplete(recordValue) },
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

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

// レップ数数え上げOFF版 Dynamic専用: タイマーなし、静的カウンター
@Composable
internal fun ProgramExecutingStepDynamicSimple(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理: 初期値は目標回数
    var repsCount by remember(currentSetIndex) { mutableIntStateOf(currentSet.targetValue) }

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

        // ループ内セットならラウンド情報を表示
        if (currentSet.loopId != null && currentSet.totalRounds > 1) {
            Text(
                text = stringResource(R.string.loop_round_current, currentSet.roundNumber, currentSet.totalRounds),
                fontSize = 16.sp,
                color = Purple400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 大きなレップ数表示
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (repsCount > 0) repsCount-- },
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
                text = "$repsCount",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = Green400
            )

            IconButton(
                onClick = { repsCount++ },
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

        Spacer(modifier = Modifier.weight(1f))

        // 前回 ｜ 目標
        PreviousTargetRow(
            previous = currentSet.previousValue,
            target = currentSet.targetValue,
            unit = stringResource(R.string.unit_reps)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 完了ボタン
        Button(
            onClick = { onSetComplete(repsCount) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.complete_with_reps, repsCount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        NextExerciseText(session = session, currentSetIndex = currentSetIndex)
    }
}

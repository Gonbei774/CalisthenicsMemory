package io.github.gonbei774.calisthenicsmemory.ui.components.program

import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.screens.playTripleBeepTwice
import io.github.gonbei774.calisthenicsmemory.util.FlashController
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// タイマーOFF版 Dynamic専用: UIはTimer ONと統一、自動遷移なし
@Composable
internal fun ProgramExecutingStepDynamicManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var adjustedReps by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = exercise.repDuration ?: 5

    // 記録するレップ数（自動カウント + 調整値）
    val recordValue = (currentCount + adjustedReps).coerceAtLeast(0)

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // プログレス（0〜1）
    val progress = repTimeElapsed.toFloat() / repDuration

    // タイマー完了フラグ
    val isTimerComplete = currentCount >= currentSet.targetValue

    // 色（一時停止中=グレー、完了=緑、実行中=オレンジ）
    val statusColor = when {
        effectivelyPaused -> Slate400
        isTimerComplete -> Green600
        else -> Orange600
    }

    val scope = rememberCoroutineScope()

    // タイマー処理（自動遷移なし、目標達成時に停止）
    LaunchedEffect(currentSetIndex, isNavigationOpen) {
        while (true) {
            if (!effectivelyPaused && currentCount < currentSet.targetValue) {
                delay(1000L)
                elapsedTime++

                // レップカウント
                if (elapsedTime % repDuration == 0) {
                    currentCount++

                    // 音とフラッシュ
                    if (currentCount >= currentSet.targetValue) {
                        // 目標達成の音
                        if (isFlashEnabled) {
                            launch { flashController.flashSetComplete() }
                        }
                        playTripleBeepTwice(toneGenerator)
                    } else {
                        // 途中のレップは短いビープ（設定ONの場合のみ）
                        if (isCountSoundEnabled) {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                            if (isFlashEnabled) {
                                launch { flashController.flashShort() }
                            }
                        }
                    }
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

        // 次の種目/セット情報（上部に配置）
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        // 状態ラベル（実行中 / 一時停止中）
        Text(
            text = stringResource(if (isPaused) R.string.paused_label else R.string.executing_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 円形タイマー（レップ内の進捗表示）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                // 背景円
                drawArc(
                    color = Slate600,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // プログレス円
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // レップ内経過時間
            Text(
                text = "$repTimeElapsed",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // レップカウント表示（+/- ボタン付き）- タイマーと同じ幅で配置
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // マイナスボタン（タイマー横と同じ位置）
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
                        Text(
                            text = "-",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // レップカウント（タイマーと同じ幅220dpの中央に配置）
            Box(
                modifier = Modifier.width(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.reps_count, recordValue),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400
                )
            }

            // プラスボタン（タイマー横と同じ位置）
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
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 目標レップ数
        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue),
            fontSize = 14.sp,
            color = Slate400
        )

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン
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

        Spacer(modifier = Modifier.height(12.dp))

        // 一時停止/再開 + 中断ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 一時停止/再開ボタン
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Green600 else Slate600
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(if (isPaused) R.string.resume_button else R.string.pause_button),
                    fontSize = 14.sp
                )
            }

            // 中断ボタン
            Button(
                onClick = { onAbort() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stop_button),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // やり直しボタン
        OutlinedButton(
            onClick = { onRetry() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Slate500)
        ) {
            Text(
                text = stringResource(R.string.retry_set_button),
                fontSize = 14.sp,
                color = Slate300
            )
        }
    }
}

// タイマーOFF版 Isometric専用: カウントダウンタイマー + 手動完了
@Composable
internal fun ProgramExecutingStepIsometricManual(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
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
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var adjustedValue by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 記録する値（経過時間 + 調整値）
    val recordValue = (elapsedTime + adjustedValue).coerceAtLeast(0)

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // タイマー完了フラグ
    val isTimerComplete = remainingTime <= 0

    // 色（一時停止中=グレー、完了=緑、実行中=オレンジ）
    val statusColor = when {
        effectivelyPaused -> Slate400  // 一時停止中はグレー
        isTimerComplete -> Green600  // 完了は緑
        else -> Orange600  // 実行中はオレンジ
    }

    // 目標達成時のビープを一度だけ鳴らすためのフラグ
    var hasPlayedCompletionBeep by remember(currentSetIndex) { mutableStateOf(false) }

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen) {
        while (true) {
            if (!effectivelyPaused && elapsedTime < currentSet.targetValue) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
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
                    playTripleBeepTwice(toneGenerator)
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

        // 次の種目/セット情報（上部に配置）
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        // 状態ラベル（実行中 / 一時停止中）
        Text(
            text = stringResource(if (isPaused) R.string.paused_label else R.string.executing_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // タイマーセクション（円形タイマー + 調整ボタン）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // マイナスボタン
            IconButton(
                onClick = { adjustedValue-- },
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
                        Text(
                            text = "-",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 円形タイマー
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    // 背景円
                    drawArc(
                        color = Slate600,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // プログレス円
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // 残り時間表示
                Text(
                    text = "$remainingTime",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // プラスボタン
            IconButton(
                onClick = { adjustedValue++ },
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
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 経過時間 / 目標時間
        Text(
            text = stringResource(R.string.elapsed_target_format, elapsedTime, currentSet.targetValue),
            fontSize = 14.sp,
            color = Slate400
        )

        Spacer(modifier = Modifier.weight(1f))

        // 完了ボタン
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

        Spacer(modifier = Modifier.height(12.dp))

        // 一時停止/再開 + 中断ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 一時停止/再開ボタン
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Green600 else Slate600
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(if (isPaused) R.string.resume_button else R.string.pause_button),
                    fontSize = 14.sp
                )
            }

            // 中断ボタン
            Button(
                onClick = { onAbort() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stop_button),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // やり直しボタン
        OutlinedButton(
            onClick = { onRetry() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Slate500)
        ) {
            Text(
                text = stringResource(R.string.retry_set_button),
                fontSize = 14.sp,
                color = Slate300
            )
        }
    }
}

// タイマーON版 Isometric専用: カウントダウンタイマー + 自動遷移
@Composable
internal fun ProgramExecutingStepIsometricAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
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
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var adjustedValue by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 記録する値（経過時間 + 調整値）
    val recordValue = (elapsedTime + adjustedValue).coerceAtLeast(0)

    // 残り時間（カウントダウン用）
    val remainingTime = (currentSet.targetValue - elapsedTime).coerceAtLeast(0)

    // プログレス（0〜1）
    val progress = if (currentSet.targetValue > 0) {
        remainingTime.toFloat() / currentSet.targetValue
    } else 0f

    // タイマー完了フラグ
    val isTimerComplete = remainingTime <= 0

    // 色（一時停止中=グレー、完了=緑、実行中=オレンジ）
    val statusColor = when {
        effectivelyPaused -> Slate400
        isTimerComplete -> Green600
        else -> Orange600
    }

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen) {
        while (true) {
            if (!effectivelyPaused && elapsedTime < currentSet.targetValue) {
                delay(1000L)
                elapsedTime++

                // 一定間隔ごとにビープ音（目標達成前のみ、設定ONの場合）
                if (isIntervalSoundEnabled && intervalSeconds > 0 && elapsedTime > 0 && elapsedTime % intervalSeconds == 0 && elapsedTime < currentSet.targetValue) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    if (isFlashEnabled) {
                        launch { flashController.flashShort() }
                    }
                }

                // 目標時間達成時に終了アラーム + 自動遷移
                if (elapsedTime >= currentSet.targetValue) {
                    if (isFlashEnabled) {
                        launch { flashController.flashSetComplete() }
                    }
                    playTripleBeepTwice(toneGenerator)
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

        // 次の種目/セット情報（上部に配置）
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        // 状態ラベル（実行中 / 一時停止中）
        Text(
            text = stringResource(if (isPaused) R.string.paused_label else R.string.executing_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // タイマーセクション（円形タイマー + 調整ボタン）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // マイナスボタン
            IconButton(
                onClick = { adjustedValue-- },
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
                        Text(
                            text = "-",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 円形タイマー
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    // 背景円
                    drawArc(
                        color = Slate600,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // プログレス円
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // 残り時間表示
                Text(
                    text = "$remainingTime",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // プラスボタン
            IconButton(
                onClick = { adjustedValue++ },
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
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 経過時間 / 目標時間
        Text(
            text = stringResource(R.string.elapsed_target_format, elapsedTime, currentSet.targetValue),
            fontSize = 14.sp,
            color = Slate400
        )

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(12.dp))

        // 一時停止/再開 + 中断ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 一時停止/再開ボタン
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Green600 else Slate600
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(if (isPaused) R.string.resume_button else R.string.pause_button),
                    fontSize = 14.sp
                )
            }

            // 中断ボタン
            Button(
                onClick = { onAbort() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stop_button),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // やり直しボタン
        OutlinedButton(
            onClick = { onRetry() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Slate500)
        ) {
            Text(
                text = stringResource(R.string.retry_set_button),
                fontSize = 14.sp,
                color = Slate300
            )
        }
    }
}

// タイマーON版 Dynamic専用: 自動レップカウント + UI統一
@Composable
internal fun ProgramExecutingStepDynamicAuto(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    toneGenerator: ToneGenerator,
    flashController: FlashController,
    isFlashEnabled: Boolean,
    isCountSoundEnabled: Boolean,
    isNavigationOpen: Boolean = false,
    onSetComplete: (Int) -> Unit,
    onAbort: () -> Unit,
    onRetry: () -> Unit,
    onOpenNavigation: () -> Unit = {}
) {
    val currentSet = session.sets[currentSetIndex]
    val (pe, exercise) = session.exercises[currentSet.exerciseIndex]

    // 状態管理
    var elapsedTime by remember(currentSetIndex) { mutableIntStateOf(0) }
    var isPaused by remember(currentSetIndex) { mutableStateOf(false) }
    var currentCount by remember(currentSetIndex) { mutableIntStateOf(0) }
    var adjustedReps by remember(currentSetIndex) { mutableIntStateOf(0) }

    // ナビゲーション表示中は強制的に一時停止
    val effectivelyPaused = isPaused || isNavigationOpen

    // 1レップの秒数（種目設定があればそれを使用、なければ5秒）
    val repDuration = exercise.repDuration ?: 5

    // 記録するレップ数（自動カウント + 調整値）
    val recordValue = (currentCount + adjustedReps).coerceAtLeast(0)

    // レップ内の経過時間（カウントアップ）
    val repTimeElapsed = elapsedTime % repDuration

    // プログレス（0〜1）
    val progress = repTimeElapsed.toFloat() / repDuration

    // 色（一時停止中=グレー、実行中=オレンジ）
    val statusColor = if (effectivelyPaused) Slate400 else Orange600

    // タイマー処理
    LaunchedEffect(currentSetIndex, isNavigationOpen) {
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
                        playTripleBeepTwice(toneGenerator)
                        onSetComplete(currentCount)
                        return@LaunchedEffect
                    } else {
                        // 途中のレップは短いビープ（設定ONの場合のみ）
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

        // 次の種目/セット情報（上部に配置）
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        // 状態ラベル（実行中 / 一時停止中）
        Text(
            text = stringResource(if (isPaused) R.string.paused_label else R.string.executing_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 円形タイマー（レップ内の進捗表示）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                // 背景円
                drawArc(
                    color = Slate600,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // プログレス円
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // レップ内経過時間
            Text(
                text = "$repTimeElapsed",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // レップカウント表示（+/- ボタン付き）- タイマーと同じ幅で配置
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // マイナスボタン（タイマー横と同じ位置）
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
                        Text(
                            text = "-",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // レップカウント（タイマーと同じ幅220dpの中央に配置）
            Box(
                modifier = Modifier.width(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.reps_count, recordValue),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400
                )
            }

            // プラスボタン（タイマー横と同じ位置）
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
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 目標レップ数
        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue),
            fontSize = 14.sp,
            color = Slate400
        )

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(12.dp))

        // 一時停止/再開 + 中断ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 一時停止/再開ボタン
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Green600 else Slate600
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(if (isPaused) R.string.resume_button else R.string.pause_button),
                    fontSize = 14.sp
                )
            }

            // 中断ボタン
            Button(
                onClick = { onAbort() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stop_button),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // やり直しボタン
        OutlinedButton(
            onClick = { onRetry() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Slate500)
        ) {
            Text(
                text = stringResource(R.string.retry_set_button),
                fontSize = 14.sp,
                color = Slate300
            )
        }
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

        // 次の種目/セット情報
        NextExerciseInfo(session = session, currentSetIndex = currentSetIndex)

        Spacer(modifier = Modifier.weight(1f))

        // カウンター表示（+/- ボタン付き）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // マイナスボタン
            IconButton(
                onClick = { if (repsCount > 0) repsCount-- },
                modifier = Modifier.size(56.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // カウンター値
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$repsCount",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green400
                )
                Text(
                    text = stringResource(R.string.reps_unit),
                    fontSize = 24.sp,
                    color = Slate400
                )
            }

            // プラスボタン
            IconButton(
                onClick = { repsCount++ },
                modifier = Modifier.size(56.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, Slate500)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 目標レップ数
        Text(
            text = stringResource(R.string.target_reps_format, currentSet.targetValue),
            fontSize = 14.sp,
            color = Slate400,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(12.dp))

        // 中断ボタン
        Button(
            onClick = { onAbort() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red600),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stop_button),
                fontSize = 14.sp
            )
        }
    }
}

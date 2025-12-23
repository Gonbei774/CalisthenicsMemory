package io.github.gonbei774.calisthenicsmemory.ui.components.program

import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ProgramCircularTimer
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
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
internal fun ProgramIntervalStep(
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
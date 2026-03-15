package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors

// 次の種目情報をテキスト1行で表示（実行中画面の下部用）
@Composable
fun NextExerciseText(
    session: ProgramExecutionSession,
    currentSetIndex: Int
) {
    val appColors = LocalAppColors.current
    // 次の未完了セットを探す（Redoモード時に完了済みセットをスキップ）
    val nextSet = ((currentSetIndex + 1) until session.sets.size)
        .map { session.sets[it] }
        .firstOrNull { !it.isCompleted && !it.isSkipped } ?: return

    val (_, nextExercise) = session.exercises[nextSet.exerciseIndex]
    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }

    val displayName = if (nextSideText != null) {
        "${nextExercise.name} - $nextSideText"
    } else {
        nextExercise.name
    }

    Text(
        text = "${stringResource(R.string.interval_next)}: $displayName",
        fontSize = 14.sp,
        color = appColors.textSecondary,
        modifier = Modifier.padding(top = 16.dp)
    )
}
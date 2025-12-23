package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate400

// 次の種目/セット情報を表示するコンポーザブル
@Composable
fun NextExerciseInfo(
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
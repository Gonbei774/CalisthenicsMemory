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
    currentSetIndex: Int,
    nextSetIndexOverride: Int? = null  // Redoモード時など、次のセットが+1でない場合に使用
) {
    val nextSetIndex = nextSetIndexOverride ?: (currentSetIndex + 1)
    val nextSet = session.sets.getOrNull(nextSetIndex) ?: return

    val (nextPe, nextExercise) = session.exercises[nextSet.exerciseIndex]
    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }
    // 次の種目の実際のセット数を計算
    val nextActualTotalSets = session.sets
        .filter { it.exerciseIndex == nextSet.exerciseIndex }
        .maxOfOrNull { it.setNumber } ?: nextPe.sets

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
                stringResource(R.string.set_format_with_side, nextSet.setNumber, nextActualTotalSets, nextSideText)
            } else {
                stringResource(R.string.set_format, nextSet.setNumber, nextActualTotalSets)
            },
            fontSize = 14.sp,
            color = Slate400
        )
    }
}
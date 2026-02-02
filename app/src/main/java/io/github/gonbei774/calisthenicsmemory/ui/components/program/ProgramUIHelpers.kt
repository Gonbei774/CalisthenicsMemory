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

// 次の種目情報を表示するコンポーザブル（実行中画面用）
// セット番号は表示しない（今のセット番号から自明のため）
@Composable
fun NextExerciseInfo(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    nextSetIndexOverride: Int? = null  // Redoモード時など、次のセットが+1でない場合に使用
) {
    val appColors = LocalAppColors.current
    val nextSetIndex = nextSetIndexOverride ?: (currentSetIndex + 1)
    val nextSet = session.sets.getOrNull(nextSetIndex) ?: return

    val (_, nextExercise) = session.exercises[nextSet.exerciseIndex]
    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }

    // 次の種目名（サイド情報があれば含める）
    val displayText = if (nextSideText != null) {
        stringResource(R.string.next_exercise_label, "${nextExercise.name} - $nextSideText")
    } else {
        stringResource(R.string.next_exercise_label, nextExercise.name)
    }

    Text(
        text = displayText,
        fontSize = 16.sp,
        color = appColors.textSecondary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
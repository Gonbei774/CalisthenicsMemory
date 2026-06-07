package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors

/** ドット表示に切り替える上限（これを超える repDuration は横バー表示） */
private const val DOT_THRESHOLD = 9

/**
 * ダイナミック種目のレップ内ペース表示。
 * repDuration が小さい場合はドット列、大きい場合は横プログレスバーで「1レップ内のどこまで来たか」を示す。
 *
 * @param repDuration 1レップあたりの秒数。
 * @param secondsInRep レップ内の経過秒（elapsedTime % repDuration、0..repDuration-1）。
 * @param paused 一時停止中は淡色表示にする。
 */
@Composable
fun RepPaceIndicator(
    repDuration: Int,
    secondsInRep: Int,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val baseAlpha = if (paused) 0.3f else 1f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (repDuration in 1..DOT_THRESHOLD) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until repDuration) {
                    val isCurrent = i == secondsInRep
                    val isDone = i < secondsInRep
                    val alpha = when {
                        isCurrent -> baseAlpha
                        isDone -> baseAlpha * 0.55f
                        else -> baseAlpha * 0.18f
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(appColors.textPrimary.copy(alpha = alpha))
                    )
                }
            }
        } else {
            val progress = (secondsInRep.toFloat() / repDuration).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(appColors.textPrimary.copy(alpha = baseAlpha * 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(appColors.textPrimary.copy(alpha = baseAlpha))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.rep_pace_format, repDuration),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = appColors.textSecondary
        )
    }
}

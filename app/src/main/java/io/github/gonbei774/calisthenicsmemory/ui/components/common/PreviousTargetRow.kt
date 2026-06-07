package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.ui.theme.Amber500
import io.github.gonbei774.calisthenicsmemory.ui.theme.Cyan600
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors

/**
 * 実行画面下部に表示する「前回 ｜ 目標」の2カラム。
 *
 * @param previous 前回の記録値。null の場合は「—」を表示。
 * @param target 目標値。
 * @param unit 単位の文字列（reps / s など）。
 */
@Composable
fun PreviousTargetRow(
    previous: Int?,
    target: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatColumn(
            label = stringResource(R.string.stat_previous_label),
            value = previous?.toString() ?: "—",
            unit = if (previous != null) unit else null,
            accent = Cyan600,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(appColors.divider)
        )
        StatColumn(
            label = stringResource(R.string.stat_target_label),
            value = target.toString(),
            unit = unit,
            accent = Amber500,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    unit: String?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = accent.copy(alpha = 0.85f)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

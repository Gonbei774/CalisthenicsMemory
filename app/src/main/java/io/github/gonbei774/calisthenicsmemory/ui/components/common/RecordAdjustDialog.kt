package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.Green400
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate500

/**
 * 完了時に自動カウント値を確認・微調整するダイアログ。
 * タイマー由来の値は推定なので、保存前に +/- で実際の回数/秒数に補正できる。
 *
 * @param initialValue 初期値（タイマーの自動カウント値）。
 * @param unit 単位（reps / s）。
 * @param onConfirm 調整後の値で保存。
 * @param onDismiss キャンセル（運動に戻る）。
 */
@Composable
fun RecordAdjustDialog(
    initialValue: Int,
    unit: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    var value by remember { mutableIntStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_confirm_title)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdjustButton(symbol = "−", enabled = value > 0) {
                    if (value > 0) value--
                }
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$value",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green400
                    )
                    Text(
                        text = " $unit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                AdjustButton(symbol = "+", enabled = true) {
                    value++
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun AdjustButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(2.dp, Slate500),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary.copy(alpha = if (enabled) 1f else 0.3f)
            )
        }
    }
}

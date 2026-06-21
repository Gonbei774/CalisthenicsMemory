package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.Amber500
import io.github.gonbei774.calisthenicsmemory.ui.theme.Amber600
import io.github.gonbei774.calisthenicsmemory.ui.theme.Green400
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate500

/**
 * 完了時に自動カウント値を確認・微調整するダイアログ。
 * タイマー由来の値は推定なので、保存前に +/- で実際の回数/秒数に補正できる。
 *
 * 種目で荷重/距離/アシスト記録が有効な場合、その項目もこの場で入力できる。
 *
 * @param initialValue 初期値（タイマーの自動カウント値）。
 * @param unit 単位（reps / s）。
 * @param targetValue 目標値。指定された場合「目標値にセット」ボタンを表示する。
 * @param weightTrackingEnabled 荷重入力欄を表示するか。
 * @param distanceTrackingEnabled 距離入力欄を表示するか。
 * @param assistanceTrackingEnabled アシスト入力欄を表示するか。
 * @param initialWeightG 荷重の初期値（g）。
 * @param initialDistanceCm 距離の初期値（cm）。
 * @param initialAssistanceG アシストの初期値（g）。
 * @param previousWeightG 前回の荷重（g、ヒント表示用）。
 * @param previousDistanceCm 前回の距離（cm、ヒント表示用）。
 * @param previousAssistanceG 前回のアシスト（g、ヒント表示用）。
 * @param onConfirm 調整後の値で保存（回数/秒, 荷重g, 距離cm, アシストg）。
 * @param onDismiss キャンセル（運動に戻る）。
 */
@Composable
fun RecordAdjustDialog(
    initialValue: Int,
    unit: String,
    targetValue: Int? = null,
    weightTrackingEnabled: Boolean = false,
    distanceTrackingEnabled: Boolean = false,
    assistanceTrackingEnabled: Boolean = false,
    initialWeightG: Int? = null,
    initialDistanceCm: Int? = null,
    initialAssistanceG: Int? = null,
    previousWeightG: Int? = null,
    previousDistanceCm: Int? = null,
    previousAssistanceG: Int? = null,
    onConfirm: (value: Int, weightG: Int?, distanceCm: Int?, assistanceG: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    var value by remember { mutableIntStateOf(initialValue) }

    var weightStr by remember { mutableStateOf(initialWeightG?.let { gToKgString(it) } ?: "") }
    var distanceStr by remember { mutableStateOf(initialDistanceCm?.toString() ?: "") }
    var assistanceStr by remember { mutableStateOf(initialAssistanceG?.let { gToKgString(it) } ?: "") }

    val hasTracking = weightTrackingEnabled || distanceTrackingEnabled || assistanceTrackingEnabled

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_confirm_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                // 目標値が渡された場合、1タップで目標値に合わせるボタンを表示
                if (targetValue != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isAtTarget = value == targetValue
                    val accent = if (isAtTarget) Amber500.copy(alpha = 0.4f) else Amber500
                    OutlinedButton(
                        onClick = { value = targetValue },
                        enabled = !isAtTarget,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isAtTarget) Amber600.copy(alpha = 0.4f) else Amber600
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Amber500,
                            disabledContentColor = Amber500.copy(alpha = 0.4f)
                        )
                    ) {
                        // ターゲット（同心円）アイコン
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val r = size.minDimension / 2f
                            drawCircle(color = accent, radius = r, style = Stroke(width = 2.dp.toPx()))
                            drawCircle(color = accent, radius = r * 0.38f)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.record_set_to_target, targetValue, unit),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 荷重/距離/アシスト入力（有効な項目のみ）
                if (hasTracking) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Slate500.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (distanceTrackingEnabled) {
                        TrackingStepperRow(
                            label = stringResource(R.string.distance_input_label),
                            valueText = distanceStr,
                            previousText = previousDistanceCm?.toString() ?: "-",
                            keyboardType = KeyboardType.Number,
                            onValueTextChange = { input ->
                                val normalized = normalizeNumber(input)
                                if (normalized.isEmpty() || normalized.toIntOrNull() != null) {
                                    distanceStr = normalized
                                }
                            },
                            onMinus = {
                                val cur = distanceStr.toIntOrNull() ?: 0
                                distanceStr = (cur - 1).coerceAtLeast(0).toString()
                            },
                            onPlus = {
                                val cur = distanceStr.toIntOrNull() ?: 0
                                distanceStr = (cur + 1).coerceAtLeast(0).toString()
                            },
                            minusEnabled = (distanceStr.toIntOrNull() ?: 0) > 0
                        )
                    }

                    if (weightTrackingEnabled) {
                        TrackingStepperRow(
                            label = stringResource(R.string.weight_input_label),
                            valueText = weightStr,
                            previousText = previousWeightG?.let { gToKgString(it) } ?: "-",
                            keyboardType = KeyboardType.Decimal,
                            onValueTextChange = { input ->
                                val normalized = normalizeNumber(input)
                                if (normalized.isEmpty() || normalized == "." ||
                                    normalized.matches(Regex("^\\d*\\.?\\d?$"))
                                ) {
                                    weightStr = normalized
                                }
                            },
                            onMinus = {
                                val cur = kgStringToG(weightStr) ?: 0
                                weightStr = gToKgString((cur - 1000).coerceAtLeast(0))
                            },
                            onPlus = {
                                val cur = kgStringToG(weightStr) ?: 0
                                weightStr = gToKgString((cur + 1000).coerceAtLeast(0))
                            },
                            minusEnabled = (kgStringToG(weightStr) ?: 0) > 0
                        )
                    }

                    if (assistanceTrackingEnabled) {
                        TrackingStepperRow(
                            label = stringResource(R.string.assistance_input_label),
                            valueText = assistanceStr,
                            previousText = previousAssistanceG?.let { gToKgString(it) } ?: "-",
                            keyboardType = KeyboardType.Decimal,
                            onValueTextChange = { input ->
                                val normalized = normalizeNumber(input)
                                if (normalized.isEmpty() || normalized == "." ||
                                    normalized.matches(Regex("^\\d*\\.?\\d?$"))
                                ) {
                                    assistanceStr = normalized
                                }
                            },
                            onMinus = {
                                val cur = kgStringToG(assistanceStr) ?: 0
                                assistanceStr = gToKgString((cur - 1000).coerceAtLeast(0))
                            },
                            onPlus = {
                                val cur = kgStringToG(assistanceStr) ?: 0
                                assistanceStr = gToKgString((cur + 1000).coerceAtLeast(0))
                            },
                            minusEnabled = (kgStringToG(assistanceStr) ?: 0) > 0
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    value,
                    if (weightTrackingEnabled) kgStringToG(weightStr) else null,
                    if (distanceTrackingEnabled) distanceStr.toIntOrNull() else null,
                    if (assistanceTrackingEnabled) kgStringToG(assistanceStr) else null
                )
            }) {
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

/**
 * 荷重/距離/アシスト用の入力行（ラベル ｜ − ｜ 数値入力 ｜ ＋）。
 * 前回値がある場合はラベル下に小さく表示する。
 */
@Composable
private fun TrackingStepperRow(
    label: String,
    valueText: String,
    previousText: String,
    keyboardType: KeyboardType,
    onValueTextChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = appColors.textSecondary
            )
            Text(
                text = stringResource(R.string.previous_value_format, previousText),
                fontSize = 11.sp,
                color = appColors.textTertiary
            )
        }
        SmallStepButton(symbol = "−", enabled = minusEnabled, onClick = onMinus)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, Slate500),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(64.dp)
                .height(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = valueText,
                    onValueChange = onValueTextChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = appColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Green400),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        SmallStepButton(symbol = "+", enabled = true, onClick = onPlus)
    }
}

@Composable
private fun SmallStepButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, Slate500),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary.copy(alpha = if (enabled) 1f else 0.3f)
            )
        }
    }
}

private fun normalizeNumber(s: String): String = s
    .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
    .replace("．", ".")

private fun kgStringToG(s: String): Int? {
    val n = normalizeNumber(s)
    if (n.isBlank() || n == ".") return null
    val kg = n.toDoubleOrNull() ?: return null
    return (kg * 1000).toInt()
}

private fun gToKgString(g: Int): String = "%.1f".format(g / 1000.0)
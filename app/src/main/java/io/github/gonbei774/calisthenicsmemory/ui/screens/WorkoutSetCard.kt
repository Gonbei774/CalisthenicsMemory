package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.Green400
import io.github.gonbei774.calisthenicsmemory.ui.theme.Green600
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate600

/**
 * ワークアウト記録画面における各セットの状態。
 * セッション限定の UI 状態であり、DB には保存しない。
 * - DONE: 完了済み（チェックを外すと未完了に戻せる）
 * - CURRENT: 実行中（NOW バッジ・緑枠で強調）
 * - PENDING: 未着手
 */
enum class SetStatus { DONE, CURRENT, PENDING }

/**
 * セットカードのコンテナ。状態に応じて枠線・グロー・不透明度・タップ挙動を変える。
 * - CURRENT: 緑枠 + 外側のソフトな緑グロー
 * - DONE: 不透明度を下げる
 * - PENDING: カード本体タップで CURRENT 化（onActivate）
 */
@Composable
fun SetCardContainer(
    status: SetStatus,
    onActivate: () -> Unit,
    appColors: AppColors,
    content: @Composable () -> Unit
) {
    val isCurrent = status == SetStatus.CURRENT
    val isDone = status == SetStatus.DONE

    val outerModifier = if (isCurrent) {
        Modifier
            .fillMaxWidth()
            .background(Green600.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(4.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = outerModifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isDone) Modifier.alpha(0.7f) else Modifier)
                .then(if (status == SetStatus.PENDING) Modifier.clickable(onClick = onActivate) else Modifier),
            colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
            shape = RoundedCornerShape(14.dp),
            border = if (isCurrent) BorderStroke(2.dp, Green600) else null
        ) {
            content()
        }
    }
}

/**
 * 「NOW」バッジ。実行中セットのヘッダーに表示。緑のピル + 拡張するパルスリング。
 * システムのアニメーション無効設定（ANIMATOR_DURATION_SCALE == 0）時はパルスを止める。
 */
@Composable
fun NowBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }

    val progress = if (animate) {
        val transition = rememberInfiniteTransition(label = "now")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nowPulse"
        ).value
    } else {
        0f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 拡張するパルスリング（ピルの背後）
        if (animate) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val scale = 1f + 0.9f * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.5f * (1f - progress)
                    }
                    .background(Green600, RoundedCornerShape(999.dp))
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Green600)
                .padding(start = 7.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = stringResource(R.string.set_now_badge),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * セットの完了チェックトグル。
 * - DONE: 緑塗り + 白チェック（タップで未完了に戻す）
 * - CURRENT: 空の丸（タップで完了ショートカット = enabled）
 * - PENDING: 空の丸（タップ無効 = enabled false。カード本体タップで CURRENT 化）
 */
@Composable
fun SetCheckToggle(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val desc = stringResource(
        if (checked) R.string.set_uncheck_desc else R.string.set_status_check_desc
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) Green600 else Color.Transparent)
            .border(
                width = if (checked) 2.dp else 1.5.dp,
                color = if (checked) Green600 else Slate600,
                shape = CircleShape
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = desc,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 実行中セットの下部に表示する「セット N を完了」ボタン。
 * メイン値（回数 / 秒数）が未入力のときは無効。
 */
@Composable
fun SetCompleteButton(
    setNumber: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Green600.copy(alpha = 0.15f),
            contentColor = Green400,
            disabledContainerColor = Green600.copy(alpha = 0.05f),
            disabledContentColor = Slate600
        ),
        border = BorderStroke(1.dp, Green600.copy(alpha = 0.3f))
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.set_complete_button, setNumber),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

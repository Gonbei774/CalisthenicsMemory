package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate600

@Composable
fun ProgramCircularTimer(
    progress: Float,
    remainingTime: Int,
    color: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawArc(
                color = Slate600,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$remainingTime",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(0f, 4f),
                    blurRadius = 8f
                )
            )
        )
    }
}
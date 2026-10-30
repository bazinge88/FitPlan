package com.fitplan.app.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitSize

/**
 * 品牌标记：组环 + 对勾（与清单打卡的视觉符号同源）。
 * 单色可 tint，最小 24dp 仍可识别；默认 72dp，调用方可在外层 Modifier.size 覆盖。
 */
@Composable
fun FitBrandMark(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(FitSize.emptyIcon)) {
        val stroke = this.size.minDimension * 0.075f
        val inset = stroke / 2f
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val check = Path().apply {
            moveTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.52f)
            lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.66f)
            lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.36f)
        }
        drawPath(
            path = check,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

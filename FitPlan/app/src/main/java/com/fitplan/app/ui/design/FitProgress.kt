package com.fitplan.app.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitType
import com.fitplan.app.ui.theme.fitColors
import com.fitplan.app.ui.theme.fitMotion

/** 环形进度：track + 品牌主色弧，spring gentle 过渡（减少动态时跳变）。 */
@Composable
fun FitRingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = MaterialTheme.fitColors.track,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val motion = MaterialTheme.fitMotion
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.gentle else snap(),
        label = "fitRingProgress",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** 线性进度：胶囊轨道 + 品牌主色填充，spring gentle 过渡。 */
@Composable
fun FitLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.fitColors.track,
) {
    val motion = MaterialTheme.fitMotion
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (motion.enabled) motion.gentle else snap(),
        label = "fitLinearProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(FitShapes.pill)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .clip(FitShapes.pill)
                .background(color),
        )
    }
}

/** 数字滚动：垂直滑入滑出 + 淡入淡出（减少动态时直接更新），数字用 tabular numerals。 */
@Composable
fun AnimatedCount(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = Color.Unspecified,
    suffix: String = "",
) {
    val motion = MaterialTheme.fitMotion
    val resolvedColor = color.takeOrElse { LocalContentColor.current }
    val mergedStyle = style.merge(FitType.numeric)
    if (!motion.enabled) {
        Text(
            text = value.toString() + suffix,
            modifier = modifier,
            style = mergedStyle,
            color = resolvedColor,
        )
        return
    }
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            (
                slideInVertically(
                    animationSpec = tween(durationMillis = motion.medium, easing = motion.standard),
                ) { fullHeight -> fullHeight / 2 } +
                    fadeIn(animationSpec = tween(durationMillis = motion.medium))
                ) togetherWith (
                slideOutVertically(
                    animationSpec = tween(durationMillis = motion.short),
                ) { fullHeight -> -fullHeight / 2 } +
                    fadeOut(animationSpec = tween(durationMillis = motion.short))
                )
        },
        label = "fitAnimatedCount",
    ) { animatedValue ->
        Text(
            text = animatedValue.toString() + suffix,
            style = mergedStyle,
            color = resolvedColor,
        )
    }
}

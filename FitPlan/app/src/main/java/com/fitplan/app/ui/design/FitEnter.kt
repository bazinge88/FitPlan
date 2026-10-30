package com.fitplan.app.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.LocalFitMotion

/**
 * 列表项入场：alpha 0→1 + 垂直位移 12dp→0，按 index 阶梯延迟（每项 35ms，最多 8 项）。
 * 减少动态效果开启时直接显示；用 rememberSaveable 记录，滚动复用时不会重复播放。
 */
@Composable
fun Modifier.fitEnter(index: Int = 0, maxStagger: Int = 8): Modifier {
    val motion = LocalFitMotion.current
    if (!motion.enabled) return this
    var started by rememberSaveable { mutableStateOf(false) }
    val staggerIndex = index.coerceAtLeast(0).coerceAtMost(maxStagger)
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.medium,
            delayMillis = staggerIndex * 35,
            easing = motion.emphasizedDecel,
        ),
        label = "fitEnter",
    )
    LaunchedEffect(Unit) { started = true }
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 12.dp.toPx()
    }
}

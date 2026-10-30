package com.fitplan.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/*
 * 动效 token（docs/iteration12-plan.md §3.6）。
 *
 * 减少动态效果：读取系统 animator_duration_scale（ValueAnimator.areAnimatorsEnabled()），
 * 并在 ON_RESUME 时刷新（用户可能刚从开发者选项改回前台）。
 * 关闭时所有组件退化：时长归零 / 纯淡入 / 直接跳变。
 */

@Immutable
data class FitMotion(
    /** 系统是否允许动画（减少动态效果关闭时为 false）。 */
    val enabled: Boolean = true,
    /** 颜色/透明度微变。 */
    val short: Int = 120,
    /** 页面转场、展开。 */
    val medium: Int = 220,
    /** BottomSheet/复杂布局。 */
    val long: Int = 320,
    /** hero/进度环首次呈现。 */
    val extraLong: Int = 480,
    /** 通用缓动。 */
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    /** 进入/展开缓动。 */
    val emphasizedDecel: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
    /** 退出/收起缓动。 */
    val emphasizedAccel: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f),
    /** 打卡圆圈、对勾。 */
    val pop: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 700f),
    /** 进度环/进度条/数字。 */
    val gentle: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 320f),
    /** 弹层进入。 */
    val sheet: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 420f),
)

val LocalFitMotion = staticCompositionLocalOf { FitMotion(enabled = true) }

/** 动效 token 入口：[MaterialTheme.fitMotion]. */
val MaterialTheme.fitMotion: FitMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalFitMotion.current

/**
 * 系统动画开关（减少动态效果检测）：初始化读一次，ON_RESUME 再刷新。
 * 组件据此决定是否降级动画。
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

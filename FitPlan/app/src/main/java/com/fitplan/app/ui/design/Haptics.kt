package com.fitplan.app.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 触觉反馈门面（跟随系统触感设置，无需 VIBRATE 权限）。
 * - tap：底部导航切换等轻触
 * - toggleOn/toggleOff：标逃/取消
 * - success：全部完成
 */
@Stable
class FitHaptics internal constructor(private val hapticFeedback: HapticFeedback) {
    fun tap() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun toggleOn() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
    }

    fun toggleOff() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
    }

    fun success() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }
}

@Composable
fun rememberFitHaptics(): FitHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { FitHaptics(hapticFeedback) }
}

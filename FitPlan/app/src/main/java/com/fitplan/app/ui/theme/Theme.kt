package com.fitplan.app.ui.theme

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * FitPlan 主题（docs/iteration12-plan.md §4.1）。
 *
 * - 深色跟随系统；Android 12+ 默认动态取色（只替换 M3 色彩角色）。
 * - 同时提供 LocalFitColors（品牌扩展色）与 LocalFitMotion（动效及减少动态降级）。
 * - 边到边由 MainActivity 的 enableEdgeToEdge 打开；这里同步系统栏图标的明暗，
 *   页面内容按 FitScaffold/FitScaffold 的 WindowInsets 避让。
 */
@Composable
fun FitPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FitDarkColorScheme
        else -> FitLightColorScheme
    }
    val fitColors = if (darkTheme) darkFitExtraColors() else lightFitExtraColors()
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val motion = remember(animationsEnabled) { FitMotion(enabled = animationsEnabled) }

    // 系统栏图标明暗随主题（系统栏透明，图标反色）。
    val view = LocalView.current
    val activity = LocalActivity.current
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    CompositionLocalProvider(
        LocalFitColors provides fitColors,
        LocalFitMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FitTypography(),
            shapes = FitShapes.material,
            content = content,
        )
    }
}

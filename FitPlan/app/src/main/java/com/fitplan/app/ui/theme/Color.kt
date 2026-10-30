package com.fitplan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * FitPlan 品牌色板（第十二轮设计 token，docs/iteration12-plan.md §3.1）。
 *
 * 浅色：暖米白纸面 + 深常青绿；深色：墨绿近黑。
 * Android 12+ 动态取色开启时，M3 色彩角色由系统色板替换；
 * 品牌识别由 FitExtraColors（固定值）、排版、形状与动效承载。
 */

// ---- 浅色品牌色板 ----

private val LightPrimary = Color(0xFF2E6B4F)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFCFEBD9)
private val LightOnPrimaryContainer = Color(0xFF0B3320)

private val LightSecondary = Color(0xFF5C635A)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFE0E7DC)
private val LightOnSecondaryContainer = Color(0xFF1B211A)

private val LightTertiary = Color(0xFF8A5A2B)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFF5E2C9)
private val LightOnTertiaryContainer = Color(0xFF3A2410)

private val LightError = Color(0xFFB3261E)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFF9DEDC)
private val LightOnErrorContainer = Color(0xFF410E0B)

private val LightBackground = Color(0xFFF6F7F2)
private val LightOnBackground = Color(0xFF191C18)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF191C18)
private val LightSurfaceVariant = Color(0xFFE2E8DB)
private val LightOnSurfaceVariant = Color(0xFF5C6357)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF0F3EB)
private val LightSurfaceContainer = Color(0xFFE9EEE3)
private val LightSurfaceContainerHigh = Color(0xFFE2E8DB)
private val LightSurfaceContainerHighest = Color(0xFFDBE2D3)
private val LightOutline = Color(0xFF8B9286)
private val LightOutlineVariant = Color(0xFFD6DCCF)
private val LightInverseSurface = Color(0xFF2F322C)
private val LightInverseOnSurface = Color(0xFFF0F1EC)

// ---- 深色品牌色板 ----

private val DarkPrimary = Color(0xFF8ED8AE)
private val DarkOnPrimary = Color(0xFF05301C)
private val DarkPrimaryContainer = Color(0xFF1F4D35)
private val DarkOnPrimaryContainer = Color(0xFFC9EFDA)

private val DarkSecondary = Color(0xFFB7C2B2)
private val DarkOnSecondary = Color(0xFF232A22)
private val DarkSecondaryContainer = Color(0xFF3A4438)
private val DarkOnSecondaryContainer = Color(0xFFD6DCD1)

private val DarkTertiary = Color(0xFFF0BE8A)
private val DarkOnTertiary = Color(0xFF4A2C0D)
private val DarkTertiaryContainer = Color(0xFF5A3E21)
private val DarkOnTertiaryContainer = Color(0xFFF5E2C9)

private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

private val DarkBackground = Color(0xFF0F1210)
private val DarkOnBackground = Color(0xFFE2E7DD)
private val DarkSurface = Color(0xFF171C17)
private val DarkOnSurface = Color(0xFFE2E7DD)
private val DarkSurfaceVariant = Color(0xFF3A4438)
private val DarkOnSurfaceVariant = Color(0xFFA9B2A4)
private val DarkSurfaceContainerLowest = Color(0xFF0A0D0B)
private val DarkSurfaceContainerLow = Color(0xFF1B211A)
private val DarkSurfaceContainer = Color(0xFF20271F)
private val DarkSurfaceContainerHigh = Color(0xFF262E24)
private val DarkSurfaceContainerHighest = Color(0xFF2C3529)
private val DarkOutline = Color(0xFF6F7A6C)
private val DarkOutlineVariant = Color(0xFF3A4438)
private val DarkInverseSurface = Color(0xFFE2E7DD)
private val DarkInverseOnSurface = Color(0xFF171C17)

internal val FitLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = Color(0xFF8ED8AE),
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    surfaceTint = LightPrimary,
)

internal val FitDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = Color(0xFF2E6B4F),
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    surfaceTint = DarkPrimary,
)

/**
 * 品牌扩展色：不参与动态取色，始终使用品牌固定值，保证完成态/PR/track 的识别稳定。
 * 读取入口：[MaterialTheme.fitColors]。
 */
@Immutable
data class FitExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val track: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val surfaceSunken: Color,
    val scrim: Color,
)

fun lightFitExtraColors(): FitExtraColors = FitExtraColors(
    success = Color(0xFF2E6B4F),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFCFEBD9),
    onSuccessContainer = Color(0xFF0B3320),
    warning = Color(0xFF8A5A2B),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFF5E2C9),
    onWarningContainer = Color(0xFF3A2410),
    track = Color(0xFFDBE2D3),
    heroTop = Color(0xFFEAF4ED),
    heroBottom = Color(0xFFF6F7F2),
    surfaceSunken = Color(0xFFEEF1E8),
    scrim = Color(0xFF0B120D).copy(alpha = 0.32f),
)

fun darkFitExtraColors(): FitExtraColors = FitExtraColors(
    success = Color(0xFF8ED8AE),
    onSuccess = Color(0xFF05301C),
    successContainer = Color(0xFF1F4D35),
    onSuccessContainer = Color(0xFFC9EFDA),
    warning = Color(0xFFF0BE8A),
    onWarning = Color(0xFF4A2C0D),
    warningContainer = Color(0xFF5A3E21),
    onWarningContainer = Color(0xFFF5E2C9),
    track = Color(0xFF2C3529),
    heroTop = Color(0xFF1B2A20),
    heroBottom = Color(0xFF0F1210),
    surfaceSunken = Color(0xFF121712),
    scrim = Color(0xFF000000).copy(alpha = 0.6f),
)

val LocalFitColors = staticCompositionLocalOf { lightFitExtraColors() }

/** 品牌扩展色入口：[MaterialTheme.fitColors]. */
val MaterialTheme.fitColors: FitExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFitColors.current

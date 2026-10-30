package com.fitplan.app.ui.design

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSpace

internal val LocalFitSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

/**
 * 取当前 FitScaffold 共享的 SnackbarHostState；不在 FitScaffold 内时返回独立的本地状态。
 * 屏幕用法：val snackbar = rememberFitSnackbarState(); scope.launch { snackbar.showMessage("文案") }
 */
@Composable
fun rememberFitSnackbarState(): SnackbarHostState {
    val shared = LocalFitSnackbarHostState.current
    return shared ?: remember { SnackbarHostState() }
}

/** 品牌化 Snackbar 宿主（L2 悬浮表面 + 轻阴影）。 */
@Composable
fun FitSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = rememberFitSnackbarState()
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = FitSpace.lg, vertical = FitSpace.sm),
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = FitShapes.field,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionContentColor = MaterialTheme.colorScheme.primary,
        )
    }
}

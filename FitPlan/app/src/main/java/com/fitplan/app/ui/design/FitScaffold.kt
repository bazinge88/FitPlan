package com.fitplan.app.ui.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 页面脚手架：统一背景色、顶栏/底栏/悬浮按钮槽位与 Snackbar 宿主。
 *
 * Snackbar 通道：内容区通过 [rememberFitSnackbarState] 取共享的 SnackbarHostState，
 * 在协程里 showMessage；默认宿主 [FitSnackbarHost] 读同一状态。
 */
@Composable
fun FitScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = { FitSnackbarHost() },
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalFitSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            containerColor = containerColor,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}

package com.fitplan.app.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fitplan.app.ui.theme.FitShapes

/**
 * 品牌对话框（28dp 圆角 + 文字按钮）。
 *
 * 行为约定：
 * - 点击确认只调用 [onConfirm]（不自动关闭），点击取消/返回/点外部只调用 [onDismiss]；
 *   显隐由调用方状态控制。
 * - [dismissLabel] 传 null 时只显示确认按钮（单按钮结果提示）。
 * - [confirmEnabled] 控制确认按钮禁用态（如保存按钮校验未通过）。
 * - 旧 ConfirmDialog 兼容层保持旧语义（先 onDismiss 再 onConfirm），见 ui/Common.kt。
 */
@Composable
fun FitDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "确定",
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = "取消",
    destructive: Boolean = false,
    text: String? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FitShapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                FitTextAction(
                    text = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    destructive = destructive,
                )
            }
        },
        dismissButton = {
            if (dismissLabel != null) {
                FitTextAction(text = dismissLabel, onClick = onDismiss)
            }
        },
    )
}

/** 品牌底部弹层：28dp 顶部圆角、拖拽把手、surface 底色。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = FitShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        content = content,
    )
}

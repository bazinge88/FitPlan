package com.fitplan.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.InputRules

/*
 * 旧通用组件兼容层（docs/iteration12-plan.md §4.3）：
 * 对外签名与业务语义保持不变，仅把外观 token 化；新页面优先使用 ui/design 下的组件。
 */

/** 数值输入解析（重量允许负号与小数）。空字符串返回 null。 */
fun parseDoubleOrNull(text: String): Double? = text.trim().toDoubleOrNull()

fun parseIntOrNull(text: String): Int? = text.trim().toIntOrNull()

/**
 * 数值输入框（兼容层）。
 * - [allowDecimalNegative] = false（默认）：整数目标字段，只收数字；
 * - [allowDecimalNegative] = true：重量字段，额外允许负号（辅助重量）与小数点。
 * 空字符串返回 null。非法非空值通过 [isError] + [supportingText] 显示错误文案。
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allowDecimalNegative: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // 整数目标只收数字（过滤负号与小数点）；重量字段放行负号/小数点
            if (
                if (allowDecimalNegative) {
                    InputRules.isWeightInputAllowed(input)
                } else {
                    InputRules.isIntInputAllowed(input)
                }
            ) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimalNegative) KeyboardType.Decimal else KeyboardType.Number,
        ),
        shape = FitShapes.field,
        colors = fitFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 输入框统一配色（兼容层与下拉选择器共用）。 */
@Composable
private fun fitFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** 简单下拉选择器（兼容层）。selected 为 null 时显示 placeholder。 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    options: List<T>,
    label: (T) -> String,
    selected: T?,
    placeholder: String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let(label) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            shape = FitShapes.field,
            colors = fitFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * 二次确认对话框（兼容层，保持旧语义：确认时先 onDismiss 再 onConfirm）。
 * 外观委托 [FitDialog]。
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "确认",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FitDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = {
            onDismiss()
            onConfirm()
        },
        dismissLabel = "取消",
        text = text,
    )
}

/** 轻量空态/加载态提示（兼容层，外观委托 [FitEmptyState]）。 */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        FitEmptyState(
            title = text,
            icon = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSpace.xxl),
        )
    }
}

/** 字段标签行内展示，如 “4 组 · 8 次 · 60”。逻辑与文案保持不变。 */
fun setsSummary(sets: Int?, reps: Int?, weight: Double?, durationMin: Int?): String {
    val parts = mutableListOf<String>()
    if (sets != null) parts.add("$sets 组")
    if (reps != null) parts.add("$reps 次")
    if (weight != null) parts.add(com.fitplan.app.util.TimeUtil.formatWeight(weight))
    if (durationMin != null) parts.add("$durationMin 分钟")
    return parts.joinToString(" · ").ifEmpty { "未设置目标" }
}

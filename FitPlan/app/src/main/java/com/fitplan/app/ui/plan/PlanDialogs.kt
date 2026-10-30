package com.fitplan.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.model.SchemeType
import com.fitplan.app.data.model.Split3Binding
import com.fitplan.app.ui.DropdownSelector
import com.fitplan.app.ui.design.FitBottomSheet
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitHairline
import com.fitplan.app.ui.design.FitTextAction
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset

/**
 * 单日安排编辑：训练日（绑定组合）/ 休息日，
 * 支持从其他日期复制、移除安排。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditSheet(
    date: Long,
    plan: DayPlanEntity?,
    combos: List<ComboEntity>,
    onSet: (isRest: Boolean, comboId: Long?) -> Unit,
    onCopyFrom: (sourceDate: Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isTraining by remember { mutableStateOf(plan?.isRest == false) }
    var comboId by remember { mutableStateOf(plan?.comboId) }
    var pickingSource by remember { mutableStateOf(false) }

    FitBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(FitSpace.lg),
        ) {
            Text(
                "${TimeUtil.formatDate(TimeUtil.fromEpochDay(date))}（${TimeUtil.weekdayName(TimeUtil.fromEpochDay(date))}）",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // 休息/训练分段选择（保留 RadioButton 语义：读屏可识别为单选）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FitShapes.pill)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(FitSpace.xxs),
                horizontalArrangement = Arrangement.spacedBy(FitSpace.xxs),
            ) {
                SegmentButton(
                    text = "休息日",
                    selected = !isTraining,
                    onClick = { isTraining = false },
                    modifier = Modifier.weight(1f),
                )
                SegmentButton(
                    text = "训练日",
                    selected = isTraining,
                    onClick = {
                        isTraining = true
                        if (comboId == null && combos.isNotEmpty()) comboId = combos.first().id
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (isTraining) {
                DropdownSelector(
                    options = combos,
                    label = { it.name },
                    selected = combos.find { it.id == comboId },
                    placeholder = "选择训练组合",
                    onSelect = { comboId = it.id },
                )
            }
            FitButton(
                text = "保存",
                onClick = { onSet(!isTraining, comboId) },
                enabled = !isTraining || comboId != null,
                modifier = Modifier.fillMaxWidth(),
            )
            // 主次分明：复制=次按钮（tonal），移除=低强调（ghost，避免误触）；
            // 均整行宽度，避免 FitButton 单行文本在窄屏/大字体下被省略
            FitButton(
                text = "从其他日期复制",
                onClick = { pickingSource = true },
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Tonal,
            )
            FitButton(
                text = "移除安排",
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                style = FitButtonStyle.Ghost,
            )
            FitHairline()
            // 快捷：直接复制前一天
            FitTextAction(
                text = "复制前一天（${TimeUtil.formatShort(TimeUtil.fromEpochDay(date - 1))}）的安排",
                onClick = { onCopyFrom(date - 1) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(FitSpace.xxl))
        }
    }

    if (pickingSource) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = TimeUtil.fromEpochDay(date).toEpochDay() * 24 * 60 * 60 * 1000,
        )
        DatePickerDialog(
            onDismissRequest = { pickingSource = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val sourceDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)
                                .toLocalDate().toEpochDay()
                            pickingSource = false
                            onCopyFrom(sourceDate)
                        }
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("复制到当天") }
            },
            dismissButton = {
                TextButton(onClick = { pickingSource = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState, title = { Text("选择源日期", Modifier.padding(16.dp)) })
        }
    }
}

/** 分段选择的一段：48dp 触达，选中态用主色容器；toggleable + RadioButton 语义与重构前一致。 */
@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = FitSize.touchMin)
            .clip(FitShapes.pill)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, FitShapes.pill)
                } else {
                    Modifier
                },
            )
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = { if (it) onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * 内置方案自动排程：三分化（三个槽位绑定组合）或练一休一（组合轮流循环）。
 * 生成后写入当前显示的周，之后可手动修改。
 */
@Composable
fun AutoScheduleDialog(
    mondayEpochDay: Long,
    combos: List<ComboEntity>,
    onApply: (SchemeType, Split3Binding, List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var scheme by remember { mutableStateOf(SchemeType.SPLIT3) }
    var slot1 by remember { mutableStateOf<Long?>(combos.getOrNull(0)?.id) }
    var slot2 by remember { mutableStateOf<Long?>(combos.getOrNull(1)?.id) }
    var slot3 by remember { mutableStateOf<Long?>(combos.getOrNull(2)?.id) }
    var cycleIds by remember { mutableStateOf(combos.take(2).map { it.id }) }

    // 组合数据异步到达后补齐默认绑定（冷启动立即打开对话框时首次 combos 可能为空）：
    // 仅填充仍为 null/空的值，不覆盖用户已手动修改的选择
    LaunchedEffect(combos) {
        if (combos.isEmpty()) return@LaunchedEffect
        if (slot1 == null) slot1 = combos.getOrNull(0)?.id
        if (slot2 == null) slot2 = combos.getOrNull(1)?.id
        if (slot3 == null) slot3 = combos.getOrNull(2)?.id
        if (cycleIds.isEmpty()) cycleIds = combos.take(2).map { it.id }
    }

    val canGenerate = combos.isNotEmpty() &&
        (scheme == SchemeType.SPLIT3 || cycleIds.isNotEmpty())

    FitDialog(
        title = "自动排程 · ${TimeUtil.formatShort(TimeUtil.fromEpochDay(mondayEpochDay))} 当周",
        onDismiss = onDismiss,
        confirmLabel = "生成",
        confirmEnabled = canGenerate,
        onConfirm = { onApply(scheme, Split3Binding(slot1, slot2, slot3), cycleIds) },
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FitSpace.sm),
            ) {
            Column(verticalArrangement = Arrangement.spacedBy(FitSpace.xs)) {
                SchemeType.entries.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = FitSize.touchMin)
                            .clip(FitShapes.small)
                            .toggleable(
                                value = scheme == s,
                                role = Role.RadioButton,
                                onValueChange = { selected ->
                                    if (selected) scheme = s
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = scheme == s, onClick = null)
                        Column(Modifier.padding(start = FitSpace.xs)) {
                            Text(
                                s.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                s.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            FitHairline()
            if (scheme == SchemeType.SPLIT3) {
                Text(
                    "槽位绑定（未绑定的槽位按休息处理）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DropdownSelector(
                    options = combos,
                    label = { it.name },
                    selected = combos.find { it.id == slot1 },
                    placeholder = "周一组合",
                    onSelect = { slot1 = it.id },
                )
                DropdownSelector(
                    options = combos,
                    label = { it.name },
                    selected = combos.find { it.id == slot2 },
                    placeholder = "周三组合",
                    onSelect = { slot2 = it.id },
                )
                DropdownSelector(
                    options = combos,
                    label = { it.name },
                    selected = combos.find { it.id == slot3 },
                    placeholder = "周五组合",
                    onSelect = { slot3 = it.id },
                )
            } else {
                Text(
                    "勾选参与循环的组合（按顺序轮流），至少一个：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                combos.forEach { combo ->
                    val checked = combo.id in cycleIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = FitSize.touchMin)
                            .clip(FitShapes.small)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { selected ->
                                    cycleIds = if (selected) {
                                        cycleIds + combo.id
                                    } else {
                                        cycleIds - combo.id
                                    }
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(
                            combo.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Text(
                "将覆盖该周已有安排，生成后可手动修改。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            }
        },
    )
}

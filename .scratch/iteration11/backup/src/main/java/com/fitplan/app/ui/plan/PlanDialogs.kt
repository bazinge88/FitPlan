package com.fitplan.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${TimeUtil.formatDate(TimeUtil.fromEpochDay(date))}（${TimeUtil.weekdayName(TimeUtil.fromEpochDay(date))}）",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = !isTraining,
                            role = Role.RadioButton,
                            onValueChange = { selected ->
                                if (selected) isTraining = false
                            },
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !isTraining, onClick = null)
                    Text("休息日")
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = isTraining,
                            role = Role.RadioButton,
                            onValueChange = { selected ->
                                if (selected) {
                                    isTraining = true
                                    if (comboId == null && combos.isNotEmpty()) comboId = combos.first().id
                                }
                            },
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isTraining, onClick = null)
                    Text("训练日")
                }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSet(!isTraining, comboId) },
                    enabled = !isTraining || comboId != null,
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
                OutlinedButton(onClick = { pickingSource = true }, modifier = Modifier.weight(1f)) {
                    Text("从其他日期复制")
                }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("移除安排")
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TextButton(
                onClick = {
                    // 快捷：直接复制前一天
                    onCopyFrom(date - 1)
                },
                modifier = Modifier.padding(bottom = 24.dp),
            ) { Text("复制前一天（${TimeUtil.formatShort(TimeUtil.fromEpochDay(date - 1))}）的安排") }
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
                            val sourceDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动排程 · ${TimeUtil.formatShort(TimeUtil.fromEpochDay(mondayEpochDay))} 当周") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SchemeType.entries.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(s.label)
                                Text(
                                    s.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                if (scheme == SchemeType.SPLIT3) {
                    Text("槽位绑定（未绑定的槽位按休息处理）")
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
                    Text("勾选参与循环的组合（按顺序轮流），至少一个：")
                    combos.forEach { combo ->
                        val checked = combo.id in cycleIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .toggleable(
                                    value = checked,
                                    role = Role.Checkbox,
                                    onValueChange = { selected ->
                                        cycleIds = if (selected) cycleIds + combo.id else cycleIds - combo.id
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(combo.name)
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
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        scheme,
                        Split3Binding(slot1, slot2, slot3),
                        cycleIds,
                    )
                },
                enabled = combos.isNotEmpty() &&
                    (scheme == SchemeType.SPLIT3 || cycleIds.isNotEmpty()),
            ) { Text("生成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

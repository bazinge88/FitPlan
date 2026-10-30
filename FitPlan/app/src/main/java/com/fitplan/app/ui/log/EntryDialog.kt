package com.fitplan.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.ui.DropdownSelector
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.parseDoubleOrNull
import com.fitplan.app.util.InputRules
import com.fitplan.app.util.TimeUtil

private data class SetRow(
    val sets: String = "",
    val reps: String = "",
    val weight: String = "",
    val duration: String = "",
)

/** 该组记录值的非法非空判断：整数目标越界/非数字、重量非数字（重量允许负数/0/小数）。 */
private fun SetRow.hasInvalidValue(): Boolean =
    InputRules.isInvalidPositiveInt(sets, InputRules.MAX_SETS) ||
        InputRules.isInvalidPositiveInt(reps, InputRules.MAX_REPS) ||
        InputRules.isInvalidPositiveInt(duration, InputRules.MAX_DURATION_MIN) ||
        InputRules.isInvalidWeight(weight)

/**
 * 添加/编辑一条训练记录（FitDialog 统一外观）。
 * 显示字段由所选动作的配置决定；键盘类型、校验与保存禁用条件沿用旧版。
 */
@Composable
fun EntryDialog(
    date: Long,
    exercises: List<ExerciseEntity>,
    unit: String,
    source: LogEntryWithSets?,
    onDismiss: () -> Unit,
    onSave: (exerciseId: Long, note: String, sets: List<SetDraft>) -> Unit,
) {
    var exerciseId by remember {
        mutableStateOf(source?.entry?.exerciseId ?: exercises.firstOrNull()?.id)
    }
    var note by remember { mutableStateOf(source?.entry?.note ?: "") }
    var rows by remember {
        mutableStateOf(
            source?.sets?.sortedBy { it.sort }?.map {
                SetRow(
                    sets = it.sets?.toString() ?: "",
                    reps = it.reps?.toString() ?: "",
                    weight = it.weight?.let { w -> TimeUtil.formatWeight(w) } ?: "",
                    duration = it.duration?.toString() ?: "",
                )
            } ?: listOf(SetRow()),
        )
    }

    val exercise = exercises.find { it.id == exerciseId }
    // 非法非空目标值（负数/0/越界/非数字）显示错误并禁用保存，清空或改值后可保存
    val canSave = exerciseId != null && rows.none { it.hasInvalidValue() }

    FitDialog(
        title = if (source == null) "添加记录" else "编辑记录",
        onDismiss = onDismiss,
        confirmLabel = "保存",
        confirmEnabled = canSave,
        onConfirm = {
            val drafts = rows.map {
                // 非法非空值已被保存按钮禁用拦截；此处仍按范围解析，越界不落库
                SetDraft(
                    sets = InputRules.parsePositiveInt(it.sets, InputRules.MAX_SETS),
                    reps = InputRules.parsePositiveInt(it.reps, InputRules.MAX_REPS),
                    weight = parseDoubleOrNull(it.weight),
                    duration = InputRules.parsePositiveInt(it.duration, InputRules.MAX_DURATION_MIN),
                )
            }
            exerciseId?.let { id -> onSave(id, note, drafts) }
        },
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = FitSpace.huge * 10)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FitSpace.md),
        ) {
            DropdownSelector(
                options = exercises,
                label = { it.name },
                selected = exercise,
                placeholder = "选择动作",
                onSelect = { exerciseId = it.id },
            )
            rows.forEachIndexed { index, row ->
                Column(verticalArrangement = Arrangement.spacedBy(FitSpace.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "第 ${index + 1} 组",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        FitIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "移除该组",
                            onClick = { rows = rows.filterIndexed { i, _ -> i != index } },
                        )
                    }
                    // 各字段独立报错：整数目标越界/非数字、重量非数字；空值 = 不填，不报错
                    val setsError = if (InputRules.isInvalidPositiveInt(row.sets, InputRules.MAX_SETS)) {
                        "组数需为 1-${InputRules.MAX_SETS}"
                    } else {
                        null
                    }
                    val repsError = if (InputRules.isInvalidPositiveInt(row.reps, InputRules.MAX_REPS)) {
                        "次数需为 1-${InputRules.MAX_REPS}"
                    } else {
                        null
                    }
                    val weightError = if (InputRules.isInvalidWeight(row.weight)) {
                        "重量需为数字（可负数/小数）"
                    } else {
                        null
                    }
                    val durationError = if (
                        InputRules.isInvalidPositiveInt(row.duration, InputRules.MAX_DURATION_MIN)
                    ) {
                        "时长需为 1-${InputRules.MAX_DURATION_MIN} 分钟"
                    } else {
                        null
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(FitSpace.sm)) {
                        if (exercise?.trackSets != false) {
                            NumberField(
                                row.sets,
                                { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(sets = v) else r } },
                                "组数",
                                Modifier.weight(1f),
                                isError = setsError != null,
                                supportingText = setsError,
                            )
                        }
                        if (exercise?.trackReps != false) {
                            NumberField(
                                row.reps,
                                { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(reps = v) else r } },
                                "次数",
                                Modifier.weight(1f),
                                isError = repsError != null,
                                supportingText = repsError,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(FitSpace.sm)) {
                        if (exercise?.trackWeight != false) {
                            NumberField(
                                row.weight,
                                { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(weight = v) else r } },
                                "重量（$unit，可负数）",
                                Modifier.weight(1f),
                                allowDecimalNegative = true,
                                isError = weightError != null,
                                supportingText = weightError,
                            )
                        }
                        if (exercise?.trackDuration == true) {
                            NumberField(
                                row.duration,
                                { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(duration = v) else r } },
                                "时长（分钟）",
                                Modifier.weight(1f),
                                isError = durationError != null,
                                supportingText = durationError,
                            )
                        }
                    }
                }
            }
            FitButton(
                text = "添加一组",
                onClick = { rows = rows + SetRow() },
                style = FitButtonStyle.Ghost,
                leadingIcon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

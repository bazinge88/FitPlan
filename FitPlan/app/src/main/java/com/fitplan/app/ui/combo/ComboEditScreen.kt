package com.fitplan.app.ui.combo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitScaffold
import com.fitplan.app.ui.design.FitTopBar
import com.fitplan.app.ui.parseDoubleOrNull
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.InputRules
import com.fitplan.app.util.TimeUtil

/** 编辑器内的一条动作行。 */
data class RowItem(
    val key: Long,
    val exerciseId: Long?,
    val targetSets: String,
    val targetReps: String,
    val targetWeight: String,
    val targetDuration: String,
)

/** 行内目标值的非法非空判断：整数目标越界/非数字、重量非数字（重量允许负数/0/小数）。 */
private fun RowItem.hasInvalidTarget(): Boolean =
    exerciseId != null && (
        InputRules.isInvalidPositiveInt(targetSets, InputRules.MAX_SETS) ||
            InputRules.isInvalidPositiveInt(targetReps, InputRules.MAX_REPS) ||
            InputRules.isInvalidPositiveInt(targetDuration, InputRules.MAX_DURATION_MIN) ||
            InputRules.isInvalidWeight(targetWeight)
        )

/**
 * 组合编辑：顶栏（返回 / 标题 / 保存）、组合名称、动作行卡（选动作 + 目标字段 + 更多菜单）、添加动作。
 * 保存语义与校验完全沿用旧版：名称非空、至少一个动作、行内目标值合法才可保存；空值 = 不设定。
 */
@Composable
fun ComboEditScreen(
    comboId: Long,
    onBack: () -> Unit,
) {
    val vm: ComboEditViewModel = viewModel(
        key = "combo-$comboId",
        factory = ComboEditViewModel.factory(comboId),
    )
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val unitIsLb by vm.unitIsLb.collectAsStateWithLifecycle()
    val weightLabel = "重量（${if (unitIsLb) "lb" else "kg"}，可负数）"

    var name by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(listOf<RowItem>()) }
    var loaded by remember { mutableStateOf(false) }
    // 行内单选动作 / 「添加动作」批量选择，两个入口共用同一个选动作对话框
    var pickingRowKey by remember { mutableStateOf<Long?>(null) }
    var showAddPicker by remember { mutableStateOf(false) }
    val markedIds = rows.mapNotNull { it.exerciseId }.toSet()

    LaunchedEffect(comboId) {
        vm.loadCombo()?.let { combo ->
            name = combo.combo.name
            rows = combo.items.sortedBy { it.sort }.map {
                RowItem(
                    key = it.id,
                    exerciseId = it.exerciseId,
                    targetSets = it.targetSets?.toString() ?: "",
                    targetReps = it.targetReps?.toString() ?: "",
                    targetWeight = it.targetWeight?.let { w -> TimeUtil.formatWeight(w) } ?: "",
                    targetDuration = it.targetDuration?.toString() ?: "",
                )
            }
        }
        loaded = true
    }

    fun newKey(): Long = (rows.maxOfOrNull { it.key } ?: 0L) + 1L

    fun updateRow(row: RowItem) {
        rows = rows.map { if (it.key == row.key) row else it }
    }

    fun moveRow(row: RowItem, delta: Int) {
        val index = rows.indexOfFirst { it.key == row.key }
        val target = index + delta
        if (index < 0 || target < 0 || target >= rows.size) return
        val mutable = rows.toMutableList()
        mutable.removeAt(index)
        mutable.add(target, row)
        rows = mutable
    }

    // 非法非空目标值（负数/0/越界/非数字）显示错误并禁用保存，清空或改值后可保存
    val canSave = loaded && name.isNotBlank() && rows.any { it.exerciseId != null } &&
        rows.none { it.hasInvalidTarget() }

    fun save() {
        val drafts = rows.filter { it.exerciseId != null }.map {
            ComboItemDraft(
                exerciseId = it.exerciseId!!,
                // 非法非空值已被保存按钮禁用拦截；此处仍按范围解析，越界不落库
                targetSets = InputRules.parsePositiveInt(it.targetSets, InputRules.MAX_SETS),
                targetReps = InputRules.parsePositiveInt(it.targetReps, InputRules.MAX_REPS),
                targetWeight = parseDoubleOrNull(it.targetWeight),
                targetDuration = InputRules.parsePositiveInt(
                    it.targetDuration,
                    InputRules.MAX_DURATION_MIN,
                ),
            )
        }
        vm.save(name.trim(), drafts, onDone = onBack)
    }

    FitScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FitTopBar(
                title = if (comboId > 0) "编辑组合" else "新建组合",
                onBack = onBack,
                actions = {
                    FitIconButton(
                        icon = Icons.Rounded.Check,
                        contentDescription = "保存",
                        onClick = { save() },
                        enabled = canSave,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(
                start = FitSpace.xl,
                end = FitSpace.xl,
                top = FitSpace.md,
                bottom = FitSpace.huge,
            ),
            verticalArrangement = Arrangement.spacedBy(FitSpace.md),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("组合名称") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 用列表重载而不是 count+索引：删除行时 key 提供者与 count 同源，避免旧索引访问越界
            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                val exercise = exercises.find { it.id == row.exerciseId }
                ItemRowCard(
                    row = row,
                    exercise = exercise,
                    weightLabel = weightLabel,
                    canMoveUp = index > 0,
                    canMoveDown = index < rows.size - 1,
                    onPick = { pickingRowKey = row.key },
                    onSetsChange = { updateRow(row.copy(targetSets = it)) },
                    onRepsChange = { updateRow(row.copy(targetReps = it)) },
                    onWeightChange = { updateRow(row.copy(targetWeight = it)) },
                    onDurationChange = { updateRow(row.copy(targetDuration = it)) },
                    onMoveUp = { moveRow(row, -1) },
                    onMoveDown = { moveRow(row, +1) },
                    onDelete = { rows = rows.filter { it.key != row.key } },
                )
            }
            item(key = "add") {
                FitButton(
                    text = "添加动作",
                    onClick = { showAddPicker = true },
                    style = FitButtonStyle.Tonal,
                    leadingIcon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pickingRowKey?.let { key ->
        ExercisePickerDialog(
            exercises = exercises,
            title = "选择动作",
            single = true,
            markedIds = markedIds,
            onConfirm = { picked ->
                val p = picked.first()
                // 已设置默认重量且行内目标重量为空时自动填入；已有值不覆盖
                val prefill = p.defaultWeight?.let { TimeUtil.formatWeight(it) }
                rows = rows.map {
                    if (it.key == key) {
                        it.copy(exerciseId = p.id, targetWeight = it.targetWeight.ifEmpty { prefill ?: "" })
                    } else {
                        it
                    }
                }
                pickingRowKey = null
            },
            onDismiss = { pickingRowKey = null },
        )
    }
    if (showAddPicker) {
        ExercisePickerDialog(
            exercises = exercises,
            title = "添加动作",
            single = false,
            markedIds = markedIds,
            onConfirm = { picked ->
                var next = newKey()
                rows = rows + picked.map {
                    RowItem(
                        key = next++,
                        exerciseId = it.id,
                        targetSets = "",
                        targetReps = "",
                        // 新行目标重量预填动作默认重量，未设置则留空
                        targetWeight = it.defaultWeight?.let { w -> TimeUtil.formatWeight(w) } ?: "",
                        targetDuration = "",
                    )
                }
                showAddPicker = false
            },
            onDismiss = { showAddPicker = false },
        )
    }
}

@Composable
private fun ItemRowCard(
    row: RowItem,
    exercise: ExerciseEntity?,
    weightLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPick: () -> Unit,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    FitCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitSpace.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 动作选择行：整行可点，弹出选动作对话框（替代长列表下拉）
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(FitShapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(onClick = onPick)
                        .heightIn(min = FitSize.touchMin)
                        .padding(horizontal = FitSpace.md, vertical = FitSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FitSize.icon),
                    )
                    Spacer(Modifier.width(FitSpace.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "选择动作",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            exercise?.name ?: "点击选择动作",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (exercise == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(FitSize.icon),
                    )
                }
                FitIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "更多操作",
                    onClick = { menuExpanded = true },
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("上移") },
                        leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null) },
                        enabled = canMoveUp,
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("下移") },
                        leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
                        enabled = canMoveDown,
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("移除") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
            if (exercise != null) {
                val showSets = exercise.trackSets
                val showReps = exercise.trackReps
                val showWeight = exercise.trackWeight
                val showDuration = exercise.trackDuration
                if (showSets || showReps || showWeight || showDuration) {
                    Text(
                        "目标（留空表示不设定）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 各字段独立报错：整数目标越界/非数字、重量非数字；空值 = 不设定，不报错
                val setsError = if (InputRules.isInvalidPositiveInt(row.targetSets, InputRules.MAX_SETS)) {
                    "组数需为 1-${InputRules.MAX_SETS}"
                } else {
                    null
                }
                val repsError = if (InputRules.isInvalidPositiveInt(row.targetReps, InputRules.MAX_REPS)) {
                    "次数需为 1-${InputRules.MAX_REPS}"
                } else {
                    null
                }
                val weightError = if (InputRules.isInvalidWeight(row.targetWeight)) {
                    "重量需为数字（可负数/小数）"
                } else {
                    null
                }
                val durationError = if (
                    InputRules.isInvalidPositiveInt(row.targetDuration, InputRules.MAX_DURATION_MIN)
                ) {
                    "时长需为 1-${InputRules.MAX_DURATION_MIN} 分钟"
                } else {
                    null
                }
                if (showSets || showReps) {
                    // 两列网格：窄屏/大字体下每字段各占一半，错误文案在框下换行不溢出
                    Row(horizontalArrangement = Arrangement.spacedBy(FitSpace.md)) {
                        if (showSets) {
                            NumberField(
                                row.targetSets,
                                onSetsChange,
                                "组数",
                                Modifier.weight(1f),
                                isError = setsError != null,
                                supportingText = setsError,
                            )
                        }
                        if (showReps) {
                            NumberField(
                                row.targetReps,
                                onRepsChange,
                                "次数",
                                Modifier.weight(1f),
                                isError = repsError != null,
                                supportingText = repsError,
                            )
                        }
                    }
                }
                if (showWeight || showDuration) {
                    Row(horizontalArrangement = Arrangement.spacedBy(FitSpace.md)) {
                        if (showWeight) {
                            NumberField(
                                row.targetWeight,
                                onWeightChange,
                                weightLabel,
                                Modifier.weight(1f),
                                allowDecimalNegative = true,
                                isError = weightError != null,
                                supportingText = weightError,
                            )
                        }
                        if (showDuration) {
                            NumberField(
                                row.targetDuration,
                                onDurationChange,
                                "时长（分钟）",
                                Modifier.weight(1f),
                                isError = durationError != null,
                                supportingText = durationError,
                            )
                        }
                    }
                }
            }
        }
    }
}

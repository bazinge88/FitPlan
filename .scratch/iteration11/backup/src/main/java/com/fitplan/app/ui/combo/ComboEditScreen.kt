package com.fitplan.app.ui.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.parseIntOrNull
import com.fitplan.app.ui.parseDoubleOrNull
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text(if (comboId > 0) "编辑组合" else "新建组合") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val drafts = rows.filter { it.exerciseId != null }.map {
                                ComboItemDraft(
                                    exerciseId = it.exerciseId!!,
                                    targetSets = parseIntOrNull(it.targetSets),
                                    targetReps = parseIntOrNull(it.targetReps),
                                    targetWeight = parseDoubleOrNull(it.targetWeight),
                                    targetDuration = parseIntOrNull(it.targetDuration),
                                )
                            }
                            vm.save(name.trim(), drafts, onDone = onBack)
                        },
                        enabled = loaded && name.isNotBlank() && rows.any { it.exerciseId != null },
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("组合名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(rows.size, key = { rows[it].key }) { index ->
                val row = rows[index]
                val exercise = exercises.find { it.id == row.exerciseId }
                ItemRowCard(
                    row = row,
                    exercise = exercise,
                    exercises = exercises,
                    weightLabel = weightLabel,
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
            item {
                Button(onClick = { showAddPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" 添加动作")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
    exercises: List<ExerciseEntity>,
    weightLabel: String,
    onPick: () -> Unit,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 只读字段 + 透明点击层：点任意位置弹出选动作对话框（替代长列表下拉）
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = exercise?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择动作") },
                        trailingIcon = {
                            Icon(Icons.Filled.ExpandMore, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onPick),
                    )
                }
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "移除")
                }
            }
            if (exercise != null) {
                Text(
                    "目标（留空表示不设定）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exercise.trackSets) {
                        NumberField(row.targetSets, onSetsChange, "组数", Modifier.weight(1f))
                    }
                    if (exercise.trackReps) {
                        NumberField(row.targetReps, onRepsChange, "次数", Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exercise.trackWeight) {
                        NumberField(
                            row.targetWeight,
                            onWeightChange,
                            weightLabel,
                            Modifier.weight(1f),
                        )
                    }
                    if (exercise.trackDuration) {
                        NumberField(
                            row.targetDuration,
                            onDurationChange,
                            "时长（分钟）",
                            Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

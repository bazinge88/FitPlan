package com.fitplan.app.ui.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.ui.EmptyHint
import com.fitplan.app.ui.setsSummary
import com.fitplan.app.util.TimeUtil

/** 训练详情页：某天的组合动作清单式打卡。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(date: Long, onBack: () -> Unit) {
    val vm: WorkoutViewModel = viewModel(
        key = "workout-$date",
        factory = WorkoutViewModel.factory(date),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val dateText = TimeUtil.formatDate(TimeUtil.fromEpochDay(date)) +
        " · " + TimeUtil.weekdayName(TimeUtil.fromEpochDay(date))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                title = {
                    Column {
                        Text(state.comboName ?: "训练详情")
                        Text(
                            dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> EmptyHint("加载中…", Modifier.padding(padding))

            state.comboName == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyHint("今天没有安排训练")
                TextButton(onClick = onBack) { Text("返回") }
            }

            else -> WorkoutChecklist(
                state = state,
                actions = remember(vm) { workoutActions(vm) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

/** 清单打卡动作载体：主页与训练页共用接线，仿 workoutFlowBundle() 先例。 */
data class WorkoutActions(
    val onCheckIn: (comboItemId: Long, setIndex: Int, reps: Int?) -> Unit,
    val onCancel: (comboItemId: Long, setIndex: Int) -> Unit,
    val onSkipSet: (comboItemId: Long, setIndex: Int) -> Unit,
    val onCancelSetSkip: (comboItemId: Long, setIndex: Int) -> Unit,
    val onAdjustReps: (comboItemId: Long, setIndex: Int, delta: Int) -> Unit,
    val onEditSetWeight: (comboItemId: Long, setIndex: Int, weight: Double?) -> Unit,
    val onRecordSessionWeight: (exerciseId: Long, weight: Double?) -> Unit,
    val onRecordExerciseNote: (exerciseId: Long, note: String) -> Unit,
    val onSyncToItem: (comboItemId: Long, exerciseId: Long, weight: Double) -> Unit,
    val onSyncToAll: (exerciseId: Long, weight: Double) -> Unit,
    val onDismissSyncPrompt: (exerciseId: Long, weight: Double) -> Unit,
)

/** 主页与训练页共用接线，仿 workoutFlowBundle() 先例。 */
fun workoutActions(vm: WorkoutViewModel): WorkoutActions = WorkoutActions(
    onCheckIn = vm::checkIn,
    onCancel = vm::cancelCheckIn,
    onSkipSet = vm::skipSet,
    onCancelSetSkip = vm::cancelSetSkip,
    onAdjustReps = vm::adjustReps,
    onEditSetWeight = vm::updateSetWeight,
    onRecordSessionWeight = vm::recordSessionWeight,
    onRecordExerciseNote = vm::recordExerciseNote,
    onSyncToItem = vm::syncToItem,
    onSyncToAll = vm::syncToAll,
    onDismissSyncPrompt = vm::dismissSyncPrompt,
)

/** 清单式打卡列表：训练详情页与主页共用。 */
@Composable
fun WorkoutChecklist(
    state: WorkoutUiState,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.items, key = { it.comboItemId }) { item ->
            WorkoutItemCard(
                item = item,
                unitIsLb = state.unitIsLb,
                actions = actions,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WorkoutItemCard(
    item: WorkoutItemUi,
    unitIsLb: Boolean,
    actions: WorkoutActions,
) {
    var expanded by rememberSaveable(item.comboItemId) { mutableStateOf(false) }
    val allDone = item.sets.isNotEmpty() && item.doneCount == item.sets.size
    val progress = if (item.targetSets != null) {
        "已完成 ${item.doneCount}/${item.targetSets} 组"
    } else {
        "已完成 ${item.doneCount} 组"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AggregateCircle(allDone = allDone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (allDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        setsSummary(item.targetSets, item.targetReps, item.targetWeight, item.targetDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            if (expanded) {
                if (item.trackWeight) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                    )
                    item.syncPromptWeight?.let { promptWeight ->
                        SyncPromptRow(
                            item = item,
                            promptWeight = promptWeight,
                            unitIsLb = unitIsLb,
                            onSyncToItem = actions.onSyncToItem,
                            onSyncToAll = actions.onSyncToAll,
                            onDismiss = actions.onDismissSyncPrompt,
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )
                    }
                    WeightRow(
                        item = item,
                        unitIsLb = unitIsLb,
                        onRecord = { actions.onRecordSessionWeight(item.exerciseId, it) },
                    )
                }
                item.sets.forEach { set ->
                    // 头部与首个组行、以及相邻组行之间各一条细分隔线（每行前都画，等价覆盖两种位置）
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                    )
                    SetRow(
                        item = item,
                        set = set,
                        unitIsLb = unitIsLb,
                        actions = actions,
                    )
                }
                ExerciseNoteField(
                    todayNote = item.todayNote,
                    lastNote = item.lastNote,
                    onSave = { actions.onRecordExerciseNote(item.exerciseId, it) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** 同步提醒条（非阻断）：历史当次重量超过基准时出现；确认后才写入组合条目，忽略则记住该重量。 */
@Composable
private fun SyncPromptRow(
    item: WorkoutItemUi,
    promptWeight: Double,
    unitIsLb: Boolean,
    onSyncToItem: (comboItemId: Long, exerciseId: Long, weight: Double) -> Unit,
    onSyncToAll: (exerciseId: Long, weight: Double) -> Unit,
    onDismiss: (exerciseId: Long, weight: Double) -> Unit,
) {
    // 勾选状态为瞬态状态，随卡片收起丢弃即可
    var syncAll by remember { mutableStateOf(false) }
    val unit = if (unitIsLb) "lb" else "kg"
    // 提醒出现时历史最大必然 > 条目目标（或无目标），文案据此区分
    val exceeds = if (item.targetWeight != null) "超过目标重量" else "当前未设目标重量"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "上次实际用了 ${TimeUtil.formatWeight(promptWeight)} $unit，$exceeds，是否同步为目标重量？",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.syncCount > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Checkbox(checked = syncAll, onCheckedChange = { syncAll = it })
                Text(
                    "同步到所有包含此动作的组合（共 ${item.syncCount} 处）",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onDismiss(item.exerciseId, promptWeight) }) {
                Text("忽略")
            }
            TextButton(onClick = {
                if (syncAll) {
                    onSyncToAll(item.exerciseId, promptWeight)
                } else {
                    onSyncToItem(item.comboItemId, item.exerciseId, promptWeight)
                }
            }) {
                Text("同步")
            }
        }
    }
}

/** 重量行：−/+ 步进 2.5，点数值精确输入；记录当次重量（只记当天，不写回组合条目）。 */
@Composable
private fun WeightRow(
    item: WorkoutItemUi,
    unitIsLb: Boolean,
    onRecord: (Double?) -> Unit,
) {
    // 对话框开关为瞬态状态，随卡片收起丢弃即可，不参与状态恢复
    var showEditor by remember { mutableStateOf(false) }
    val unit = if (unitIsLb) "lb" else "kg"
    val step = 2.5
    // 显示优先级：当次重量 > 组合目标重量
    val sessionWeight = item.sessionWeight
    val shown = sessionWeight ?: item.targetWeight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "重量",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onRecord((shown ?: 0.0) - step) }) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "减少重量",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = shown?.let { "${TimeUtil.formatWeight(it)} $unit" } ?: "未设定",
                style = MaterialTheme.typography.titleMedium,
                color = if (shown == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showEditor = true }
                    .semantics { contentDescription = "编辑当次重量" }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
            if (shown != null) {
                Text(
                    text = if (sessionWeight != null) "当次" else "目标",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onRecord((shown ?: 0.0) + step) }) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "增加重量",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showEditor) {
        SessionWeightDialog(
            initial = shown,
            unit = unit,
            hasSession = sessionWeight != null,
            onConfirm = { value ->
                onRecord(value)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

/** 当次重量精确输入：允许负数（辅助重量）与小数；清空并确定 = 取消当次记录（显示回落目标重量）。 */
@Composable
private fun SessionWeightDialog(
    initial: Double?,
    unit: String,
    hasSession: Boolean,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial?.let { TimeUtil.formatWeight(it) } ?: "") }
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("当次重量（$unit）") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        // 允许：数字、小数点、负号（辅助重量）
                        if (input.isEmpty() || input.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                            text = input
                        }
                    },
                    label = { Text("正数负重 · 0 自重 · 负数辅助") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "只记录今天这次训练，不写入组合目标重量" +
                        if (hasSession) "；清空并确定 = 取消当次记录。" else "。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed.toDoubleOrNull()) },
                enabled = trimmed.isEmpty() || trimmed.toDoubleOrNull() != null,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 动作级聚合圈：全部组完成 = 实心对勾；否则 = 描边空圆。填色/描边带颜色过渡，完成瞬间有可见动画。 */
@Composable
private fun AggregateCircle(allDone: Boolean) {
    // 打卡完成态切换用颜色过渡动画呈现，所有动作（含最后一项）表现一致
    val backgroundColor by animateColorAsState(
        targetValue = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "aggregateCircleFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "aggregateCircleBorder",
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (allDone) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SetRow(
    item: WorkoutItemUi,
    set: WorkoutSetUi,
    unitIsLb: Boolean,
    actions: WorkoutActions,
) {
    // 完成态 = 该组打卡行存在（与 reps 是否记录无关，「只记完成」同样算完成）
    val checked = set.checked
    var showWeightEditor by remember { mutableStateOf(false) }
    val unit = if (unitIsLb) "lb" else "kg"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetCircle(
            checked = checked,
            number = set.setIndex + 1,
            onCheckIn = { actions.onCheckIn(item.comboItemId, set.setIndex, defaultRepsFor(item, set.setIndex)) },
            onCancel = { actions.onCancel(item.comboItemId, set.setIndex) },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "第 ${set.setIndex + 1} 组",
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        val reps = set.reps
        when {
            reps != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                // 原子增减在 SQL 内完成，无需读旧值判边界（下限 1 由 DAO 保证）
                IconButton(onClick = { actions.onAdjustReps(item.comboItemId, set.setIndex, -1) }) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "减少次数",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "$reps 次",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { actions.onAdjustReps(item.comboItemId, set.setIndex, +1) }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "增加次数",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            checked -> Text(
                "已完成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            set.skipped -> Text(
                "已逃",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> Text(
                item.targetReps?.let { "目标 $it 次" } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 标逃/取消逃标入口（力竭跳组）：未打卡组可标逃，已逃组可取消恢复未练；
        // 已逃组直接点打卡圈即转为打卡（逃标消失）
        if (!checked) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (set.skipped) "取消逃标" else "标逃",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (set.skipped) {
                            actions.onCancelSetSkip(item.comboItemId, set.setIndex)
                        } else {
                            actions.onSkipSet(item.comboItemId, set.setIndex)
                        }
                    }
                    .semantics {
                        contentDescription = if (set.skipped) {
                            "取消第 ${set.setIndex + 1} 组逃标"
                        } else {
                            "标记第 ${set.setIndex + 1} 组逃组"
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (item.trackWeight && checked) {
            Spacer(Modifier.width(8.dp))
            // 每组自己的重量：独立于动作级当次重量，可单独设置/编辑/清除
            val weightText = set.weight
                ?.let { TimeUtil.formatWeight(it) + " " + unit }
                ?: "记重量"
            Text(
                text = weightText,
                style = MaterialTheme.typography.labelLarge,
                color = if (set.weight == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showWeightEditor = true }
                    .semantics { contentDescription = "编辑第 ${set.setIndex + 1} 组重量" }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    if (showWeightEditor) {
        SetWeightDialog(
            setTitle = set.setIndex + 1,
            initial = set.weight,
            unit = unit,
            onConfirm = { value ->
                actions.onEditSetWeight(item.comboItemId, set.setIndex, value)
                showWeightEditor = false
            },
            onDismiss = { showWeightEditor = false },
        )
    }
}

/** 动作备注输入：预填当天备注，下方灰字回显上次备注（无历史不显示）；只写/改，无清空入口。 */
@Composable
private fun ExerciseNoteField(
    todayNote: String?,
    lastNote: String?,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(todayNote) { mutableStateOf(todayNote ?: "") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("动作备注") },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text("保存") }
        }
        if (lastNote != null) {
            Text(
                text = "上次：$lastNote",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 单组重量精确输入：允许负数（辅助重量）与小数；清空并确定 = 清除该组重量（下次打卡重新按当次重量快照）。 */
@Composable
private fun SetWeightDialog(
    setTitle: Int,
    initial: Double?,
    unit: String,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial?.let { TimeUtil.formatWeight(it) } ?: "") }
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第 $setTitle 组重量（$unit）") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        // 允许：数字、小数点、负号（辅助重量）
                        if (input.isEmpty() || input.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                            text = input
                        }
                    },
                    label = { Text("正数负重 · 0 自重 · 负数辅助") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "只作用于这一组，不同组可设不同重量；留空并确定 = 清除该组重量。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed.toDoubleOrNull()) },
                enabled = trimmed.isEmpty() || trimmed.toDoubleOrNull() != null,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 组打卡圆圈：未打卡 = 2dp 描边圆显示组号，点击打卡；已打卡 = 实心圆对勾，点击取消。填色带颜色过渡动画。 */
@Composable
private fun SetCircle(
    checked: Boolean,
    number: Int,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "setCircleFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "setCircleBorder",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                contentDescription =
                    if (checked) "取消第 $number 组打卡" else "完成第 $number 组打卡"
            }
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = if (checked) onCancel else onCheckIn),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                number.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

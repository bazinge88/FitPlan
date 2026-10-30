package com.fitplan.app.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitTextAction
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset

/**
 * 训练记录：大标题 + 日期切换卡（前一天 / 日期 / 后一天 / 今天）+ 记录卡 + 添加 FAB。
 * 日期状态、条目增删改、删除二次确认与加载态口径均沿用旧版。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val vm: LogViewModel = viewModel(factory = LogViewModel.Factory)
    val date by vm.date.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    // 加载态契约：entries 初始为 null、数据到达后非 null；显式可空局部变量标明契约
    val entriesOrNull: List<LogEntryWithSets>? = entries

    var editingEntry by remember { mutableStateOf<LogEntryWithSets?>(null) }
    var pendingDelete by remember { mutableStateOf<LogEntryWithSets?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    val unit = if (settings.unitIsLb) "lb" else "kg"
    val dateState = remember(date) { TimeUtil.fromEpochDay(date) }

    // Tab 页：外层 MainScaffold 已提供系统栏/底部导航 padding，这里不再自建 Scaffold
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FitSpace.xl, end = FitSpace.xl, top = FitSpace.xl),
            ) {
                FitScreenTitle(title = "训练记录")
                Spacer(Modifier.height(FitSpace.md))
                FitCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = FitSpace.sm,
                        end = FitSpace.sm,
                        top = FitSpace.xs,
                        bottom = FitSpace.xs,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FitIconButton(
                            icon = Icons.Rounded.ChevronLeft,
                            contentDescription = "前一天",
                            onClick = { vm.setDate(date - 1) },
                        )
                        val dateLine = "${TimeUtil.formatDate(dateState)}（${TimeUtil.weekdayName(dateState)}）"
                        Text(
                            text = dateLine,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                // 整块日期文本是唯一可点目标：role=Button + 语义描述「选择日期」
                                .clickable(role = Role.Button) { pickingDate = true }
                                .padding(vertical = FitSpace.md)
                                .semantics { contentDescription = "选择日期：$dateLine" },
                        )
                        FitIconButton(
                            icon = Icons.Rounded.ChevronRight,
                            contentDescription = "后一天",
                            onClick = { vm.setDate(date + 1) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FitTextAction(
                            text = "今天",
                            onClick = { vm.setDate(TimeUtil.today().toEpochDay()) },
                        )
                    }
                }
            }
            when {
                // 冷启动数据未到：显示加载中，不先闪「这一天还没有记录」
                entriesOrNull == null -> FitEmptyState(
                    title = "加载中…",
                    modifier = Modifier.weight(1f),
                    icon = null,
                )

                entriesOrNull.isEmpty() -> FitEmptyState(
                    title = "这一天还没有记录",
                    description = "训练记录是可选的，用于留档和导出数据。",
                    modifier = Modifier.weight(1f),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = FitSpace.xl,
                        end = FitSpace.xl,
                        top = FitSpace.md,
                        bottom = FitSpace.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(FitSpace.md),
                ) {
                    // 用列表重载而不是 count+索引：删除记录时 key 提供者与 count 同源，避免旧索引访问越界
                    itemsIndexed(entriesOrNull, key = { _, withSets -> withSets.entry.id }) { index, withSets ->
                        val exercise = exercises.find { it.id == withSets.entry.exerciseId }
                        EntryCard(
                            withSets = withSets,
                            exercise = exercise,
                            unit = unit,
                            onEdit = { editingEntry = withSets },
                            // 删除不可撤销：先弹二次确认，确认后才真正删除
                            onDelete = { pendingDelete = withSets },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fitEnter(index),
                        )
                    }
                }
            }
            // 底部为悬浮 FAB 预留一条空白带（20dp 外边距 + 56dp FAB + 20dp 间隔 = 96dp）：
            // 记录卡内容止于该带之上，任意滚动位置都不会被按钮遮住
            Spacer(Modifier.height(FitSpace.xl + FitSize.fab + FitSpace.xl))
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("添加记录") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = FitSpace.xl, bottom = FitSpace.xl),
        )
    }

    if (showAdd) {
        EntryDialog(
            date = date,
            exercises = exercises,
            unit = unit,
            source = null,
            onDismiss = { showAdd = false },
            onSave = { exerciseId, note, sets ->
                vm.saveEntry(0, date, exerciseId, note, sets)
                showAdd = false
            },
        )
    }

    editingEntry?.let { source ->
        EntryDialog(
            date = date,
            exercises = exercises,
            unit = unit,
            source = source,
            onDismiss = { editingEntry = null },
            onSave = { exerciseId, note, sets ->
                vm.saveEntry(
                    source.entry.id,
                    source.entry.date,
                    exerciseId,
                    note,
                    sets,
                    originalSort = source.entry.sort,
                )
                editingEntry = null
            },
        )
    }

    pendingDelete?.let { target ->
        FitDialog(
            title = "删除训练记录",
            text = "确定删除这条训练记录？删除后不可恢复。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                vm.deleteEntry(target.entry)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date * 24 * 60 * 60 * 1000,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            vm.setDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay(),
                            )
                        }
                        pickingDate = false
                    },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun EntryCard(
    withSets: LogEntryWithSets,
    exercise: ExerciseEntity?,
    unit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FitCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = exercise?.name ?: "未知动作",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
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
                    text = { Text("编辑") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(FitSpace.xs)) {
            withSets.sets.sortedBy { it.sort }.forEachIndexed { index, s ->
                // 组摘要用 FlowRow：窄屏/大字体下多字段自动换行，不横向溢出
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FitSpace.md),
                    verticalArrangement = Arrangement.spacedBy(FitSpace.xxs),
                ) {
                    s.sets?.let {
                        Text("第 ${index + 1} 组 · $it 组", style = MaterialTheme.typography.bodyMedium)
                    }
                    s.reps?.let { Text("$it 次", style = MaterialTheme.typography.bodyMedium) }
                    s.weight?.let {
                        Text(
                            "${TimeUtil.formatWeight(it)} $unit",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    s.duration?.let { Text("$it 分钟", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (withSets.entry.note.isNotBlank()) {
                Text(
                    "备注：${withSets.entry.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

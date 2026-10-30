package com.fitplan.app.ui.log

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.ui.EmptyHint
import com.fitplan.app.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val vm: LogViewModel = viewModel(factory = LogViewModel.Factory)
    val date by vm.date.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()

    var editingEntry by remember { mutableStateOf<LogEntryWithSets?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    val unit = if (settings.unitIsLb) "lb" else "kg"
    val dateState = remember(date) { TimeUtil.fromEpochDay(date) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            // 日期切换行与 TopAppBar 整合，紧凑布局
            Column {
                TopAppBar(
                    title = { Text("训练记录") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    actions = {
                        TextButton(onClick = { vm.setDate(TimeUtil.today().toEpochDay()) }) {
                            Text("今天")
                        }
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row {
                        TextButton(onClick = { vm.setDate(date - 1) }) { Text("‹ 前一天") }
                        TextButton(onClick = { vm.setDate(date + 1) }) { Text("后一天 ›") }
                    }
                    TextButton(onClick = { pickingDate = true }) {
                        Text(
                            "${TimeUtil.formatDate(dateState)}（${TimeUtil.weekdayName(dateState)}）",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加记录")
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyHint(
                "这一天还没有记录。训练记录是可选的，用于留档和导出数据。",
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries.size, key = { entries[it].entry.id }) { index ->
                    val withSets = entries[index]
                    val exercise = exercises.find { it.id == withSets.entry.exerciseId }
                    EntryCard(
                        withSets = withSets,
                        exercise = exercise,
                        unit = unit,
                        onEdit = { editingEntry = withSets },
                        onDelete = { vm.deleteEntry(withSets.entry) },
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise?.name ?: "未知动作",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
            withSets.sets.sortedBy { it.sort }.forEachIndexed { index, s ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    s.sets?.let { Text("第 ${index + 1} 组 · $it 组", style = MaterialTheme.typography.bodyMedium) }
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

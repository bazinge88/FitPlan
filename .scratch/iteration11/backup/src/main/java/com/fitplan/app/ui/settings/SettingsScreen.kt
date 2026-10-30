package com.fitplan.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.ui.ConfirmDialog
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showClearFirst by remember { mutableStateOf(false) }
    var showClearSecond by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val users by vm.users.collectAsStateWithLifecycle()
    var showAddUser by remember { mutableStateOf(false) }
    var pendingDeleteUser by remember { mutableStateOf<UserEntity?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri: Uri? ->
        val content = pendingExport
        if (uri != null && content != null) {
            scope.launch {
                writeMarkdown(context, uri, content)
                pendingExport = null
            }
        } else {
            pendingExport = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        SectionTitle("重量单位")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !settings.unitIsLb,
                onClick = { vm.setUnit(false) },
            )
            Text("kg（默认）", modifier = Modifier.padding(start = 4.dp))
            RadioButton(
                selected = settings.unitIsLb,
                onClick = { vm.setUnit(true) },
                modifier = Modifier.padding(start = 16.dp),
            )
            Text("lb", modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            "仅影响界面显示习惯；重量由你自行输入，负数表示辅助重量。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()
        SectionTitle("默认记录字段（新建动作时预启用）")
        var sets by remember(settings.defTrackSets) { mutableStateOf(settings.defTrackSets) }
        var reps by remember(settings.defTrackReps) { mutableStateOf(settings.defTrackReps) }
        var weight by remember(settings.defTrackWeight) { mutableStateOf(settings.defTrackWeight) }
        var duration by remember(settings.defTrackDuration) { mutableStateOf(settings.defTrackDuration) }
        FieldCheck("组数", sets) { v -> sets = v; vm.setDefaultFields(v, reps, weight, duration) }
        FieldCheck("次数", reps) { v -> reps = v; vm.setDefaultFields(sets, v, weight, duration) }
        FieldCheck("重量", weight) { v -> weight = v; vm.setDefaultFields(sets, reps, v, duration) }
        FieldCheck("时长", duration) { v -> duration = v; vm.setDefaultFields(sets, reps, weight, v) }

        HorizontalDivider()
        SectionTitle("训练者")
        Text(
            "打卡与 PR 记录按训练者分开统计，互不覆盖。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        users.forEach { user ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.activeUserId == user.id,
                    onClick = { vm.selectUser(user.id) },
                )
                Text(user.name, modifier = Modifier.weight(1f))
                if (users.size > 1) {
                    TextButton(onClick = { pendingDeleteUser = user }) { Text("删除") }
                }
            }
        }
        TextButton(onClick = { showAddUser = true }) { Text("添加训练者") }

        HorizontalDivider()
        SectionTitle("导出 Markdown")
        Text(
            "导出设置、动作库、训练组合、周计划与训练记录，格式稳定、结构化，方便 AI 分析。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { showExportDialog = true }) { Text("导出 Markdown…") }

        HorizontalDivider()
        SectionTitle("数据")
        OutlinedButton(onClick = { showClearFirst = true }) { Text("清空所有数据") }

        HorizontalDivider()
        SectionTitle("关于")
        Text("健身计划 FitPlan v1.0.0", style = MaterialTheme.typography.bodyMedium)
        Text(
            "本地、离线使用的健身周计划与记录应用。核心是安排训练计划：动作库 → 训练组合 → 周计划（周一起始）→ 可选训练记录 → Markdown 导出。无联网、无账号、无统计图表。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddUser) {
        AddUserDialog(
            onConfirm = { name ->
                showAddUser = false
                vm.addUser(name)
            },
            onDismiss = { showAddUser = false },
        )
    }
    if (pendingDeleteUser != null) {
        ConfirmDialog(
            title = "删除训练者",
            text = "将删除「${pendingDeleteUser!!.name}」的全部打卡与当次重量记录，PR 一并消失。此操作不可撤销，确定继续？",
            confirmLabel = "删除",
            onConfirm = {
                vm.deleteUser(pendingDeleteUser!!)
                pendingDeleteUser = null
            },
            onDismiss = { pendingDeleteUser = null },
        )
    }
    if (showExportDialog) {
        ExportScopeDialog(
            onConfirm = { exportScope ->
                showExportDialog = false
                scope.launch {
                    val markdown = vm.buildMarkdown(exportScope)
                    pendingExport = markdown
                    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(java.time.LocalDateTime.now())
                    exportLauncher.launch("fitplan-export-$stamp.md")
                }
            },
            onDismiss = { showExportDialog = false },
        )
    }

    if (showClearFirst) {
        ConfirmDialog(
            title = "清空所有数据",
            text = "将删除全部动作、训练组合、周计划与训练记录（设置保留）。此操作不可撤销，确定继续？",
            confirmLabel = "继续",
            onConfirm = {
                showClearFirst = false
                showClearSecond = true
            },
            onDismiss = { showClearFirst = false },
        )
    }
    if (showClearSecond) {
        ConfirmDialog(
            title = "二次确认",
            text = "再次确认：真的要清空所有数据吗？删除后无法恢复。",
            confirmLabel = "确认清空",
            onConfirm = {
                showClearSecond = false
                vm.clearAllData {}
            },
            onDismiss = { showClearSecond = false },
        )
    }
}

private suspend fun writeMarkdown(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(content.toByteArray(Charsets.UTF_8))
    }
}

/** 添加训练者对话框：名称非空才可确认。 */
@Composable
private fun AddUserDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加训练者") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun FieldCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 导出范围选择：全部 / 指定周 / 日期范围。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScopeDialog(
    onConfirm: (ExportScope) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(0) } // 0 全部 1 指定周 2 日期范围
    var weekPicker by remember { mutableStateOf(false) }
    var fromPicker by remember { mutableStateOf(false) }
    var toPicker by remember { mutableStateOf(false) }
    var weekMonday by remember { mutableStateOf(TimeUtil.mondayOf(TimeUtil.today()).toEpochDay()) }
    var fromDate by remember { mutableStateOf(TimeUtil.today().toEpochDay()) }
    var toDate by remember { mutableStateOf(TimeUtil.today().toEpochDay()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == 0, onClick = { kind = 0 })
                    Text("全部")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == 1, onClick = { kind = 1 })
                    TextButton(onClick = { weekPicker = true }) {
                        Text(
                            "指定周：" +
                                TimeUtil.formatDate(TimeUtil.fromEpochDay(weekMonday)) + " 起",
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == 2, onClick = { kind = 2 })
                    Column {
                        TextButton(onClick = { fromPicker = true }) {
                            Text("从 ${TimeUtil.formatDate(TimeUtil.fromEpochDay(fromDate))}")
                        }
                        TextButton(onClick = { toPicker = true }) {
                            Text("到 ${TimeUtil.formatDate(TimeUtil.fromEpochDay(toDate))}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    when (kind) {
                        1 -> ExportScope.Week(weekMonday)
                        2 -> ExportScope.Range(
                            minOf(fromDate, toDate),
                            maxOf(fromDate, toDate),
                        )
                        else -> ExportScope.All
                    },
                )
            }) { Text("导出") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (weekPicker) {
        SimpleDatePicker(
            title = "选择该周中的任意一天",
            initialEpochDay = weekMonday,
            onPick = { day ->
                weekPicker = false
                weekMonday = TimeUtil.mondayOfEpochDay(day)
            },
            onClose = { weekPicker = false },
        )
    }
    if (fromPicker) {
        SimpleDatePicker(
            title = "开始日期",
            initialEpochDay = fromDate,
            onPick = { day ->
                fromPicker = false
                fromDate = day
            },
            onClose = { fromPicker = false },
        )
    }
    if (toPicker) {
        SimpleDatePicker(
            title = "结束日期",
            initialEpochDay = toDate,
            onPick = { day ->
                toPicker = false
                toDate = day
            },
            onClose = { toPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePicker(
    title: String,
    initialEpochDay: Long,
    onPick: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay * 24 * 60 * 60 * 1000,
    )
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    }
                },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    ) {
        DatePicker(state = state, title = { Text(title, Modifier.padding(16.dp)) })
    }
}

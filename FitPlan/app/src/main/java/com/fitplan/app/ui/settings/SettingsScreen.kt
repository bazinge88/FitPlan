package com.fitplan.app.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.BuildConfig
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitHairline
import com.fitplan.app.ui.design.FitListRow
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitSectionHeader
import com.fitplan.app.ui.design.FitTextAction
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置：大标题 + 分组列表（训练者 / 重量单位 / 默认记录字段 / 导出 / 数据 / 关于）。
 * 增删切换训练者、单位、默认字段、导出 SAF 与清空数据的流程/文案全部沿用旧版；
 * 危险操作使用 Destructive 样式，行内开关整行可点。
 */
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
                // 写入失败（openOutputStream 为 null 或抛异常）给出可读反馈；取消 SAF 不提示
                val ok = writeMarkdown(context, uri, content)
                pendingExport = null
                Toast.makeText(
                    context,
                    if (ok) "已导出 Markdown" else "导出失败，请重试",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            // 用户取消 SAF：静默，不打扰
            pendingExport = null
        }
    }

    // Tab 页：外层 MainScaffold 已提供系统栏/底部导航 padding，这里不再自建 Scaffold
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
            FitScreenTitle(
                title = "设置",
                modifier = Modifier.padding(start = FitSpace.xl, end = FitSpace.xl, top = FitSpace.xl),
            )

            // 训练者
            FitSectionHeader(
                title = "训练者",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            Text(
                "打卡与 PR 记录按训练者分开统计，互不覆盖。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            Spacer(Modifier.height(FitSpace.sm))
            FitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
                contentPadding = PaddingValues(),
            ) {
                users.forEachIndexed { index, user ->
                    // 整行 selectable(Role.RadioButton) 是唯一交互目标，RadioButton 仅作展示（onClick=null）
                    RadioRow(
                        title = user.name,
                        selected = settings.activeUserId == user.id,
                        supporting = if (settings.activeUserId == user.id) "当前训练者" else null,
                        onSelect = { vm.selectUser(user.id) },
                        trailing = if (users.size > 1) {
                            {
                                FitTextAction(
                                    text = "删除",
                                    onClick = { pendingDeleteUser = user },
                                    destructive = true,
                                )
                            }
                        } else {
                            null
                        },
                    )
                    if (index < users.lastIndex) {
                        FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                    }
                }
                if (users.isNotEmpty()) FitHairline()
                FitListRow(
                    title = "添加训练者",
                    leading = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                    onClick = { showAddUser = true },
                )
            }

            // 重量单位
            FitSectionHeader(
                title = "重量单位",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            FitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
                contentPadding = PaddingValues(),
            ) {
                RadioRow(
                    title = "kg（默认）",
                    selected = !settings.unitIsLb,
                    onSelect = { vm.setUnit(false) },
                )
                FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                RadioRow(
                    title = "lb",
                    selected = settings.unitIsLb,
                    onSelect = { vm.setUnit(true) },
                )
            }
            Text(
                "仅影响界面显示习惯；重量由你自行输入，负数表示辅助重量。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FitSpace.xl, vertical = FitSpace.sm),
            )

            // 默认记录字段
            FitSectionHeader(
                title = "默认记录字段（新建动作时预启用）",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            var sets by remember(settings.defTrackSets) { mutableStateOf(settings.defTrackSets) }
            var reps by remember(settings.defTrackReps) { mutableStateOf(settings.defTrackReps) }
            var weight by remember(settings.defTrackWeight) { mutableStateOf(settings.defTrackWeight) }
            var duration by remember(settings.defTrackDuration) { mutableStateOf(settings.defTrackDuration) }
            FitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
                contentPadding = PaddingValues(),
            ) {
                FieldSwitchRow("组数", sets) { v -> sets = v; vm.setDefaultFields(v, reps, weight, duration) }
                FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                FieldSwitchRow("次数", reps) { v -> reps = v; vm.setDefaultFields(sets, v, weight, duration) }
                FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                FieldSwitchRow("重量", weight) { v -> weight = v; vm.setDefaultFields(sets, reps, v, duration) }
                FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                FieldSwitchRow("时长", duration) { v -> duration = v; vm.setDefaultFields(sets, reps, weight, v) }
            }

            // 导出
            FitSectionHeader(
                title = "导出 Markdown",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            Text(
                "导出设置、动作库、训练组合、周计划与训练记录，格式稳定、结构化，方便 AI 分析。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            Spacer(Modifier.height(FitSpace.md))
            FitButton(
                text = "导出 Markdown…",
                onClick = { showExportDialog = true },
                style = FitButtonStyle.Tonal,
                leadingIcon = Icons.Rounded.FileDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
            )

            // 数据
            FitSectionHeader(
                title = "数据",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            FitButton(
                text = "清空所有数据",
                onClick = { showClearFirst = true },
                style = FitButtonStyle.Destructive,
                leadingIcon = Icons.Rounded.DeleteSweep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
            )

            // 关于
            FitSectionHeader(
                title = "关于",
                modifier = Modifier.padding(horizontal = FitSpace.xl),
            )
            FitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSpace.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(FitSpace.sm)) {
                    // 版本号取自构建产物（versionName），不再硬编码
                    Text(
                        "健身计划 FitPlan v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "本地、离线使用的健身周计划与记录应用。核心是安排训练计划：动作库 → 训练组合 → 周计划（周一起始）→ 可选训练记录 → Markdown 导出。无联网、无账号、无统计图表。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        Spacer(Modifier.height(FitSpace.huge))
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
        FitDialog(
            title = "删除训练者",
            text = "将删除「${pendingDeleteUser!!.name}」的全部打卡与当次重量记录，PR 一并消失。此操作不可撤销，确定继续？",
            confirmLabel = "删除",
            destructive = true,
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
        FitDialog(
            title = "清空所有数据",
            text = "将删除全部动作、训练组合、周计划与训练记录（设置保留）。此操作不可撤销，确定继续？",
            confirmLabel = "继续",
            destructive = true,
            onConfirm = {
                showClearFirst = false
                showClearSecond = true
            },
            onDismiss = { showClearFirst = false },
        )
    }
    if (showClearSecond) {
        FitDialog(
            title = "二次确认",
            text = "再次确认：真的要清空所有数据吗？删除后无法恢复。",
            confirmLabel = "确认清空",
            destructive = true,
            onConfirm = {
                showClearSecond = false
                vm.clearAllData {
                    Toast.makeText(context, "已清空所有数据", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearSecond = false },
        )
    }
}

/** 写入 SAF 目标文件：成功返回 true；openOutputStream 为 null 或写入抛异常返回 false（不崩溃）。 */
private suspend fun writeMarkdown(context: Context, uri: Uri, content: String): Boolean = try {
    // 导出大文件时把实际写盘放到 IO 线程，避免主线程卡顿
    withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri)
        if (stream == null) {
            false
        } else {
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            true
        }
    }
} catch (e: Exception) {
    false
}

/** 添加训练者对话框：名称非空才可确认。 */
@Composable
private fun AddUserDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    FitDialog(
        title = "添加训练者",
        onDismiss = onDismiss,
        confirmLabel = "添加",
        confirmEnabled = name.trim().isNotEmpty(),
        onConfirm = { onConfirm(name) },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("名称") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 单选行：整行 selectable(selected, role=Role.RadioButton) 是唯一交互目标，
 * 左侧 RadioButton 仅作展示（onClick = null），读屏只播报一个可点目标与选中态。
 */
@Composable
private fun RadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.rowMin)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = FitSpace.lg, vertical = FitSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(FitSpace.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supporting != null) {
                Spacer(Modifier.height(FitSpace.xxs))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(FitSpace.md))
            trailing()
        }
    }
}

/** 整行可切换的开关行：toggleable + Role.Switch，触摸目标 ≥48dp。 */
@Composable
private fun FieldSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.rowMin)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = FitSpace.xl, vertical = FitSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 导出范围选择：全部 / 指定周 / 日期范围。 */
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

    FitDialog(
        title = "导出范围",
        onDismiss = onDismiss,
        confirmLabel = "导出",
        onConfirm = {
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
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitSpace.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FitSize.touchMin)
                    .selectable(selected = kind == 0, role = Role.RadioButton) { kind = 0 },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 展示用 RadioButton：交互由整行 selectable(Role.RadioButton) 承担
                RadioButton(selected = kind == 0, onClick = null)
                Text("全部")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FitSize.touchMin)
                    .selectable(selected = kind == 1, role = Role.RadioButton) { kind = 1 },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 展示用 RadioButton：交互由整行 selectable(Role.RadioButton) 承担
                RadioButton(selected = kind == 1, onClick = null)
                TextButton(onClick = { weekPicker = true }) {
                    Text(
                        "指定周：" +
                            TimeUtil.formatDate(TimeUtil.fromEpochDay(weekMonday)) + " 起",
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FitSize.touchMin)
                    .selectable(selected = kind == 2, role = Role.RadioButton) { kind = 2 },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 展示用 RadioButton：交互由整行 selectable(Role.RadioButton) 承担
                RadioButton(selected = kind == 2, onClick = null)
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
    }

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
    val state = rememberDatePickerState(
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
        DatePicker(state = state, title = { Text(title, Modifier.padding(FitSpace.lg)) })
    }
}

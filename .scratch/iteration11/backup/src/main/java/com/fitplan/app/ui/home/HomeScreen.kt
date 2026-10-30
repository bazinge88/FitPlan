package com.fitplan.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.Graph
import com.fitplan.app.ui.EmptyHint
import com.fitplan.app.ui.plan.WorkoutChecklist
import com.fitplan.app.ui.plan.WorkoutViewModel
import com.fitplan.app.ui.plan.workoutActions
import com.fitplan.app.util.TimeUtil

/** 主页：今日概览，训练日可直接逐组打卡。 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 以当天日期作 key，跨天回到前台时重建 ViewModel 以切换到新的一天
    val today = TimeUtil.today().toEpochDay()
    val vm: WorkoutViewModel = viewModel(
        key = "home-$today",
        factory = WorkoutViewModel.factory(today),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val activeDate by Graph.sessionManager.activeDate.collectAsStateWithLifecycle()
    val dateText = TimeUtil.formatDate(TimeUtil.fromEpochDay(today)) +
        " · " + TimeUtil.weekdayName(TimeUtil.fromEpochDay(today))

    val context = LocalContext.current
    // 权限 launcher 必须在组合层级提前创建，不能放进点击回调
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            Graph.sessionManager.start(today)
        } else {
            Toast.makeText(context, "未授予通知权限，无法挂出训练通知", Toast.LENGTH_SHORT).show()
        }
    }
    // 开始训练：已授通知权限直接开始，否则走系统弹窗（拒绝则不开始）
    val startWithPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Graph.sessionManager.start(today)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 不能加 verticalScroll：下方的 WorkoutChecklist 自带同向 LazyColumn，嵌套会因高度无界而崩溃
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            dateText,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            when {
                state.loading -> "加载中…"
                state.comboName == null -> "今天没有安排训练"
                else -> "今日计划：${state.comboName}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        when {
            state.loading -> EmptyHint("加载中…", Modifier.weight(1f))

            state.comboName == null -> Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("今天休息", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "今天没有安排训练，可到「计划」页查看或安排",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    // 休息日同样可记当天体重：入口只在主页，可选记录不催记
                    BodyWeightRow(
                        weight = state.bodyWeight,
                        unitIsLb = state.unitIsLb,
                        onRecord = vm::recordBodyWeight,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    DayNoteRow(
                        note = state.dayNote,
                        onRecord = vm::recordDayNote,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // weight(1f) 移到外层包装 Column 上，WorkoutChecklist 自身不再带 weight
            else -> Column(Modifier.weight(1f)) {
                TextButton(
                    onClick = {
                        if (activeDate == today) {
                            Graph.sessionManager.stop()
                        } else {
                            startWithPermission()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (activeDate == today) "结束训练" else "开始训练")
                }
                // 训练日清单上方：当天体重录入/修改入口（可选记录，不催记）
                BodyWeightRow(
                    weight = state.bodyWeight,
                    unitIsLb = state.unitIsLb,
                    onRecord = vm::recordBodyWeight,
                )
                DayNoteRow(
                    note = state.dayNote,
                    onRecord = vm::recordDayNote,
                )
                WorkoutChecklist(
                    state = state,
                    actions = remember(vm) { workoutActions(vm) },
                )
            }
        }
    }
}

/** 当天体重行：显示当前值（未记录则提示），「记录/修改」弹精确输入；一天一条，重复输入覆盖。 */
@Composable
private fun BodyWeightRow(
    weight: Double?,
    unitIsLb: Boolean,
    onRecord: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 对话框开关为瞬态状态，随离开主页丢弃即可，不参与状态恢复
    var showEditor by remember { mutableStateOf(false) }
    val unit = if (unitIsLb) "lb" else "kg"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("体重", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = weight?.let { "今天 ${TimeUtil.formatWeight(it)} $unit" } ?: "今天未记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { showEditor = true }) {
            Text(if (weight == null) "记录" else "修改")
        }
    }
    if (showEditor) {
        BodyWeightDialog(
            initial = weight,
            unit = unit,
            onConfirm = { value ->
                onRecord(value)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

/** 体重输入：库内原值口径（lb 只换标签不换算数值）；确定即覆盖当天旧值。 */
@Composable
private fun BodyWeightDialog(
    initial: Double?,
    unit: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial?.let { TimeUtil.formatWeight(it) } ?: "") }
    val value = text.trim().toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("今天体重（$unit）") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    // 与 ui/Common.kt NumberField 的差别：体重无负数语义，不接受负号（负号留给辅助重量）
                    if (input.isEmpty() || input.matches(Regex("\\d*(\\.\\d*)?"))) {
                        text = input
                    }
                },
                label = { Text("体重数值") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value!!) },
                enabled = value != null,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 当日备注行：显示当前内容（未写则提示），「记录/修改」弹多行输入；空文本确定 = 清除（删行）。 */
@Composable
private fun DayNoteRow(
    note: String?,
    onRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 对话框开关为瞬态状态，随离开主页丢弃即可，不参与状态恢复
    var showEditor by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("备注", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (note.isNullOrBlank()) "未写" else note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = { showEditor = true }) {
            Text(if (note.isNullOrBlank()) "记录" else "修改")
        }
    }
    if (showEditor) {
        DayNoteDialog(
            initial = note,
            onConfirm = { value ->
                onRecord(value)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

/** 当日备注输入：多行文本；确定即覆盖，空文本 = 清除（当天备注删行）。 */
@Composable
private fun DayNoteDialog(
    initial: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("当日备注") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("备注内容") },
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

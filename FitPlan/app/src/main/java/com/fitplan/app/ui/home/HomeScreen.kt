package com.fitplan.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.Graph
import com.fitplan.app.ui.design.FitBrandMark
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitListRow
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitSectionCard
import com.fitplan.app.ui.design.FitTextAction
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.plan.WorkoutActions
import com.fitplan.app.ui.plan.WorkoutChecklist
import com.fitplan.app.ui.plan.WorkoutProgressHero
import com.fitplan.app.ui.plan.WorkoutUiState
import com.fitplan.app.ui.plan.WorkoutViewModel
import com.fitplan.app.ui.plan.fitDateLine
import com.fitplan.app.ui.plan.workoutActions
import com.fitplan.app.ui.plan.workoutProgress
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil

/** 主页：今日概览，训练日可直接逐组打卡；休息日可一键切到「计划」页安排。 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenPlan: () -> Unit = {},
) {
    // 以当天日期作 key，跨天回到前台时重建 ViewModel 以切换到新的一天
    var today by remember { mutableStateOf(TimeUtil.today().toEpochDay()) }
    // 隔夜常驻后台再回前台：重算今天，避免日期/清单/「开始训练」仍指向昨天
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = TimeUtil.today().toEpochDay()
    }
    val vm: WorkoutViewModel = viewModel(
        key = "home-$today",
        factory = WorkoutViewModel.factory(today),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val activeDate by Graph.sessionManager.activeDate.collectAsStateWithLifecycle()
    val dateText = fitDateLine(today)

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
    val actions: WorkoutActions = remember(vm) { workoutActions(vm) }
    // 会话进行中：按钮切换为「结束训练」（Tonal + 停止图标）
    val sessionActive = activeDate == today

    // 不能加 verticalScroll：训练日分支下方的 WorkoutChecklist 自带同向 LazyColumn，嵌套会因高度无界而崩溃
    Column(modifier = modifier.fillMaxSize()) {
        FitScreenTitle(
            title = "今天",
            eyebrow = dateText,
            subtitle = when {
                state.loading -> "加载中…"
                state.comboName == null -> null
                else -> "今日计划：${state.comboName}"
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSize.gutter)
                .padding(top = FitSpace.lg)
                .fitEnter(0),
        )
        when {
            state.loading -> FitEmptyState(
                title = "加载中…",
                modifier = Modifier.weight(1f),
                icon = null,
            )

            state.comboName == null -> RestDayContent(
                state = state,
                onOpenPlan = onOpenPlan,
                onRecordWeight = vm::recordBodyWeight,
                onRecordNote = vm::recordDayNote,
                modifier = Modifier.weight(1f),
            )

            // 空组合（有组合但没有任何动作）：隐藏开始训练，给出编辑引导而非空白清单
            state.items.isEmpty() -> EmptyComboContent(
                state = state,
                onRecordWeight = vm::recordBodyWeight,
                onRecordNote = vm::recordDayNote,
                modifier = Modifier.weight(1f),
            )

            else -> TrainingDayContent(
                state = state,
                actions = actions,
                sessionActive = sessionActive,
                onToggleSession = {
                    if (sessionActive) {
                        Graph.sessionManager.stop()
                    } else {
                        startWithPermission()
                    }
                },
                onRecordWeight = vm::recordBodyWeight,
                onRecordNote = vm::recordDayNote,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 训练日：开始/结束训练主次按钮 + 进度 hero + 今日记录 + 共用清单。 */
@Composable
private fun TrainingDayContent(
    state: WorkoutUiState,
    actions: WorkoutActions,
    sessionActive: Boolean,
    onToggleSession: () -> Unit,
    onRecordWeight: (Double) -> Unit,
    onRecordNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = workoutProgress(state.items)
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSize.gutter)
                .padding(top = FitSpace.lg),
        ) {
            FitButton(
                text = if (sessionActive) "结束训练" else "开始训练",
                onClick = onToggleSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .fitEnter(1),
                style = if (sessionActive) FitButtonStyle.Tonal else FitButtonStyle.Primary,
                leadingIcon = if (sessionActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            )
            Spacer(Modifier.height(FitSpace.lg))
            WorkoutProgressHero(progress = progress, modifier = Modifier.fitEnter(2))
            Spacer(Modifier.height(FitSpace.xxl))
            TodayRecordsCard(
                bodyWeight = state.bodyWeight,
                unitIsLb = state.unitIsLb,
                dayNote = state.dayNote,
                onRecordWeight = onRecordWeight,
                onRecordNote = onRecordNote,
                modifier = Modifier.fitEnter(3),
            )
            Spacer(Modifier.height(FitSpace.md))
        }
        WorkoutChecklist(state = state, actions = actions, modifier = Modifier.weight(1f))
    }
}

/** 休息日：品牌标记 + 今天休息 + 说明 + 今日记录 + 去「计划」页安排（可滚动，避免大字体溢出）。 */
@Composable
private fun RestDayContent(
    state: WorkoutUiState,
    onOpenPlan: () -> Unit,
    onRecordWeight: (Double) -> Unit,
    onRecordNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FitSize.gutter)
            .padding(top = FitSpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FitBrandMark(
            modifier = Modifier.size(FitSpace.huge),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(FitSpace.md))
        Text(
            "今天休息",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(FitSpace.xs))
        Text(
            "今天没有安排训练，可到「计划」页查看或安排",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FitSpace.xxl))
        TodayRecordsCard(
            bodyWeight = state.bodyWeight,
            unitIsLb = state.unitIsLb,
            dayNote = state.dayNote,
            onRecordWeight = onRecordWeight,
            onRecordNote = onRecordNote,
        )
        Spacer(Modifier.height(FitSpace.xl))
        // 休息日一键去「计划」页排训练（切换底部 Tab）
        FitButton(
            text = "去「计划」页安排",
            onClick = onOpenPlan,
            modifier = Modifier.fillMaxWidth(),
            style = FitButtonStyle.Primary,
        )
        Spacer(Modifier.height(FitSpace.huge))
    }
}

/** 空组合：隐藏开始训练，保留今日记录与编辑引导。 */
@Composable
private fun EmptyComboContent(
    state: WorkoutUiState,
    onRecordWeight: (Double) -> Unit,
    onRecordNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSize.gutter)
                .padding(top = FitSpace.lg),
        ) {
            TodayRecordsCard(
                bodyWeight = state.bodyWeight,
                unitIsLb = state.unitIsLb,
                dayNote = state.dayNote,
                onRecordWeight = onRecordWeight,
                onRecordNote = onRecordNote,
                modifier = Modifier.fitEnter(1),
            )
        }
        FitEmptyState(
            title = "今日计划为空",
            description = "这个组合还没有动作，请到「组合」页添加。",
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 今日记录卡：当天体重与当日备注的紧凑入口。
 * 体重 lb 只换标签不换算（库内原值）；备注空文本确定 = 清除（由 ViewModel 处理）。
 */
@Composable
private fun TodayRecordsCard(
    bodyWeight: Double?,
    unitIsLb: Boolean,
    dayNote: String?,
    onRecordWeight: (Double) -> Unit,
    onRecordNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 对话框开关为瞬态状态，随离开主页丢弃即可，不参与状态恢复
    var showWeightEditor by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    val unit = if (unitIsLb) "lb" else "kg"

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "今日记录",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = FitSpace.xs, bottom = FitSpace.sm),
        )
        FitSectionCard(modifier = Modifier.fillMaxWidth()) {
            FitListRow(
                title = "体重",
                supporting = bodyWeight?.let { "今天 ${TimeUtil.formatWeight(it)} $unit" } ?: "今天未记录",
                trailing = {
                    FitTextAction(
                        text = if (bodyWeight == null) "记录" else "修改",
                        onClick = { showWeightEditor = true },
                    )
                },
            )
            FitListRow(
                title = "备注",
                supporting = if (dayNote.isNullOrBlank()) "未写" else dayNote,
                trailing = {
                    FitTextAction(
                        text = if (dayNote.isNullOrBlank()) "记录" else "修改",
                        onClick = { showNoteEditor = true },
                    )
                },
            )
        }
    }

    if (showWeightEditor) {
        BodyWeightDialog(
            initial = bodyWeight,
            unit = unit,
            onConfirm = { value ->
                onRecordWeight(value)
                showWeightEditor = false
            },
            onDismiss = { showWeightEditor = false },
        )
    }
    if (showNoteEditor) {
        DayNoteDialog(
            initial = dayNote,
            onConfirm = { value ->
                onRecordNote(value)
                showNoteEditor = false
            },
            onDismiss = { showNoteEditor = false },
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

    FitDialog(
        title = "今天体重（$unit）",
        onDismiss = onDismiss,
        confirmLabel = "确定",
        confirmEnabled = value != null,
        onConfirm = { value?.let(onConfirm) },
        content = {
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
    )
}

/** 当日备注输入：多行文本；确定即覆盖，空文本 = 清除（当天备注删行）。 */
@Composable
private fun DayNoteDialog(
    initial: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial ?: "") }

    FitDialog(
        title = "当日备注",
        onDismiss = onDismiss,
        confirmLabel = "确定",
        onConfirm = { onConfirm(text.trim()) },
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("备注内容") },
                minLines = 2,
            )
        },
    )
}

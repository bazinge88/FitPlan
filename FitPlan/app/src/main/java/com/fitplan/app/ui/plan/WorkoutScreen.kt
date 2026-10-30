package com.fitplan.app.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.ui.design.AnimatedCount
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitHairline
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitLinearProgress
import com.fitplan.app.ui.design.FitRingProgress
import com.fitplan.app.ui.design.FitScaffold
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitTextAction
import com.fitplan.app.ui.design.FitTopBar
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.design.rememberFitHaptics
import com.fitplan.app.ui.setsSummary
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.theme.fitColors
import com.fitplan.app.ui.theme.fitMotion
import com.fitplan.app.util.TimeUtil

/** 日期 eyebrow（月日 + 周几）：主页与训练详情共用，避免各页日期口径不一致。 */
internal fun fitDateLine(epochDay: Long): String {
    val date = TimeUtil.fromEpochDay(epochDay)
    return "${date.monthValue}月${date.dayOfMonth}日 · ${TimeUtil.weekdayName(date)}"
}

/** 训练详情页：某天的组合动作清单式打卡。 */
@Composable
fun WorkoutScreen(date: Long, onBack: () -> Unit) {
    val vm: WorkoutViewModel = viewModel(
        key = "workout-$date",
        factory = WorkoutViewModel.factory(date),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val dateText = fitDateLine(date)
    val actions = remember(vm) { workoutActions(vm) }

    FitScaffold(
        topBar = { FitTopBar(title = "训练详情", onBack = onBack) },
    ) { padding ->
        when {
            state.loading -> FitEmptyState(
                title = "加载中…",
                modifier = Modifier.fillMaxSize().padding(padding),
                icon = null,
            )

            // 无计划（休息日/未安排）与空组合（组合存在但没有动作）共用居中提示 + 返回按钮。
            state.comboName == null || state.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FitEmptyState(
                    title = if (state.comboName == null) {
                        "今天没有安排训练"
                    } else {
                        "这个组合还没有动作，请到「组合」页添加"
                    },
                    modifier = Modifier.weight(1f),
                )
                FitButton(
                    text = "返回",
                    onClick = onBack,
                    modifier = Modifier.padding(bottom = FitSpace.lg),
                    style = FitButtonStyle.Ghost,
                )
            }

            else -> WorkoutDetailContent(
                state = state,
                actions = actions,
                dateText = dateText,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** 详情页正文：大标题（组合名 + 日期 eyebrow）+ 进度 hero + 共用清单。 */
@Composable
private fun WorkoutDetailContent(
    state: WorkoutUiState,
    actions: WorkoutActions,
    dateText: String,
    modifier: Modifier = Modifier,
) {
    val progress = workoutProgress(state.items)
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FitSize.gutter)
                .padding(top = FitSpace.lg),
        ) {
            FitScreenTitle(
                title = state.comboName ?: "训练详情",
                eyebrow = dateText,
                modifier = Modifier.fitEnter(0),
            )
            Spacer(Modifier.height(FitSpace.lg))
            WorkoutProgressHero(progress = progress, modifier = Modifier.fitEnter(1))
        }
        Spacer(Modifier.height(FitSpace.md))
        WorkoutChecklist(state = state, actions = actions, modifier = Modifier.weight(1f))
    }
}

/**
 * 今日/当日训练进度 hero：环形进度 + 已完成组数。主页与训练详情共用，
 * 进度数字用 AnimatedCount、环用 spring gentle（组件内置），减少动态时由组件降级。
 */
@Composable
fun WorkoutProgressHero(
    progress: WorkoutProgress,
    modifier: Modifier = Modifier,
) {
    val ratio = if (progress.totalSets > 0) {
        (progress.doneSets.toFloat() / progress.totalSets).coerceIn(0f, 1f)
    } else {
        0f
    }
    FitCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FitRingProgress(
                progress = ratio,
                size = FitSize.ring,
                strokeWidth = FitSize.ringStroke,
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedCount(
                        value = progress.doneSets,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (progress.allDone) {
                            MaterialTheme.fitColors.success
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Text(
                        "/ ${progress.totalSets}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(FitSpace.xl))
            Column(Modifier.weight(1f)) {
                Text(
                    text = progress.summaryText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (progress.allDone) {
                        MaterialTheme.fitColors.success
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
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

/** 清单式打卡列表：训练详情页与主页共用（对外签名不变）。 */
@Composable
fun WorkoutChecklist(
    state: WorkoutUiState,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = FitSize.gutter,
            end = FitSize.gutter,
            bottom = FitSpace.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(FitSpace.md),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.comboItemId }) { index, item ->
            WorkoutItemCard(
                item = item,
                unitIsLb = state.unitIsLb,
                actions = actions,
                modifier = Modifier.fitEnter(index),
            )
        }
        item { Spacer(Modifier.height(FitSpace.lg)) }
    }
}

@Composable
private fun WorkoutItemCard(
    item: WorkoutItemUi,
    unitIsLb: Boolean,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(item.comboItemId) { mutableStateOf(false) }
    val motion = MaterialTheme.fitMotion
    val haptics = rememberFitHaptics()
    val allDone = item.sets.isNotEmpty() && item.doneCount == item.sets.size
    val progress = if (item.targetSets != null) {
        "已完成 ${item.doneCount}/${item.targetSets} 组"
    } else {
        "已完成 ${item.doneCount} 组"
    }
    // 全部完成时一次 success 触感（只在从未完成变为完成的那一次触发，不影响进入页面）
    var wasAllDone by remember(item.comboItemId) { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !wasAllDone) haptics.success()
        wasAllDone = allDone
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (motion.enabled) tween(motion.medium, easing = motion.standard) else tween(0),
        label = "workoutItemArrow",
    )
    val enter = if (motion.enabled) {
        expandVertically(animationSpec = tween<IntSize>(motion.medium, easing = motion.emphasizedDecel)) +
            fadeIn(animationSpec = tween(motion.short))
    } else {
        EnterTransition.None
    }
    val exit = if (motion.enabled) {
        shrinkVertically(animationSpec = tween<IntSize>(motion.short, easing = motion.emphasizedAccel)) +
            fadeOut(animationSpec = tween(motion.short))
    } else {
        ExitTransition.None
    }

    FitCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FitSize.rowMin)
                .clickable { expanded = !expanded }
                .padding(horizontal = FitSpace.xl, vertical = FitSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AggregateCircle(allDone = allDone)
            Spacer(Modifier.width(FitSpace.md))
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
                // 动作级细进度条：无组行（sets 为空）时不渲染，避免除零
                if (item.sets.isNotEmpty()) {
                    FitLinearProgress(
                        progress = item.doneCount.toFloat() / item.sets.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FitSpace.xs),
                        height = FitSpace.xs,
                    )
                }
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(arrowRotation),
            )
        }
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            Column(Modifier.fillMaxWidth()) {
                if (item.trackWeight) {
                    item.syncPromptWeight?.let { promptWeight ->
                        FitHairline()
                        SyncPromptRow(
                            item = item,
                            promptWeight = promptWeight,
                            unitIsLb = unitIsLb,
                            onSyncToItem = actions.onSyncToItem,
                            onSyncToAll = actions.onSyncToAll,
                            onDismiss = actions.onDismissSyncPrompt,
                        )
                    }
                    FitHairline()
                    WeightRow(
                        item = item,
                        unitIsLb = unitIsLb,
                        onRecord = { actions.onRecordSessionWeight(item.exerciseId, it) },
                    )
                }
                item.sets.forEach { set ->
                    FitHairline()
                    SetRow(
                        item = item,
                        set = set,
                        unitIsLb = unitIsLb,
                        actions = actions,
                    )
                }
                FitHairline()
                ExerciseNoteField(
                    todayNote = item.todayNote,
                    lastNote = item.lastNote,
                    onSave = { actions.onRecordExerciseNote(item.exerciseId, it) },
                    modifier = Modifier.padding(vertical = FitSpace.sm),
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
            .padding(horizontal = FitSpace.xl, vertical = FitSpace.md),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FitShapes.field)
                .background(MaterialTheme.fitColors.warningContainer)
                .padding(FitSpace.lg),
            verticalArrangement = Arrangement.spacedBy(FitSpace.xs),
        ) {
            Text(
                "上次实际用了 ${TimeUtil.formatWeight(promptWeight)} $unit，$exceeds，是否同步为目标重量？",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.fitColors.onWarningContainer,
            )
            if (item.syncCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = syncAll, onCheckedChange = { syncAll = it })
                    Text(
                        "同步到所有包含此动作的组合（共 ${item.syncCount} 处）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.fitColors.onWarningContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FitTextAction("忽略", onClick = { onDismiss(item.exerciseId, promptWeight) })
                Spacer(Modifier.width(FitSpace.sm))
                FitButton(
                    text = "同步",
                    onClick = {
                        if (syncAll) {
                            onSyncToAll(item.exerciseId, promptWeight)
                        } else {
                            onSyncToItem(item.comboItemId, item.exerciseId, promptWeight)
                        }
                    },
                    style = FitButtonStyle.Tonal,
                )
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
    // 读屏需要听到当前值：数值文本本身没有语义时只播标签会丢信息
    val weightValueDescription = shown?.let {
        "编辑当次重量，当前 ${TimeUtil.formatWeight(it)} $unit"
    } ?: "编辑当次重量，未设定"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.rowMin)
            .padding(horizontal = FitSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "重量",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FitIconButton(
            icon = Icons.Filled.Remove,
            contentDescription = "减少重量",
            onClick = { onRecord((shown ?: 0.0) - step) },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .heightIn(min = FitSize.touchMin)
                .clip(FitShapes.field)
                .clickable { showEditor = true }
                .semantics { contentDescription = weightValueDescription }
                .padding(horizontal = FitSpace.md),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = shown?.let { "${TimeUtil.formatWeight(it)} $unit" } ?: "未设定",
                style = MaterialTheme.typography.titleMedium,
                color = if (shown == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (shown != null) {
                Text(
                    text = if (sessionWeight != null) "当次" else "目标",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FitIconButton(
            icon = Icons.Filled.Add,
            contentDescription = "增加重量",
            onClick = { onRecord((shown ?: 0.0) + step) },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

    FitDialog(
        title = "当次重量（$unit）",
        onDismiss = onDismiss,
        confirmLabel = "确定",
        // 非法中间态（仅 "-"/"." 等）禁用确定：与重构前一致，不写入、不取消当次记录
        confirmEnabled = trimmed.isEmpty() || trimmed.toDoubleOrNull() != null,
        onConfirm = { onConfirm(trimmed.toDoubleOrNull()) },
        content = {
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
                modifier = Modifier.padding(top = FitSpace.xs),
            )
        },
    )
}

/** 动作级聚合圈（品牌组环风格）：全部组完成 = 实心对勾 + 一次 spring pop；填色/描边带颜色过渡。 */
@Composable
private fun AggregateCircle(allDone: Boolean) {
    val motion = MaterialTheme.fitMotion
    val scale = remember { Animatable(1f) }
    var lastDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone != lastDone) {
            if (motion.enabled) {
                scale.animateTo(1.18f, motion.pop)
                scale.animateTo(1f, motion.pop)
            }
            lastDone = allDone
        }
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = if (motion.enabled) tween(motion.short) else tween(0),
        label = "aggregateCircleFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = if (motion.enabled) tween(motion.short) else tween(0),
        label = "aggregateCircleBorder",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (allDone) 1f else 0f,
        animationSpec = if (motion.enabled) tween(motion.short) else tween(0),
        label = "aggregateCircleCheck",
    )
    Box(
        modifier = Modifier
            .size(FitSize.aggregateCircle)
            // graphicsLayer 缩放：只影响绘制，动画期间不触发重组/重排（性能）
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(FitShapes.circle)
            .background(backgroundColor, FitShapes.circle)
            .border(FitSize.border, borderColor, FitShapes.circle)
            .semantics {
                contentDescription = if (allDone) "动作已完成" else "动作未完成"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checkAlpha > 0f) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(FitSize.checkIcon)
                    .alpha(checkAlpha),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val haptics = rememberFitHaptics()
    val repsColor = if (checked) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // FlowRow：常规屏一行排布（56dp 高）；窄屏/大字体时右侧控件自动换行，不产生横向溢出
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.rowMin)
            .padding(horizontal = FitSpace.xl, vertical = FitSpace.xs),
        horizontalArrangement = Arrangement.spacedBy(FitSpace.xs),
        verticalArrangement = Arrangement.spacedBy(FitSpace.xs),
    ) {
        Row(
            modifier = Modifier.heightIn(min = FitSize.touchMin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SetCircle(
                checked = checked,
                number = set.setIndex + 1,
                onCheckIn = {
                    actions.onCheckIn(item.comboItemId, set.setIndex, defaultRepsFor(item, set.setIndex))
                },
                onCancel = { actions.onCancel(item.comboItemId, set.setIndex) },
            )
            Spacer(Modifier.width(FitSpace.xs + FitSpace.xxs))
            Text(
                "第 ${set.setIndex + 1} 组",
                style = MaterialTheme.typography.bodyMedium,
                color = repsColor,
            )
        }
        val reps = set.reps
        when {
            reps != null -> RepsStepper(
                reps = reps,
                onDecrease = { actions.onAdjustReps(item.comboItemId, set.setIndex, -1) },
                onIncrease = { actions.onAdjustReps(item.comboItemId, set.setIndex, +1) },
            )

            checked -> SetStateText(
                text = "已完成",
                style = MaterialTheme.typography.bodyMedium,
            )

            set.skipped -> SetStateText(
                text = "已逃",
                style = MaterialTheme.typography.bodyMedium,
            )

            else -> SetStateText(
                text = item.targetReps?.let { "目标 $it 次" } ?: "—",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // 标逃/取消逃标入口（力竭跳组）：未打卡组可标逃，已逃组可取消恢复未练；
        // 已逃组直接点打卡圈即转为打卡（逃标消失）
        if (!checked) {
            ActionChip(
                text = if (set.skipped) "取消逃标" else "标逃",
                spokenDescription = if (set.skipped) {
                    "取消第 ${set.setIndex + 1} 组逃标"
                } else {
                    "标记第 ${set.setIndex + 1} 组逃组"
                },
                onClick = {
                    if (set.skipped) {
                        actions.onCancelSetSkip(item.comboItemId, set.setIndex)
                        haptics.toggleOff()
                    } else {
                        actions.onSkipSet(item.comboItemId, set.setIndex)
                        haptics.toggleOn()
                    }
                },
                highlighted = set.skipped,
            )
        }
        // 每组自己的重量：独立于动作级当次重量，可单独设置/编辑/清除
        if (item.trackWeight && checked) {
            ActionChip(
                text = set.weight?.let { TimeUtil.formatWeight(it) + " " + unit } ?: "记重量",
                spokenDescription = "编辑第 ${set.setIndex + 1} 组重量",
                onClick = { showWeightEditor = true },
                highlighted = set.weight != null,
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

/** 组行状态文字（已完成/已逃/目标次数/—）：包一层 48dp 居中盒，与 FlowRow 内其他控件对齐。 */
@Composable
private fun SetStateText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
) {
    Box(
        modifier = Modifier.heightIn(min = FitSize.touchMin),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 次数 stepper：紧凑 pill（− 数字 +），两个按钮各 48dp 触达；原子增减由 DAO 保证下限 1。 */
@Composable
private fun RepsStepper(
    reps: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = FitSize.touchMin)
            .clip(FitShapes.pill)
            .background(MaterialTheme.fitColors.surfaceSunken),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FitIconButton(
            icon = Icons.Filled.Remove,
            contentDescription = "减少次数",
            onClick = onDecrease,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "$reps 次",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FitIconButton(
            icon = Icons.Filled.Add,
            contentDescription = "增加次数",
            onClick = onIncrease,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 组行内的轻量动作 chip（标逃/组重量）：48dp 触达，选中态用主色容器。 */
@Composable
private fun ActionChip(
    text: String,
    spokenDescription: String,
    onClick: () -> Unit,
    highlighted: Boolean,
) {
    Box(
        modifier = Modifier
            .heightIn(min = FitSize.touchMin)
            // 短文本 chip（如「标逃」）也保证 48dp 宽触达（AC-08）
            .widthIn(min = FitSize.touchMin)
            .clip(FitShapes.pill)
            .background(
                if (highlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.fitColors.surfaceSunken
                },
            )
            .clickable(role = Role.Button) { onClick() }
            .semantics { contentDescription = spokenDescription }
            .padding(horizontal = FitSpace.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** 组打卡圆圈：未打卡 = 2dp 描边圆显示组号，点击打卡；已打卡 = 实心圆对勾 + spring pop，点击取消。 */
@Composable
private fun SetCircle(
    checked: Boolean,
    number: Int,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit,
) {
    val motion = MaterialTheme.fitMotion
    val haptics = rememberFitHaptics()
    val scale = remember { Animatable(1f) }
    var lastChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked != lastChecked) {
            if (motion.enabled) {
                scale.animateTo(1.15f, motion.pop)
                scale.animateTo(1f, motion.pop)
            }
            lastChecked = checked
        }
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = if (motion.enabled) tween(motion.short) else tween(0),
        label = "setCircleFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = if (motion.enabled) tween(motion.short) else tween(0),
        label = "setCircleBorder",
    )
    Box(
        modifier = Modifier
            .size(FitSize.setCircle)
            // graphicsLayer 缩放：只影响绘制，避免动画期间整行重组（性能）
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            // 用 toggleable 暴露勾选语义（读屏可识别为复选框 + 勾选状态），点击行为与 clickable 一致
            .clip(FitShapes.circle)
            .background(backgroundColor, FitShapes.circle)
            .border(FitSize.border, borderColor, FitShapes.circle)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = {
                    if (checked) {
                        onCancel()
                    } else {
                        onCheckIn()
                    }
                    haptics.tap()
                },
            )
            .semantics {
                contentDescription =
                    if (checked) "取消第 $number 组打卡" else "完成第 $number 组打卡"
                stateDescription = if (checked) "已打卡" else "未打卡"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(FitSize.checkIconLarge),
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

/** 动作备注输入：预填当天备注，下方灰字回显上次备注（无历史不显示）；只写/改，无清空入口。 */
@Composable
private fun ExerciseNoteField(
    todayNote: String?,
    lastNote: String?,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(todayNote) { mutableStateOf(todayNote ?: "") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FitSpace.xl),
        verticalArrangement = Arrangement.spacedBy(FitSpace.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("动作备注") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(FitSpace.sm))
            FitTextAction(
                text = "保存",
                onClick = { onSave(text.trim()) },
                // 仅在内容非空且与当天已有备注不同时启用，避免重复保存同一段文字
                enabled = text.isNotBlank() && text.trim() != (todayNote ?: ""),
            )
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

    FitDialog(
        title = "第 $setTitle 组重量（$unit）",
        onDismiss = onDismiss,
        confirmLabel = "确定",
        confirmEnabled = trimmed.isEmpty() || trimmed.toDoubleOrNull() != null,
        onConfirm = { onConfirm(trimmed.toDoubleOrNull()) },
        content = {
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
                modifier = Modifier.padding(top = FitSpace.xs),
            )
        },
    )
}

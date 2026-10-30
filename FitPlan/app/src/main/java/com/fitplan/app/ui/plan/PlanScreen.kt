package com.fitplan.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitChipTone
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitStatusPill
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil
import com.fitplan.app.util.durationText
import kotlinx.coroutines.launch

private const val PAGER_OFFSET = 10000
private const val PAGER_COUNT = 20001

@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    onOpenWorkout: (Long) -> Unit = {},
) {
    val vm: PlanViewModel = viewModel(factory = PlanViewModel.Factory)
    val dayPlans by vm.dayPlans.collectAsStateWithLifecycle()
    // 加载态契约（T-L6）：dayPlans 初始为 null、数据到达后非 null。
    val plansOrNull: Map<Long, DayPlanEntity>? = dayPlans
    val combos by vm.combos.collectAsStateWithLifecycle()
    val durations by vm.durations.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // 初始页定位到今天所在周（页码 p 对应周一 = BASE_MONDAY + (p - PAGER_OFFSET) * 7）
    val initialPage = remember {
        PAGER_OFFSET + TimeUtil.pageFor(TimeUtil.mondayOf(TimeUtil.today()).toEpochDay())
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { PAGER_COUNT })
    var editingDate by remember { mutableStateOf<Long?>(null) }
    var showAutoSchedule by remember { mutableStateOf(false) }
    var confirmCopyWeek by remember { mutableStateOf(false) }
    var copyWeekResult by remember { mutableStateOf<String?>(null) }

    val mondayEpochDay = TimeUtil.BASE_MONDAY + (pagerState.currentPage - PAGER_OFFSET) * 7L

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FitSize.gutter, end = FitSpace.sm, top = FitSpace.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FitScreenTitle(
                title = "计划",
                subtitle = weekTitle(mondayEpochDay),
                modifier = Modifier
                    .weight(1f)
                    .fitEnter(0),
            )
            HeaderAction(
                icon = Icons.Filled.ContentCopy,
                text = "复制上周",
                onClick = { confirmCopyWeek = true },
            )
            HeaderAction(
                icon = Icons.Filled.AutoAwesome,
                text = "自动排程",
                onClick = { showAutoSchedule = true },
            )
        }
        if (plansOrNull == null) {
            // 冷启动数据未到：先显示加载中，不闪「未安排」（顶部标题保留）
            FitEmptyState(
                title = "加载中…",
                modifier = Modifier.weight(1f),
                icon = null,
            )
        } else {
            // 周切换：上一周/下一周图标按钮 + 居中「本周」胶囊（保留 HorizontalPager 滑动）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FitSize.gutter, vertical = FitSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FitIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一周",
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = FitSize.touchMin)
                        .clip(FitShapes.pill)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(role = Role.Button) {
                            scope.launch { pagerState.animateScrollToPage(initialPage) }
                        }
                        .padding(horizontal = FitSpace.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "本周",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                FitIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一周",
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val monday = TimeUtil.BASE_MONDAY + (page - PAGER_OFFSET) * 7L
                WeekColumn(
                    mondayEpochDay = monday,
                    dayPlans = plansOrNull,
                    durations = durations,
                    comboName = { id -> combos.find { it.id == id }?.name ?: "未知组合" },
                    onDayClick = { editingDate = it },
                    onOpenWorkout = onOpenWorkout,
                )
            }
        }
    }

    editingDate?.let { date ->
        DayEditSheet(
            date = date,
            plan = plansOrNull?.get(date),
            combos = combos,
            onSet = { isRest, comboId ->
                vm.setDay(date, isRest, comboId)
                editingDate = null
            },
            onCopyFrom = { source ->
                vm.copyDay(source, date)
                editingDate = null
            },
            onClear = {
                vm.clearDay(date)
                editingDate = null
            },
            onDismiss = { editingDate = null },
        )
    }

    if (confirmCopyWeek) {
        FitDialog(
            title = "复制上周计划",
            text = "把上一周（${weekTitle(mondayEpochDay - 7)}）的安排复制到当前显示的周（${weekTitle(mondayEpochDay)}）？",
            confirmLabel = "复制",
            onConfirm = {
                confirmCopyWeek = false
                vm.copyWeek(mondayEpochDay - 7, mondayEpochDay) { count ->
                    copyWeekResult = if (count == 0) "上一周没有安排可复制" else "已复制 $count 天的安排"
                }
            },
            onDismiss = { confirmCopyWeek = false },
        )
    }

    copyWeekResult?.let { msg ->
        // 结果提示只有一个「好的」按钮
        FitDialog(
            title = "复制上周计划",
            text = msg,
            confirmLabel = "好的",
            dismissLabel = null,
            onConfirm = { copyWeekResult = null },
            onDismiss = { copyWeekResult = null },
        )
    }

    if (showAutoSchedule) {
        AutoScheduleDialog(
            mondayEpochDay = mondayEpochDay,
            combos = combos,
            onApply = { type, split3, cycle ->
                vm.applyScheme(type, mondayEpochDay, split3, cycle)
                showAutoSchedule = false
            },
            onDismiss = { showAutoSchedule = false },
        )
    }
}

/** 顶栏紧凑动作：图标 + 可见文字（48dp 触达、Button 语义），保证可发现性。 */
@Composable
private fun HeaderAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = FitSize.touchMin)
            .clip(FitShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FitSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(FitSize.iconSmall),
        )
        Spacer(Modifier.width(FitSpace.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun weekTitle(mondayEpochDay: Long): String {
    val monday = TimeUtil.fromEpochDay(mondayEpochDay)
    val sunday = monday.plusDays(6)
    return "${TimeUtil.formatShort(monday)} - ${TimeUtil.formatShort(sunday)}"
}

@Composable
private fun WeekColumn(
    mondayEpochDay: Long,
    dayPlans: Map<Long, DayPlanEntity>,
    durations: Map<Long, Long>,
    comboName: (Long) -> String,
    onDayClick: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FitSize.gutter,
            end = FitSize.gutter,
            top = FitSpace.xs,
            bottom = FitSpace.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(FitSpace.md),
    ) {
        items(count = 7, key = { index -> mondayEpochDay + index }) { index ->
            val date = mondayEpochDay + index
            val plan = dayPlans[date]
            val isTraining = plan != null && !plan.isRest
            DayCard(
                date = date,
                plan = plan,
                isTraining = isTraining,
                comboName = if (isTraining) comboName(plan?.comboId ?: -1L) else "",
                durationMillis = durations[date],
                index = index,
                onClick = {
                    // 训练日进入训练详情打卡，休息日/未安排保持打开编辑面板
                    if (isTraining) onOpenWorkout(date) else onDayClick(date)
                },
                onEdit = { onDayClick(date) },
            )
        }
    }
}

/** 单日卡：左星期/日期，中组合名（或休息/未安排）+ 用时，右「今天」胶囊 + 编辑入口。 */
@Composable
private fun DayCard(
    date: Long,
    plan: DayPlanEntity?,
    isTraining: Boolean,
    comboName: String,
    durationMillis: Long?,
    index: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val isToday = TimeUtil.isToday(date)
    // 今天用 primaryContainer 底色 + onPrimaryContainer 文字，避免只用标签导致弱提示
    val containerColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val primaryTextColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryTextColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val accentColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val weekday = TimeUtil.weekdayName(TimeUtil.fromEpochDay(date))
    val shortDate = TimeUtil.formatShort(TimeUtil.fromEpochDay(date))
    val statusText = when {
        isTraining -> "训练日"
        plan == null -> "未安排"
        else -> "休息日"
    }

    FitCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .fitEnter(index),
        containerColor = containerColor,
        contentPadding = PaddingValues(horizontal = FitSpace.lg, vertical = FitSpace.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(end = FitSpace.md)) {
                Text(
                    weekday,
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryTextColor,
                )
                Text(
                    shortDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                )
            }
            Column(Modifier.weight(1f)) {
                if (isTraining) {
                    Text(
                        comboName,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                    )
                    Text(
                        "训练日",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                    )
                    // 当日训练用时（首末打卡时刻差，与通知同口径）；无打卡的天不显示
                    durationMillis?.let { millis ->
                        Text(
                            durationText("用时", millis),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                        )
                    }
                } else {
                    // 休息/未安排不是主叙事：用 bodyLarge + onSurfaceVariant 保持卡片安静
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = secondaryTextColor,
                    )
                }
            }
            if (isToday) {
                // 今日卡底色已是 primaryContainer，胶囊用中性色保证可见
                FitStatusPill(text = "今天", tone = FitChipTone.Neutral)
            }
            if (isTraining) {
                FitIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = "编辑安排",
                    onClick = onEdit,
                    tint = if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

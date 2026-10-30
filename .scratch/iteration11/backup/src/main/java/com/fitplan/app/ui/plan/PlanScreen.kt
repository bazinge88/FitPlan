package com.fitplan.app.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.ui.ConfirmDialog
import com.fitplan.app.util.TimeUtil
import com.fitplan.app.util.durationText
import kotlinx.coroutines.launch

private const val PAGER_OFFSET = 10000
private const val PAGER_COUNT = 20001

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    onOpenWorkout: (Long) -> Unit = {},
) {
    val vm: PlanViewModel = viewModel(factory = PlanViewModel.Factory)
    val dayPlans by vm.dayPlans.collectAsStateWithLifecycle()
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
        TopAppBar(
            title = { Text(weekTitle(mondayEpochDay)) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            actions = {
                TextButton(onClick = { confirmCopyWeek = true }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" 复制上周")
                }
                TextButton(onClick = { showAutoSchedule = true }) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Text(" 自动排程")
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }) {
                Text("‹ 上一周")
            }
            TextButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(initialPage) }
            }) {
                Text("本周")
            }
            TextButton(onClick = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Text("下一周 ›")
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val monday = TimeUtil.BASE_MONDAY + (page - PAGER_OFFSET) * 7L
            WeekColumn(
                mondayEpochDay = monday,
                dayPlans = dayPlans,
                durations = durations,
                comboName = { id -> combos.find { it.id == id }?.name ?: "未知组合" },
                onDayClick = { editingDate = it },
                onOpenWorkout = onOpenWorkout,
            )
        }
    }

    editingDate?.let { date ->
        DayEditSheet(
            date = date,
            plan = dayPlans[date],
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
        ConfirmDialog(
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
        AlertDialog(
            onDismissRequest = { copyWeekResult = null },
            title = { Text("复制上周计划") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { copyWeekResult = null }) { Text("好的") }
            },
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(7) { index ->
            val date = mondayEpochDay + index
            val plan = dayPlans[date]
            val isTraining = plan != null && !plan.isRest
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 训练日进入训练详情打卡，休息日/未安排保持打开编辑面板
                        if (isTraining) onOpenWorkout(date) else onDayClick(date)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(96.dp)) {
                        Text(
                            TimeUtil.weekdayName(TimeUtil.fromEpochDay(date)),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            TimeUtil.formatShort(TimeUtil.fromEpochDay(date)) +
                                if (TimeUtil.isToday(date)) " · 今天" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        if (isTraining) {
                            Text(
                                comboName(plan?.comboId ?: -1L),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text("训练日", style = MaterialTheme.typography.bodySmall)
                            // 当日训练用时（首末打卡时刻差，与通知同口径）；无打卡的天不显示
                            durations[date]?.let { millis ->
                                Text(
                                    durationText("用时", millis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text(
                                if (plan == null) "未安排" else "休息日",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (isTraining) {
                        IconButton(onClick = { onDayClick(date) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "编辑安排",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

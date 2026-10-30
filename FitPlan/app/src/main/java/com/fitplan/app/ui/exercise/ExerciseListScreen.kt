package com.fitplan.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.design.FitButton
import com.fitplan.app.ui.design.FitButtonStyle
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitChipTone
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitHairline
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitMetaChip
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitSectionHeader
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace

/** 按分类分组（动作库 DAO 已按 category, name 排序，这里只做连续同类聚合）。 */
private fun groupByCategory(exercises: List<ExerciseEntity>): List<Pair<String, List<ExerciseEntity>>> {
    val groups = mutableListOf<Pair<String, MutableList<ExerciseEntity>>>()
    exercises.forEach { exercise ->
        val key = exercise.category.ifEmpty { "未分类" }
        if (groups.isNotEmpty() && groups.last().first == key) {
            groups.last().second.add(exercise)
        } else {
            groups.add(key to mutableListOf(exercise))
        }
    }
    return groups.map { (key, list) -> key to list.toList() }
}

/**
 * 动作库：大标题 + PR 入口 + 分类分组卡 + 新建 FAB。
 * 每行：动作名 + 启用字段徽标（FlowRow 自动换行）+ 更多菜单（编辑/删除，删除前确认，文案不变）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseListScreen(
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit,
    onOpenPr: () -> Unit = {},
) {
    val vm: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory)
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    val groups = remember(exercises) { groupByCategory(exercises) }

    // Tab 页：外层 MainScaffold 已提供系统栏/底部导航 padding，这里不再自建 Scaffold
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = FitSpace.xl,
                        end = FitSpace.xl,
                        top = FitSpace.xl,
                        bottom = FitSpace.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FitScreenTitle(
                    title = "动作库",
                    subtitle = if (exercises.isEmpty()) null else "共 ${exercises.size} 个动作",
                    modifier = Modifier.weight(1f),
                )
                FitButton(
                    text = "PR 记录",
                    onClick = onOpenPr,
                    style = FitButtonStyle.Ghost,
                    leadingIcon = Icons.Rounded.EmojiEvents,
                )
            }
            if (exercises.isEmpty()) {
                FitEmptyState(
                    title = "还没有动作",
                    description = "点击右下角新建，如卧推、深蹲、辅助引体向上。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = FitSpace.xl,
                        end = FitSpace.xl,
                        top = FitSpace.xs,
                        bottom = FitSpace.md,
                    ),
                ) {
                    groups.forEachIndexed { groupIndex, (category, list) ->
                        item(key = "header-$category") {
                            // 区块标题自带 24dp 上间距、12dp 下间距（FitSectionHeader 内建）
                            FitSectionHeader(title = category)
                        }
                        item(key = "group-$category") {
                            FitCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fitEnter(groupIndex),
                                contentPadding = PaddingValues(),
                            ) {
                                list.forEachIndexed { index, exercise ->
                                    ExerciseRow(
                                        exercise = exercise,
                                        onEdit = { onEdit(exercise.id) },
                                        onDelete = { pendingDelete = exercise },
                                    )
                                    if (index < list.lastIndex) {
                                        FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 底部为悬浮 FAB 预留一条空白带（20dp 外边距 + 56dp FAB + 20dp 间隔 = 96dp）：
            // 列表/空态内容止于该带之上，任意滚动位置都不会被按钮遮住
            Spacer(Modifier.height(FitSpace.xl + FitSize.fab + FitSpace.xl))
        }

        ExtendedFloatingActionButton(
            onClick = { onEdit(-1L) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("新建动作") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = FitSpace.xl, bottom = FitSpace.xl),
        )
    }

    pendingDelete?.let { target ->
        FitDialog(
            title = "删除动作",
            text = "确定删除「${target.name}」？相关组合目标与训练记录会一并删除，" +
                "该动作的打卡与逃组记录也会一并清理。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                vm.delete(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.rowMin)
            .padding(start = FitSpace.xl, end = FitSpace.sm, top = FitSpace.md, bottom = FitSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // 动作名单行省略；字段徽标放 FlowRow，窄屏/大字体自动换行不溢出
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FitSpace.sm),
                verticalArrangement = Arrangement.spacedBy(FitSpace.xs),
                modifier = Modifier.padding(top = FitSpace.sm),
            ) {
                if (exercise.trackSets) FitMetaChip(text = "组数", tone = FitChipTone.Neutral)
                if (exercise.trackReps) FitMetaChip(text = "次数", tone = FitChipTone.Neutral)
                if (exercise.trackWeight) FitMetaChip(text = "重量", tone = FitChipTone.Neutral)
                if (exercise.trackDuration) FitMetaChip(text = "时长", tone = FitChipTone.Neutral)
            }
        }
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
}

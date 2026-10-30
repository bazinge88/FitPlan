package com.fitplan.app.ui.combo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
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
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.setsSummary
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace

/**
 * 组合列表：大标题 + 组合卡（名称 / 动作摘要 / 更多菜单）+ 新建 FAB。
 * 业务口径与旧版一致：加载态「加载中…」、空态引导、摘要最多 3 行后收尾「等 N 个动作」、
 * 编辑/复制/删除（删除前确认，文案不变）。
 */
@Composable
fun ComboListScreen(
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit,
) {
    val vm: ComboListViewModel = viewModel(factory = ComboListViewModel.Factory)
    val combos by vm.combos.collectAsStateWithLifecycle()
    // 加载态契约：combos 初始为 null、数据到达后非 null；显式可空局部变量标明契约
    val combosOrNull: List<ComboWithItems>? = combos
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ComboWithItems?>(null) }

    // Tab 页：外层 MainScaffold 已提供系统栏/底部导航 padding，这里不再自建 Scaffold
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FitScreenTitle(
                title = "训练组合",
                subtitle = combosOrNull?.let { "共 ${it.size} 个组合" },
                modifier = Modifier.padding(
                    start = FitSpace.xl,
                    end = FitSpace.xl,
                    top = FitSpace.xl,
                    bottom = FitSpace.md,
                ),
            )
            when {
                // 冷启动数据未到：显示加载中，不先闪「还没有训练组合」
                combosOrNull == null -> FitEmptyState(
                    title = "加载中…",
                    modifier = Modifier.weight(1f),
                    icon = null,
                )

                combosOrNull.isEmpty() -> FitEmptyState(
                    title = "还没有训练组合",
                    description = "把若干动作固定成一组，再安排到周计划里。",
                    modifier = Modifier.weight(1f),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = FitSpace.xl,
                        end = FitSpace.xl,
                        top = FitSpace.xs,
                        bottom = FitSpace.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(FitSpace.md),
                ) {
                    itemsIndexed(combosOrNull, key = { _, combo -> combo.combo.id }) { index, combo ->
                        ComboCard(
                            combo = combo,
                            exerciseName = { id -> exercises.find { it.id == id }?.name ?: "?" },
                            onEdit = { onEdit(combo.combo.id) },
                            onCopy = { vm.copyCombo(combo) },
                            onDelete = { pendingDelete = combo },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fitEnter(index),
                        )
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
            text = { Text("新建组合") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = FitSpace.xl, bottom = FitSpace.xl),
        )
    }

    pendingDelete?.let { target ->
        FitDialog(
            title = "删除组合",
            text = "确定删除「${target.combo.name}」？周计划中绑定该组合的天会变为休息日。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                vm.delete(target.combo.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ComboCard(
    combo: ComboWithItems,
    exerciseName: (Long) -> String,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sortedItems = combo.items.sortedBy { it.sort }

    FitCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                // 组合名单行省略，避免长名称把行内操作挤出卡片
                Text(
                    text = combo.combo.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(FitSpace.sm))
                if (sortedItems.isEmpty()) {
                    Text(
                        "未包含动作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // 动作摘要最多 3 行，超出以「等 N 个动作」收尾，避免长组合撑破卡片
                    sortedItems.take(3).forEach { item ->
                        Text(
                            "${exerciseName(item.exerciseId)}（${setsSummary(item.targetSets, item.targetReps, item.targetWeight, item.targetDuration)}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (sortedItems.size > 3) {
                        Text(
                            "等 ${sortedItems.size} 个动作",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row {
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
                        text = { Text("复制") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCopy()
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
    }
}

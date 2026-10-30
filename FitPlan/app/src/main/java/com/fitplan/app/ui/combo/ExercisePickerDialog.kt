package com.fitplan.app.ui.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.design.FitChipTone
import com.fitplan.app.ui.design.FitDialog
import com.fitplan.app.ui.design.FitMetaChip
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace

/**
 * 选动作对话框：搜索 + 分类筛选 + 已添加标记。
 * 单选模式点行即确认；多选模式勾选后批量确认，返回顺序与动作库顺序一致（跨分类切换筛选不丢项）。
 * 行为与旧对话框一致，仅外观改为品牌 FitDialog；键盘弹出时由对话框窗口自行避让。
 */
@Composable
fun ExercisePickerDialog(
    exercises: List<ExerciseEntity>,
    title: String,
    single: Boolean,
    markedIds: Set<Long> = emptySet(),
    preselectedIds: Set<Long> = emptySet(),
    onConfirm: (List<ExerciseEntity>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<Long>().apply { addAll(preselectedIds) } }
    val categories = remember(exercises) {
        exercises.map { it.category.ifEmpty { "未分类" } }.distinct()
    }
    val trimmedQuery = query.trim()
    val filtered = exercises.filter { e ->
        (category == null || e.category.ifEmpty { "未分类" } == category) &&
            (trimmedQuery.isEmpty() || e.name.contains(trimmedQuery, ignoreCase = true))
    }

    // 多选确认：按全部勾选返回（与按钮计数一致），跨分类切换筛选不丢项
    val confirmAction: () -> Unit = { onConfirm(exercises.filter { it.id in selected }) }

    FitDialog(
        title = title,
        onDismiss = onDismiss,
        // 单选模式点行即确认，只有「关闭」；多选模式显示计数按钮
        confirmLabel = when {
            single -> "添加"
            selected.isEmpty() -> "添加"
            else -> "添加（${selected.size}）"
        },
        onConfirm = if (single) null else confirmAction,
        confirmEnabled = !single && selected.isNotEmpty(),
        dismissLabel = if (single) "关闭" else "取消",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FitSpace.md)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索动作") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (categories.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(FitSpace.sm),
                ) {
                    FilterChip(
                        selected = category == null,
                        onClick = { category = null },
                        label = { Text("全部") },
                    )
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = if (category == c) null else c },
                            label = { Text(c) },
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    text = if (exercises.isEmpty()) "还没有动作，请先在「动作」页添加" else "没有匹配的动作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FitSpace.xxl),
                )
            } else {
                // 列表最大高度按 token 推导（huge=40dp × 9），避免长列表把对话框撑满整屏
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = FitSpace.huge * 9)
                        .verticalScroll(rememberScrollState()),
                ) {
                    filtered.forEach { exercise ->
                        val isSelected = exercise.id in selected
                        // 单选：点行即确认；多选：整行是唯一交互目标（role=Checkbox + toggleable，
                        // Checkbox 仅作展示），读屏播报选中态，避免整行与 Checkbox 双目标
                        val rowModifier = if (single) {
                            Modifier.clickable { onConfirm(listOf(exercise)) }
                        } else {
                            Modifier.toggleable(
                                value = isSelected,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    if (checked) selected.add(exercise.id) else selected.remove(exercise.id)
                                },
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = FitSize.rowMin)
                                .then(rowModifier),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                if (exercise.category.isNotBlank()) {
                                    Text(
                                        exercise.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (exercise.id in markedIds) {
                                FitMetaChip(
                                    text = "已添加",
                                    tone = FitChipTone.Primary,
                                    modifier = Modifier.padding(end = FitSpace.sm),
                                )
                            }
                            if (!single) {
                                // 展示用 Checkbox：交互由整行 toggleable 承担（onCheckedChange = null）
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

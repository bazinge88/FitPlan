package com.fitplan.app.ui.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitplan.app.data.db.ExerciseEntity

/**
 * 选动作对话框：搜索 + 分类筛选。
 * 单选模式点行即确认；多选模式勾选后批量确认，返回顺序与列表一致。
 * markedIds 打「已添加」标（仍在组合中的动作），仅提示不阻止选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索动作") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (categories.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        if (exercises.isEmpty()) "还没有动作，请先在「动作」页添加" else "没有匹配的动作",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(filtered, key = { it.id }) { exercise ->
                            val isSelected = exercise.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        if (single) {
                                            onConfirm(listOf(exercise))
                                        } else if (isSelected) {
                                            selected.remove(exercise.id)
                                        } else {
                                            selected.add(exercise.id)
                                        }
                                    },
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
                                    Text(
                                        "已添加",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (!single) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selected.add(exercise.id) else selected.remove(exercise.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!single) {
                // 按全部勾选返回（与按钮计数一致）：跨分类勾选后切换筛选不丢项
                TextButton(
                    onClick = { onConfirm(exercises.filter { it.id in selected }) },
                    enabled = selected.isNotEmpty(),
                ) {
                    Text(if (selected.isEmpty()) "添加" else "添加（${selected.size}）")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (single) "关闭" else "取消") }
        },
    )
}

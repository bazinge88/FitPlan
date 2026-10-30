package com.fitplan.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.ConfirmDialog
import com.fitplan.app.ui.EmptyHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit,
    onOpenPr: () -> Unit = {},
) {
    val vm: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory)
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text("动作库") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                actions = {
                    TextButton(onClick = onOpenPr) { Text("PR 记录") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(-1L) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建动作")
            }
        },
    ) { padding ->
        if (exercises.isEmpty()) {
            EmptyHint(
                "还没有动作。点击右下角新建，如卧推、深蹲、辅助引体向上。",
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(exercises.size, key = { exercises[it].id }) { index ->
                    val exercise = exercises[index]
                    val showHeader =
                        index == 0 || exercises[index - 1].category != exercise.category
                    Column {
                        if (showHeader) {
                            Text(
                                text = exercise.category.ifEmpty { "未分类" },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 6.dp),
                                    ) {
                                        FieldBadge("组数", exercise.trackSets)
                                        FieldBadge("次数", exercise.trackReps)
                                        FieldBadge("重量", exercise.trackWeight)
                                        FieldBadge("时长", exercise.trackDuration)
                                    }
                                }
                                IconButton(onClick = { onEdit(exercise.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                                }
                                IconButton(onClick = { pendingDelete = exercise }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 24.dp)) }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = "删除动作",
            text = "确定删除「${target.name}」？相关组合目标与训练记录也会一并删除。",
            confirmLabel = "删除",
            onConfirm = { vm.delete(target) },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** 轻量 tonal 小徽标，用于展示已启用的记录字段。 */
@Composable
private fun FieldBadge(label: String, enabled: Boolean) {
    if (enabled) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

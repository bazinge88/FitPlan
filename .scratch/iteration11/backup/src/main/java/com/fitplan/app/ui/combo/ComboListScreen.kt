package com.fitplan.app.ui.combo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.ui.ConfirmDialog
import com.fitplan.app.ui.EmptyHint
import com.fitplan.app.ui.setsSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboListScreen(
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit,
) {
    val vm: ComboListViewModel = viewModel(factory = ComboListViewModel.Factory)
    val combos by vm.combos.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ComboWithItems?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text("训练组合") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(-1L) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建组合")
            }
        },
    ) { padding ->
        if (combos.isEmpty()) {
            EmptyHint(
                "还没有训练组合。把若干动作固定成一组，再安排到周计划里。",
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
                items(combos.size, key = { combos[it].combo.id }) { index ->
                    val combo = combos[index]
                    val exerciseName = { id: Long ->
                        exercises.find { it.id == id }?.name ?: "?"
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
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(combo.combo.name, style = MaterialTheme.typography.titleMedium)
                                if (combo.items.isEmpty()) {
                                    Text(
                                        "未包含动作",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    combo.items.sortedBy { it.sort }.forEach { item ->
                                        Text(
                                            "${exerciseName(item.exerciseId)}（${setsSummary(item.targetSets, item.targetReps, item.targetWeight, item.targetDuration)}）",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onEdit(combo.combo.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { vm.copyCombo(combo) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "复制")
                            }
                            IconButton(onClick = { pendingDelete = combo }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = "删除组合",
            text = "确定删除「${target.combo.name}」？周计划中绑定该组合的天会变为休息日。",
            confirmLabel = "删除",
            onConfirm = { vm.delete(target.combo.id) },
            onDismiss = { pendingDelete = null },
        )
    }
}

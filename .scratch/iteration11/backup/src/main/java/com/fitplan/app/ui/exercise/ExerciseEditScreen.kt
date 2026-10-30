package com.fitplan.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fitplan.app.ui.DropdownSelector
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.parseDoubleOrNull
import com.fitplan.app.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditScreen(
    exerciseId: Long,
    onBack: () -> Unit,
) {
    val vm: ExerciseEditViewModel = viewModel(
        key = "exercise-$exerciseId",
        factory = ExerciseEditViewModel.factory(exerciseId),
    )
    val categories by vm.categories.collectAsStateWithLifecycle()
    val defaults by vm.defaults.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var trackSets by remember { mutableStateOf(true) }
    var trackReps by remember { mutableStateOf(true) }
    var trackWeight by remember { mutableStateOf(true) }
    var trackDuration by remember { mutableStateOf(false) }
    var defaultWeight by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    val unit = if (defaults.unitIsLb) "lb" else "kg"

    fun buildEntity(): ExerciseEntity = ExerciseEntity(
        id = if (exerciseId > 0) exerciseId else 0,
        name = name.trim(),
        category = category.trim(),
        trackSets = trackSets,
        trackReps = trackReps,
        trackWeight = trackWeight,
        trackDuration = trackDuration,
        defaultWeight = parseDoubleOrNull(defaultWeight),
    )

    LaunchedEffect(exerciseId) {
        if (exerciseId > 0) {
            vm.loadExisting()?.let { e ->
                name = e.name
                category = e.category
                trackSets = e.trackSets
                trackReps = e.trackReps
                trackWeight = e.trackWeight
                trackDuration = e.trackDuration
                defaultWeight = e.defaultWeight?.let { TimeUtil.formatWeight(it) } ?: ""
            }
        } else {
            val d = vm.loadDefaults()
            trackSets = d.defTrackSets
            trackReps = d.defTrackReps
            trackWeight = d.defTrackWeight
            trackDuration = d.defTrackDuration
        }
        loaded = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId > 0) "编辑动作" else "新建动作") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (loaded && name.isNotBlank()) {
                                vm.save(buildEntity(), onDone = onBack)
                            }
                        },
                        enabled = loaded && name.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("动作名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DropdownSelector(
                options = categories,
                label = { it },
                selected = categories.find { it == category },
                placeholder = "分类（可选）",
                onSelect = { category = it },
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("自定义分类（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("需要记录的字段", style = MaterialTheme.typography.titleMedium)
            Text(
                "未启用的字段在训练组合目标和训练记录中都不显示。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    CheckRow("组数", trackSets) { trackSets = it }
                    CheckRow("次数", trackReps) { trackReps = it }
                    CheckRow("重量（正=负重，0=自重，负=辅助重量）", trackWeight) { trackWeight = it }
                    CheckRow("时长", trackDuration) { trackDuration = it }
                }
            }

            if (trackWeight) {
                Text("默认重量", style = MaterialTheme.typography.titleMedium)
                Text(
                    "添加到组合时自动预填目标重量；各组合的目标重量相互独立，可在训练中记录当次重量后再确认同步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NumberField(
                            value = defaultWeight,
                            onValueChange = { defaultWeight = it },
                            label = "默认重量（$unit，可负数，留空不设定）",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

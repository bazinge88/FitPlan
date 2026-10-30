package com.fitplan.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.ui.DropdownSelector
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.parseIntOrNull
import com.fitplan.app.ui.parseDoubleOrNull
import com.fitplan.app.util.TimeUtil

private data class SetRow(
    val sets: String = "",
    val reps: String = "",
    val weight: String = "",
    val duration: String = "",
)

/** 添加/编辑一条训练记录。显示字段由所选动作的配置决定。 */
@Composable
fun EntryDialog(
    date: Long,
    exercises: List<ExerciseEntity>,
    unit: String,
    source: LogEntryWithSets?,
    onDismiss: () -> Unit,
    onSave: (exerciseId: Long, note: String, sets: List<SetDraft>) -> Unit,
) {
    var exerciseId by remember {
        mutableStateOf(source?.entry?.exerciseId ?: exercises.firstOrNull()?.id)
    }
    var note by remember { mutableStateOf(source?.entry?.note ?: "") }
    var rows by remember {
        mutableStateOf(
            source?.sets?.sortedBy { it.sort }?.map {
                SetRow(
                    sets = it.sets?.toString() ?: "",
                    reps = it.reps?.toString() ?: "",
                    weight = it.weight?.let { w -> TimeUtil.formatWeight(w) } ?: "",
                    duration = it.duration?.toString() ?: "",
                )
            } ?: listOf(SetRow()),
        )
    }

    val exercise = exercises.find { it.id == exerciseId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (source == null) "添加记录" else "编辑记录") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DropdownSelector(
                    options = exercises,
                    label = { it.name },
                    selected = exercise,
                    placeholder = "选择动作",
                    onSelect = { exerciseId = it.id },
                )
                rows.forEachIndexed { index, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "第 ${index + 1} 组",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { rows = rows.filterIndexed { i, _ -> i != index } },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "移除该组")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (exercise?.trackSets != false) {
                                NumberField(row.sets, { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(sets = v) else r } }, "组数", Modifier.weight(1f))
                            }
                            if (exercise?.trackReps != false) {
                                NumberField(row.reps, { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(reps = v) else r } }, "次数", Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (exercise?.trackWeight != false) {
                                NumberField(
                                    row.weight,
                                    { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(weight = v) else r } },
                                    "重量（$unit，可负数）",
                                    Modifier.weight(1f),
                                )
                            }
                            if (exercise?.trackDuration == true) {
                                NumberField(
                                    row.duration,
                                    { v -> rows = rows.mapIndexed { i, r -> if (i == index) r.copy(duration = v) else r } },
                                    "时长（分钟）",
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { rows = rows + SetRow() }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" 添加一组")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val drafts = rows.map {
                        SetDraft(
                            sets = parseIntOrNull(it.sets),
                            reps = parseIntOrNull(it.reps),
                            weight = parseDoubleOrNull(it.weight),
                            duration = parseIntOrNull(it.duration),
                        )
                    }
                    exerciseId?.let { id -> onSave(id, note, drafts) }
                },
                enabled = exerciseId != null,
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

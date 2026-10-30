package com.fitplan.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.ui.DropdownSelector
import com.fitplan.app.ui.NumberField
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitIconButton
import com.fitplan.app.ui.design.FitScaffold
import com.fitplan.app.ui.design.FitSectionHeader
import com.fitplan.app.ui.design.FitTopBar
import com.fitplan.app.ui.parseDoubleOrNull
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.util.TimeUtil

/**
 * 动作编辑：顶栏（返回 / 标题 / 保存）+ 基本信息、需要记录的字段、默认重量三张分组卡。
 * 表单字段、说明文案与保存校验沿用旧版；字段开关改为整行 toggleable + Switch（语义等价）。
 */
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

    val canSave = loaded && name.isNotBlank()

    FitScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FitTopBar(
                title = if (exerciseId > 0) "编辑动作" else "新建动作",
                onBack = onBack,
                actions = {
                    FitIconButton(
                        icon = Icons.Rounded.Check,
                        contentDescription = "保存",
                        onClick = {
                            if (canSave) vm.save(buildEntity(), onDone = onBack)
                        },
                        enabled = canSave,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = FitSpace.xl, end = FitSpace.xl, bottom = FitSpace.huge),
            verticalArrangement = Arrangement.spacedBy(FitSpace.md),
        ) {
            Spacer(Modifier.height(FitSpace.xs))
            FitSectionHeader(title = "基本信息")
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(FitSpace.md)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("动作名称") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
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
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            FitSectionHeader(title = "需要记录的字段")
            Text(
                "未启用的字段在训练组合目标和训练记录中都不显示。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    FieldSwitchRow("组数", trackSets) { trackSets = it }
                    FieldSwitchRow("次数", trackReps) { trackReps = it }
                    FieldSwitchRow("重量（正=负重，0=自重，负=辅助重量）", trackWeight) { trackWeight = it }
                    FieldSwitchRow("时长", trackDuration) { trackDuration = it }
                }
            }

            if (trackWeight) {
                FitSectionHeader(title = "默认重量")
                Text(
                    "添加到组合时自动预填目标重量；各组合的目标重量相互独立，可在训练中记录当次重量后再确认同步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    NumberField(
                        value = defaultWeight,
                        onValueChange = { defaultWeight = it },
                        label = "默认重量（$unit，可负数，留空不设定）",
                        // 重量字段：允许负号（辅助重量）与小数，整数目标字段不接受
                        allowDecimalNegative = true,
                    )
                }
            }
        }
    }
}

/** 整行可切换的字段开关：toggleable + Role.Switch，触摸目标 ≥48dp。 */
@Composable
private fun FieldSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FitSize.touchMin)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = FitSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

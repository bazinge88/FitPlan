package com.fitplan.app.ui.pr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitplan.app.ui.design.FitCard
import com.fitplan.app.ui.design.FitChipTone
import com.fitplan.app.ui.design.FitEmptyState
import com.fitplan.app.ui.design.FitHairline
import com.fitplan.app.ui.design.FitMetaChip
import com.fitplan.app.ui.design.FitScreenTitle
import com.fitplan.app.ui.design.FitScaffold
import com.fitplan.app.ui.design.FitSectionHeader
import com.fitplan.app.ui.design.FitTopBar
import com.fitplan.app.ui.design.fitEnter
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.theme.FitType

/**
 * 重量与单位之间补空格（全 App「52.5 kg」口径一致）。
 * weightText 由 PrViewModel 的纯函数拼成（现状无空格），前端只在展示层归一，不改 VM 契约。
 */
private fun spacedWeightText(weightText: String): String = when {
    weightText.endsWith("kg") -> weightText.dropLast(2) + " kg"
    weightText.endsWith("lb") -> weightText.dropLast(2) + " lb"
    else -> weightText
}

/**
 * PR 记录：顶栏返回 + 内容区大标题「PR 记录」+ 当前训练者 chip + 按动作分组卡（档位行）。
 * 空态/加载态文案与「PR@n：重量（首达 x/x）」口径不变。
 */
@Composable
fun PrScreen(onBack: () -> Unit) {
    val vm: PrViewModel = viewModel(factory = PrViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()

    FitScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // 大标题放内容区，顶栏只留返回
            FitTopBar(title = "", onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FitSpace.xl, end = FitSpace.xl, top = FitSpace.md),
            ) {
                FitScreenTitle(title = "PR 记录")
                // 本地多训练者：页内展示当前统计归属
                state.userName?.let { name ->
                    FitMetaChip(
                        text = name,
                        tone = FitChipTone.Neutral,
                        modifier = Modifier.padding(top = FitSpace.md),
                    )
                }
            }
            when {
                state.loading -> FitEmptyState(
                    title = "加载中…",
                    modifier = Modifier.weight(1f),
                    icon = null,
                )

                state.groups.isEmpty() -> FitEmptyState(
                    title = "暂无记录",
                    description = "打卡时记录重量后自动生成。",
                    modifier = Modifier.weight(1f),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = FitSpace.xl,
                        end = FitSpace.xl,
                        top = FitSpace.md,
                        bottom = FitSpace.huge,
                    ),
                ) {
                    state.groups.forEachIndexed { groupIndex, group ->
                        item(key = "group-${group.exerciseId}") {
                            // 区块标题自带 24dp 上间距、12dp 下间距（FitSectionHeader 内建）
                            FitSectionHeader(title = group.exerciseName)
                        }
                        item(key = "card-${group.exerciseId}") {
                            FitCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fitEnter(groupIndex),
                                contentPadding = PaddingValues(),
                            ) {
                                group.rows.forEachIndexed { index, row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = FitSize.rowMin)
                                            .padding(horizontal = FitSpace.xl, vertical = FitSpace.md),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            // 档位行文案沿用旧口径，仅在单位前补空格
                                            text = "PR@${row.reps}：${spacedWeightText(row.weightText)}（首达 ${row.firstDateText}）",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFeatureSettings = FitType.numeric.fontFeatureSettings,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (index < group.rows.lastIndex) {
                                        FitHairline(modifier = Modifier.padding(start = FitSpace.xl))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

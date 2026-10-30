package com.fitplan.app.ui.pr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.ui.plan.DEFAULT_USER_ID
import com.fitplan.app.util.TimeUtil
import com.fitplan.app.util.buildPrEntries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** PR 页单个次数档行：重量与首达日期的展示文案。 */
data class PrRowUi(
    val reps: Int,
    /** 已带单位（kg/lb）的重量文案。 */
    val weightText: String,
    val firstDateText: String,
)

/** PR 页单个动作分组。 */
data class PrGroupUi(
    val exerciseId: Long,
    val exerciseName: String,
    /** 次数档升序。 */
    val rows: List<PrRowUi>,
)

data class PrUiState(
    val loading: Boolean = true,
    /** 当前训练者名（PR 按训练者分别统计）。 */
    val userName: String? = null,
    val groups: List<PrGroupUi> = emptyList(),
)

/** 纯函数：真实打卡 + 组合条目 + 动作库 + 设置 + 训练者 → PR 页状态（只统计当前训练者，分组沿用动作库顺序，档位升序）。 */
fun buildPrUiState(
    checkins: List<SetCheckinEntity>,
    items: List<ComboItemEntity>,
    exercises: List<ExerciseEntity>,
    settings: SettingsEntity?,
    users: List<UserEntity> = emptyList(),
): PrUiState {
    val activeUserId = settings?.activeUserId ?: DEFAULT_USER_ID
    val userName = users.find { it.id == activeUserId }?.name
    // 重量单位沿用全局设置：全 App 均为「库内原值 + 单位标签」显示，不做 kg/lb 数值换算
    val unit = if (settings?.unitIsLb == true) "lb" else "kg"
    val entriesByExercise = buildPrEntries(
        checkins.filter { it.userId == activeUserId },
        items,
    ).groupBy { it.exerciseId }
    val groups = exercises.mapNotNull { exercise ->
        val entries = entriesByExercise[exercise.id] ?: return@mapNotNull null
        PrGroupUi(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            rows = entries.sortedBy { it.reps }.map { entry ->
                PrRowUi(
                    reps = entry.reps,
                    weightText = "${TimeUtil.formatWeight(entry.maxWeight)}$unit",
                    firstDateText = TimeUtil.formatShort(TimeUtil.fromEpochDay(entry.firstDate)),
                )
            },
        )
    }
    return PrUiState(loading = false, userName = userName, groups = groups)
}

/** PR 记录页：当前训练者的真实打卡组按动作 × 次数档聚合出的历史 PR。 */
class PrViewModel(private val daos: AllDaos) : ViewModel() {

    /** 全部打卡 + 全部组合条目 + 动作库 + 设置/训练者合并为页面状态（设置与训练者合并为一层，保持四参以内）。 */
    val state: StateFlow<PrUiState> = combine(
        daos.setCheckinDao.observeAll(),
        daos.comboDao.observeAllWithItems(),
        daos.exerciseDao.observeAll(),
        combine(daos.settingsDao.observe(), daos.userDao.observeAll(), ::Pair),
    ) { checkins, combos, exercises, (settings, users) ->
        buildPrUiState(checkins, combos.flatMap { it.items }, exercises, settings, users)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrUiState())

    companion object {
        val Factory = viewModelFactory { initializer { PrViewModel(Graph.daos) } }
    }
}

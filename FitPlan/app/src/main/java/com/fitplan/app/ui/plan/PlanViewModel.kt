package com.fitplan.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.model.SchemeType
import com.fitplan.app.data.model.Split3Binding
import com.fitplan.app.data.model.Schemes
import com.fitplan.app.util.buildTrainingDurations
import com.fitplan.app.util.durationsForUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(private val daos: AllDaos) : ViewModel() {

    /** date(epochDay) -> 当日安排；初始 null = 数据未到（UI 显示加载中），到达后非 null。 */
    val dayPlans: StateFlow<Map<Long, DayPlanEntity>?> = daos.dayPlanDao.observeAll()
        .map { list -> list.associateBy { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val combos = daos.comboDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** date(epochDay) -> 当前训练者当日训练用时（周计划页展示；无打卡的天不出现）。 */
    val durations: StateFlow<Map<Long, Long>> = combine(
        daos.setCheckinDao.observeAll(),
        daos.settingsDao.observe(),
    ) { checkins, settings ->
        durationsForUser(
            buildTrainingDurations(checkins),
            settings?.activeUserId ?: DEFAULT_USER_ID,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun comboName(id: Long): String? = combos.value.find { it.id == id }?.name

    /** 保存某天安排：isRest=true 时 comboId 置空。 */
    fun setDay(date: Long, isRest: Boolean, comboId: Long?) = viewModelScope.launch {
        daos.dayPlanDao.upsert(
            DayPlanEntity(date = date, isRest = isRest, comboId = if (isRest) null else comboId),
        )
    }

    /** 移除某天安排（回到未安排）；该天历史记录（打卡/逃组/备注/体重/当次重量）按 date 存储不受影响。 */
    fun clearDay(date: Long) = viewModelScope.launch {
        daos.dayPlanDao.deleteByDate(date)
    }

    /** 复制某天到另一天（源天未设置则清除目标天）。 */
    fun copyDay(from: Long, to: Long) = viewModelScope.launch {
        val source = daos.dayPlanDao.getByDate(from)
        if (source == null) {
            daos.dayPlanDao.deleteByDate(to)
        } else {
            daos.dayPlanDao.upsert(source.copy(date = to))
        }
    }

    /**
     * 复制上周（sourceMonday 周）计划到目标周（targetMonday）：按 7 天逐日镜像——
     * 源周无安排的日子删除目标周旧安排，有安排的复制过来；计数只算复制天数（L03/L01 事务）。
     */
    fun copyWeek(sourceMonday: Long, targetMonday: Long, onDone: (Int) -> Unit) =
        viewModelScope.launch {
            var copied = 0
            daos.runInTransaction {
                val source = daos.dayPlanDao.getRange(sourceMonday, sourceMonday + 6)
                    .associateBy { it.date }
                copied = 0
                for (offset in 0L..6L) {
                    val targetDate = targetMonday + offset
                    val src = source[sourceMonday + offset]
                    if (src == null) {
                        daos.dayPlanDao.deleteByDate(targetDate)
                    } else {
                        daos.dayPlanDao.upsert(src.copy(date = targetDate))
                        copied++
                    }
                }
            }
            onDone(copied)
        }

    /** 应用内置方案生成一周计划（覆盖该周已有安排）。 */
    fun applyScheme(
        type: SchemeType,
        mondayEpochDay: Long,
        split3: Split3Binding,
        cycleComboIds: List<Long>,
        onDone: () -> Unit = {},
    ) = viewModelScope.launch {
        daos.dayPlanDao.upsertAll(
            Schemes.generate(type, mondayEpochDay, split3, cycleComboIds),
        )
        onDone()
    }

    companion object {
        val Factory = viewModelFactory { initializer { PlanViewModel(Graph.daos) } }
    }
}

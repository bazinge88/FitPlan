package com.fitplan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.export.ExportData
import com.fitplan.app.export.ExportRequest
import com.fitplan.app.export.MarkdownExporter
import com.fitplan.app.util.TimeUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 导出范围。 */
sealed interface ExportScope {
    data object All : ExportScope
    /** 指定周（该周周一）。 */
    data class Week(val mondayEpochDay: Long) : ExportScope
    /** 日期范围（含首尾）。 */
    data class Range(val fromEpochDay: Long, val toEpochDay: Long) : ExportScope
}

/** 按导出范围做内存过滤；range 为 null 表示全部。 */
private fun <T> List<T>.inExportRange(range: Pair<Long, Long>?, dateOf: (T) -> Long): List<T> =
    if (range == null) this else filter { dateOf(it) in range.first..range.second }

class SettingsViewModel(private val daos: AllDaos) : ViewModel() {

    val settings = daos.settingsDao.observe()
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    /** 训练者档案列表（打卡与 PR 按训练者区分）。 */
    val users = daos.userDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 切换当前训练者：打卡归属与 PR 统计随之切换。 */
    fun selectUser(userId: Long) = updateSettings { it.copy(activeUserId = userId) }

    /** 添加训练者（至少保留一个）。 */
    fun addUser(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            daos.userDao.upsert(UserEntity(name = trimmed, sort = daos.userDao.getAll().size))
        }
    }

    /** 删除训练者：连同其打卡、逃标、当次重量、体重、当日备注与动作备注一并清理；最后一名不可删除；删的是当前训练者时切回第一名。 */
    fun deleteUser(user: UserEntity) = viewModelScope.launch {
        val users = daos.userDao.getAll()
        if (users.size <= 1) return@launch
        daos.setCheckinDao.deleteForUser(user.id)
        daos.setSkipDao.deleteForUser(user.id)
        daos.sessionWeightDao.deleteForUser(user.id)
        daos.bodyWeightDao.deleteForUser(user.id)
        daos.dayNoteDao.deleteForUser(user.id)
        daos.exerciseNoteDao.deleteForUser(user.id)
        daos.userDao.delete(user)
        val settings = daos.settingsDao.get() ?: SettingsEntity()
        if (settings.activeUserId == user.id) {
            daos.settingsDao.upsert(settings.copy(activeUserId = users.first { it.id != user.id }.id))
        }
    }

    fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) = viewModelScope.launch {
        val current = daos.settingsDao.get() ?: SettingsEntity()
        daos.settingsDao.upsert(transform(current))
    }

    fun setUnit(isLb: Boolean) = updateSettings { it.copy(unitIsLb = isLb) }

    fun setDefaultFields(sets: Boolean, reps: Boolean, weight: Boolean, duration: Boolean) =
        updateSettings {
            it.copy(defTrackSets = sets, defTrackReps = reps, defTrackWeight = weight, defTrackDuration = duration)
        }

    /** 组装导出内容。 */
    suspend fun buildMarkdown(scope: ExportScope): String {
        val settings = daos.settingsDao.get() ?: SettingsEntity()
        val exercises = daos.exerciseDao.observeAll().first()
        val combos = daos.comboDao.observeAllWithItems().first()
        val dayPlansAll = daos.dayPlanDao.observeAll().first()
        val range: Pair<Long, Long>? = when (scope) {
            is ExportScope.All -> null
            is ExportScope.Week -> scope.mondayEpochDay to (scope.mondayEpochDay + 6)
            is ExportScope.Range -> scope.fromEpochDay to scope.toEpochDay
        }
        val logs = if (range == null) {
            daos.logDao.getAllWithSets()
        } else {
            daos.logDao.getWithSetsForRange(range.first, range.second)
        }
        val checkins = daos.setCheckinDao.getAll().inExportRange(range) { it.date }
        val skips = daos.setSkipDao.getAll().inExportRange(range) { it.date }
        val bodyWeights = daos.bodyWeightDao.observeAll().first().inExportRange(range) { it.date }
        val dayNotes = daos.dayNoteDao.observeAll().first().inExportRange(range) { it.date }
        val exerciseNotes = daos.exerciseNoteDao.observeAll().first().inExportRange(range) { it.date }
        val users = daos.userDao.getAll()
        val dayPlans = dayPlansAll.inExportRange(range) { it.date }
        val scopeLabel = when (scope) {
            is ExportScope.All -> "全部"
            is ExportScope.Week ->
                "指定周 ${TimeUtil.formatDate(TimeUtil.fromEpochDay(scope.mondayEpochDay))} 至 " +
                    TimeUtil.formatDate(TimeUtil.fromEpochDay(scope.mondayEpochDay + 6))
            is ExportScope.Range ->
                "日期范围 ${TimeUtil.formatDate(TimeUtil.fromEpochDay(scope.fromEpochDay))} 至 " +
                    TimeUtil.formatDate(TimeUtil.fromEpochDay(scope.toEpochDay))
        }
        return MarkdownExporter.export(
            ExportData(
                settings = settings,
                exercises = exercises,
                combos = combos,
                dayPlans = dayPlans,
                logEntries = logs,
                checkins = checkins,
                comboNames = combos.associate { it.combo.id to it.combo.name },
                exerciseNames = exercises.associate { it.id to it.name },
                userNames = users.associate { it.id to it.name },
                bodyWeights = bodyWeights,
                dayNotes = dayNotes,
                exerciseNotes = exerciseNotes,
                skips = skips,
            ),
            ExportRequest(scopeLabel = scopeLabel, rangeFrom = range?.first, rangeTo = range?.second),
        )
    }

    /** 清空所有业务数据（保留设置）。 */
    fun clearAllData(onDone: () -> Unit) = viewModelScope.launch {
        daos.logDao.clearSets()
        daos.logDao.clearEntries()
        daos.setCheckinDao.clearAll()
        daos.setSkipDao.clearAll()
        daos.sessionWeightDao.clearAll()
        daos.bodyWeightDao.clearAll()
        daos.dayNoteDao.clearAll()
        daos.exerciseNoteDao.clearAll()
        daos.dayPlanDao.clearAll()
        // 组合与动作按外键级联删除其子表（组合条目 / 记录组）
        daos.comboDao.getAll().forEach { daos.comboDao.deleteById(it.id) }
        daos.exerciseDao.observeAll().first().forEach { daos.exerciseDao.delete(it) }
        onDone()
    }

    companion object {
        val Factory = viewModelFactory { initializer { SettingsViewModel(Graph.daos) } }
    }
}

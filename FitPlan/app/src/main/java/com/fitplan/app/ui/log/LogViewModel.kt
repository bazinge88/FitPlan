package com.fitplan.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.LogEntryEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.data.db.LogSetEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.util.TimeUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 保存用的一组数据草稿。 */
data class SetDraft(
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Double? = null,
    val duration: Int? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(private val daos: AllDaos) : ViewModel() {

    private val _date = MutableStateFlow(TimeUtil.today().toEpochDay())
    val date: MutableStateFlow<Long> = _date

    val exercises = daos.exerciseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings = daos.settingsDao.observe()
        .map { it ?: SettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsEntity())

    /** 初始 null = 数据未到（UI 显示加载中）；数据到达后非 null。 */
    val entries: StateFlow<List<LogEntryWithSets>?> = _date.flatMapLatest { day ->
        daos.logDao.observeForDate(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setDate(epochDay: Long) {
        _date.value = epochDay
    }

    fun deleteEntry(entry: LogEntryEntity) = viewModelScope.launch {
        daos.logDao.deleteEntry(entry)
    }

    /** 保存条目：entryId>0 为编辑（整条重写组数据，保留原排序）。 */
    fun saveEntry(
        entryId: Long,
        date: Long,
        exerciseId: Long,
        note: String,
        sets: List<SetDraft>,
        originalSort: Int? = null,
        onDone: () -> Unit = {},
    ) = viewModelScope.launch {
        // 事务：条目行与组行重写整体提交或回滚（L01）；onDone 在事务外
        daos.runInTransaction {
            val sort = originalSort ?: ((daos.logDao.minSortForDate(date) ?: 0) - 1)
            val id = daos.logDao.upsertEntry(
                LogEntryEntity(
                    id = if (entryId > 0) entryId else 0,
                    date = date,
                    exerciseId = exerciseId,
                    note = note.trim(),
                    sort = sort,
                ),
            )
            val finalId = if (entryId > 0) entryId else id
            daos.logDao.clearSets(finalId)
            daos.logDao.insertSets(
                sets.mapIndexed { index, draft ->
                    LogSetEntity(
                        entryId = finalId,
                        sort = index,
                        sets = draft.sets,
                        reps = draft.reps,
                        weight = draft.weight,
                        duration = draft.duration,
                    )
                },
            )
        }
        onDone()
    }

    companion object {
        val Factory = viewModelFactory { initializer { LogViewModel(Graph.daos) } }
    }
}

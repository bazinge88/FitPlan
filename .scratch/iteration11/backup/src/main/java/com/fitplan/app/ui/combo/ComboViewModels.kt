package com.fitplan.app.ui.combo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.ExerciseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 保存用的一条组合动作草稿。 */
data class ComboItemDraft(
    val exerciseId: Long,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeight: Double? = null,
    val targetDuration: Int? = null,
)

class ComboListViewModel(private val daos: AllDaos) : ViewModel() {
    val combos = daos.comboDao.observeAllWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises = daos.exerciseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(comboId: Long) = viewModelScope.launch {
        // 先把绑定该组合的天置为休息日，与删除确认文案一致
        daos.dayPlanDao.getByCombo(comboId).forEach {
            daos.dayPlanDao.upsert(it.copy(isRest = true, comboId = null))
        }
        // 组合删除会级联删条目，但打卡/逃标表无外键，需按条目 id 清理孤儿打卡与逃标
        val itemIds = daos.comboDao.getWithItems(comboId)?.items?.map { it.id }.orEmpty()
        daos.comboDao.deleteById(comboId)
        if (itemIds.isNotEmpty()) {
            daos.setCheckinDao.deleteForComboItems(itemIds)
            daos.setSkipDao.deleteForComboItems(itemIds)
        }
    }

    fun copyCombo(source: ComboWithItems) = viewModelScope.launch {
        val sort = (daos.comboDao.maxSort() ?: 0) + 1
        val newCombo = ComboEntity(name = source.combo.name + " 副本", sort = sort)
        val newId = daos.comboDao.upsert(newCombo)
        daos.comboDao.insertItems(
            source.items.sortedBy { it.sort }.map {
                ComboItemEntity(
                    comboId = newId,
                    exerciseId = it.exerciseId,
                    sort = it.sort,
                    targetSets = it.targetSets,
                    targetReps = it.targetReps,
                    targetWeight = it.targetWeight,
                    targetDuration = it.targetDuration,
                )
            },
        )
    }

    companion object {
        val Factory = viewModelFactory { initializer { ComboListViewModel(Graph.daos) } }
    }
}

class ComboEditViewModel(
    private val daos: AllDaos,
    private val comboId: Long,
) : ViewModel() {
    val exercises = daos.exerciseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unitIsLb = daos.settingsDao.observe()
        .map { it?.unitIsLb ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun loadCombo(): ComboWithItems? =
        if (comboId > 0) daos.comboDao.getWithItems(comboId) else null

    fun save(name: String, drafts: List<ComboItemDraft>, onDone: () -> Unit) =
        viewModelScope.launch {
            val id = if (comboId > 0) comboId else {
                daos.comboDao.upsert(ComboEntity(name = name))
            }
            val existing = daos.comboDao.getWithItems(id)
            daos.comboDao.upsert(
                ComboEntity(
                    id = id,
                    name = name,
                    sort = existing?.combo?.sort ?: (daos.comboDao.maxSort() ?: 0) + 1,
                ),
            )
            // 增量保存：exerciseId 对得上的旧条目沿用原 id（保住历史打卡，重排也不丢），
            // 只删除真正移除的条目并同步清理其打卡，其余按新增插入
            val oldItems = existing?.items?.sortedBy { it.sort }.orEmpty()
            val matchedIds = mutableMapOf<Int, Long>() // 草稿下标 -> 沿用的旧条目 id
            val usedOldIds = mutableSetOf<Long>()
            drafts.forEachIndexed { index, draft ->
                val old = oldItems.firstOrNull { it.id !in usedOldIds && it.exerciseId == draft.exerciseId }
                if (old != null) {
                    matchedIds[index] = old.id
                    usedOldIds += old.id
                }
            }
            val removedIds = oldItems.filter { it.id !in usedOldIds }.map { it.id }
            if (removedIds.isNotEmpty()) {
                daos.comboDao.deleteItemsByIds(removedIds)
                daos.setCheckinDao.deleteForComboItems(removedIds)
                daos.setSkipDao.deleteForComboItems(removedIds)
            }
            matchedIds.forEach { (index, oldId) ->
                val draft = drafts[index]
                daos.comboDao.updateItem(
                    ComboItemEntity(
                        id = oldId,
                        comboId = id,
                        exerciseId = draft.exerciseId,
                        sort = index,
                        targetSets = draft.targetSets,
                        targetReps = draft.targetReps,
                        targetWeight = draft.targetWeight,
                        targetDuration = draft.targetDuration,
                    ),
                )
            }
            val inserted = drafts.mapIndexedNotNull { index, draft ->
                if (index in matchedIds) {
                    null
                } else {
                    ComboItemEntity(
                        comboId = id,
                        exerciseId = draft.exerciseId,
                        sort = index,
                        targetSets = draft.targetSets,
                        targetReps = draft.targetReps,
                        targetWeight = draft.targetWeight,
                        targetDuration = draft.targetDuration,
                    )
                }
            }
            if (inserted.isNotEmpty()) daos.comboDao.insertItems(inserted)
            onDone()
        }

    companion object {
        fun factory(comboId: Long) = viewModelFactory {
            initializer { ComboEditViewModel(Graph.daos, comboId) }
        }
    }
}

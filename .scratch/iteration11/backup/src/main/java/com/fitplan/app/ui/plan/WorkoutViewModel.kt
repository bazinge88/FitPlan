package com.fitplan.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitplan.app.Graph
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 训练详情页某一组的数据。checked = 该组已打卡（行存在）；reps 为完成次数（null = 只记完成）。 */
data class WorkoutSetUi(
    val setIndex: Int,
    val checked: Boolean,
    val reps: Int?,
    /** 该组自己的重量（快照或手动编辑值）；未记为 null。 */
    val weight: Double? = null,
    /** 该组被标逃（逃标行存在，行存在即逃）；打卡覆盖逃标后为 false，逃不算完成。 */
    val skipped: Boolean = false,
)

/** 训练详情页单个动作卡片的数据。 */
data class WorkoutItemUi(
    val comboItemId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeight: Double?,
    val targetDuration: Int?,
    /** 该动作是否记录重量（不记录则不显示重量调整行）。 */
    val trackWeight: Boolean = true,
    /** 当天的当次重量（仅当天有效，不写回组合条目）。 */
    val sessionWeight: Double? = null,
    /** 同步提醒：历史当次最大重量超过基准时非 null，值为该最大重量。 */
    val syncPromptWeight: Double? = null,
    /** 该动作在全部组合中出现的条目数（>1 时精确输入对话框提供「同步到所有组合」选项）。 */
    val syncCount: Int = 1,
    /** 当天该动作的动作备注（当前训练者），未写为 null。 */
    val todayNote: String? = null,
    /** 上次动作备注：当天之前最近一次、同一训练者的同动作备注，无历史为 null。 */
    val lastNote: String? = null,
    val sets: List<WorkoutSetUi>,
    val doneCount: Int,
)

data class WorkoutUiState(
    val loading: Boolean = true,
    val comboName: String? = null,
    /** 重量单位（来自设置）。 */
    val unitIsLb: Boolean = false,
    /** 当天当前训练者的体重（null = 未记录；可选记录，不做任何催记或漏记提示）。 */
    val bodyWeight: Double? = null,
    /** 当天当日备注（当前训练者），未写为 null。 */
    val dayNote: String? = null,
    val items: List<WorkoutItemUi> = emptyList(),
)

/** 打卡默认次数：目标次数优先，其次沿用前一组已打卡的次数，否则 null（只记完成）。 */
fun defaultRepsFor(item: WorkoutItemUi, setIndex: Int): Int? =
    item.targetReps ?: item.sets.getOrNull(setIndex - 1)?.reps

/** 默认训练者 id：迁移播种与设置行缺省值一致。 */
const val DEFAULT_USER_ID = 1L

/** 设置、当次重量、逃标与体重的合并载体：内层 combine 五参数重载的上半段（仅由 workoutFlowBundle() 使用）。 */
data class WorkoutCoreBundle(
    val settings: SettingsEntity?,
    val sessionWeights: List<SessionWeightEntity>,
    val skips: List<SetSkipEntity>,
    val bodyWeights: List<BodyWeightEntity>,
)

/** 设置、当次重量、逃标、体重与两轨备注的合并载体：让 combine 五参数重载容纳七条流。 */
data class WorkoutFlowBundle(
    val settings: SettingsEntity?,
    val sessionWeights: List<SessionWeightEntity>,
    val skips: List<SetSkipEntity>,
    val bodyWeights: List<BodyWeightEntity>,
    val dayNotes: List<DayNoteEntity>,
    val exerciseNotes: List<ExerciseNoteEntity>,
)

/** 六条流的合并接线（内层两级嵌套保持 combine 五参数重载）：ViewModel 与通知路径共用的唯一实现。 */
fun workoutFlowBundle(daos: AllDaos, date: Long): Flow<WorkoutFlowBundle> =
    combine(
        combine(
            daos.settingsDao.observe(),
            daos.sessionWeightDao.observeAll(),
            daos.setSkipDao.observeForDate(date),
            daos.bodyWeightDao.observeAll(),
            ::WorkoutCoreBundle,
        ),
        daos.dayNoteDao.observeAll(),
        daos.exerciseNoteDao.observeAll(),
    ) { core, dayNotes, exerciseNotes ->
        WorkoutFlowBundle(
            core.settings, core.sessionWeights, core.skips, core.bodyWeights,
            dayNotes, exerciseNotes,
        )
    }

/** 纯函数：当天计划 + 组合 + 动作 + 打卡 + WorkoutFlowBundle（设置 + 当次重量 + 逃标 + 体重 + 两轨备注）→ 页面状态（ViewModel 与训练会话通知共用）。 */
fun buildWorkoutUiState(
    date: Long,
    plans: List<DayPlanEntity>,
    combos: List<ComboWithItems>,
    exercises: List<ExerciseEntity>,
    checkins: List<SetCheckinEntity>,
    flow: WorkoutFlowBundle,
): WorkoutUiState {
    val (settings, sessionWeights, skips, bodyWeights, dayNotes, exerciseNotes) = flow
    val plan = plans.find { it.date == date }
    val combo = plan?.takeIf { !it.isRest && it.comboId != null }
        ?.let { p -> combos.find { it.combo.id == p.comboId } }
    // 本地多训练者：只统计当前训练者的打卡、逃标、当次重量与体重，不同用户互不可见、互不覆盖
    val activeUserId = settings?.activeUserId ?: DEFAULT_USER_ID
    val userBodyWeight = bodyWeights.find { it.date == date && it.userId == activeUserId }?.weight
    val dayNote = dayNotes.find { it.date == date && it.userId == activeUserId }?.note
    if (combo == null) {
        // 休息日/无计划：清单为空，但体重入口仍可用（休息日同样可记），单位照常跟随设置
        return WorkoutUiState(
            loading = false,
            unitIsLb = settings?.unitIsLb ?: false,
            bodyWeight = userBodyWeight,
            dayNote = dayNote,
        )
    }
    val userCheckins = checkins.filter { it.userId == activeUserId }
    val userSkips = skips.filter { it.userId == activeUserId }
    val userSessionWeights = sessionWeights.filter { it.userId == activeUserId }
    val byKey = userCheckins.associateBy { it.comboItemId to it.setIndex }
    val skipKeys = userSkips.map { it.comboItemId to it.setIndex }.toSet()
    // 已逃组序可稀疏（如缩组遗留的越界逃标），行数须盖住最大已逃组序而非逃标条数
    val skipMaxIndexByItem = userSkips.groupBy { it.comboItemId }
        .mapValues { (_, rows) -> rows.maxOf { it.setIndex } }
    // 历史当次最大重量（当天之前，按动作）：同步提醒的触发基准
    val histMaxByExercise = userSessionWeights
        .filter { it.date < date }
        .groupBy { it.exerciseId }
        .mapValues { (_, records) -> records.maxOf { it.weight } }
    // 每个动作在全部组合中出现的条目数，供「同步到所有组合」选项展示影响范围
    val syncCountByExercise = combos
        .flatMap { it.items }
        .groupingBy { it.exerciseId }
        .eachCount()
    val items = combo.items.sortedBy { it.sort }.map { item ->
        val exercise = exercises.find { it.id == item.exerciseId }
        val doneCount = byKey.count { it.key.first == item.id }
        // 行数与直接打卡指针同口径（QuickCheckIn.nextSet）：目标组数决定行数，
        // 已打组数与最大已逃组序超过目标同样增行，保证每行可见可操作；无目标时至少一行
        val rowCount = maxOf(
            item.targetSets ?: 0, doneCount, (skipMaxIndexByItem[item.id] ?: -1) + 1, 1,
        )
        val trackWeight = exercise?.trackWeight ?: true
        // 提醒条件：动作记录重量、历史最大 > 已处理重量（未处理视为 -∞）且 > 当前条目目标（无目标视为需要）
        val histMax = histMaxByExercise[item.exerciseId]
        val ack = exercise?.ackMaxWeight
        val syncPromptWeight = if (
            trackWeight && histMax != null && histMax > (ack ?: Double.NEGATIVE_INFINITY) &&
            (item.targetWeight == null || histMax > item.targetWeight)
        ) {
            histMax
        } else {
            null
        }
        WorkoutItemUi(
            comboItemId = item.id,
            exerciseId = item.exerciseId,
            exerciseName = exercise?.name ?: "未知动作",
            targetSets = item.targetSets,
            targetReps = item.targetReps,
            targetWeight = item.targetWeight,
            targetDuration = item.targetDuration,
            trackWeight = trackWeight,
            sessionWeight = userSessionWeights
                .find { it.date == date && it.exerciseId == item.exerciseId }?.weight,
            syncPromptWeight = syncPromptWeight,
            syncCount = syncCountByExercise[item.exerciseId] ?: 1,
            todayNote = exerciseNotes.find {
                it.date == date && it.exerciseId == item.exerciseId && it.userId == activeUserId
            }?.note,
            lastNote = exerciseNotes.filter {
                it.exerciseId == item.exerciseId && it.userId == activeUserId && it.date < date
            }.maxByOrNull { it.date }?.note,
            sets = (0 until rowCount).map { i ->
                val row = byKey[item.id to i]
                WorkoutSetUi(
                    setIndex = i,
                    checked = row != null,
                    reps = row?.reps,
                    weight = row?.weight,
                    skipped = (item.id to i) in skipKeys,
                )
            },
            doneCount = doneCount,
        )
    }
    return WorkoutUiState(
        loading = false,
        comboName = combo.combo.name,
        unitIsLb = settings?.unitIsLb ?: false,
        bodyWeight = userBodyWeight,
        dayNote = dayNote,
        items = items,
    )
}

class WorkoutViewModel(private val daos: AllDaos, private val date: Long) : ViewModel() {

    /** 当天安排 + 组合内动作 + 打卡与逃标数据合并为页面状态。 */
    val state: StateFlow<WorkoutUiState> = combine(
        daos.dayPlanDao.observeAll(),
        daos.comboDao.observeAllWithItems(),
        daos.exerciseDao.observeAll(),
        daos.setCheckinDao.observeForDate(date),
        // 设置、当次重量、逃标与体重先并一组，再并入两轨备注（与通知路径共用 workoutFlowBundle）
        workoutFlowBundle(daos, date),
    ) { plans, combos, exercises, checkins, bundle ->
        buildWorkoutUiState(date, plans, combos, exercises, checkins, bundle)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutUiState())

    /** 打卡：行已存在则只覆盖次数并刷新打卡时刻（保留该组已编辑的重量，动作级当次重量不覆盖具体组）；
     *  新行则记录打卡时刻、快照该训练者该动作当天的当次重量；没记过当次重量则 weight=null（只记次数）。
     *  首组打卡时打卡时刻与会话起点同源同值（与通知接收器路径一致）。 */
    fun checkIn(comboItemId: Long, setIndex: Int, reps: Int?) = viewModelScope.launch {
        val userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
        val now = System.currentTimeMillis()
        val existed = daos.setCheckinDao.updateRepsIfPresent(date, comboItemId, setIndex, userId, reps, now)
        if (existed == 0) {
            // 快照式重量：解析 comboItemId → exerciseId，取当天的当次重量写入该组
            val exerciseId = daos.comboDao.observeAllWithItems().first()
                .flatMap { it.items }.find { it.id == comboItemId }?.exerciseId
            val weight = exerciseId?.let { id ->
                daos.sessionWeightDao.observeAll().first()
                    .find { it.date == date && it.exerciseId == id && it.userId == userId }?.weight
            }
            daos.setCheckinDao.upsert(
                SetCheckinEntity(
                    date = date, comboItemId = comboItemId, setIndex = setIndex,
                    reps = reps, weight = weight, userId = userId, checkedAt = now,
                ),
            )
        }
        // 打卡覆盖逃标：该组若被标逃，打卡即清除逃标（行存在即打卡，逃标消失）
        daos.setSkipDao.delete(date, comboItemId, setIndex, userId)
        // 首组打卡写用时起点：会话进行中、任一入口的首次打卡都写（与通知接收器路径幂等共存）
        val session = daos.workoutSessionDao.get()
        if (session != null && session.date == date && session.startedAt == null) {
            daos.workoutSessionDao.upsert(session.copy(startedAt = now))
        }
    }

    /** 取消打卡：删行（按当前训练者）。 */
    fun cancelCheckIn(comboItemId: Long, setIndex: Int) = viewModelScope.launch {
        daos.setCheckinDao.delete(
            date, comboItemId, setIndex, daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID,
        )
    }

    /** 标逃：当前训练者把该组标为逃组（行存在即逃）；逃不算完成，直接打卡跳过已逃组继续推进。 */
    fun skipSet(comboItemId: Long, setIndex: Int) {
        viewModelScope.launch {
            daos.setSkipDao.upsert(
                SetSkipEntity(
                    date = date,
                    comboItemId = comboItemId,
                    setIndex = setIndex,
                    userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID,
                ),
            )
        }
    }

    /** 取消逃标：删行（按当前训练者），该组回到未练。 */
    fun cancelSetSkip(comboItemId: Long, setIndex: Int) {
        viewModelScope.launch {
            daos.setSkipDao.delete(
                date, comboItemId, setIndex, daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID,
            )
        }
    }

    /** 已完成组次数原子增减：SQL 内基于当前值计算，快速连点不丢增量，下限 1；打卡时刻刷新为本次。 */
    fun adjustReps(comboItemId: Long, setIndex: Int, delta: Int) {
        viewModelScope.launch {
            daos.setCheckinDao.adjustReps(
                date, comboItemId, setIndex,
                daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID, delta,
                System.currentTimeMillis(),
            )
        }
    }

    /** 编辑单组重量：仅写该训练者该组自己的 weight，动作级当次重量与其互不覆盖。 */
    fun updateSetWeight(comboItemId: Long, setIndex: Int, weight: Double?) {
        viewModelScope.launch {
            daos.setCheckinDao.updateSetWeight(
                date, comboItemId, setIndex, daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID, weight,
            )
        }
    }

    /** 记录当次重量（只记当天、当前训练者，不写回组合条目）；null 表示取消当次记录。 */
    fun recordSessionWeight(exerciseId: Long, weight: Double?) {
        viewModelScope.launch {
            val userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
            if (weight == null) {
                daos.sessionWeightDao.delete(date, exerciseId, userId)
            } else {
                daos.sessionWeightDao.upsert(
                    SessionWeightEntity(date = date, exerciseId = exerciseId, weight = weight, userId = userId),
                )
            }
        }
    }

    /** 记录当天体重（当前训练者，一天一条，重复输入覆盖）。 */
    fun recordBodyWeight(weight: Double) {
        viewModelScope.launch {
            val userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
            daos.bodyWeightDao.upsert(BodyWeightEntity(date = date, weight = weight, userId = userId))
        }
    }

    /** 记录/覆盖当天当日备注（当前训练者）；空文本 = 清除（删行）。 */
    fun recordDayNote(note: String) {
        viewModelScope.launch {
            val userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
            if (note.isBlank()) {
                daos.dayNoteDao.delete(date, userId)
            } else {
                daos.dayNoteDao.upsert(DayNoteEntity(date = date, note = note, userId = userId))
            }
        }
    }

    /** 记录/覆盖当天该动作的动作备注（当前训练者）；票面未授权清空，空文本直接忽略。 */
    fun recordExerciseNote(exerciseId: Long, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            val userId = daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
            daos.exerciseNoteDao.upsert(
                ExerciseNoteEntity(date = date, exerciseId = exerciseId, note = note, userId = userId),
            )
        }
    }

    /** 提醒同步（仅当前条目）：当前组合条目写为目标重量，并记住该重量不再提醒。 */
    fun syncToItem(comboItemId: Long, exerciseId: Long, weight: Double) {
        viewModelScope.launch {
            daos.comboDao.updateTargetWeight(comboItemId, weight)
            daos.exerciseDao.updateAckMaxWeight(exerciseId, weight)
        }
    }

    /** 提醒同步（全部条目）：该动作在所有组合中的目标重量统一写入，并记住该重量不再提醒。 */
    fun syncToAll(exerciseId: Long, weight: Double) {
        viewModelScope.launch {
            daos.comboDao.updateTargetWeightForExercise(exerciseId, weight)
            daos.exerciseDao.updateAckMaxWeight(exerciseId, weight)
        }
    }

    /** 忽略提醒：记住该重量，同重量不再提醒（更大重量出现时重新提醒）。 */
    fun dismissSyncPrompt(exerciseId: Long, weight: Double) {
        viewModelScope.launch { daos.exerciseDao.updateAckMaxWeight(exerciseId, weight) }
    }

    companion object {
        fun factory(date: Long) = viewModelFactory {
            initializer { WorkoutViewModel(Graph.daos, date) }
        }
    }
}

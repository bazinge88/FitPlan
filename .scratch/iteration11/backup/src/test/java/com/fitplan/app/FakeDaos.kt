package com.fitplan.app

import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.BodyWeightDao
import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboDao
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteDao
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanDao
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseNoteDao
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.ExerciseDao
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.LogDao
import com.fitplan.app.data.db.LogEntryEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.data.db.LogSetEntity
import com.fitplan.app.data.db.SetCheckinDao
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipDao
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SessionWeightDao
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SettingsDao
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserDao
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.data.db.WorkoutSessionDao
import com.fitplan.app.data.db.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 内存版 DayPlanDao：测试直接向 plans 灌数据。 */
class FakeDayPlanDao : DayPlanDao {
    val plans = MutableStateFlow<List<DayPlanEntity>>(emptyList())

    override fun observeAll(): Flow<List<DayPlanEntity>> = plans
    override fun observeRange(from: Long, to: Long): Flow<List<DayPlanEntity>> =
        plans.map { list -> list.filter { it.date in from..to } }
    override suspend fun getRange(from: Long, to: Long): List<DayPlanEntity> =
        plans.value.filter { it.date in from..to }
    override suspend fun getByDate(date: Long): DayPlanEntity? = plans.value.find { it.date == date }
    override suspend fun getByCombo(comboId: Long): List<DayPlanEntity> =
        plans.value.filter { it.comboId == comboId }

    override suspend fun upsert(plan: DayPlanEntity) {
        val list = plans.value.toMutableList()
        val idx = list.indexOfFirst { it.date == plan.date }
        if (idx >= 0) list[idx] = plan else list.add(plan)
        plans.value = list
    }
    override suspend fun upsertAll(plans: List<DayPlanEntity>) {
        plans.forEach { upsert(it) }
    }
    override suspend fun deleteByDate(date: Long) {
        plans.value = plans.value.filterNot { it.date == date }
    }
    override suspend fun clearAll() {
        plans.value = emptyList()
    }
}

/** 内存版 ComboDao：combos 直接存 ComboWithItems，条目 id 全局自增模拟 autoGenerate。 */
class FakeComboDao : ComboDao {
    val combos = MutableStateFlow<List<ComboWithItems>>(emptyList())

    private fun mutateCombos(transform: (MutableList<ComboWithItems>) -> Unit) {
        val list = combos.value.toMutableList()
        transform(list)
        combos.value = list
    }

    override fun observeAllWithItems(): Flow<List<ComboWithItems>> = combos
    override suspend fun getWithItems(id: Long): ComboWithItems? =
        combos.value.find { it.combo.id == id }
    override fun observeAll(): Flow<List<ComboEntity>> =
        combos.map { list -> list.map { it.combo } }
    override suspend fun getAll(): List<ComboEntity> = combos.value.map { it.combo }

    override suspend fun upsert(combo: ComboEntity): Long {
        if (combo.id > 0L) {
            mutateCombos { list ->
                val idx = list.indexOfFirst { it.combo.id == combo.id }
                if (idx >= 0) list[idx] = list[idx].copy(combo = combo) else list.add(ComboWithItems(combo, emptyList()))
            }
            return combo.id
        }
        val newId = (combos.value.maxOfOrNull { it.combo.id } ?: 0L) + 1L
        mutateCombos { list -> list.add(ComboWithItems(combo.copy(id = newId), emptyList())) }
        return newId
    }
    override suspend fun insertItems(items: List<ComboItemEntity>) {
        mutateCombos { list ->
            // 游标在批量内自增：一次插入 N 条各得不同 id，与 Room autoGenerate 语义一致
            var cursor = (list.flatMap { it.items }.maxOfOrNull { it.id } ?: 0L) + 1L
            items.forEach { item ->
                val idx = list.indexOfFirst { it.combo.id == item.comboId }
                check(idx >= 0) { "combo ${item.comboId} 不存在" }
                list[idx] = list[idx].copy(items = list[idx].items + item.copy(id = cursor++))
            }
        }
    }
    override suspend fun updateItem(item: ComboItemEntity) {
        mutateCombos { list ->
            val idx = list.indexOfFirst { c -> c.items.any { it.id == item.id } }
            check(idx >= 0) { "combo item ${item.id} 不存在" }
            list[idx] = list[idx].copy(items = list[idx].items.map { if (it.id == item.id) item else it })
        }
    }
    override suspend fun deleteItem(item: ComboItemEntity) = deleteItemsByIds(listOf(item.id))
    override suspend fun clearItems(comboId: Long) {
        mutateCombos { list ->
            val idx = list.indexOfFirst { it.combo.id == comboId }
            if (idx >= 0) list[idx] = list[idx].copy(items = emptyList())
        }
    }
    override suspend fun deleteItemsByIds(ids: List<Long>) {
        mutateCombos { list ->
            for (i in list.indices) {
                list[i] = list[i].copy(items = list[i].items.filterNot { it.id in ids })
            }
        }
    }
    override suspend fun updateTargetWeight(id: Long, weight: Double?) {
        mutateCombos { list ->
            for (i in list.indices) {
                val items = list[i].items
                val itemIdx = items.indexOfFirst { it.id == id }
                if (itemIdx >= 0) {
                    list[i] = list[i].copy(items = items.toMutableList().apply {
                        this[itemIdx] = this[itemIdx].copy(targetWeight = weight)
                    })
                }
            }
        }
    }
    override suspend fun updateTargetWeightForExercise(exerciseId: Long, weight: Double?) {
        mutateCombos { list ->
            for (i in list.indices) {
                val items = list[i].items
                if (items.any { it.exerciseId == exerciseId }) {
                    list[i] = list[i].copy(
                        items = items.map {
                            if (it.exerciseId == exerciseId) it.copy(targetWeight = weight) else it
                        },
                    )
                }
            }
        }
    }
    override suspend fun deleteById(id: Long) {
        mutateCombos { list -> list.removeAll { it.combo.id == id } }
    }
    override suspend fun maxSort(): Int? = combos.value.maxOfOrNull { it.combo.sort }
}

/** 内存版 ExerciseDao。 */
class FakeExerciseDao : ExerciseDao {
    val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())

    override fun observeAll(): Flow<List<ExerciseEntity>> = exercises
    override suspend fun getById(id: Long): ExerciseEntity? =
        exercises.value.find { it.id == id }

    override suspend fun upsert(exercise: ExerciseEntity): Long {
        val list = exercises.value.toMutableList()
        if (exercise.id > 0L) {
            val idx = list.indexOfFirst { it.id == exercise.id }
            if (idx >= 0) {
                list[idx] = exercise
                exercises.value = list
                return exercise.id
            }
        }
        val newId = if (exercise.id > 0L) exercise.id else (list.maxOfOrNull { it.id } ?: 0L) + 1L
        list.add(exercise.copy(id = newId))
        exercises.value = list
        return newId
    }
    override suspend fun delete(exercise: ExerciseEntity) {
        exercises.value = exercises.value.filterNot { it.id == exercise.id }
    }
    override suspend fun countByCategory(category: String): Int =
        exercises.value.count { it.category == category }

    override suspend fun updateAckMaxWeight(id: Long, weight: Double) {
        val list = exercises.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        check(idx >= 0) { "exercise $id 不存在" }
        list[idx] = list[idx].copy(ackMaxWeight = weight)
        exercises.value = list
    }
}

/** 内存版 UserDao。 */
class FakeUserDao : UserDao {
    val users = MutableStateFlow<List<UserEntity>>(emptyList())

    override fun observeAll(): Flow<List<UserEntity>> = users
    override suspend fun getAll(): List<UserEntity> = users.value

    override suspend fun upsert(user: UserEntity): Long {
        val list = users.value.toMutableList()
        if (user.id > 0L) {
            val idx = list.indexOfFirst { it.id == user.id }
            if (idx >= 0) {
                list[idx] = user
                users.value = list
                return user.id
            }
        }
        val newId = if (user.id > 0L) user.id else (list.maxOfOrNull { it.id } ?: 0L) + 1L
        list.add(user.copy(id = newId))
        users.value = list
        return newId
    }

    override suspend fun delete(user: UserEntity) {
        users.value = users.value.filterNot { it.id == user.id }
    }
}

/** 内存版 SetCheckinDao：真实实现 REPLACE / 删行 / 原子增减 / 按日过滤 / 清理 / 清空。唯一键含 userId。 */
class FakeSetCheckinDao : SetCheckinDao {
    val rows = MutableStateFlow<List<SetCheckinEntity>>(emptyList())

    override suspend fun upsert(checkin: SetCheckinEntity) {
        // REPLACE 语义：命中 (date, comboItemId, setIndex, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == checkin.date && it.comboItemId == checkin.comboItemId &&
                it.setIndex == checkin.setIndex && it.userId == checkin.userId
        }
        if (idx >= 0) list[idx] = checkin else list.add(checkin)
        rows.value = list
    }

    override suspend fun delete(date: Long, comboItemId: Long, setIndex: Int, userId: Long) {
        rows.value = rows.value.filterNot {
            it.date == date && it.comboItemId == comboItemId &&
                it.setIndex == setIndex && it.userId == userId
        }
    }

    override suspend fun updateRepsIfPresent(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        reps: Int?,
        checkedAt: Long,
    ): Int {
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == date && it.comboItemId == comboItemId &&
                it.setIndex == setIndex && it.userId == userId
        }
        if (idx < 0) return 0
        list[idx] = list[idx].copy(reps = reps, checkedAt = checkedAt)
        rows.value = list
        return 1
    }

    override suspend fun updateSetWeight(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        weight: Double?,
    ) {
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == date && it.comboItemId == comboItemId &&
                it.setIndex == setIndex && it.userId == userId
        }
        if (idx >= 0) {
            list[idx] = list[idx].copy(weight = weight)
            rows.value = list
        }
    }

    override suspend fun adjustReps(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        delta: Int,
        checkedAt: Long,
    ) {
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == date && it.comboItemId == comboItemId &&
                it.setIndex == setIndex && it.userId == userId
        }
        // 与 SQL 语义一致：行不存在或只记完成（reps 为 null）时不动，下限 1
        if (idx >= 0 && list[idx].reps != null) {
            list[idx] = list[idx].copy(reps = maxOf(1, list[idx].reps!! + delta), checkedAt = checkedAt)
            rows.value = list
        }
    }

    override fun observeForDate(date: Long): Flow<List<SetCheckinEntity>> =
        rows.map { list -> list.filter { it.date == date } }

    override suspend fun getAll(): List<SetCheckinEntity> = rows.value

    override fun observeAll(): Flow<List<SetCheckinEntity>> = rows

    override suspend fun deleteForComboItems(comboItemIds: List<Long>) {
        rows.value = rows.value.filterNot { it.comboItemId in comboItemIds }
    }

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 SetSkipDao：真实实现 REPLACE / 删行 / 按日过滤 / 清理 / 清空。唯一键含 userId。 */
class FakeSetSkipDao : SetSkipDao {
    val rows = MutableStateFlow<List<SetSkipEntity>>(emptyList())

    override suspend fun upsert(skip: SetSkipEntity) {
        // REPLACE 语义：命中 (date, comboItemId, setIndex, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == skip.date && it.comboItemId == skip.comboItemId &&
                it.setIndex == skip.setIndex && it.userId == skip.userId
        }
        if (idx >= 0) list[idx] = skip else list.add(skip)
        rows.value = list
    }

    override suspend fun delete(date: Long, comboItemId: Long, setIndex: Int, userId: Long) {
        rows.value = rows.value.filterNot {
            it.date == date && it.comboItemId == comboItemId &&
                it.setIndex == setIndex && it.userId == userId
        }
    }

    override fun observeForDate(date: Long): Flow<List<SetSkipEntity>> =
        rows.map { list -> list.filter { it.date == date } }

    override suspend fun getAll(): List<SetSkipEntity> = rows.value

    override suspend fun deleteForComboItems(comboItemIds: List<Long>) {
        rows.value = rows.value.filterNot { it.comboItemId in comboItemIds }
    }

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 SessionWeightDao：真实实现 REPLACE / 删行 / 全量观察 / 清空。唯一键含 userId。 */
class FakeSessionWeightDao : SessionWeightDao {
    val rows = MutableStateFlow<List<SessionWeightEntity>>(emptyList())

    override suspend fun upsert(record: SessionWeightEntity) {
        // REPLACE 语义：命中 (date, exerciseId, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == record.date && it.exerciseId == record.exerciseId && it.userId == record.userId
        }
        if (idx >= 0) list[idx] = record else list.add(record)
        rows.value = list
    }

    override suspend fun delete(date: Long, exerciseId: Long, userId: Long) {
        rows.value = rows.value.filterNot {
            it.date == date && it.exerciseId == exerciseId && it.userId == userId
        }
    }

    override fun observeAll(): Flow<List<SessionWeightEntity>> = rows

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 BodyWeightDao：真实实现 REPLACE / 全量观察 / 清空。唯一键 (date, userId)。 */
class FakeBodyWeightDao : BodyWeightDao {
    val rows = MutableStateFlow<List<BodyWeightEntity>>(emptyList())

    override suspend fun upsert(record: BodyWeightEntity) {
        // REPLACE 语义：命中 (date, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == record.date && it.userId == record.userId
        }
        if (idx >= 0) list[idx] = record else list.add(record)
        rows.value = list
    }

    override fun observeAll(): Flow<List<BodyWeightEntity>> = rows

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 DayNoteDao：真实实现 REPLACE / 全量观察 / 删行 / 清空。唯一键 (date, userId)。 */
class FakeDayNoteDao : DayNoteDao {
    val rows = MutableStateFlow<List<DayNoteEntity>>(emptyList())

    override suspend fun upsert(record: DayNoteEntity) {
        // REPLACE 语义：命中 (date, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst { it.date == record.date && it.userId == record.userId }
        if (idx >= 0) list[idx] = record else list.add(record)
        rows.value = list
    }

    override fun observeAll(): Flow<List<DayNoteEntity>> = rows

    override suspend fun delete(date: Long, userId: Long) {
        rows.value = rows.value.filterNot { it.date == date && it.userId == userId }
    }

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 ExerciseNoteDao：真实实现 REPLACE / 全量观察 / 清空。唯一键 (date, exerciseId, userId)。 */
class FakeExerciseNoteDao : ExerciseNoteDao {
    val rows = MutableStateFlow<List<ExerciseNoteEntity>>(emptyList())

    override suspend fun upsert(record: ExerciseNoteEntity) {
        // REPLACE 语义：命中 (date, exerciseId, userId) 唯一键则替换为一行
        val list = rows.value.toMutableList()
        val idx = list.indexOfFirst {
            it.date == record.date && it.exerciseId == record.exerciseId && it.userId == record.userId
        }
        if (idx >= 0) list[idx] = record else list.add(record)
        rows.value = list
    }

    override fun observeAll(): Flow<List<ExerciseNoteEntity>> = rows

    override suspend fun deleteForUser(userId: Long) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

/** 内存版 WorkoutSessionDao：单行语义（upsert 覆盖、delete 清空）。 */
class FakeWorkoutSessionDao : WorkoutSessionDao {
    val rows = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())

    override fun observe(): Flow<WorkoutSessionEntity?> =
        rows.map { list -> list.firstOrNull() }

    override suspend fun get(): WorkoutSessionEntity? = rows.value.firstOrNull()

    override suspend fun upsert(session: WorkoutSessionEntity) {
        rows.value = listOf(session)
    }

    override suspend fun delete() {
        rows.value = emptyList()
    }
}

/** WorkoutViewModel 不使用，全部占位。 */
object FakeLogDao : LogDao {
    override fun observeForDate(date: Long): Flow<List<LogEntryWithSets>> = TODO()
    override suspend fun getForRange(from: Long, to: Long): List<LogEntryEntity> = TODO()
    override suspend fun getWithSetsForRange(from: Long, to: Long): List<LogEntryWithSets> = emptyList()
    override suspend fun getAllWithSets(): List<LogEntryWithSets> = emptyList()
    override suspend fun upsertEntry(entry: LogEntryEntity): Long = TODO()
    override suspend fun insertSets(sets: List<LogSetEntity>) = TODO()
    override suspend fun clearSets(entryId: Long) = TODO()
    override suspend fun minSortForDate(date: Long): Int? = TODO()
    override suspend fun deleteEntry(entry: LogEntryEntity) = TODO()
    override suspend fun deleteSet(set: LogSetEntity) = TODO()
    override suspend fun clearEntries() { /* 内存 fake 无记录行可清 */ }
    override suspend fun clearSets() { /* 内存 fake 无记录行可清 */ }
}

/** 内存版 SettingsDao。 */
class FakeSettingsDao : SettingsDao {
    val settings = MutableStateFlow<SettingsEntity?>(SettingsEntity())

    override fun observe(): Flow<SettingsEntity?> = settings
    override suspend fun get(): SettingsEntity? = settings.value
    override suspend fun upsert(settings: SettingsEntity) {
        this.settings.value = settings
    }
}

/** AllDaos 聚合的内存实现，直接把各 fake 暴露为属性供测试灌数据。 */
class FakeDaos(
    val dayPlans: FakeDayPlanDao = FakeDayPlanDao(),
    val combos: FakeComboDao = FakeComboDao(),
    val exercises: FakeExerciseDao = FakeExerciseDao(),
    val checkins: FakeSetCheckinDao = FakeSetCheckinDao(),
    val skips: FakeSetSkipDao = FakeSetSkipDao(),
    val appSettings: FakeSettingsDao = FakeSettingsDao(),
    val sessionWeights: FakeSessionWeightDao = FakeSessionWeightDao(),
    val bodyWeights: FakeBodyWeightDao = FakeBodyWeightDao(),
    val dayNotes: FakeDayNoteDao = FakeDayNoteDao(),
    val exerciseNotes: FakeExerciseNoteDao = FakeExerciseNoteDao(),
    val workoutSessions: FakeWorkoutSessionDao = FakeWorkoutSessionDao(),
    val appUsers: FakeUserDao = FakeUserDao(),
) : AllDaos {
    override val dayPlanDao: DayPlanDao get() = dayPlans
    override val comboDao: ComboDao get() = combos
    override val exerciseDao: ExerciseDao get() = exercises
    override val setCheckinDao: SetCheckinDao get() = checkins
    override val setSkipDao: SetSkipDao get() = skips
    override val logDao: LogDao get() = FakeLogDao
    override val settingsDao: SettingsDao get() = appSettings
    override val sessionWeightDao: SessionWeightDao get() = sessionWeights
    override val bodyWeightDao: BodyWeightDao get() = bodyWeights
    override val dayNoteDao: DayNoteDao get() = dayNotes
    override val exerciseNoteDao: ExerciseNoteDao get() = exerciseNotes
    override val workoutSessionDao: WorkoutSessionDao get() = workoutSessions
    override val userDao: UserDao get() = appUsers
}

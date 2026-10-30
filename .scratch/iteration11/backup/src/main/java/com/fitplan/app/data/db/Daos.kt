package com.fitplan.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY sort, id")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY sort, id")
    suspend fun getAll(): List<UserEntity>

    @Upsert
    suspend fun upsert(user: UserEntity): Long

    @Delete
    suspend fun delete(user: UserEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY category, name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity): Long

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercises WHERE category = :category")
    suspend fun countByCategory(category: String): Int

    /** 记录动作的已处理重量（同步提醒被确认或忽略时调用），同重量不再提醒。 */
    @Query("UPDATE exercises SET ackMaxWeight = :weight WHERE id = :id")
    suspend fun updateAckMaxWeight(id: Long, weight: Double)
}

@Dao
interface ComboDao {
    @Transaction
    @Query("SELECT * FROM combos ORDER BY sort, id")
    fun observeAllWithItems(): Flow<List<ComboWithItems>>

    @Transaction
    @Query("SELECT * FROM combos WHERE id = :id")
    suspend fun getWithItems(id: Long): ComboWithItems?

    @Query("SELECT * FROM combos ORDER BY sort, id")
    fun observeAll(): Flow<List<ComboEntity>>

    @Query("SELECT * FROM combos ORDER BY sort, id")
    suspend fun getAll(): List<ComboEntity>

    @Upsert
    suspend fun upsert(combo: ComboEntity): Long

    @Insert
    suspend fun insertItems(items: List<ComboItemEntity>)

    @Update
    suspend fun updateItem(item: ComboItemEntity)

    @Delete
    suspend fun deleteItem(item: ComboItemEntity)

    @Query("DELETE FROM combo_items WHERE comboId = :comboId")
    suspend fun clearItems(comboId: Long)

    @Query("DELETE FROM combo_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>)

    /** 提醒同步（仅当前条目）：更新某一条组合条目的目标重量。null 表示清除目标。 */
    @Query("UPDATE combo_items SET targetWeight = :weight WHERE id = :id")
    suspend fun updateTargetWeight(id: Long, weight: Double?)

    /** 提醒同步（全部条目）：把某动作在所有组合中的目标重量统一更新为同一值（含动作在某组合内出现多次的情况）。 */
    @Query("UPDATE combo_items SET targetWeight = :weight WHERE exerciseId = :exerciseId")
    suspend fun updateTargetWeightForExercise(exerciseId: Long, weight: Double?)

    @Query("DELETE FROM combos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT MAX(sort) FROM combos")
    suspend fun maxSort(): Int?
}

@Dao
interface DayPlanDao {
    @Query("SELECT * FROM day_plans")
    fun observeAll(): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM day_plans WHERE date BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM day_plans WHERE date BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<DayPlanEntity>

    @Query("SELECT * FROM day_plans WHERE date = :date")
    suspend fun getByDate(date: Long): DayPlanEntity?

    @Query("SELECT * FROM day_plans WHERE comboId = :comboId")
    suspend fun getByCombo(comboId: Long): List<DayPlanEntity>

    @Upsert
    suspend fun upsert(plan: DayPlanEntity)

    @Upsert
    suspend fun upsertAll(plans: List<DayPlanEntity>)

    @Query("DELETE FROM day_plans WHERE date = :date")
    suspend fun deleteByDate(date: Long)

    @Query("DELETE FROM day_plans")
    suspend fun clearAll()
}

/** 训练会话数据访问：单行语义（同一时刻至多一个进行中的会话）。 */
@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_session LIMIT 1")
    fun observe(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_session LIMIT 1")
    suspend fun get(): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_session")
    suspend fun delete()
}

@Dao
interface LogDao {
    @Transaction
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY sort, id")
    fun observeForDate(date: Long): Flow<List<LogEntryWithSets>>

    @Query("SELECT * FROM log_entries WHERE date BETWEEN :from AND :to ORDER BY date, sort, id")
    suspend fun getForRange(from: Long, to: Long): List<LogEntryEntity>

    @Transaction
    @Query("SELECT * FROM log_entries WHERE date BETWEEN :from AND :to ORDER BY date, sort, id")
    suspend fun getWithSetsForRange(from: Long, to: Long): List<LogEntryWithSets>

    @Transaction
    @Query("SELECT * FROM log_entries ORDER BY date, sort, id")
    suspend fun getAllWithSets(): List<LogEntryWithSets>

    @Upsert
    suspend fun upsertEntry(entry: LogEntryEntity): Long

    @Insert
    suspend fun insertSets(sets: List<LogSetEntity>)

    @Query("DELETE FROM log_sets WHERE entryId = :entryId")
    suspend fun clearSets(entryId: Long)

    @Query("SELECT MIN(sort) - 1 FROM log_entries WHERE date = :date")
    suspend fun minSortForDate(date: Long): Int?

    @Delete
    suspend fun deleteEntry(entry: LogEntryEntity)

    @Delete
    suspend fun deleteSet(set: LogSetEntity)

    @Query("DELETE FROM log_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM log_sets")
    suspend fun clearSets()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}

/** 训练打卡数据访问。唯一键含 userId：不同训练者同天同组互不覆盖。 */
@Dao
interface SetCheckinDao {
    /** REPLACE 可命中 (date, comboItemId, setIndex, userId) 唯一索引完成替换。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkin: SetCheckinEntity)

    /** 取消打卡（删行，按训练者）。 */
    @Query(
        "DELETE FROM set_checkins WHERE date = :date AND comboItemId = :comboItemId " +
            "AND setIndex = :setIndex AND userId = :userId",
    )
    suspend fun delete(date: Long, comboItemId: Long, setIndex: Int, userId: Long)

    /** 已存在行的次数覆盖（保留该组已编辑的重量），打卡时刻刷新为本次：命中返回 1，无行返回 0。 */
    @Query(
        "UPDATE set_checkins SET reps = :reps, checkedAt = :checkedAt " +
            "WHERE date = :date AND comboItemId = :comboItemId " +
            "AND setIndex = :setIndex AND userId = :userId",
    )
    suspend fun updateRepsIfPresent(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        reps: Int?,
        checkedAt: Long,
    ): Int

    /** 编辑单组重量（仅该训练者该组，动作级当次重量不得覆盖此值）。 */
    @Query(
        "UPDATE set_checkins SET weight = :weight WHERE date = :date AND comboItemId = :comboItemId " +
            "AND setIndex = :setIndex AND userId = :userId",
    )
    suspend fun updateSetWeight(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        weight: Double?,
    )

    /** 原子增减次数（下限 1）：在 SQL 内基于当前值计算，快速连点不丢增量，打卡时刻刷新为本次；行不存在或只记完成（reps 为 null）时不动。 */
    @Query(
        "UPDATE set_checkins SET reps = MAX(1, reps + :delta), checkedAt = :checkedAt " +
            "WHERE date = :date AND comboItemId = :comboItemId AND setIndex = :setIndex " +
            "AND userId = :userId AND reps IS NOT NULL",
    )
    suspend fun adjustReps(
        date: Long,
        comboItemId: Long,
        setIndex: Int,
        userId: Long,
        delta: Int,
        checkedAt: Long,
    )

    @Query("SELECT * FROM set_checkins WHERE date = :date")
    fun observeForDate(date: Long): Flow<List<SetCheckinEntity>>

    @Query("SELECT * FROM set_checkins")
    suspend fun getAll(): List<SetCheckinEntity>

    @Query("SELECT * FROM set_checkins")
    fun observeAll(): Flow<List<SetCheckinEntity>>

    /** 清理指定组合条目的全部打卡（条目被移除 / 组合被删时防孤儿行）。 */
    @Query("DELETE FROM set_checkins WHERE comboItemId IN (:comboItemIds)")
    suspend fun deleteForComboItems(comboItemIds: List<Long>)

    /** 删除某训练者的全部打卡（删除训练者档案时调用）。 */
    @Query("DELETE FROM set_checkins WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM set_checkins")
    suspend fun clearAll()
}

/** 逃组数据访问。唯一键含 userId：逃标按训练者隔离；「打卡覆盖逃标」由打卡方删行完成。 */
@Dao
interface SetSkipDao {
    /** REPLACE 可命中 (date, comboItemId, setIndex, userId) 唯一索引完成替换。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(skip: SetSkipEntity)

    /** 取消逃标（删行，按训练者），该组回到未练。 */
    @Query(
        "DELETE FROM set_skips WHERE date = :date AND comboItemId = :comboItemId " +
            "AND setIndex = :setIndex AND userId = :userId",
    )
    suspend fun delete(date: Long, comboItemId: Long, setIndex: Int, userId: Long)

    @Query("SELECT * FROM set_skips WHERE date = :date")
    fun observeForDate(date: Long): Flow<List<SetSkipEntity>>

    @Query("SELECT * FROM set_skips")
    suspend fun getAll(): List<SetSkipEntity>

    /** 清理指定组合条目的全部逃标（条目被移除 / 组合被删时防孤儿行）。 */
    @Query("DELETE FROM set_skips WHERE comboItemId IN (:comboItemIds)")
    suspend fun deleteForComboItems(comboItemIds: List<Long>)

    /** 删除某训练者的全部逃标（删除训练者档案时调用）。 */
    @Query("DELETE FROM set_skips WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM set_skips")
    suspend fun clearAll()
}

/** 当次重量数据访问。唯一键 (date, exerciseId, userId)：同天同动作按训练者分开。 */
@Dao
interface SessionWeightDao {
    /** REPLACE 可命中 (date, exerciseId, userId) 唯一索引完成覆盖：同天同动作同训练者重复输入取最后一次。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SessionWeightEntity)

    /** 取消当次记录（删行），显示回落到组合目标重量。 */
    @Query(
        "DELETE FROM session_weights WHERE date = :date AND exerciseId = :exerciseId AND userId = :userId",
    )
    suspend fun delete(date: Long, exerciseId: Long, userId: Long)

    @Query("SELECT * FROM session_weights")
    fun observeAll(): Flow<List<SessionWeightEntity>>

    /** 删除某训练者的当次重量（删除训练者档案时调用）。 */
    @Query("DELETE FROM session_weights WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM session_weights")
    suspend fun clearAll()
}

/** 体重数据访问。唯一键 (date, userId)：同天同训练者一条，重复输入覆盖。 */
@Dao
interface BodyWeightDao {
    /** REPLACE 可命中 (date, userId) 唯一索引完成覆盖：同天同训练者重复输入取最后一次。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: BodyWeightEntity)

    @Query("SELECT * FROM body_weights")
    fun observeAll(): Flow<List<BodyWeightEntity>>

    /** 删除某训练者的体重（删除训练者档案时调用）。 */
    @Query("DELETE FROM body_weights WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM body_weights")
    suspend fun clearAll()
}

/** 当日备注数据访问。唯一键 (date, userId)：同天同训练者一条，重复输入覆盖。 */
@Dao
interface DayNoteDao {
    /** REPLACE 可命中 (date, userId) 唯一索引完成覆盖：同天同训练者重复输入取最后一次。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DayNoteEntity)

    @Query("SELECT * FROM day_notes")
    fun observeAll(): Flow<List<DayNoteEntity>>

    /** 清除某天某训练者的当日备注（当日备注对话框空文本确定时调用）。 */
    @Query("DELETE FROM day_notes WHERE date = :date AND userId = :userId")
    suspend fun delete(date: Long, userId: Long)

    /** 删除某训练者的当日备注（删除训练者档案时调用）。 */
    @Query("DELETE FROM day_notes WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM day_notes")
    suspend fun clearAll()
}

/** 动作备注数据访问。唯一键 (date, exerciseId, userId)：同天同动作按训练者分开。 */
@Dao
interface ExerciseNoteDao {
    /** REPLACE 可命中 (date, exerciseId, userId) 唯一索引完成覆盖：同天同动作同训练者重复输入取最后一次。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ExerciseNoteEntity)

    @Query("SELECT * FROM exercise_notes")
    fun observeAll(): Flow<List<ExerciseNoteEntity>>

    /** 删除某训练者的动作备注（删除训练者档案时调用）。 */
    @Query("DELETE FROM exercise_notes WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long)

    @Query("DELETE FROM exercise_notes")
    suspend fun clearAll()
}

/** 数据访问入口，由 AppDatabase 暴露。 */
interface AllDaos {
    val userDao: UserDao
    val exerciseDao: ExerciseDao
    val comboDao: ComboDao
    val dayPlanDao: DayPlanDao
    val logDao: LogDao
    val settingsDao: SettingsDao
    val setCheckinDao: SetCheckinDao
    val setSkipDao: SetSkipDao
    val sessionWeightDao: SessionWeightDao
    val bodyWeightDao: BodyWeightDao
    val dayNoteDao: DayNoteDao
    val exerciseNoteDao: ExerciseNoteDao
    val workoutSessionDao: WorkoutSessionDao
}

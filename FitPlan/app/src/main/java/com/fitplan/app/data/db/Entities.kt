package com.fitplan.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** 训练者档案：本地多用户（无账号），打卡与 PR 按训练者区分。 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sort: Int = 0,
)

/** 动作，如卧推、深蹲、辅助引体向上。 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    /** 该动作启用哪些记录字段，未启用的字段不显示。 */
    val trackSets: Boolean = true,
    val trackReps: Boolean = true,
    val trackWeight: Boolean = true,
    val trackDuration: Boolean = false,
    /** 默认重量（旧称根源重量）：添加到组合时预填目标重量，不参与同步。 */
    val defaultWeight: Double? = null,
    /** 已处理重量：同步提醒中被确认或忽略过的历史最大当次重量，同重量不再提醒。 */
    val ackMaxWeight: Double? = null,
)

/** 训练组合 / 动作组。 */
@Entity(tableName = "combos")
data class ComboEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sort: Int = 0,
)

/** 组合内的一个动作及其目标（目标字段均可留空）。 */
@Entity(
    tableName = "combo_items",
    foreignKeys = [
        ForeignKey(
            entity = ComboEntity::class,
            parentColumns = ["id"],
            childColumns = ["comboId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comboId"), Index("exerciseId")],
)
data class ComboItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comboId: Long,
    val exerciseId: Long,
    val sort: Int = 0,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    /** 目标重量，允许负数（辅助重量）、0（自重）、小数。 */
    val targetWeight: Double? = null,
    /** 目标时长，单位分钟。 */
    val targetDuration: Int? = null,
)

data class ComboWithItems(
    @Embedded val combo: ComboEntity,
    @Relation(parentColumn = "id", entityColumn = "comboId")
    val items: List<ComboItemEntity>,
)

/** 某一天的周计划安排。date 为 epoch day。未插入记录的天视为未安排（按休息日处理）。 */
@Entity(tableName = "day_plans")
data class DayPlanEntity(
    @PrimaryKey val date: Long,
    val isRest: Boolean = true,
    val comboId: Long? = null,
    /** 天级逃课标记（已弃用：逃标细化为组级，见 set_skips；列仅为存量 schema 兼容保留，无任何读者）。 */
    val skipped: Boolean = false,
)

/** 训练记录的一条动作条目（一天可有多条）。 */
@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date"), Index("exerciseId")],
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val exerciseId: Long,
    val note: String = "",
    val sort: Int = 0,
)

/** 记录条目中的一组数据，各字段可选，实际显示由动作配置决定。 */
@Entity(
    tableName = "log_sets",
    foreignKeys = [
        ForeignKey(
            entity = LogEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class LogSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val sort: Int = 0,
    val sets: Int? = null,
    val reps: Int? = null,
    /** 重量，允许负数（辅助重量）、0（自重）、小数。 */
    val weight: Double? = null,
    /** 时长，单位分钟。 */
    val duration: Int? = null,
)

data class LogEntryWithSets(
    @Embedded val entry: LogEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val sets: List<LogSetEntity>,
)

/** 训练打卡：一行 = 某天某训练者某组合动作的某一组（该行存在即视为已完成）。 */
@Entity(
    tableName = "set_checkins",
    indices = [
        Index("date"),
        Index("comboItemId"),
        Index(value = ["date", "comboItemId", "setIndex", "userId"], unique = true),
    ],
)
data class SetCheckinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    val comboItemId: Long, // combo_items.id
    val setIndex: Int,     // 0-based 组序
    val reps: Int? = null, // 该组完成次数
    /** 打卡时快照的当次重量；未记重量或旧数据为 null。 */
    val weight: Double? = null,
    /** 打卡时刻(epoch millis)：打卡/重复打卡/调整次数时刷新为最新一次；历史数据为 null，不参与用时推导。 */
    val checkedAt: Long? = null,
    /** 打卡归属的训练者（本地多用户，PR 按用户分别统计）。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 逃组：一行 = 某天某训练者某组合动作的某一组被标逃（行存在即逃；打卡覆盖逃标，逃不算完成）。 */
@Entity(
    tableName = "set_skips",
    indices = [
        Index("date"),
        Index("comboItemId"),
        Index(value = ["date", "comboItemId", "setIndex", "userId"], unique = true),
    ],
)
data class SetSkipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    val comboItemId: Long, // combo_items.id
    val setIndex: Int,     // 0-based 组序
    /** 逃标归属的训练者（本地多用户，逃标按训练者隔离）。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 当次重量：某天某训练者训练某动作实际使用的重量，只记当天不写回组合条目；一行 = 某天某训练者某动作，重复输入覆盖。 */
@Entity(
    tableName = "session_weights",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("date"),
        Index("exerciseId"),
        Index(value = ["date", "exerciseId", "userId"], unique = true),
    ],
)
data class SessionWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    val exerciseId: Long,
    val weight: Double,
    /** 当次重量归属的训练者，与打卡同维度。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 体重记录：一行 = 某训练者某天的体重，一天一条，重复输入覆盖；可选记录，不催记、不标记漏记。 */
@Entity(
    tableName = "body_weights",
    indices = [
        Index("date"),
        Index(value = ["date", "userId"], unique = true),
    ],
)
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    /** 体重，kg 库内原值口径（lb 不换算，显示层只加单位标签）。 */
    val weight: Double,
    /** 体重归属的训练者（本地多用户，按训练者隔离）。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 当日备注：一行 = 某训练者某天的一句话备注，训练日与休息日都可记，重复输入覆盖；可选记录，不催记。 */
@Entity(
    tableName = "day_notes",
    indices = [
        Index("date"),
        Index(value = ["date", "userId"], unique = true),
    ],
)
data class DayNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    val note: String,
    /** 当日备注归属的训练者（本地多用户，按训练者隔离）。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 动作备注：一行 = 某训练者某天对某动作的备注，同天同动作一条，重复输入覆盖。 */
@Entity(
    tableName = "exercise_notes",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("date"),
        Index("exerciseId"),
        Index(value = ["date", "exerciseId", "userId"], unique = true),
    ],
)
data class ExerciseNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // epoch day
    val exerciseId: Long,
    val note: String,
    /** 动作备注归属的训练者，与打卡同维度。 */
    @ColumnInfo(defaultValue = "1") val userId: Long = 1,
)

/** 训练会话：一行 = 正在进行的训练日（常驻通知挂着）；行删除即会话结束。 */
@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    /** epoch day，同时是主键：同一时刻至多一个进行中的会话。 */
    @PrimaryKey val date: Long,
    /** 首组打卡时刻(epoch millis);尚未打卡为 null */
    val startedAt: Long? = null,
)

/** 全局设置，单行表。 */
@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    /** false = kg（默认），true = lb。 */
    val unitIsLb: Boolean = false,
    /** 当前训练者：打卡归属与 PR 统计按此用户过滤。 */
    @ColumnInfo(defaultValue = "1") val activeUserId: Long = 1,
    val defTrackSets: Boolean = true,
    val defTrackReps: Boolean = true,
    val defTrackWeight: Boolean = true,
    val defTrackDuration: Boolean = false,
)

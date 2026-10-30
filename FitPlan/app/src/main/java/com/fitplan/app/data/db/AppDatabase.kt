package com.fitplan.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        ExerciseEntity::class,
        ComboEntity::class,
        ComboItemEntity::class,
        DayPlanEntity::class,
        LogEntryEntity::class,
        LogSetEntity::class,
        SettingsEntity::class,
        SetCheckinEntity::class,
        SetSkipEntity::class,
        SessionWeightEntity::class,
        BodyWeightEntity::class,
        DayNoteEntity::class,
        ExerciseNoteEntity::class,
        WorkoutSessionEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase(), AllDaos {
    abstract override val userDao: UserDao
    abstract override val exerciseDao: ExerciseDao
    abstract override val comboDao: ComboDao
    abstract override val dayPlanDao: DayPlanDao
    abstract override val logDao: LogDao
    abstract override val settingsDao: SettingsDao
    abstract override val setCheckinDao: SetCheckinDao
    abstract override val setSkipDao: SetSkipDao
    abstract override val sessionWeightDao: SessionWeightDao
    abstract override val bodyWeightDao: BodyWeightDao
    abstract override val dayNoteDao: DayNoteDao
    abstract override val exerciseNoteDao: ExerciseNoteDao
    abstract override val workoutSessionDao: WorkoutSessionDao

    /** 事务边界：块内跨 DAO 写入在同一 Room 事务中整体提交/回滚（L01）。 */
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = withTransaction(block)

    companion object {
        /** v1 -> v2：新增训练打卡表（非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS set_checkins " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "comboItemId INTEGER NOT NULL, setIndex INTEGER NOT NULL, reps INTEGER)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_checkins_date ON set_checkins (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_checkins_comboItemId ON set_checkins (comboItemId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_set_checkins_date_comboItemId_setIndex " +
                        "ON set_checkins (date, comboItemId, setIndex)",
                )
            }
        }

        /** v2 -> v3：动作表新增默认重量 defaultWeight（第六轮前称根源重量；非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN defaultWeight REAL")
            }
        }

        /** v3 -> v4：新增当次重量表 + 动作表新增已处理重量 ackMaxWeight（非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS session_weights " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "exerciseId INTEGER NOT NULL, weight REAL NOT NULL, " +
                        "FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_weights_date ON session_weights (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_weights_exerciseId ON session_weights (exerciseId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_session_weights_date_exerciseId " +
                        "ON session_weights (date, exerciseId)",
                )
                db.execSQL("ALTER TABLE exercises ADD COLUMN ackMaxWeight REAL")
            }
        }

        /** v4 -> v5：周计划新增逃课标记 skipped + 新增训练会话表（非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE day_plans ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_session " +
                        "(date INTEGER NOT NULL, startedAt INTEGER, PRIMARY KEY(date))",
                )
            }
        }

        /** v5 -> v6：训练打卡新增快照重量 weight（打卡时快照的当次重量，旧数据为 NULL；非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_checkins ADD COLUMN weight REAL")
            }
        }

        /**
         * v6 -> v7：本地多训练者。
         * - 新增 users 训练者表并播种默认训练者「我」（id=1）；
         * - set_checkins / session_weights 增加 userId 列（历史数据归入默认训练者），
         *   唯一键扩为含 userId，两张表均按 Room 生成规则重建（非破坏，数据原样迁移）；
         * - app_settings 增加 activeUserId 当前训练者列。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS users " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sort INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO users (id, name, sort) VALUES (1, '我', 0)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS set_checkins_new " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "comboItemId INTEGER NOT NULL, setIndex INTEGER NOT NULL, reps INTEGER, weight REAL, " +
                        "userId INTEGER NOT NULL DEFAULT 1)",
                )
                db.execSQL(
                    "INSERT INTO set_checkins_new (id, date, comboItemId, setIndex, reps, weight, userId) " +
                        "SELECT id, date, comboItemId, setIndex, reps, weight, 1 FROM set_checkins",
                )
                db.execSQL("DROP TABLE set_checkins")
                db.execSQL("ALTER TABLE set_checkins_new RENAME TO set_checkins")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_checkins_date ON set_checkins (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_checkins_comboItemId ON set_checkins (comboItemId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_set_checkins_date_comboItemId_setIndex_userId " +
                        "ON set_checkins (date, comboItemId, setIndex, userId)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS session_weights_new " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "exerciseId INTEGER NOT NULL, weight REAL NOT NULL, userId INTEGER NOT NULL DEFAULT 1, " +
                        "FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO session_weights_new (id, date, exerciseId, weight, userId) " +
                        "SELECT id, date, exerciseId, weight, 1 FROM session_weights",
                )
                db.execSQL("DROP TABLE session_weights")
                db.execSQL("ALTER TABLE session_weights_new RENAME TO session_weights")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_weights_date ON session_weights (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_weights_exerciseId ON session_weights (exerciseId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_session_weights_date_exerciseId_userId " +
                        "ON session_weights (date, exerciseId, userId)",
                )
                db.execSQL("ALTER TABLE app_settings ADD COLUMN activeUserId INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v7 -> v8：训练打卡新增打卡时刻 checkedAt。
         * 打卡/重复打卡/调整次数时写入；历史行 NULL，不参与用时推导
         * （非破坏迁移，SQL 与 Room 生成规则一致）。
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_checkins ADD COLUMN checkedAt INTEGER")
            }
        }

        /**
         * v8 -> v9：逃组模型（组级逃标取代天级逃课）。
         * - 新增 set_skips 逃组表（唯一键 date+comboItemId+setIndex+userId，行存在即逃）；
         * - 历史天级逃课（day_plans.skipped=1）的天，其所有未打卡组按训练者回填 set_skips，
         *   已打卡组保留打卡语义不动；day_plans.skipped 列保留但弃用
         * （非破坏迁移，SQL 与 Room 生成规则一致）。
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS set_skips " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "comboItemId INTEGER NOT NULL, setIndex INTEGER NOT NULL, " +
                        "userId INTEGER NOT NULL DEFAULT 1)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_skips_date ON set_skips (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_set_skips_comboItemId ON set_skips (comboItemId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_set_skips_date_comboItemId_setIndex_userId " +
                        "ON set_skips (date, comboItemId, setIndex, userId)",
                )
                // 回填：seq 递归生成 0..98 的组序（每组至少 1 行，与训练页行数下限一致），
                // 逐训练者排除已打卡组
                db.execSQL(
                    "INSERT INTO set_skips (date, comboItemId, setIndex, userId) " +
                        "WITH RECURSIVE seq(idx) AS ( " +
                        "SELECT 0 UNION ALL SELECT idx + 1 FROM seq WHERE idx < 98 " +
                        ") " +
                        "SELECT p.date, ci.id, s.idx, u.id " +
                        "FROM day_plans p " +
                        "JOIN combo_items ci ON ci.comboId = p.comboId " +
                        "JOIN seq s ON s.idx < MAX(COALESCE(ci.targetSets, 0), 1) " +
                        "CROSS JOIN users u " +
                        "WHERE p.skipped = 1 AND p.comboId IS NOT NULL AND NOT EXISTS ( " +
                        "SELECT 1 FROM set_checkins sc WHERE sc.date = p.date AND sc.comboItemId = ci.id " +
                        "AND sc.setIndex = s.idx AND sc.userId = u.id)",
                )
            }
        }

        /** v9 -> v10：新增体重记录表（天 + 训练者唯一，重复输入覆盖；可选记录不催记；非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS body_weights " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "weight REAL NOT NULL, userId INTEGER NOT NULL DEFAULT 1)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_body_weights_date ON body_weights (date)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_body_weights_date_userId " +
                        "ON body_weights (date, userId)",
                )
            }
        }

        /** v10 -> v11：新增当日备注与动作备注表（重复覆盖；可选记录不催记；非破坏迁移，SQL 与 Room 生成规则一致）。 */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS day_notes " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "note TEXT NOT NULL, userId INTEGER NOT NULL DEFAULT 1)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_day_notes_date ON day_notes (date)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_day_notes_date_userId " +
                        "ON day_notes (date, userId)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exercise_notes " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, " +
                        "exerciseId INTEGER NOT NULL, note TEXT NOT NULL, userId INTEGER NOT NULL DEFAULT 1, " +
                        "FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_notes_date ON exercise_notes (date)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercise_notes_exerciseId ON exercise_notes (exerciseId)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_notes_date_exerciseId_userId " +
                        "ON exercise_notes (date, exerciseId, userId)",
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitplan.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11,
                )
                .fallbackToDestructiveMigration()
                .build()
    }
}

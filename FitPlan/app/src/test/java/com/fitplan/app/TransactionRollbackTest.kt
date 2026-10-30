package com.fitplan.app

import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.data.db.WorkoutSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-L0 事务边界：FakeDaos 快照/恢复语义 + 组合保存多段写中途失败的整体回滚。
 *
 * 注意：两个用例都刻意**不经 viewModelScope** 触发失败。VM 协程里的未捕获异常会泄漏到
 * 测试调度器/coroutines-test 的未捕获异常记录，在全量 clean 运行时表现为后续用例
 * UncaughtExceptionsBeforeTest 的顺序性失败（单类运行不复现）。
 * 因此这里在测试体内直接调用 daos.runInTransaction 复刻 ComboEditViewModel.save 的写入序列，
 * 异常在事务边界同步抛出并被 runCatching 捕获；VM 成功路径接线由 ComboEditViewModelTest 覆盖。
 */
class TransactionRollbackTest {

    /** 组合（条目 11=动作1、12=动作2）+ 三天打卡 + 一条逃标。 */
    private fun seedCombo(daos: FakeDaos) {
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
            ExerciseEntity(id = 3, name = "硬拉"),
        )
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日", sort = 0),
                items = listOf(
                    ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0, targetSets = 3),
                    ComboItemEntity(id = 12, comboId = 10, exerciseId = 2, sort = 1, targetSets = 4),
                ),
            ),
        )
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = 100, comboItemId = 11, setIndex = 0, reps = 8),
            SetCheckinEntity(date = 100, comboItemId = 12, setIndex = 0, reps = 10),
            SetCheckinEntity(date = 101, comboItemId = 12, setIndex = 1, reps = 9),
        )
        daos.skips.rows.value = listOf(
            SetSkipEntity(date = 100, comboItemId = 11, setIndex = 2),
        )
    }

    /**
     * 组合保存事务（AC-01）：复刻 ComboEditViewModel.save 的写序列——
     * 组合改名 → 删旧条目 12 并清理其打卡 → 更新条目 11 → 插入新动作条目时注入失败，
     * 断言组合条目、组合名、打卡、逃标全部回到保存前状态。
     */
    @Test
    fun `组合保存事务 - 删旧条目后插入失败 条目打卡逃标整体回滚`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        daos.combos.failOnNextInsertItems = true

        val failure = runCatching {
            daos.runInTransaction {
                daos.comboDao.upsert(ComboEntity(id = 10, name = "胸腿日（改名）", sort = 0))
                daos.comboDao.deleteItemsByIds(listOf(12L))
                daos.setCheckinDao.deleteForComboItems(listOf(12L))
                daos.setSkipDao.deleteForComboItems(listOf(12L))
                // 更新条目 11（草稿未填目标 → targetSets 置 null）；回滚后必须回到 3
                daos.comboDao.updateItem(ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0))
                // 新增动作 3 的条目走插入路径 → Fake 注入 IllegalStateException
                daos.comboDao.insertItems(listOf(ComboItemEntity(comboId = 10, exerciseId = 3, sort = 1)))
            }
        }

        // 异常在事务边界同步抛出并被捕获，不遗留协程未捕获异常（不得改成 viewModelScope 触发）
        assertTrue(failure.exceptionOrNull() is IllegalStateException)
        assertFalse(daos.combos.failOnNextInsertItems) // 一次性注入开关被消费 = 插入路径确实抛过异常
        assertEquals(1, daos.rollbackCount) // 失败路径确实进入回滚分支

        val combo = daos.combos.combos.value.single()
        assertEquals("胸腿日", combo.combo.name) // 改名被回滚
        assertEquals(listOf(11L, 12L), combo.items.map { it.id }.sorted()) // 旧条目 12 未被删
        assertEquals(3, combo.items.first { it.id == 11L }.targetSets) // 改前目标组数 3 保持（未被覆盖为 null）
        assertEquals(3, daos.checkins.rows.value.size) // 条目 12 的打卡未被清理
        assertEquals(1, daos.skips.rows.value.size)
    }

    /** Fake runInTransaction 的通用语义：成功提交保留写入并返回结果；异常恢复全部内存表后重抛。 */
    @Test
    fun `Fake 事务 - 成功提交保留写入 失败恢复全部内存表`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = 100, isRest = false, comboId = 10))
        daos.appSettings.settings.value = SettingsEntity(id = 1, unitIsLb = true, activeUserId = 1)
        daos.sessionWeights.rows.value = listOf(SessionWeightEntity(date = 100, exerciseId = 1, weight = 60.0))
        daos.bodyWeights.rows.value = listOf(BodyWeightEntity(date = 100, weight = 70.0))
        daos.dayNotes.rows.value = listOf(DayNoteEntity(date = 100, note = "状态好"))
        daos.exerciseNotes.rows.value = listOf(ExerciseNoteEntity(date = 100, exerciseId = 1, note = "肩"))
        daos.workoutSessions.rows.value = listOf(WorkoutSessionEntity(date = 100, startedAt = 123L))
        daos.appUsers.users.value = listOf(UserEntity(id = 1, name = "我", sort = 0))

        val before = listOf(
            daos.dayPlans.plans.value,
            daos.combos.combos.value,
            daos.exercises.exercises.value,
            daos.checkins.rows.value,
            daos.skips.rows.value,
            listOf(daos.appSettings.settings.value),
            daos.sessionWeights.rows.value,
            daos.bodyWeights.rows.value,
            daos.dayNotes.rows.value,
            daos.exerciseNotes.rows.value,
            daos.workoutSessions.rows.value,
            daos.appUsers.users.value,
        )

        val failure = runCatching {
            daos.runInTransaction {
                // 跨 11 张表的破坏性写入，随后抛异常：必须整体回滚
                daos.dayPlans.clearAll()
                daos.combos.combos.value = emptyList()
                daos.exercises.exercises.value = emptyList()
                daos.checkins.clearAll()
                daos.skips.clearAll()
                daos.appSettings.settings.value = SettingsEntity()
                daos.sessionWeights.clearAll()
                daos.bodyWeights.clearAll()
                daos.dayNotes.clearAll()
                daos.exerciseNotes.clearAll()
                daos.workoutSessions.delete()
                daos.appUsers.users.value = emptyList()
                throw IllegalStateException("boom")
            }
        }
        assertTrue(failure.exceptionOrNull() is IllegalStateException)
        assertEquals(1, daos.rollbackCount)

        val after = listOf(
            daos.dayPlans.plans.value,
            daos.combos.combos.value,
            daos.exercises.exercises.value,
            daos.checkins.rows.value,
            daos.skips.rows.value,
            listOf(daos.appSettings.settings.value),
            daos.sessionWeights.rows.value,
            daos.bodyWeights.rows.value,
            daos.dayNotes.rows.value,
            daos.exerciseNotes.rows.value,
            daos.workoutSessions.rows.value,
            daos.appUsers.users.value,
        )
        assertEquals(before, after) // 快照覆盖全部内存 fake 表

        // 正常提交：写入保留且返回块结果
        val result = daos.runInTransaction {
            daos.checkins.clearAll()
            42
        }
        assertEquals(42, result)
        assertEquals(0, daos.checkins.rows.value.size)
    }
}

package com.fitplan.app.ui.settings

import com.fitplan.app.FakeDaos
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** T-L6/T-L0：清空数据补删训练会话、去掉多表顺序删的半成品；删训练者事务与当前训练者切换。 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 全部业务表各灌一行（当前训练者 2），用于断言清空只清业务数据、保留设置与训练者。 */
    private fun seedAllTables(daos: FakeDaos) {
        daos.appUsers.users.value = listOf(
            UserEntity(id = 1, name = "我", sort = 0),
            UserEntity(id = 2, name = "小红", sort = 1),
        )
        daos.appSettings.settings.value = SettingsEntity(id = 1, unitIsLb = true, activeUserId = 2)
        daos.workoutSessions.rows.value = listOf(WorkoutSessionEntity(date = 100, startedAt = 123L))
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0)),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = 100, isRest = false, comboId = 10))
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = 100, comboItemId = 11, setIndex = 0, reps = 8, userId = 2),
        )
        daos.skips.rows.value = listOf(SetSkipEntity(date = 100, comboItemId = 11, setIndex = 1, userId = 2))
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = 100, exerciseId = 1, weight = 60.0, userId = 2),
        )
        daos.bodyWeights.rows.value = listOf(BodyWeightEntity(date = 100, weight = 55.0, userId = 2))
        daos.dayNotes.rows.value = listOf(DayNoteEntity(date = 100, note = "小红备注", userId = 2))
        daos.exerciseNotes.rows.value = listOf(
            ExerciseNoteEntity(date = 100, exerciseId = 1, note = "小红动作备注", userId = 2),
        )
    }

    @Test
    fun `清空所有数据 - 训练会话行一并清空 设置与训练者保留`() = runTest {
        val daos = FakeDaos()
        seedAllTables(daos)
        val vm = SettingsViewModel(daos)

        var done = false
        vm.clearAllData { done = true }
        advanceUntilIdle()

        assertTrue(done)
        // L07：进行中会话行一并删除，避免通知/会话悬空
        assertTrue(daos.workoutSessions.rows.value.isEmpty())
        // 设置与训练者保留
        assertEquals(SettingsEntity(id = 1, unitIsLb = true, activeUserId = 2), daos.appSettings.settings.value)
        assertEquals(listOf(1L, 2L), daos.appUsers.users.value.map { it.id })
        // 全部业务数据清空
        assertTrue(daos.checkins.rows.value.isEmpty())
        assertTrue(daos.skips.rows.value.isEmpty())
        assertTrue(daos.sessionWeights.rows.value.isEmpty())
        assertTrue(daos.bodyWeights.rows.value.isEmpty())
        assertTrue(daos.dayNotes.rows.value.isEmpty())
        assertTrue(daos.exerciseNotes.rows.value.isEmpty())
        assertTrue(daos.dayPlans.plans.value.isEmpty())
        assertTrue(daos.combos.combos.value.isEmpty())
        assertTrue(daos.exercises.exercises.value.isEmpty())
    }

    @Test
    fun `删除训练者 - 清理其全部记录并切换当前训练者 最后一名不可删`() = runTest {
        val daos = FakeDaos()
        seedAllTables(daos)
        // 训练者 1 的一条打卡必须保留
        daos.checkins.rows.value = daos.checkins.rows.value + SetCheckinEntity(
            date = 100, comboItemId = 11, setIndex = 0, reps = 10, userId = 1,
        )
        val vm = SettingsViewModel(daos)

        vm.deleteUser(daos.appUsers.users.value.first { it.id == 2L })
        advanceUntilIdle()

        // 训练者 2 删除、其全部记录清理；训练者 1 的打卡保留
        assertEquals(listOf(1L), daos.appUsers.users.value.map { it.id })
        assertEquals(listOf(1L), daos.checkins.rows.value.map { it.userId })
        assertTrue(daos.skips.rows.value.isEmpty())
        assertTrue(daos.sessionWeights.rows.value.isEmpty())
        assertTrue(daos.bodyWeights.rows.value.isEmpty())
        assertTrue(daos.dayNotes.rows.value.isEmpty())
        assertTrue(daos.exerciseNotes.rows.value.isEmpty())
        // 删的是当前训练者（activeUserId=2）→ 自动切回第一名
        assertEquals(1L, daos.appSettings.settings.value?.activeUserId)

        // 最后一名训练者不可删除
        vm.deleteUser(daos.appUsers.users.value.single())
        advanceUntilIdle()
        assertEquals(listOf(1L), daos.appUsers.users.value.map { it.id })
    }
}

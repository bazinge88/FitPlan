package com.fitplan.app.ui.plan

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.seedTrainingDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 建 VM 并订阅 durations 与 dayPlans（stateIn 为 WhileSubscribed，须有订阅者）。 */
    private fun TestScope.buildVm(daos: FakeDaos): PlanViewModel {
        val vm = PlanViewModel(daos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.durations.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.dayPlans.collect {} }
        return vm
    }

    @Test
    fun `用时 - 周计划按当前训练者显示每日用时 切换后互不可见`() = runTest {
        val daos = FakeDaos()
        val t = 1_800_000_000L
        daos.checkins.rows.value = listOf(
            // 训练者 1：100 日两组跨 30 分钟；101 日单组约 0 分钟
            SetCheckinEntity(date = 100, comboItemId = 101, setIndex = 0, reps = 8, checkedAt = t, userId = 1),
            SetCheckinEntity(date = 100, comboItemId = 101, setIndex = 1, reps = 8, checkedAt = t + 1_800_000, userId = 1),
            SetCheckinEntity(date = 101, comboItemId = 101, setIndex = 0, reps = 8, checkedAt = t, userId = 1),
            // 训练者 2：100 日单组约 0 分钟（同键不同 userId，与训练者 1 互不覆盖）
            SetCheckinEntity(date = 100, comboItemId = 102, setIndex = 0, reps = 6, checkedAt = t, userId = 2),
        )
        val vm = buildVm(daos)
        assertEquals(mapOf(100L to 1_800_000L, 101L to 0L), vm.durations.value)

        // 切换训练者：只看到训练者 2 的用时，无打卡日不出现
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        assertEquals(mapOf(100L to 0L), vm.durations.value)
    }

    @Test
    fun `移除安排 - 删该天计划行 回到未安排 六类记录原样保留`() = runTest {
        val daos = FakeDaos()
        val date = 100L
        val t = 1_800_000_000L
        val targetPlan = DayPlanEntity(date = date, isRest = false, comboId = 1)
        val otherPlan = DayPlanEntity(date = date + 1, isRest = false, comboId = 1)
        daos.dayPlans.plans.value = listOf(targetPlan, otherPlan)
        // 六类记录各灌 1-2 行落在目标天（userId=1）：移除安排不得波及
        val checkin = SetCheckinEntity(
            date = date, comboItemId = 101, setIndex = 0, reps = 8, checkedAt = t, userId = 1,
        )
        daos.checkins.rows.value = listOf(checkin)
        val skip = SetSkipEntity(date = date, comboItemId = 101, setIndex = 1, userId = 1)
        daos.skips.rows.value = listOf(skip)
        val dayNote = DayNoteEntity(date = date, note = "出差", userId = 1)
        daos.dayNotes.rows.value = listOf(dayNote)
        val exerciseNote = ExerciseNoteEntity(date = date, exerciseId = 1, note = "肩膀不舒服", userId = 1)
        daos.exerciseNotes.rows.value = listOf(exerciseNote)
        val bodyWeight = BodyWeightEntity(date = date, weight = 70.0, userId = 1)
        daos.bodyWeights.rows.value = listOf(bodyWeight)
        val sessionWeight = SessionWeightEntity(date = date, exerciseId = 1, weight = 60.0, userId = 1)
        daos.sessionWeights.rows.value = listOf(sessionWeight)
        val vm = buildVm(daos)

        vm.clearDay(date)
        advanceUntilIdle()

        // 计划侧：目标天回到未安排，另一天的安排不受影响
        assertEquals(mapOf(date + 1L to otherPlan), vm.dayPlans.value)
        // 记录侧：六张表逐行原样（data class 整行相等）
        assertEquals(listOf(checkin), daos.checkins.rows.value)
        assertEquals(listOf(skip), daos.skips.rows.value)
        assertEquals(listOf(dayNote), daos.dayNotes.rows.value)
        assertEquals(listOf(exerciseNote), daos.exerciseNotes.rows.value)
        assertEquals(listOf(bodyWeight), daos.bodyWeights.rows.value)
        assertEquals(listOf(sessionWeight), daos.sessionWeights.rows.value)
    }

    @Test
    fun `移除安排 - 重新排程后计划恢复 历史打卡仍可回看`() = runTest {
        val daos = FakeDaos()
        val date = 100L
        val t = 1_800_000_000L
        // 单动作条目（comboItemId=101），保证 WorkoutViewModel 能产出 items；comboId=1 供 setDay 断言
        seedTrainingDay(daos, date = date, comboId = 1L)
        // 目标天的历史打卡：重排后仍按 date 关联回看
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = date, comboItemId = 101, setIndex = 0, reps = 8, checkedAt = t, userId = 1),
        )
        val vm = buildVm(daos)

        vm.clearDay(date)
        advanceUntilIdle()
        assertEquals(emptyMap<Long, DayPlanEntity>(), vm.dayPlans.value)

        vm.setDay(date, false, 1L)
        advanceUntilIdle()
        val plan = vm.dayPlans.value!![date]!!
        assertFalse(plan.isRest)
        assertEquals(1L, plan.comboId)

        // 回看证明：同一天建 WorkoutViewModel，打卡历史与计划行无关，重排后仍算已完成
        val wvm = WorkoutViewModel(daos, date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { wvm.state.collect {} }
        val item = wvm.state.value.items.single()
        assertEquals(101L, item.comboItemId)
        assertEquals(1, item.doneCount)
    }

    /** T-L2 复制上周：目标周逐日镜像源周，源周无安排的日子在目标周删除；计数只算复制天数。 */
    @Test
    fun `复制上周 - 目标周逐日镜像源周 多余旧安排删除 计数只算复制天数`() = runTest {
        val daos = FakeDaos()
        val sourceMonday = 700L
        val targetMonday = 707L
        val sourceTraining = DayPlanEntity(date = sourceMonday, isRest = false, comboId = 10)
        val sourceRest = DayPlanEntity(date = sourceMonday + 2, isRest = true, comboId = null)
        daos.dayPlans.plans.value = listOf(sourceTraining, sourceRest) +
            (0L..6L).map { DayPlanEntity(date = targetMonday + it, isRest = false, comboId = 99) }
        val vm = buildVm(daos)

        var copied = -1
        vm.copyWeek(sourceMonday, targetMonday) { copied = it }
        advanceUntilIdle()

        assertEquals(2, copied)
        val target = daos.dayPlans.plans.value
            .filter { it.date in targetMonday..targetMonday + 6 }
            .associateBy { it.date }
        // 目标周只剩源周有安排的 2 天（其余 5 天旧安排被删）
        assertEquals(setOf(targetMonday, targetMonday + 2), target.keys)
        assertEquals(sourceTraining.copy(date = targetMonday), target[targetMonday])
        assertEquals(sourceRest.copy(date = targetMonday + 2), target[targetMonday + 2])
        // 源周原样
        assertEquals(
            listOf(sourceTraining, sourceRest),
            daos.dayPlans.plans.value.filter { it.date in sourceMonday..sourceMonday + 6 },
        )
    }

    /** T-L6 加载态契约：dayPlans 初始值 null（UI 显示加载中），有订阅者后数据到达非 null。 */
    @Test
    fun `dayPlans 加载态 - 初始 null 数据到达后非 null`() = runTest {
        val daos = FakeDaos()
        val vm = PlanViewModel(daos)
        assertNull(vm.dayPlans.value) // DuringSubscribed：未订阅时保持初始 null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.dayPlans.collect {} }
        advanceUntilIdle()
        assertNotNull(vm.dayPlans.value)
        assertEquals(emptyMap<Long, DayPlanEntity>(), vm.dayPlans.value)
    }
}

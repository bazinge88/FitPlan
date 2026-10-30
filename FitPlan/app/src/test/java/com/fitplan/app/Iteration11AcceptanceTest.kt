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
import com.fitplan.app.ui.exercise.ExerciseListViewModel
import com.fitplan.app.ui.plan.PlanViewModel
import com.fitplan.app.ui.plan.WorkoutFlowBundle
import com.fitplan.app.ui.plan.WorkoutViewModel
import com.fitplan.app.ui.plan.buildWorkoutUiState
import com.fitplan.app.ui.plan.isWorkoutAllDone
import com.fitplan.app.ui.settings.ExportScope
import com.fitplan.app.ui.settings.SettingsViewModel
import com.fitplan.app.util.buildPrEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 第十一轮跨切面验收测试（T-V1，verifier 独立编写，不与开发测试重复）。
 *
 * 覆盖计划 §5 T-V1 要求的四个跨切面场景：
 * 1) 多训练者完整隔离：打卡 + 当日备注 + 体重 + 当次重量 + PR + 导出；
 * 2) 空组合：状态构建空 items 且完成判定为假；
 * 3) 复制上周精确镜像 + 移除安排 + 历史保留；
 * 4) 删除动作孤儿清理 + 其他动作记录保留。
 *
 * 只用 FakeDaos + 公开 API，不引入 UI 测试框架；日期一律用固定 epochDay，避免时间/时区抖动。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Iteration11AcceptanceTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 建 WorkoutViewModel 并订阅 state（stateIn 为 WhileSubscribed，须有订阅者），返回后可直接断言。 */
    private fun TestScope.buildWorkoutVm(daos: FakeDaos, date: Long): WorkoutViewModel {
        val vm = WorkoutViewModel(daos, date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    /** 建 PlanViewModel 并订阅 nullable dayPlans 与 durations（WhileSubscribed）。 */
    private fun TestScope.buildPlanVm(daos: FakeDaos): PlanViewModel {
        val vm = PlanViewModel(daos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.dayPlans.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.durations.collect {} }
        return vm
    }

    @Test
    fun `多训练者完整隔离 - 打卡备注体重当次重量PR与导出`() = runTest {
        val daos = FakeDaos()
        val date = 100L
        daos.appUsers.users.value = listOf(
            UserEntity(id = 1, name = "阿明", sort = 0),
            UserEntity(id = 2, name = "小红", sort = 1),
        )
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 1)
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(
                    ComboItemEntity(
                        id = 101, comboId = 10, exerciseId = 1, sort = 0,
                        targetSets = 2, targetReps = 8, targetWeight = 50.0,
                    ),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val vm = buildWorkoutVm(daos, date)
        val comboItems = daos.combos.combos.value.flatMap { it.items }

        // 训练者 1（当前训练者）：当次重量 → 打卡（快照）→ 体重 → 当日备注
        vm.recordSessionWeight(exerciseId = 1L, weight = 60.0)
        vm.checkIn(comboItemId = 101L, setIndex = 0, reps = 8)
        vm.recordBodyWeight(70.0)
        vm.recordDayNote("阿明状态好")

        val a = vm.state.value
        assertEquals("胸腿日", a.comboName)
        val aItem = a.items.single()
        assertTrue(aItem.sets.first().checked)
        assertEquals(8, aItem.sets.first().reps)
        assertEquals(60.0, aItem.sets.first().weight!!, 0.0)
        assertEquals(1, aItem.doneCount)
        assertEquals(70.0, a.bodyWeight!!, 0.0)
        assertEquals("阿明状态好", a.dayNote)
        // PR：只统计训练者 1 自己的有效打卡（次数 + 重量 + 归属齐全）
        assertTrue(buildPrEntries(daos.checkins.rows.value.filter { it.userId == 1L }, comboItems).isNotEmpty())

        // 切换到训练者 2：训练者 1 的进度、当次重量、体重、备注、PR 全部不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        val bBefore = vm.state.value
        assertFalse(bBefore.items.single().sets.first().checked)
        assertEquals(0, bBefore.items.single().doneCount)
        assertNull(bBefore.items.single().sessionWeight)
        assertNull(bBefore.bodyWeight)
        assertNull(bBefore.dayNote)
        assertTrue(buildPrEntries(daos.checkins.rows.value.filter { it.userId == 2L }, comboItems).isEmpty())

        // 训练者 2 记录自己的数据：同天同组不同 userId 各一行，互不覆盖
        vm.recordSessionWeight(exerciseId = 1L, weight = 80.0)
        vm.checkIn(comboItemId = 101L, setIndex = 0, reps = 10)
        vm.recordBodyWeight(55.0)
        vm.recordDayNote("小红恢复中")

        assertEquals(2, daos.checkins.rows.value.size)
        val user2Row = daos.checkins.rows.value.single { it.userId == 2L }
        assertEquals(10, user2Row.reps)
        assertEquals(80.0, user2Row.weight!!, 0.0)

        // 切回训练者 1：自己的数据原样，训练者 2 的数据不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 1)
        val aAgain = vm.state.value
        assertEquals(8, aAgain.items.single().sets.first().reps)
        assertEquals(60.0, aAgain.items.single().sets.first().weight!!, 0.0)
        assertEquals(70.0, aAgain.bodyWeight!!, 0.0)
        assertEquals("阿明状态好", aAgain.dayNote)

        // 导出：多训练者「用户」列、两人打卡与体重、两条当日备注带用户名且按 userId 升序
        val markdown = SettingsViewModel(daos).buildMarkdown(ExportScope.All)
        assertTrue(markdown.contains("| 用户 | 组合 | 动作 | 组 | 完成次数 |"))
        assertTrue(markdown.contains("| 阿明 | 胸腿日 | 卧推 | 1 | 8×60 |"))
        assertTrue(markdown.contains("| 小红 | 胸腿日 | 卧推 | 1 | 10×80 |"))
        assertTrue(markdown.contains("| 日期 | 星期 | 用户 | 体重（kg） |"))
        assertTrue(markdown.contains("| 阿明 | 70 |"))
        assertTrue(markdown.contains("| 小红 | 55 |"))
        assertTrue(markdown.contains("当日备注（阿明）：阿明状态好"))
        assertTrue(markdown.contains("当日备注（小红）：小红恢复中"))
        assertTrue(markdown.indexOf("当日备注（阿明）") < markdown.indexOf("当日备注（小红）"))
    }

    @Test
    fun `空组合 - 状态构建无动作且完成判定为假`() {
        val daos = FakeDaos()
        val date = 100L
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(combo = ComboEntity(id = 10, name = "空组合"), items = emptyList()),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))

        val state = buildWorkoutUiState(
            date = date,
            plans = daos.dayPlans.plans.value,
            combos = daos.combos.combos.value,
            exercises = daos.exercises.exercises.value,
            checkins = emptyList(),
            flow = WorkoutFlowBundle(
                settings = SettingsEntity(id = 1, activeUserId = 1),
                sessionWeights = emptyList(),
                skips = emptyList(),
                bodyWeights = emptyList(),
                dayNotes = emptyList(),
                exerciseNotes = emptyList(),
            ),
        )

        // 有组合但无条目：comboName 非空、清单为空、loading 完成（UI 据此显示空态引导而非空白）
        assertEquals("空组合", state.comboName)
        assertTrue(state.items.isEmpty())
        assertFalse(state.loading)
        // 完成判定：空列表不算完成（空组合不得被误判为今日完成）
        assertFalse(isWorkoutAllDone(emptyList()))
        assertFalse(isWorkoutAllDone(state.items))
    }

    @Test
    fun `复制上周 - 逐日镜像删除幽灵安排 移除安排不损六类历史`() = runTest {
        val daos = FakeDaos()
        val sourceMonday = 700L
        val targetMonday = 707L
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(
                    ComboItemEntity(id = 111, comboId = 10, exerciseId = 1, sort = 0, targetSets = 2),
                ),
            ),
        )
        // 源周仅 2 天有安排（训练日 + 休息日）；目标周 7 天预置旧安排（comboid 99 的幽灵计划）
        val sourceTraining = DayPlanEntity(date = sourceMonday, isRest = false, comboId = 10)
        val sourceRest = DayPlanEntity(date = sourceMonday + 2, isRest = true, comboId = null)
        daos.dayPlans.plans.value = listOf(sourceTraining, sourceRest) +
            (0L..6L).map { DayPlanEntity(date = targetMonday + it, isRest = false, comboId = 99) }

        // 将被删除（711）与被覆盖（707）的两天预置六类历史记录，复制与移除安排都不得波及
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = targetMonday, comboItemId = 111, setIndex = 0, reps = 8, userId = 1),
            SetCheckinEntity(date = targetMonday + 4, comboItemId = 111, setIndex = 0, reps = 10, userId = 1),
        )
        daos.skips.rows.value = listOf(
            SetSkipEntity(date = targetMonday + 4, comboItemId = 111, setIndex = 1, userId = 1),
        )
        daos.dayNotes.rows.value = listOf(
            DayNoteEntity(date = targetMonday, note = "旧备注一", userId = 1),
            DayNoteEntity(date = targetMonday + 4, note = "旧备注二", userId = 1),
        )
        daos.exerciseNotes.rows.value = listOf(
            ExerciseNoteEntity(date = targetMonday, exerciseId = 1, note = "旧动作备注一", userId = 1),
            ExerciseNoteEntity(date = targetMonday + 4, exerciseId = 1, note = "旧动作备注二", userId = 1),
        )
        daos.bodyWeights.rows.value = listOf(
            BodyWeightEntity(date = targetMonday, weight = 70.0, userId = 1),
            BodyWeightEntity(date = targetMonday + 4, weight = 71.5, userId = 1),
        )
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = targetMonday, exerciseId = 1, weight = 60.0, userId = 1),
            SessionWeightEntity(date = targetMonday + 4, exerciseId = 1, weight = 61.0, userId = 1),
        )
        val historyBefore = listOf(
            daos.checkins.rows.value,
            daos.skips.rows.value,
            daos.dayNotes.rows.value,
            daos.exerciseNotes.rows.value,
            daos.bodyWeights.rows.value,
            daos.sessionWeights.rows.value,
        )

        val vm = buildPlanVm(daos)
        var copied = -1
        vm.copyWeek(sourceMonday, targetMonday) { copied = it }
        advanceUntilIdle()

        // 计数只算复制天数（源周 2 天），其余 5 天幽灵安排被删
        assertEquals(2, copied)
        val targetPlans = daos.dayPlans.plans.value
            .filter { it.date in targetMonday..targetMonday + 6 }
            .associateBy { it.date }
        assertEquals(setOf(targetMonday, targetMonday + 2), targetPlans.keys)
        assertEquals(sourceTraining.copy(date = targetMonday), targetPlans[targetMonday])
        assertEquals(sourceRest.copy(date = targetMonday + 2), targetPlans[targetMonday + 2])
        // 源周不受影响
        assertEquals(
            listOf(sourceTraining, sourceRest),
            daos.dayPlans.plans.value.filter { it.date in sourceMonday..sourceMonday + 6 },
        )
        // 目标周被删除/被覆盖日期的六类历史逐行原样
        assertEquals(
            historyBefore,
            listOf(
                daos.checkins.rows.value,
                daos.skips.rows.value,
                daos.dayNotes.rows.value,
                daos.exerciseNotes.rows.value,
                daos.bodyWeights.rows.value,
                daos.sessionWeights.rows.value,
            ),
        )

        // 移除安排：只删该天计划行，历史仍原样
        vm.clearDay(targetMonday)
        advanceUntilIdle()
        assertNull(daos.dayPlans.plans.value.find { it.date == targetMonday })
        assertEquals(
            historyBefore,
            listOf(
                daos.checkins.rows.value,
                daos.skips.rows.value,
                daos.dayNotes.rows.value,
                daos.exerciseNotes.rows.value,
                daos.bodyWeights.rows.value,
                daos.sessionWeights.rows.value,
            ),
        )

        // 重新排程后历史按 date 回看：707 的旧打卡仍算在该条目进度里
        vm.setDay(targetMonday, isRest = false, comboId = 10)
        advanceUntilIdle()
        val reloaded = buildWorkoutVm(daos, targetMonday)
        assertEquals(1, reloaded.state.value.items.single().doneCount)
    }

    @Test
    fun `删除动作 - 清理该动作全部组合条目的孤儿打卡逃标 其他动作保留`() = runTest {
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
        )
        // 动作 A(1) 同时出现在两个组合（条目 101、201）；动作 B(2) 在组合 10（条目 102）
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0),
                    ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 1),
                ),
            ),
            ComboWithItems(
                combo = ComboEntity(id = 20, name = "背日"),
                items = listOf(ComboItemEntity(id = 201, comboId = 20, exerciseId = 1, sort = 0)),
            ),
        )
        // 两个训练者在 A 的条目上都有打卡/逃标；B 的记录必须保留
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = 100, comboItemId = 101, setIndex = 0, reps = 8, userId = 1),
            SetCheckinEntity(date = 101, comboItemId = 101, setIndex = 1, reps = 6, userId = 2),
            SetCheckinEntity(date = 100, comboItemId = 201, setIndex = 0, reps = 5, userId = 2),
            SetCheckinEntity(date = 100, comboItemId = 102, setIndex = 0, reps = 10, userId = 1),
        )
        daos.skips.rows.value = listOf(
            SetSkipEntity(date = 100, comboItemId = 101, setIndex = 2, userId = 1),
            SetSkipEntity(date = 101, comboItemId = 201, setIndex = 1, userId = 2),
            SetSkipEntity(date = 100, comboItemId = 102, setIndex = 1, userId = 1),
        )

        val exerciseA = daos.exercises.exercises.value.single { it.id == 1L }
        ExerciseListViewModel(daos).delete(exerciseA)
        advanceUntilIdle()

        // 动作 A 消失，动作 B 保留
        assertEquals(listOf(2L), daos.exercises.exercises.value.map { it.id })
        // A 的条目（101、201）在两表中的全部训练者行清空；B 的条目 102 原样
        assertEquals(listOf(102L), daos.checkins.rows.value.map { it.comboItemId }.distinct())
        assertEquals(1, daos.checkins.rows.value.size)
        assertEquals(listOf(102L), daos.skips.rows.value.map { it.comboItemId }.distinct())
        assertEquals(1, daos.skips.rows.value.size)
        assertTrue(daos.checkins.rows.value.none { it.comboItemId == 101L || it.comboItemId == 201L })
        assertTrue(daos.skips.rows.value.none { it.comboItemId == 101L || it.comboItemId == 201L })
    }
}

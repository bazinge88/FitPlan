package com.fitplan.app.ui.plan

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val date = 100L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 建 VM 并订阅 state（stateIn 为 WhileSubscribed，须有订阅者），返回后断言 vm.state.value。 */
    private fun TestScope.buildVm(daos: FakeDaos, date: Long = this@WorkoutViewModelTest.date): WorkoutViewModel {
        val vm = WorkoutViewModel(daos, date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    /** 预置：动作 + 单组合（单动作条目）+ 当日非休息计划，返回 comboItemId。 */
    private fun seedSingleItem(
        daos: FakeDaos,
        itemId: Long = 101L,
        exerciseName: String = "卧推",
        targetSets: Int? = 3,
        targetReps: Int? = 8,
        targetWeight: Double? = null,
    ): Long {
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = exerciseName))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(
                    ComboItemEntity(
                        id = itemId, comboId = 10, exerciseId = 1, sort = 0,
                        targetSets = targetSets, targetReps = targetReps, targetWeight = targetWeight,
                    ),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        return itemId
    }

    @Test
    fun `当日非休息计划 - 组合名 动作名排序与目标字段正确`() = runTest {
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲", trackWeight = false),
        )
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                // 故意乱序插入，验证按 sort 排序输出
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 2,
                        targetSets = 3, targetReps = 8, targetWeight = 50.0),
                    ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 1,
                        targetDuration = 10),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val vm = buildVm(daos)

        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals("胸腿日", s.comboName)
        assertFalse(s.unitIsLb)
        assertEquals(2, s.items.size)
        assertEquals(102L, s.items[0].comboItemId) // sort=1 在前
        assertEquals(101L, s.items[1].comboItemId)
        assertEquals("深蹲", s.items[0].exerciseName)
        assertEquals("卧推", s.items[1].exerciseName)
        assertEquals(3, s.items[1].targetSets)
        assertEquals(8, s.items[1].targetReps)
        assertEquals(50.0, s.items[1].targetWeight!!, 0.0)
        assertEquals(10, s.items[0].targetDuration)
        // trackWeight 跟随动作配置
        assertFalse(s.items[0].trackWeight)
        assertTrue(s.items[1].trackWeight)
    }

    @Test
    fun `单位 - 跟随设置 unitIsLb`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos)
        daos.appSettings.settings.value = SettingsEntity(id = 1, unitIsLb = true)
        val vm = buildVm(daos)
        assertTrue(vm.state.value.unitIsLb)
    }

    @Test
    fun `当日无计划 - comboName 为空且结束加载`() = runTest {
        val daos = FakeDaos()
        val vm = buildVm(daos)
        val s = vm.state.value
        assertFalse(s.loading)
        assertNull(s.comboName)
        assertEquals(0, s.items.size)
    }

    @Test
    fun `当日休息日 - comboName 为空且结束加载`() = runTest {
        val daos = FakeDaos()
        daos.combos.combos.value = listOf(ComboWithItems(ComboEntity(10, "胸腿日"), emptyList()))
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = true, comboId = 10))
        val vm = buildVm(daos)
        val s = vm.state.value
        assertFalse(s.loading)
        assertNull(s.comboName)
        assertEquals(0, s.items.size)
    }

    @Test
    fun `当日 comboId 为空 - comboName 为空且结束加载`() = runTest {
        val daos = FakeDaos()
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = null))
        val vm = buildVm(daos)
        val s = vm.state.value
        assertFalse(s.loading)
        assertNull(s.comboName)
        assertEquals(0, s.items.size)
    }

    @Test
    fun `组行数 - 无目标至少一行 doneCount 超过目标组数则增行`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, itemId = 101, targetSets = 3)
        val vm = buildVm(daos)
        assertEquals(3, vm.state.value.items.single().sets.size) // targetSets=3 → 3 行

        // 无目标组数：至少 1 行
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(ComboItemEntity(id = 102, comboId = 10, exerciseId = 1, sort = 0, targetSets = null)),
            ),
        )
        var item = vm.state.value.items.single()
        assertEquals(102L, item.comboItemId)
        assertEquals(1, item.sets.size)
        assertEquals(0, item.doneCount)

        // doneCount=1 未超过 rowCount=max(0,1,1)=1 → 行数仍为 1（setIndex=5 的行在可见范围之外）
        vm.checkIn(102L, 5, null)
        item = vm.state.value.items.single()
        assertEquals(1, item.sets.size)
        assertEquals(1, item.doneCount)

        // doneCount 超过目标组数 → rowCount 增长（新增一行）
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(ComboItemEntity(id = 103, comboId = 10, exerciseId = 1, sort = 0, targetSets = 2)),
            ),
        )
        vm.checkIn(103L, 0, 8)
        vm.checkIn(103L, 1, 9)
        vm.checkIn(103L, 3, 7) // 超出目标组数的位置
        item = vm.state.value.items.single()
        assertEquals(3, item.sets.size) // rowCount = max(2, 3, 1) = 3
        assertEquals(3, item.doneCount)
    }

    @Test
    fun `checkIn - 记录次数 同键重复打卡 REPLACE 且不重复计数`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)
        var item = vm.state.value.items.single()
        assertEquals(8, item.sets[0].reps)
        assertEquals(1, item.doneCount)

        // 同键重复打卡不同次数：仍只有一行，reps 为新值
        vm.checkIn(itemId, 0, 10)
        item = vm.state.value.items.single()
        assertEquals(2, item.sets.size)   // 未额外增行
        assertEquals(10, item.sets[0].reps)
        assertEquals(1, item.doneCount)   // doneCount 不重复累加
    }

    @Test
    fun `打卡 - 会话进行中主页首组打卡写入用时起点`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        // 会话已挂出（行存在、尚未打卡）
        daos.workoutSessions.rows.value = listOf(WorkoutSessionEntity(date = date))
        val vm = buildVm(daos)

        // 首次打卡：写用时起点
        vm.checkIn(itemId, 0, 8)
        val firstStartedAt = daos.workoutSessions.rows.value.single().startedAt
        assertTrue(firstStartedAt != null)

        // 再次打卡：起点不覆盖（与通知接收器路径幂等共存）
        vm.checkIn(itemId, 1, 9)
        assertEquals(firstStartedAt, daos.workoutSessions.rows.value.single().startedAt)
    }

    @Test
    fun `打卡 - 无会话时主页打卡不新建会话行`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)
        // 打卡本身生效，但不凭空创建会话行
        assertEquals(8, vm.state.value.items.single().sets[0].reps)
        assertNull(daos.workoutSessionDao.get())
    }

    @Test
    fun `打卡 - 主页打卡快照当次重量到该组`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        // 预置当天该动作的当次重量
        daos.sessionWeights.rows.value =
            listOf(SessionWeightEntity(date = date, exerciseId = 1, weight = 52.5))
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)

        // 该组记录快照了当天的当次重量
        val row = daos.checkins.rows.value.single()
        assertEquals(date, row.date)
        assertEquals(8, row.reps)
        assertEquals(52.5, row.weight!!, 0.0)
    }

    @Test
    fun `打卡 - 无当次重量时该组只记次数`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)

        // 未记当次重量：该组只记次数，weight 为 null
        val row = daos.checkins.rows.value.single()
        assertEquals(8, row.reps)
        assertNull(row.weight)
    }

    @Test
    fun `cancelCheckIn - 行消失且 doneCount 回落`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)
        vm.checkIn(itemId, 1, 9)
        vm.checkIn(itemId, 3, 7) // 超出目标组数的位置
        var item = vm.state.value.items.single()
        assertEquals(3, item.sets.size)   // rowCount = max(2, 3, 1) = 3
        assertEquals(3, item.doneCount)

        // 取消 setIndex=3：行数回落到目标组数，doneCount 回落
        vm.cancelCheckIn(itemId, 3)
        item = vm.state.value.items.single()
        assertEquals(2, item.sets.size)
        assertEquals(2, item.doneCount)
        assertEquals(8, item.sets[0].reps)
        assertEquals(9, item.sets[1].reps)
    }

    @Test
    fun `adjustReps - 原子增减 下限1 未打卡与只记完成不受影响`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)

        vm.checkIn(itemId, 0, 8)
        vm.adjustReps(itemId, 0, +1)
        assertEquals(9, vm.state.value.items.single().sets[0].reps)

        vm.adjustReps(itemId, 0, -2)
        assertEquals(7, vm.state.value.items.single().sets[0].reps)

        // 减到下限 1 不会再低
        vm.adjustReps(itemId, 0, -100)
        assertEquals(1, vm.state.value.items.single().sets[0].reps)

        // 未打卡的行：无效果（不凭空打卡）
        vm.adjustReps(itemId, 1, +1)
        assertNull(vm.state.value.items.single().sets[1].reps)

        // 只记完成（reps 为 null）：无效果，仍保持只记完成
        vm.checkIn(itemId, 1, null)
        vm.adjustReps(itemId, 1, +1)
        val item = vm.state.value.items.single()
        assertNull(item.sets[1].reps)
        assertEquals(2, item.doneCount)
    }

    @Test
    fun `recordSessionWeight - 当次重量记录到当天且不写回组合条目`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = 50.0)
        val vm = buildVm(daos)

        vm.recordSessionWeight(exerciseId = 1, weight = 52.5)

        // 当次重量落库：一行 = (当天, 动作)
        assertEquals(1, daos.sessionWeights.rows.value.size)
        val record = daos.sessionWeights.rows.value.single()
        assertEquals(date, record.date)
        assertEquals(1L, record.exerciseId)
        assertEquals(52.5, record.weight, 0.0)
        // 不写回组合条目：目标重量保持 50
        assertEquals(50.0, daos.combos.combos.value.single().items.single().targetWeight!!, 0.0)
    }

    @Test
    fun `sessionWeight - 状态带出当天当次重量 无记录时为 null`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = 50.0)
        val vm = buildVm(daos)
        assertNull(vm.state.value.items.single().sessionWeight)

        vm.recordSessionWeight(exerciseId = 1, weight = 52.5)
        assertEquals(52.5, vm.state.value.items.single().sessionWeight!!, 0.0)
    }

    @Test
    fun `recordSessionWeight - null 取消当次记录`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos)
        val vm = buildVm(daos)

        vm.recordSessionWeight(exerciseId = 1, weight = 52.5)
        assertEquals(1, daos.sessionWeights.rows.value.size)

        vm.recordSessionWeight(exerciseId = 1, weight = null)
        assertEquals(0, daos.sessionWeights.rows.value.size)
        assertNull(vm.state.value.items.single().sessionWeight)
    }

    @Test
    fun `syncPrompt - 负数辅助重量按数值比较`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = -22.5)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = -20.0),
        )
        val vm = buildVm(daos)
        // -20 > -22.5：数值更大的辅助重量（辅助更少）视为更重，触发提醒
        assertEquals(-20.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)
    }

    @Test
    fun `syncPrompt - 历史当次最大超过目标重量时提示 当天记录不计入`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = 50.0)
        // 前天 48（低于目标不提示）、昨天 55（超过目标 → 提示 55）
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 2, exerciseId = 1, weight = 48.0),
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 55.0),
        )
        val vm = buildVm(daos)
        assertEquals(55.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)

        // 当天的记录不提醒自己：再记 60，提示仍指向 55
        vm.recordSessionWeight(exerciseId = 1, weight = 60.0)
        assertEquals(55.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)
    }

    @Test
    fun `syncPrompt - 忽略后记住该重量不再提示 更大重量重新提示`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = 50.0)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 55.0),
        )
        val vm = buildVm(daos)
        assertEquals(55.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)

        // 忽略：动作记录已处理重量，同重量不再提示
        vm.dismissSyncPrompt(exerciseId = 1, weight = 55.0)
        assertEquals(55.0, daos.exercises.exercises.value.single().ackMaxWeight!!, 0.0)
        assertNull(vm.state.value.items.single().syncPromptWeight)

        // 更大重量出现：重新提示
        daos.sessionWeights.rows.value = daos.sessionWeights.rows.value +
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 60.0)
        assertEquals(60.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)
    }

    /** 预置：动作 1 在两个组合各出现一处（目标分别为 50 / 45），今天计划组合 10、明天计划组合 20。 */
    private fun seedTwoCombos(daos: FakeDaos) {
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "推A"),
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0, targetWeight = 50.0),
                ),
            ),
            ComboWithItems(
                combo = ComboEntity(id = 20, name = "推B"),
                items = listOf(
                    ComboItemEntity(id = 201, comboId = 20, exerciseId = 1, sort = 0, targetWeight = 45.0),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(
            DayPlanEntity(date = date, isRest = false, comboId = 10),
            DayPlanEntity(date = date + 1, isRest = false, comboId = 20),
        )
    }

    @Test
    fun `syncToItem - 仅当前条目写为目标重量并记住该重量`() = runTest {
        val daos = FakeDaos()
        seedTwoCombos(daos)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 55.0),
        )
        val vm = buildVm(daos)
        val vmTomorrow = buildVm(daos, date = date + 1)
        // 两个组合条目都因历史最大 55 超过各自目标而提示
        assertEquals(55.0, vm.state.value.items.single().syncPromptWeight!!, 0.0)
        assertEquals(55.0, vmTomorrow.state.value.items.single().syncPromptWeight!!, 0.0)

        vm.syncToItem(comboItemId = 101, exerciseId = 1, weight = 55.0)

        // 仅当前条目写为目标重量；另一组合条目不动
        val items = daos.combos.combos.value.flatMap { it.items }.associateBy { it.id }
        assertEquals(55.0, items[101L]!!.targetWeight!!, 0.0)
        assertEquals(45.0, items[201L]!!.targetWeight!!, 0.0)
        // 已处理重量按动作记录：同重量在其他组合也不再提示（目标仍是 45）
        assertEquals(55.0, daos.exercises.exercises.value.single().ackMaxWeight!!, 0.0)
        assertNull(vm.state.value.items.single().syncPromptWeight)
        assertNull(vmTomorrow.state.value.items.single().syncPromptWeight)
        assertEquals(45.0, vmTomorrow.state.value.items.single().targetWeight!!, 0.0)
    }

    @Test
    fun `syncToAll - 写入该动作全部条目并记住该重量`() = runTest {
        val daos = FakeDaos()
        seedTwoCombos(daos)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 55.0),
        )
        val vm = buildVm(daos)
        val vmTomorrow = buildVm(daos, date = date + 1)

        vm.syncToAll(exerciseId = 1, weight = 55.0)

        val items = daos.combos.combos.value.flatMap { it.items }.associateBy { it.id }
        assertEquals(55.0, items[101L]!!.targetWeight!!, 0.0)
        assertEquals(55.0, items[201L]!!.targetWeight!!, 0.0)
        assertEquals(55.0, daos.exercises.exercises.value.single().ackMaxWeight!!, 0.0)
        assertNull(vm.state.value.items.single().syncPromptWeight)
        assertNull(vmTomorrow.state.value.items.single().syncPromptWeight)
    }

    @Test
    fun `syncPrompt - trackWeight 为 false 的动作不提示`() = runTest {
        val daos = FakeDaos()
        seedSingleItem(daos, targetWeight = 50.0)
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推", trackWeight = false))
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date - 1, exerciseId = 1, weight = 55.0),
        )
        val vm = buildVm(daos)
        assertNull(vm.state.value.items.single().syncPromptWeight)
    }

    @Test
    fun `标逃 - 组行带逃标 取消后恢复未练`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 3)
        val vm = buildVm(daos)

        vm.skipSet(itemId, 1)
        var item = vm.state.value.items.single()
        assertTrue(item.sets[1].skipped)
        assertFalse(item.sets[1].checked)
        assertEquals(1, daos.skips.rows.value.size)

        vm.cancelSetSkip(itemId, 1)
        item = vm.state.value.items.single()
        assertFalse(item.sets[1].skipped)
        assertEquals(0, daos.skips.rows.value.size)
    }

    @Test
    fun `打卡覆盖逃标 - 已逃组打卡转为打卡 逃标消失`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)
        vm.skipSet(itemId, 0)

        vm.checkIn(itemId, 0, 8)

        assertEquals(0, daos.skips.rows.value.size)
        val item = vm.state.value.items.single()
        assertTrue(item.sets[0].checked)
        assertFalse(item.sets[0].skipped)
        assertEquals(8, item.sets[0].reps)
        assertEquals(1, item.doneCount)
    }

    @Test
    fun `完成判定 - 逃组不算完成 全部组打卡才完成`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)
        vm.checkIn(itemId, 0, 8)
        vm.skipSet(itemId, 1)

        // 全部组打卡才算完成：另一组已逃 → doneCount < 行数
        var item = vm.state.value.items.single()
        assertTrue(item.sets[0].checked)
        assertTrue(item.sets[1].skipped)
        assertFalse(item.sets[1].checked)
        assertEquals(1, item.doneCount)
        assertEquals(2, item.sets.size)
        assertFalse(item.doneCount == item.sets.size)

        // 补打卡覆盖逃标后才满足完成判定
        vm.checkIn(itemId, 1, 6)
        item = vm.state.value.items.single()
        assertEquals(2, item.doneCount)
        assertEquals(item.sets.size, item.doneCount)
        assertFalse(item.sets[1].skipped)
    }

    @Test
    fun `多训练者 - 逃标按用户隔离 互不可见互不覆盖`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)
        vm.skipSet(itemId, 0)
        assertTrue(vm.state.value.items.single().sets[0].skipped)

        // 切换训练者：同一组的逃标不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        var s = vm.state.value.items.single()
        assertFalse(s.sets[0].skipped)
        assertEquals(0, s.doneCount)

        // 训练者 2 标逃同一组：各得一行互不覆盖
        vm.skipSet(itemId, 0)
        assertEquals(2, daos.skips.rows.value.size)
        assertTrue(daos.skips.rows.value.any { it.userId == 1L })
        assertTrue(daos.skips.rows.value.any { it.userId == 2L })
        s = vm.state.value.items.single()
        assertTrue(s.sets[0].skipped)
    }

    @Test
    fun `逃标行 - 超出目标组数仍渲染 可见且可取消`() = runTest {
        // 场景：组合编辑缩小目标组数后遗留的越界逃标（与打卡超目标的增行行为对齐）
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 1)
        val vm = buildVm(daos)
        vm.skipSet(itemId, 0)
        var item = vm.state.value.items.single()
        assertEquals(1, item.sets.size)
        assertTrue(item.sets[0].skipped)

        // 越界逃标同样增行渲染（行数盖住最大已逃组序），行可见即可手动取消
        daos.skips.rows.value = daos.skips.rows.value +
            SetSkipEntity(date = date, comboItemId = itemId, setIndex = 2)
        item = vm.state.value.items.single()
        assertEquals(3, item.sets.size)
        assertTrue(item.sets[2].skipped)

        vm.cancelSetSkip(itemId, 2)
        item = vm.state.value.items.single()
        assertEquals(1, item.sets.size)
        assertTrue(item.sets[0].skipped)
    }

    @Test
    fun `syncCount - 状态报告各动作在全部组合中的条目数`() = runTest {
        val daos = FakeDaos()
        // 动作 1 出现在两个组合共 3 处（组合 2 里出现两次）；动作 2 仅 1 处
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
        )
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸日"),
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0, targetWeight = 40.0),
                    ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 1, targetWeight = 80.0),
                ),
            ),
            ComboWithItems(
                combo = ComboEntity(id = 20, name = "腿日"),
                items = listOf(
                    ComboItemEntity(id = 201, comboId = 20, exerciseId = 1, sort = 0, targetWeight = 45.0),
                    ComboItemEntity(id = 202, comboId = 20, exerciseId = 1, sort = 1, targetWeight = 50.0),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val vm = buildVm(daos)

        // 状态里带出各动作的同步命中数（含自身），供提醒条「同步到所有组合」展示影响范围
        assertEquals(3, vm.state.value.items.first { it.comboItemId == 101L }.syncCount)
        assertEquals(1, vm.state.value.items.first { it.comboItemId == 102L }.syncCount)
    }

    @Test
    fun `defaultRepsFor - 目标优先 其次沿用前一组 否则 null`() = runTest {
        val base = WorkoutItemUi(
            comboItemId = 1, exerciseId = 1, exerciseName = "卧推", targetSets = 3,
            targetReps = null, targetWeight = null, targetDuration = null,
            sets = emptyList(), doneCount = 0,
        )
        // targetReps 非 null 时优先
        assertEquals(10, defaultRepsFor(base.copy(targetReps = 10), 0))

        // targetReps 为 null：沿用前一组已打卡的 reps
        val withSets = base.copy(sets = listOf(WorkoutSetUi(0, checked = true, reps = 8), WorkoutSetUi(1, checked = true, reps = null)))
        assertEquals(8, defaultRepsFor(withSets, 1))
        assertNull(defaultRepsFor(withSets, 2)) // 前一组未打卡 → null
        assertNull(defaultRepsFor(withSets, 0)) // 第一组无前一组 → null
    }

    @Test
    fun `只记完成的组 - checked 为真 reps 仍为 null`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetReps = null)
        val vm = buildVm(daos)
        vm.checkIn(itemId, 0, null)
        val item = vm.state.value.items.single()
        assertEquals(1, item.doneCount)
        val set = item.sets.first { it.setIndex == 0 }
        assertTrue(set.checked)
        assertNull(set.reps)
    }

    @Test
    fun `最后一项动作打卡 - 完成态与前面动作一致 不被边界跳过`() = runTest {
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
            ExerciseEntity(id = 3, name = "平板支撑"),
        )
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "全身日"),
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0,
                        targetSets = 2, targetReps = 8),
                    ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 1,
                        targetSets = 2, targetReps = 10),
                    // 最后一项无目标次数：打卡只记完成，行存在即完成
                    ComboItemEntity(id = 103, comboId = 10, exerciseId = 3, sort = 2,
                        targetSets = 1, targetReps = null),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val vm = buildVm(daos)
        vm.checkIn(101, 0, 8)
        vm.checkIn(101, 1, 8)
        vm.checkIn(102, 0, 10)
        vm.checkIn(102, 1, 10)
        vm.checkIn(103, 0, null)
        val items = vm.state.value.items
        assertEquals(listOf(101L, 102L, 103L), items.map { it.comboItemId })
        items.forEach { item ->
            assertEquals("item ${item.comboItemId}", item.sets.size, item.doneCount)
            item.sets.forEach { set -> assertTrue("item ${item.comboItemId} set ${set.setIndex}", set.checked) }
        }
        // 最后一项：行存在即完成（reps 为 null 也算），聚合完成判定与前面动作一致
        val last = items.last()
        assertEquals(1, last.sets.size)
        assertTrue(last.sets[0].checked)
        assertNull(last.sets[0].reps)
        assertEquals(last.sets.size, last.doneCount)
    }

    @Test
    fun `打卡 - 组已存在时重复打卡保留该组重量`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = date, comboItemId = itemId, setIndex = 0, reps = 8, weight = 55.0),
        )
        // 动作级当次重量 60 不得覆盖该组已有的 55
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date, exerciseId = 1, weight = 60.0),
        )
        val vm = buildVm(daos)
        vm.checkIn(itemId, 0, 6)
        val row = daos.checkins.rows.value.single()
        assertEquals(6, row.reps)
        assertEquals(55.0, row.weight!!, 0.0)
    }

    @Test
    fun `updateSetWeight - 只改该组重量 不影响他组与当次重量`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date, exerciseId = 1, weight = 50.0),
        )
        val vm = buildVm(daos)
        vm.checkIn(itemId, 0, 8)
        vm.checkIn(itemId, 1, 8)
        vm.updateSetWeight(itemId, 1, 57.5)
        val rows = daos.checkins.rows.value.sortedBy { it.setIndex }
        assertEquals(50.0, rows[0].weight!!, 0.0)
        assertEquals(57.5, rows[1].weight!!, 0.0)
        assertEquals(50.0, daos.sessionWeights.rows.value.single().weight, 0.0)
        // 清空并确定 = 清除该组重量
        vm.updateSetWeight(itemId, 1, null)
        assertNull(daos.checkins.rows.value.first { it.setIndex == 1 }.weight)
    }

    @Test
    fun `bug报告复现 - 等长动作只记完成3组 完成态与组行一致 重量回落目标`() = runTest {
        // 场景：等长单腿臀推 —— 目标 3 组 × 15kg × 每组 1 分钟，无目标次数（等长/计时动作）
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "等长单腿臀推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "后链肌群强化"),
                items = listOf(
                    ComboItemEntity(
                        id = 101, comboId = 10, exerciseId = 1, sort = 0,
                        targetSets = 3, targetReps = null, targetWeight = 15.0, targetDuration = 1,
                    ),
                ),
            ),
        )
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val vm = buildVm(daos)

        // 复现路径：完成 3 组（无目标次数 → defaultRepsFor 返回 null = 只记完成），未记当次重量
        repeat(3) { i -> vm.checkIn(101, i, defaultRepsFor(vm.state.value.items.single(), i)) }

        val item = vm.state.value.items.single()
        // 聚合完成态：绿色对勾 + 「已完成 3/3 组」的判定依据（doneCount == 行数）
        assertEquals(3, item.doneCount)
        assertEquals(3, item.sets.size)
        assertTrue(item.sets.isNotEmpty() && item.doneCount == item.sets.size)
        // 展开后组行：修复后每行 checked=true（实心圈 + 「已完成」），不再出现未打卡的「—」
        item.sets.forEach { set ->
            assertTrue("set ${set.setIndex} 应显示已完成", set.checked)
            assertNull("set ${set.setIndex} 无次数记录", set.reps)
            assertNull("set ${set.setIndex} 无重量记录", set.weight)
        }
        // 重量栏：未记当次重量时回落显示目标 15kg 并带「目标」标注（sessionWeight 为 null 即标注「目标」）
        assertNull(item.sessionWeight)
        assertEquals(15.0, item.targetWeight!!, 0.0)
    }

    @Test
    fun `bug报告复现 - 有目标次数动作打卡后组行带次数 不出现空档`() = runTest {
        // 对照组：普通负重动作打卡后，组行应有实际次数，聚合态与组行数据一致
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 3, targetReps = 8)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date, exerciseId = 1, weight = 15.0),
        )
        val vm = buildVm(daos)
        repeat(3) { i -> vm.checkIn(itemId, i, 8) }
        val item = vm.state.value.items.single()
        assertEquals(3, item.doneCount)
        item.sets.forEach { set ->
            assertTrue(set.checked)
            assertEquals(8, set.reps)
            assertEquals(15.0, set.weight!!, 0.0)
        }
    }

    @Test
    fun `多训练者 - 打卡与当次重量按用户隔离 互不覆盖`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 1)
        daos.sessionWeights.rows.value = listOf(
            SessionWeightEntity(date = date, exerciseId = 1, weight = 50.0, userId = 1),
        )
        val vm = buildVm(daos)
        // 训练者 1 打卡
        vm.checkIn(itemId, 0, 8)
        // 切换到训练者 2：同一组视为未打卡、当次重量不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        var s = vm.state.value.items.single()
        assertEquals(0, s.doneCount)
        assertFalse(s.sets[0].checked)
        assertNull(s.sessionWeight)
        // 训练者 2 打卡同一组：各得一行，互不覆盖
        daos.sessionWeights.rows.value = daos.sessionWeights.rows.value +
            SessionWeightEntity(date = date, exerciseId = 1, weight = 30.0, userId = 2)
        vm.checkIn(itemId, 0, 6)
        val rows = daos.checkins.rows.value
        assertEquals(2, rows.size)
        val u1 = rows.first { it.userId == 1L }
        val u2 = rows.first { it.userId == 2L }
        assertEquals(8, u1.reps)
        assertEquals(50.0, u1.weight!!, 0.0)
        assertEquals(6, u2.reps)
        assertEquals(30.0, u2.weight!!, 0.0)
        // 训练者 2 视角完成态正确
        s = vm.state.value.items.single()
        assertEquals(1, s.doneCount)
        assertTrue(s.sets[0].checked)
        assertEquals(30.0, s.sessionWeight!!, 0.0)
    }

    @Test
    fun `打卡时刻 - 打卡写入 重复打卡与调整次数刷新为最新`() = runTest {
        val daos = FakeDaos()
        val itemId = seedSingleItem(daos, targetSets = 2)
        val vm = buildVm(daos)

        // 首次打卡：该组带上打卡时刻（系统时钟窗口内）
        val t0 = System.currentTimeMillis()
        vm.checkIn(itemId, 0, 8)
        val t1 = System.currentTimeMillis()
        val first = daos.checkins.rows.value.single().checkedAt
        assertTrue("打卡后该组应带时刻", first != null && first in t0..t1)

        // 重复打卡（行已存在，走次数覆盖路径）：时刻刷新为最新一次
        Thread.sleep(10)
        vm.checkIn(itemId, 0, 10)
        val second = daos.checkins.rows.value.single().checkedAt
        assertTrue("重复打卡应刷新时刻", second!! > first!!)

        // 调整次数（−/+ 步进）：时刻同样刷新为最新一次
        Thread.sleep(10)
        vm.adjustReps(itemId, 0, +1)
        val row = daos.checkins.rows.value.single()
        assertTrue("调整次数应刷新时刻", row.checkedAt!! > second)
        assertEquals(11, row.reps)
    }
}

package com.fitplan.app.ui.combo

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.SetCheckinEntity
import kotlinx.coroutines.Dispatchers
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
class ComboEditViewModelTest {

    private val date = 100L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 预置：两个动作 + 单组合（条目 id 11=动作1、12=动作2）+ 两天的打卡行。 */
    private fun seedCombo(daos: FakeDaos): Long {
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
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
            SetCheckinEntity(date = date, comboItemId = 11, setIndex = 0, reps = 8),
            SetCheckinEntity(date = date, comboItemId = 12, setIndex = 0, reps = 10),
            SetCheckinEntity(date = date + 1, comboItemId = 12, setIndex = 1, reps = 9),
        )
        return 10L
    }

    @Test
    fun `保存 - 未变动作沿用原 id 打卡保留`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        val vm = ComboEditViewModel(daos, 10L)

        vm.save(
            "胸腿日",
            listOf(
                ComboItemDraft(exerciseId = 1, targetSets = 5),
                ComboItemDraft(exerciseId = 2, targetSets = 4),
            ),
            onDone = {},
        )

        val items = daos.combos.combos.value.single().items.sortedBy { it.sort }
        assertEquals(listOf(11L, 12L), items.map { it.id }) // id 未变
        assertEquals(5, items[0].targetSets) // 目标已更新
        // 两天的打卡行原样保留
        assertEquals(3, daos.checkins.rows.value.size)
        assertEquals(8, daos.checkins.rows.value.first { it.comboItemId == 11L }.reps)
    }

    @Test
    fun `保存 - 重排后 id 跟随动作`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        val vm = ComboEditViewModel(daos, 10L)

        vm.save(
            "胸腿日",
            listOf(ComboItemDraft(exerciseId = 2), ComboItemDraft(exerciseId = 1)),
            onDone = {},
        )

        val items = daos.combos.combos.value.single().items.sortedBy { it.sort }
        assertEquals(listOf(12L, 11L), items.map { it.id }) // 深蹲(12) 排到前面，id 跟随
        assertEquals(2, items[0].exerciseId)
        assertEquals(3, daos.checkins.rows.value.size) // 打卡不受重排影响
    }

    @Test
    fun `保存 - 移除的条目连同全部打卡清理`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        val vm = ComboEditViewModel(daos, 10L)

        vm.save("胸腿日", listOf(ComboItemDraft(exerciseId = 1)), onDone = {})

        val items = daos.combos.combos.value.single().items
        assertEquals(listOf(11L), items.map { it.id }) // 动作2 的条目被删
        // 条目 12 的打卡（含隔天的）一并清理，条目 11 的保留
        assertEquals(1, daos.checkins.rows.value.size)
        assertEquals(11L, daos.checkins.rows.value.single().comboItemId)
    }

    @Test
    fun `保存 - 新增动作分配新 id 并插入到指定位置`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        daos.exercises.exercises.value += ExerciseEntity(id = 3, name = "硬拉")
        val vm = ComboEditViewModel(daos, 10L)

        vm.save(
            "胸腿日",
            listOf(
                ComboItemDraft(exerciseId = 1),
                ComboItemDraft(exerciseId = 3, targetReps = 12),
                ComboItemDraft(exerciseId = 2),
            ),
            onDone = {},
        )

        val items = daos.combos.combos.value.single().items.sortedBy { it.sort }
        assertEquals(3, items.size)
        assertEquals(listOf(1L, 3L, 2L), items.map { it.exerciseId }) // 新动作插在中间
        assertEquals(listOf(11L, 13L, 12L), items.map { it.id })      // 旧条目 id 不变
        assertEquals(12, items[1].targetReps)
        assertEquals(3, daos.checkins.rows.value.size) // 未误删打卡
    }

    @Test
    fun `保存 - 新建组合走新增路径`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        val before = daos.checkins.rows.value.size
        val vm = ComboEditViewModel(daos, 0L)

        vm.save(
            "手臂日",
            listOf(ComboItemDraft(exerciseId = 1, targetSets = 3, targetWeight = 40.0)),
            onDone = {},
        )

        val created = daos.combos.combos.value.first { it.combo.name == "手臂日" }
        assertEquals(11L, created.combo.id) // 新组合 id 跟在种子组合(10)之后自增
        assertEquals(1, created.items.size)
        assertEquals(40.0, created.items.single().targetWeight!!, 0.0)
        assertEquals(before, daos.checkins.rows.value.size) // 不影响既有打卡
    }

    @Test
    fun `保存 - 一次批量插入多个新动作各得不同 id`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        daos.exercises.exercises.value += listOf(
            ExerciseEntity(id = 3, name = "硬拉"),
            ExerciseEntity(id = 4, name = "划船"),
        )
        val vm = ComboEditViewModel(daos, 10L)

        // 多选批量添加：旧条目全删，两条新条目一次插入
        vm.save(
            "胸腿日",
            listOf(ComboItemDraft(exerciseId = 3), ComboItemDraft(exerciseId = 4)),
            onDone = {},
        )

        val items = daos.combos.combos.value.single().items.sortedBy { it.sort }
        assertEquals(2, items.size)
        assertEquals(listOf(3L, 4L), items.map { it.exerciseId })
        // 回归锁：批量插入各得不同 id（此前 Fake 曾给一次插入的 N 条分配同一 id）
        assertEquals(2, items.map { it.id }.toSet().size)
        assertEquals(0, daos.checkins.rows.value.size) // 旧条目移除后打卡随之清理
    }

    @Test
    fun `删除组合 - 天置休息且打卡清理`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = 10))
        val listVm = ComboListViewModel(daos)

        listVm.delete(10L)

        assertFalse(daos.combos.combos.value.any { it.combo.id == 10L })
        assertEquals(
            listOf(DayPlanEntity(date = date, isRest = true, comboId = null)),
            daos.dayPlans.plans.value,
        )
        assertEquals(0, daos.checkins.rows.value.size) // 两个条目的打卡全部清理
    }

    @Test
    fun `updateTargetWeight - 写回组合条目`() = runTest {
        val daos = FakeDaos()
        seedCombo(daos)
        val dao = daos.comboDao

        dao.updateTargetWeight(11L, 52.5)
        assertEquals(52.5, daos.combos.combos.value.single().items.first { it.id == 11L }.targetWeight!!, 0.0)
        // null 清除
        dao.updateTargetWeight(11L, null)
        assertNull(daos.comboDao.getWithItems(10L)!!.items.first { it.id == 11L }.targetWeight)
    }
}

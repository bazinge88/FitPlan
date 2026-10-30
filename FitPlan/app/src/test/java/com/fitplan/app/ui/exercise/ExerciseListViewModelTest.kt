package com.fitplan.app.ui.exercise

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
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

/** T-L4：删除动作前清理其在所有组合条目上的孤儿打卡与逃标。 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 两个动作 A(1)/B(2)，同一组合各一条目 11/12；打卡与逃标按条目铺开（含隔天）。 */
    private fun seed(daos: FakeDaos) {
        daos.exercises.exercises.value = listOf(
            ExerciseEntity(id = 1, name = "卧推"),
            ExerciseEntity(id = 2, name = "深蹲"),
        )
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸腿日"),
                items = listOf(
                    ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                    ComboItemEntity(id = 12, comboId = 10, exerciseId = 2, sort = 1),
                ),
            ),
        )
        daos.checkins.rows.value = listOf(
            SetCheckinEntity(date = 100, comboItemId = 11, setIndex = 0, reps = 8),
            SetCheckinEntity(date = 101, comboItemId = 11, setIndex = 1, reps = 6),
            SetCheckinEntity(date = 100, comboItemId = 12, setIndex = 0, reps = 10),
        )
        daos.skips.rows.value = listOf(
            SetSkipEntity(date = 100, comboItemId = 11, setIndex = 2),
            SetSkipEntity(date = 100, comboItemId = 12, setIndex = 1),
        )
    }

    @Test
    fun `删除动作 - 清理该动作条目的打卡与逃标 其他动作记录保留`() = runTest {
        val daos = FakeDaos()
        seed(daos)
        val vm = ExerciseListViewModel(daos)

        val exerciseA = daos.exercises.exercises.value.first { it.id == 1L }
        vm.delete(exerciseA)
        advanceUntilIdle()

        // 动作 A 消失，动作 B 仍在
        assertEquals(listOf(2L), daos.exercises.exercises.value.map { it.id })
        // 条目 11（动作 A）的打卡与逃标清空；条目 12（动作 B）原样保留
        assertEquals(listOf(12L), daos.checkins.rows.value.map { it.comboItemId })
        assertEquals(listOf(12L), daos.skips.rows.value.map { it.comboItemId })
        assertEquals(1, daos.checkins.rows.value.size)
        assertEquals(1, daos.skips.rows.value.size)
    }

    @Test
    fun `删除动作 - 无组合引用时不误伤其他动作的记录`() = runTest {
        val daos = FakeDaos()
        seed(daos)
        daos.exercises.exercises.value += ExerciseEntity(id = 3, name = "硬拉")
        val vm = ExerciseListViewModel(daos)

        vm.delete(daos.exercises.exercises.value.first { it.id == 3L })
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), daos.exercises.exercises.value.map { it.id })
        // 无引用的动作删除不影响既有打卡/逃标
        assertEquals(3, daos.checkins.rows.value.size)
        assertEquals(2, daos.skips.rows.value.size)
        assertTrue(daos.skips.rows.value.any { it.comboItemId == 11L })
    }
}

package com.fitplan.app.ui.exercise

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.ExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseEditViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save - 动作与默认重量持久化`() = runTest {
        val daos = FakeDaos()
        val vm = ExerciseEditViewModel(daos, 0L)

        vm.save(ExerciseEntity(id = 5, name = "硬拉", defaultWeight = 100.0), onDone = {})

        val saved = daos.exerciseDao.getById(5L)!!
        assertEquals("硬拉", saved.name)
        assertEquals(100.0, saved.defaultWeight!!, 0.0)
    }

    @Test
    fun `save - 仅保存动作 不改写任何组合条目目标重量`() = runTest {
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推"))
        daos.combos.combos.value = listOf(
            ComboWithItems(
                combo = ComboEntity(id = 10, name = "胸日"),
                items = listOf(
                    ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0, targetWeight = 40.0),
                ),
            ),
        )
        val vm = ExerciseEditViewModel(daos, 1L)

        // 保存动作（含默认重量）后，组合条目目标重量保持独立、不被批量改写
        vm.save(ExerciseEntity(id = 1, name = "卧推", defaultWeight = 52.5), onDone = {})

        assertEquals(
            40.0,
            daos.combos.combos.value.single().items.single().targetWeight!!,
            0.0,
        )
        assertEquals(52.5, daos.exerciseDao.getById(1L)!!.defaultWeight!!, 0.0)
    }

    @Test
    fun `save - 已处理重量以库内现值为准 编辑保存不覆盖`() = runTest {
        val daos = FakeDaos()
        daos.exercises.exercises.value = listOf(ExerciseEntity(id = 1, name = "卧推", ackMaxWeight = 55.0))
        val vm = ExerciseEditViewModel(daos, 1L)

        // 编辑页不维护已处理重量（训练流程的状态），保存动作不得把它清掉
        vm.save(ExerciseEntity(id = 1, name = "卧推改", ackMaxWeight = null), onDone = {})

        val saved = daos.exerciseDao.getById(1L)!!
        assertEquals("卧推改", saved.name)
        assertEquals(55.0, saved.ackMaxWeight!!, 0.0)
    }
}

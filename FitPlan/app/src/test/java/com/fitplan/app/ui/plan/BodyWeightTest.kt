package com.fitplan.app.ui.plan

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.seedTrainingDay
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
class BodyWeightTest {

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
    private fun TestScope.buildVm(daos: FakeDaos, date: Long = this@BodyWeightTest.date): WorkoutViewModel {
        val vm = WorkoutViewModel(daos, date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun `recordBodyWeight - 落库到当天当前训练者 状态带出`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)
        assertNull(vm.state.value.bodyWeight)

        vm.recordBodyWeight(72.5)

        val row = daos.bodyWeights.rows.value.single()
        assertEquals(date, row.date)
        assertEquals(72.5, row.weight, 0.0)
        assertEquals(1L, row.userId)
        assertEquals(72.5, vm.state.value.bodyWeight!!, 0.0)
    }

    @Test
    fun `recordBodyWeight - 同天重复输入覆盖旧值 不留多条`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordBodyWeight(72.5)
        vm.recordBodyWeight(73.0)

        // 一天一条：仍只有一行，weight 为最后一次输入
        assertEquals(1, daos.bodyWeights.rows.value.size)
        assertEquals(73.0, daos.bodyWeights.rows.value.single().weight, 0.0)
        assertEquals(73.0, vm.state.value.bodyWeight!!, 0.0)
    }

    @Test
    fun `recordBodyWeight - 多训练者各记各的 互不可见互不覆盖`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordBodyWeight(72.5)

        // 切换训练者：训练者 1 的体重不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        assertNull(vm.state.value.bodyWeight)

        // 训练者 2 记录自己的：各得一行，互不覆盖
        vm.recordBodyWeight(55.0)
        assertEquals(2, daos.bodyWeights.rows.value.size)
        assertEquals(55.0, vm.state.value.bodyWeight!!, 0.0)

        // 切回训练者 1：看到的仍是自己的 72.5
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 1)
        assertEquals(72.5, vm.state.value.bodyWeight!!, 0.0)
    }

    @Test
    fun `休息日 - 状态也带出当天体重与单位 休息日同样可记`() = runTest {
        val daos = FakeDaos()
        // 无任何计划：休息日分支（comboName 为空）
        val vm = buildVm(daos)
        daos.appSettings.settings.value = SettingsEntity(id = 1, unitIsLb = true, activeUserId = 1)

        vm.recordBodyWeight(160.0)

        val s = vm.state.value
        assertNull(s.comboName)
        assertFalse(s.loading)
        assertTrue(s.unitIsLb)
        assertEquals(160.0, s.bodyWeight!!, 0.0)
    }
}

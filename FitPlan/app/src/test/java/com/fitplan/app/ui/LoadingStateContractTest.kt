package com.fitplan.app.ui

import com.fitplan.app.FakeDaos
import com.fitplan.app.ui.combo.ComboListViewModel
import com.fitplan.app.ui.log.LogViewModel
import com.fitplan.app.ui.plan.PlanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** T-L6 加载态契约（前端 F3 依赖）：三个列表 VM 的 StateFlow 初始值必须为 null，UI 以 null 显示「加载中…」。 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LoadingStateContractTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `加载态契约 - 三个列表 StateFlow 初始值均为 null`() {
        val daos = FakeDaos()
        assertNull(PlanViewModel(daos).dayPlans.value)
        assertNull(LogViewModel(daos).entries.value)
        assertNull(ComboListViewModel(daos).combos.value)
    }
}

package com.fitplan.app.ui.settings

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SettingsViewModel.buildMarkdown 的 VM 层接线与导出范围过滤（票 06：导出扩展）。 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsExportTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `buildMarkdown - 体重按导出范围过滤`() = runTest {
        val daos = FakeDaos()
        daos.bodyWeights.rows.value = listOf(
            BodyWeightEntity(date = 100, weight = 70.0),
            BodyWeightEntity(date = 200, weight = 75.5),
        )
        val vm = SettingsViewModel(daos)

        val out = vm.buildMarkdown(ExportScope.Range(fromEpochDay = 100, toEpochDay = 150))

        val expectedLabel = "日期范围 ${TimeUtil.formatDate(TimeUtil.fromEpochDay(100))} 至 " +
            TimeUtil.formatDate(TimeUtil.fromEpochDay(150))
        assertTrue(out.contains(expectedLabel)) // scopeLabel 正常组装
        assertTrue(out.contains("## 体重记录"))
        assertTrue(out.contains("| 70 |")) // 范围内的体重行原值显示
        assertFalse(out.contains("75.5")) // 范围外的体重不出现
    }

    @Test
    fun `buildMarkdown - 无新数据旧库导出不崩 合理缺省`() = runTest {
        val daos = FakeDaos()
        val vm = SettingsViewModel(daos)

        val out = vm.buildMarkdown(ExportScope.All)

        assertFalse(out.contains("## 体重记录")) // 无体重数据整段省略
        assertTrue(out.contains("（无打卡记录）")) // 打卡段保留占位
    }
}

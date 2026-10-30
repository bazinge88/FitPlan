package com.fitplan.app.ui.plan

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.seedTrainingDay
import com.fitplan.app.ui.settings.SettingsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotesTest {

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
    private fun TestScope.buildVm(daos: FakeDaos, date: Long = this@NotesTest.date): WorkoutViewModel {
        val vm = WorkoutViewModel(daos, date)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun `recordDayNote - 落库到当天当前训练者 状态带出`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)
        assertNull(vm.state.value.dayNote)

        vm.recordDayNote("状态差")

        val row = daos.dayNotes.rows.value.single()
        assertEquals(date, row.date)
        assertEquals(1L, row.userId)
        assertEquals("状态差", row.note)
        assertEquals("状态差", vm.state.value.dayNote)
    }

    @Test
    fun `recordDayNote - 同天重复输入覆盖旧值 不留多条`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordDayNote("状态差")
        vm.recordDayNote("改天再练")

        // 一天一条：仍只有一行，note 为最后一次输入
        assertEquals(1, daos.dayNotes.rows.value.size)
        assertEquals("改天再练", daos.dayNotes.rows.value.single().note)
        assertEquals("改天再练", vm.state.value.dayNote)
    }

    @Test
    fun `recordDayNote - 空文本确定清除 删行`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordDayNote("出差")
        vm.recordDayNote("")

        assertEquals(0, daos.dayNotes.rows.value.size)
        assertNull(vm.state.value.dayNote)
    }

    @Test
    fun `recordExerciseNote - 落库与同天同动作覆盖 不留多条`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordExerciseNote(1, "肩膀不舒服")
        vm.recordExerciseNote(1, "重量减半")

        // 同天同动作同训练者一条：仍只有一行，note 为最后一次输入
        val row = daos.exerciseNotes.rows.value.single()
        assertEquals(date, row.date)
        assertEquals(1L, row.exerciseId)
        assertEquals(1L, row.userId)
        assertEquals("重量减半", row.note)
        assertEquals("重量减半", vm.state.value.items.single { it.exerciseId == 1L }.todayNote)
    }

    @Test
    fun `动作备注 - 展开区回显上次备注 取当天之前最近一次`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        daos.exerciseNotes.rows.value = listOf(
            ExerciseNoteEntity(date = date - 3, exerciseId = 1, note = "三天前肩膀酸", userId = 1),
            ExerciseNoteEntity(date = date - 1, exerciseId = 1, note = "昨天重量减半", userId = 1),
        )
        val vm = buildVm(daos)

        val item = vm.state.value.items.single { it.exerciseId == 1L }
        assertEquals("昨天重量减半", item.lastNote)
        assertNull(item.todayNote)
    }

    @Test
    fun `动作备注 - 无历史不显示上次 当天备注不算上次`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        assertNull(vm.state.value.items.single { it.exerciseId == 1L }.lastNote)

        vm.recordExerciseNote(1, "今天慢点做")

        val item = vm.state.value.items.single { it.exerciseId == 1L }
        assertNull(item.lastNote)
        assertEquals("今天慢点做", item.todayNote)
    }

    @Test
    fun `多训练者隔离 - 当日与动作备注互不可见互不覆盖`() = runTest {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val vm = buildVm(daos)

        vm.recordDayNote("状态差")
        vm.recordExerciseNote(1, "肩膀不舒服")
        assertEquals("状态差", vm.state.value.dayNote)
        assertEquals("肩膀不舒服", vm.state.value.items.single { it.exerciseId == 1L }.todayNote)

        // 切换训练者：训练者 1 的备注全部不可见
        daos.appSettings.settings.value = SettingsEntity(id = 1, activeUserId = 2)
        assertNull(vm.state.value.dayNote)
        assertNull(vm.state.value.items.single { it.exerciseId == 1L }.todayNote)
        assertNull(vm.state.value.items.single { it.exerciseId == 1L }.lastNote)

        // 训练者 2 写同动作备注：各得一行，训练者 1 的行原样
        vm.recordExerciseNote(1, "二号轻点做")
        assertEquals(2, daos.exerciseNotes.rows.value.size)
        val user1Row = daos.exerciseNotes.rows.value.single { it.userId == 1L }
        assertEquals("肩膀不舒服", user1Row.note)
        assertEquals("二号轻点做", vm.state.value.items.single { it.exerciseId == 1L }.todayNote)
        // 当日备注仍只有训练者 1 的一条，未被触碰
        assertEquals(1, daos.dayNotes.rows.value.size)
        assertEquals(1L, daos.dayNotes.rows.value.single().userId)
    }

    @Test
    fun `休息日 - 状态也带出当日备注 休息日可写`() = runTest {
        val daos = FakeDaos()
        // 无任何计划：休息日分支（comboName 为空）
        val vm = buildVm(daos)

        vm.recordDayNote("出差")

        val s = vm.state.value
        assertNull(s.comboName)
        assertEquals("出差", s.dayNote)
    }

    @Test
    fun `清理连带 - clearAllData 清空两张备注表`() = runTest {
        val daos = FakeDaos()
        daos.dayNotes.rows.value = listOf(
            DayNoteEntity(date = date, note = "一", userId = 1),
            DayNoteEntity(date = date, note = "二", userId = 2),
        )
        daos.exerciseNotes.rows.value = listOf(
            ExerciseNoteEntity(date = date - 1, exerciseId = 1, note = "一", userId = 1),
            ExerciseNoteEntity(date = date - 1, exerciseId = 1, note = "二", userId = 2),
        )
        val settingsVm = SettingsViewModel(daos)

        settingsVm.clearAllData { }
        advanceUntilIdle()

        assertEquals(0, daos.dayNotes.rows.value.size)
        assertEquals(0, daos.exerciseNotes.rows.value.size)
    }

    @Test
    fun `清理连带 - deleteUser 清该训练者两表备注 保留其他训练者`() = runTest {
        val daos = FakeDaos()
        daos.appUsers.users.value = listOf(
            UserEntity(id = 1, name = "甲"),
            UserEntity(id = 2, name = "乙", sort = 1),
        )
        daos.dayNotes.rows.value = listOf(
            DayNoteEntity(date = date, note = "甲的备注", userId = 1),
            DayNoteEntity(date = date, note = "乙的备注", userId = 2),
        )
        daos.exerciseNotes.rows.value = listOf(
            ExerciseNoteEntity(date = date - 1, exerciseId = 1, note = "甲的动作备注", userId = 1),
            ExerciseNoteEntity(date = date - 1, exerciseId = 1, note = "乙的动作备注", userId = 2),
        )
        val settingsVm = SettingsViewModel(daos)

        settingsVm.deleteUser(UserEntity(id = 1, name = "甲"))
        advanceUntilIdle()

        val dayRow = daos.dayNotes.rows.value.single()
        assertEquals(2L, dayRow.userId)
        assertEquals("乙的备注", dayRow.note)
        val exerciseRow = daos.exerciseNotes.rows.value.single()
        assertEquals(2L, exerciseRow.userId)
        assertEquals("乙的动作备注", exerciseRow.note)
    }
}

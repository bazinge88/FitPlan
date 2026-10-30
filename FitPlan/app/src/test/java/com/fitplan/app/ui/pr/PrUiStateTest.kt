package com.fitplan.app.ui.pr

import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** buildPrUiState 的训练者归属口径：只统计当前训练者的打卡。 */
class PrUiStateTest {

    @Test
    fun `PR - 按训练者过滤 只统计当前用户打卡`() {
        val items = listOf(ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0))
        val exercises = listOf(ExerciseEntity(id = 1, name = "卧推"))
        val checkins = listOf(
            // 训练者 1：PR@8 = 60
            SetCheckinEntity(id = 1, date = 100, comboItemId = 101, setIndex = 0, reps = 8, weight = 60.0, userId = 1),
            // 训练者 2：PR@8 = 70
            SetCheckinEntity(id = 2, date = 101, comboItemId = 101, setIndex = 0, reps = 8, weight = 70.0, userId = 2),
        )

        val forUser1 = buildPrUiState(
            checkins, items, exercises, SettingsEntity(activeUserId = 1),
            listOf(UserEntity(id = 1, name = "我"), UserEntity(id = 2, name = "小王")),
        )
        assertEquals(60.0, forUser1.groups.single().rows.single().weightText.dropLast(2).toDouble(), 0.0)

        val forUser2 = buildPrUiState(
            checkins, items, exercises, SettingsEntity(activeUserId = 2),
            listOf(UserEntity(id = 1, name = "我"), UserEntity(id = 2, name = "小王")),
        )
        assertEquals(70.0, forUser2.groups.single().rows.single().weightText.dropLast(2).toDouble(), 0.0)
    }

    @Test
    fun `PR - 同动作不同训练者档位互不合并`() {
        val items = listOf(ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0))
        val exercises = listOf(ExerciseEntity(id = 1, name = "卧推"))
        // 训练者 1 只有 8 次档；训练者 2 只有 10 次档
        val checkins = listOf(
            SetCheckinEntity(id = 1, date = 100, comboItemId = 101, setIndex = 0, reps = 8, weight = 60.0, userId = 1),
            SetCheckinEntity(id = 2, date = 101, comboItemId = 101, setIndex = 0, reps = 10, weight = 50.0, userId = 2),
        )
        val forUser2 = buildPrUiState(
            checkins, items, exercises, SettingsEntity(activeUserId = 2),
            listOf(UserEntity(id = 2, name = "小王")),
        )
        val group = forUser2.groups.single()
        assertEquals(listOf(10), group.rows.map { it.reps })
        assertEquals("小王", forUser2.userName)
    }

    @Test
    fun `PR - 用户表无当前用户时用户名为空`() {
        val items = listOf(ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0))
        val exercises = listOf(ExerciseEntity(id = 1, name = "卧推"))
        val state = buildPrUiState(emptyList(), items, exercises, SettingsEntity(activeUserId = 9), emptyList())
        assertNull(state.userName)
        assertEquals(0, state.groups.size)
    }
}

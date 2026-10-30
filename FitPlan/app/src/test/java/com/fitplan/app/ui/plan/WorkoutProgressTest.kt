package com.fitplan.app.ui.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 主页今日进度纯函数：0 组/部分完成/全部完成三态（逃组不计入完成数）。 */
class WorkoutProgressTest {

    /** 造一个动作卡：total 个组行，其中前 done 组已打卡。 */
    private fun item(done: Int, total: Int, comboItemId: Long = 1L) = WorkoutItemUi(
        comboItemId = comboItemId,
        exerciseId = comboItemId + 100,
        exerciseName = "卧推",
        targetSets = total,
        targetReps = 8,
        targetWeight = null,
        targetDuration = null,
        sets = (0 until total).map { index ->
            WorkoutSetUi(setIndex = index, checked = index < done, reps = if (index < done) 8 else null)
        },
        doneCount = done,
    )

    @Test
    fun `空清单 - 0 组且不算完成`() {
        val progress = workoutProgress(emptyList())
        assertEquals(0, progress.doneSets)
        assertEquals(0, progress.totalSets)
        assertFalse(progress.allDone)
        assertEquals("已完成 0/0 组", progress.summaryText)
    }

    @Test
    fun `部分完成 - 按动作卡组行合计`() {
        val progress = workoutProgress(
            listOf(item(done = 1, total = 3, comboItemId = 1L), item(done = 0, total = 2, comboItemId = 2L)),
        )
        assertEquals(1, progress.doneSets)
        assertEquals(5, progress.totalSets)
        assertFalse(progress.allDone)
        assertEquals("已完成 1/5 组", progress.summaryText)
    }

    @Test
    fun `全部完成 - 今日完成文案`() {
        val progress = workoutProgress(
            listOf(item(done = 3, total = 3, comboItemId = 1L), item(done = 2, total = 2, comboItemId = 2L)),
        )
        assertEquals(5, progress.doneSets)
        assertEquals(5, progress.totalSets)
        assertTrue(progress.allDone)
        assertEquals("今日完成 · 5/5 组", progress.summaryText)
    }
}

package com.fitplan.app.ui.plan

import com.fitplan.app.FakeDaos
import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SessionWeightEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.seedTrainingDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 直击纯函数 buildWorkoutUiState 的既有行为：不起 VM、不起协程，Fake DAO 播种后取 .value 直调。 */
class WorkoutUiStateTest {

    /** 默认参 helper：收敛各测试 WorkoutFlowBundle 六字段具名构造的重复。 */
    private fun workoutFlowBundleOf(
        settings: SettingsEntity? = null,
        sessionWeights: List<SessionWeightEntity> = emptyList(),
        skips: List<SetSkipEntity> = emptyList(),
        bodyWeights: List<BodyWeightEntity> = emptyList(),
        dayNotes: List<DayNoteEntity> = emptyList(),
        exerciseNotes: List<ExerciseNoteEntity> = emptyList(),
    ) = WorkoutFlowBundle(
        settings = settings,
        sessionWeights = sessionWeights,
        skips = skips,
        bodyWeights = bodyWeights,
        dayNotes = dayNotes,
        exerciseNotes = exerciseNotes,
    )

    /** 休息日/无计划早退分支：清单为空，但体重、当日备注与单位照常带出。 */
    @Test
    fun restDayBranchCarriesBodyWeightDayNoteAndUnit() {
        val daos = FakeDaos()
        val date = 200L
        daos.bodyWeights.rows.value = listOf(
            BodyWeightEntity(date = date, weight = 72.5, userId = 1),
        )
        daos.dayNotes.rows.value = listOf(
            DayNoteEntity(date = date, note = "腿日顺延", userId = 1),
        )
        daos.appSettings.settings.value = SettingsEntity(id = 1, unitIsLb = true, activeUserId = 1)

        val state = buildWorkoutUiState(
            date = date,
            plans = daos.dayPlans.plans.value,
            combos = daos.combos.combos.value,
            exercises = daos.exercises.exercises.value,
            checkins = daos.checkins.rows.value,
            flow = workoutFlowBundleOf(
                settings = daos.appSettings.settings.value,
                bodyWeights = daos.bodyWeights.rows.value,
                dayNotes = daos.dayNotes.rows.value,
            ),
        )

        assertNull(state.comboName)
        assertEquals(0, state.items.size)
        assertFalse(state.loading)
        assertEquals(72.5, state.bodyWeight!!, 0.0)
        assertEquals("腿日顺延", state.dayNote)
        assertTrue(state.unitIsLb)
    }

    /** 正常训练日：组合名、动作条目与组行按目标组数逻辑构建，未打卡时各行为未完成。 */
    @Test
    fun trainingDayBuildsComboItemsAndSets() {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val date = 100L
        val plans = daos.dayPlans.plans.value
        val combos = daos.combos.combos.value
        val exercises = daos.exercises.exercises.value
        val settings = SettingsEntity(id = 1, unitIsLb = false, activeUserId = 1)

        val state = buildWorkoutUiState(
            date = date,
            plans = plans,
            combos = combos,
            exercises = exercises,
            checkins = emptyList(),
            flow = workoutFlowBundleOf(
                settings = settings,
            ),
        )

        assertEquals("胸腿日", state.comboName)
        assertEquals(1, state.items.size)
        val item = state.items.single()
        // 播种条目未设 targetSets（null）：行数按「无目标时至少一行」为 1
        assertNull(item.targetSets)
        assertEquals(1, item.sets.size)
        assertTrue(item.sets.all { !it.checked })
    }

    /** 逃组：逃标行存在即逃，该组不算完成（checked 为 false、skipped 为 true）。 */
    @Test
    fun skippedSetIsNotChecked() {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val date = 100L
        val comboItemId = 101L
        daos.skips.rows.value = listOf(
            SetSkipEntity(date = date, comboItemId = comboItemId, setIndex = 0, userId = 1),
        )
        val plans = daos.dayPlans.plans.value
        val combos = daos.combos.combos.value
        val exercises = daos.exercises.exercises.value
        val settings = SettingsEntity(id = 1, unitIsLb = false, activeUserId = 1)

        val state = buildWorkoutUiState(
            date = date,
            plans = plans,
            combos = combos,
            exercises = exercises,
            checkins = emptyList(),
            flow = workoutFlowBundleOf(
                settings = settings,
                skips = daos.skips.rows.value,
            ),
        )

        val firstSet = state.items.single().sets.first()
        assertEquals(0, firstSet.setIndex)
        assertFalse(firstSet.checked)
        assertTrue(firstSet.skipped)
        // 动作级聚合完成数只统计打卡，逃组不计入
        assertEquals(0, state.items.single().doneCount)
    }

    /** T-L1 空组合：组合存在但无条目时 comboName 非空、items 为空、loading=false（UI 据此显示空态引导）。 */
    @Test
    fun emptyComboHasNameButNoItems() {
        val daos = FakeDaos()
        seedTrainingDay(daos)
        val date = 100L
        daos.combos.combos.value = listOf(
            ComboWithItems(combo = ComboEntity(id = 10, name = "空组合"), items = emptyList()),
        )

        val state = buildWorkoutUiState(
            date = date,
            plans = daos.dayPlans.plans.value,
            combos = daos.combos.combos.value,
            exercises = daos.exercises.exercises.value,
            checkins = emptyList(),
            flow = workoutFlowBundleOf(settings = SettingsEntity(activeUserId = 1)),
        )

        assertEquals("空组合", state.comboName)
        assertTrue(state.items.isEmpty())
        assertFalse(state.loading)
    }

    /** T-L1 完成判定纯函数：空列表不算完成（空组合不得被判完成）、部分完成 false、全部完成 true。 */
    @Test
    fun isWorkoutAllDoneRequiresNonEmptyAndAllSetsChecked() {
        fun item(doneCount: Int, total: Int) = WorkoutItemUi(
            comboItemId = 1,
            exerciseId = 1,
            exerciseName = "卧推",
            targetSets = total,
            targetReps = null,
            targetWeight = null,
            targetDuration = null,
            sets = (0 until total).map { i -> WorkoutSetUi(setIndex = i, checked = i < doneCount, reps = 8) },
            doneCount = doneCount,
        )

        assertFalse(isWorkoutAllDone(emptyList()))
        assertFalse(isWorkoutAllDone(listOf(item(doneCount = 1, total = 2))))
        assertTrue(isWorkoutAllDone(listOf(item(doneCount = 2, total = 2))))
        assertFalse(isWorkoutAllDone(listOf(item(doneCount = 2, total = 2), item(doneCount = 0, total = 1))))
    }
}

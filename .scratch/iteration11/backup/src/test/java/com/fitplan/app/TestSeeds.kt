package com.fitplan.app

import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity

/** 预置：动作 + 单组合（单动作条目）+ 当日非休息计划；BodyWeight/Notes/Plan 三个测试共用的唯一 seed。 */
internal fun seedTrainingDay(
    daos: FakeDaos,
    date: Long = 100L,
    comboId: Long = 10L,
    comboItemId: Long = 101L,
    exerciseId: Long = 1L,
) {
    daos.exercises.exercises.value = listOf(ExerciseEntity(id = exerciseId, name = "卧推"))
    daos.combos.combos.value = listOf(
        ComboWithItems(
            combo = ComboEntity(id = comboId, name = "胸腿日"),
            items = listOf(
                ComboItemEntity(id = comboItemId, comboId = comboId, exerciseId = exerciseId, sort = 0),
            ),
        ),
    )
    daos.dayPlans.plans.value = listOf(DayPlanEntity(date = date, isRest = false, comboId = comboId))
}

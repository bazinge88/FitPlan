package com.fitplan.app.ui.plan

/**
 * 今日训练进度汇总（主页进度行）。
 * doneSets/totalSets 按动作卡的目标组行合计；逃组不算完成（doneCount 只统计打卡）。
 */
data class WorkoutProgress(val doneSets: Int, val totalSets: Int) {
    /** 全部组完成才算今日完成；空清单（无动作）不算完成。 */
    val allDone: Boolean = totalSets > 0 && doneSets >= totalSets

    /** 进度文案：未完成「已完成 3/12 组」；全部完成「今日完成 · 12/12 组」。 */
    val summaryText: String =
        if (allDone) "今日完成 · $doneSets/$totalSets 组" else "已完成 $doneSets/$totalSets 组"
}

/** 纯函数：由清单动作聚合今日进度（主页概览用，不改动任何训练数据）。 */
fun workoutProgress(items: List<WorkoutItemUi>): WorkoutProgress = WorkoutProgress(
    doneSets = items.sumOf { it.doneCount },
    totalSets = items.sumOf { it.sets.size },
)

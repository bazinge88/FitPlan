package com.fitplan.app.export

import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.LogEntryWithSets
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.util.TimeUtil
import com.fitplan.app.util.buildTrainingDurations
import com.fitplan.app.util.durationWords
import com.fitplan.app.util.durationsForUser
import java.time.LocalDate

data class ExportRequest(
    val scopeLabel: String,
    val rangeFrom: Long?,   // epoch day，null 表示全部
    val rangeTo: Long?,
)

data class ExportData(
    val settings: SettingsEntity,
    val exercises: List<ExerciseEntity>,
    val combos: List<ComboWithItems>,
    val dayPlans: List<DayPlanEntity>,
    val logEntries: List<LogEntryWithSets>,
    val checkins: List<SetCheckinEntity> = emptyList(),
    /** comboId -> 组合名称，用于周计划展示。 */
    val comboNames: Map<Long, String> = emptyMap(),
    /** exerciseId -> 动作名称，用于组合与记录展示。 */
    val exerciseNames: Map<Long, String> = emptyMap(),
    /** userId -> 训练者名称；多于一名训练者时打卡表追加「用户」列区分归属。 */
    val userNames: Map<Long, String> = emptyMap(),
    val bodyWeights: List<BodyWeightEntity> = emptyList(),
    val dayNotes: List<DayNoteEntity> = emptyList(),
    val exerciseNotes: List<ExerciseNoteEntity> = emptyList(),
    /** 当天逃组（未被打卡覆盖的行渲染为「逃」）。 */
    val skips: List<SetSkipEntity> = emptyList(),
)

/**
 * 结构化 Markdown 导出：设置 / 动作库 / 训练组合 / 周计划 / 训练打卡 / 训练记录。
 * 重量保留原始符号（负数表示辅助重量），格式稳定，便于 AI 分析。
 */
object MarkdownExporter {

    fun export(data: ExportData, request: ExportRequest): String = buildString {
        appendLine("# 健身计划导出")
        appendLine()
        appendLine("- 导出时间：${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
        appendLine("- 导出范围：${request.scopeLabel}")
        appendLine("- 重量说明：正数=负重，0=自重，负数=辅助重量（如辅助引体向上 -20）。")
        appendLine()
        appendSettings(data.settings)
        appendExercises(data.exercises)
        appendCombos(data.combos, data.exerciseNames)
        val durations = durationsForUser(buildTrainingDurations(data.checkins), data.settings.activeUserId)
        appendDayPlans(data.dayPlans, data.comboNames, durations)
        appendCheckins(
            checkins = data.checkins,
            skips = data.skips,
            dayNotes = data.dayNotes,
            exerciseNotes = data.exerciseNotes,
            combos = data.combos,
            exerciseNames = data.exerciseNames,
            userNames = data.userNames,
            durations = durations,
            activeUserId = data.settings.activeUserId,
        )
        appendBodyWeights(data.bodyWeights, data.userNames, data.settings.unitIsLb)
        appendLogs(data.logEntries, data.exercises)
    }

    private fun StringBuilder.appendSettings(s: SettingsEntity) {
        appendLine("## 设置")
        appendLine()
        appendLine("- 重量单位：${if (s.unitIsLb) "lb" else "kg"}")
        appendLine("- 周起始：周一")
        appendLine(
            "- 默认记录字段：" + listOfNotNull(
                "组数".takeIf { s.defTrackSets },
                "次数".takeIf { s.defTrackReps },
                "重量".takeIf { s.defTrackWeight },
                "时长".takeIf { s.defTrackDuration },
            ).joinToString("、").ifEmpty { "无" }
        )
        appendLine()
    }

    private fun StringBuilder.appendExercises(exercises: List<ExerciseEntity>) {
        appendLine("## 动作库")
        appendLine()
        if (exercises.isEmpty()) {
            appendLine("（空）")
            appendLine()
            return
        }
        appendLine("| 动作 | 分类 | 默认重量 | 记录字段 |")
        appendLine("| --- | --- | --- | --- |")
        for (e in exercises) {
            val fields = listOfNotNull(
                "组数".takeIf { e.trackSets },
                "次数".takeIf { e.trackReps },
                "重量".takeIf { e.trackWeight },
                "时长".takeIf { e.trackDuration },
            ).joinToString("、").ifEmpty { "无" }
            appendLine(
                "| ${e.name} | ${e.category.ifEmpty { "未分类" }} | " +
                    (e.defaultWeight?.let { TimeUtil.formatWeight(it) } ?: "-") + " | $fields |",
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendCombos(combos: List<ComboWithItems>, exerciseNames: Map<Long, String>) {
        appendLine("## 训练组合")
        appendLine()
        if (combos.isEmpty()) {
            appendLine("（空）")
            appendLine()
            return
        }
        for (c in combos) {
            appendLine("### ${c.combo.name}")
            appendLine()
            if (c.items.isEmpty()) {
                appendLine("（未包含动作）")
                appendLine()
                continue
            }
            appendLine("| 序号 | 动作 | 目标组数 | 目标次数 | 目标重量 | 目标时长 |")
            appendLine("| --- | --- | --- | --- | --- | --- |")
            c.items.sortedBy { it.sort }.forEachIndexed { i, item ->
                appendLine(
                    "| ${i + 1} | ${exerciseNames[item.exerciseId] ?: "?"} | " +
                        "${item.targetSets ?: "-"} | ${item.targetReps ?: "-"} | " +
                        (item.targetWeight?.let { TimeUtil.formatWeight(it) } ?: "-") + " | " +
                        (item.targetDuration?.let { "$it 分钟" } ?: "-") + " |"
                )
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendDayPlans(
        plans: List<DayPlanEntity>,
        comboNames: Map<Long, String>,
        durations: Map<Long, Long>,
    ) {
        appendLine("## 周计划")
        appendLine()
        if (plans.isEmpty()) {
            appendLine("（范围内未安排）")
            appendLine()
            return
        }
        appendLine("| 日期 | 星期 | 类型 | 训练组合 | 用时 |")
        appendLine("| --- | --- | --- | --- | --- |")
        for (p in plans.sortedBy { it.date }) {
            val d = TimeUtil.fromEpochDay(p.date)
            val type = if (p.isRest) "休息日" else "训练日"
            val combo = p.comboId?.let { comboNames[it] } ?: "-"
            val duration = durations[p.date]?.let { durationWords(it) } ?: "-"
            appendLine("| ${TimeUtil.formatDate(d)} | ${TimeUtil.weekdayName(d)} | $type | $combo | $duration |")
        }
        appendLine()
    }

    /** 打卡段单日的一行：打卡行或未被覆盖的逃组行。 */
    private data class DayRow(val checkin: SetCheckinEntity?, val skip: SetSkipEntity?) {
        val comboItemId: Long get() = checkin?.comboItemId ?: skip!!.comboItemId
        val setIndex: Int get() = checkin?.setIndex ?: skip!!.setIndex
        val userId: Long get() = checkin?.userId ?: skip!!.userId
    }

    private fun StringBuilder.appendCheckins(
        checkins: List<SetCheckinEntity>,
        skips: List<SetSkipEntity>,
        dayNotes: List<DayNoteEntity>,
        exerciseNotes: List<ExerciseNoteEntity>,
        combos: List<ComboWithItems>,
        exerciseNames: Map<Long, String>,
        userNames: Map<Long, String>,
        durations: Map<Long, Long>,
        activeUserId: Long,
    ) {
        appendLine("## 训练打卡")
        appendLine()
        val days = (checkins.map { it.date } + dayNotes.map { it.date } + skips.map { it.date })
            .distinct()
            .sorted()
        if (days.isEmpty()) {
            appendLine("（无打卡记录）")
            appendLine()
            return
        }
        // 单训练者保持旧表结构；多训练者时追加「用户」列标注归属（日期/星期由天标题承载）
        val multiUser = userNames.size > 1
        for (day in days) {
            val d = TimeUtil.fromEpochDay(day)
            appendLine("### ${TimeUtil.formatDate(d)}（${TimeUtil.weekdayName(d)}）")
            appendLine()
            // 当日备注（L06）：多训练者逐条输出并带用户名（按 userId 升序），单训练者保持旧格式
            val dayNotesOfDay = dayNotes.filter { it.date == day }.sortedBy { it.userId }
            if (multiUser) {
                dayNotesOfDay.forEach { note ->
                    appendLine("当日备注（${userNames[note.userId] ?: "?"}）：${note.note}")
                }
            } else {
                dayNotesOfDay.find { it.userId == activeUserId }?.let {
                    appendLine("当日备注：${it.note}")
                }
            }
            appendLine("用时：${durations[day]?.let(::durationWords) ?: "-"}")
            appendLine()
            val dayCheckins = checkins.filter { it.date == day }
            // 逃组行：当天未被打卡覆盖的逃标（打卡覆盖逃标 = 同 (date, comboItemId, setIndex, userId) 已有打卡行）
            val daySkips = skips.filter { skip ->
                skip.date == day && dayCheckins.none { c ->
                    c.comboItemId == skip.comboItemId && c.setIndex == skip.setIndex && c.userId == skip.userId
                }
            }
            if (dayCheckins.isEmpty() && daySkips.isEmpty()) {
                // 纯备注天：没有表，表位写占位
                appendLine("（无打卡记录）")
                appendLine()
                continue
            }
            if (multiUser) {
                appendLine("| 用户 | 组合 | 动作 | 组 | 完成次数 |")
                appendLine("| --- | --- | --- | --- | --- |")
            } else {
                appendLine("| 组合 | 动作 | 组 | 完成次数 |")
                appendLine("| --- | --- | --- | --- |")
            }
            val rows = (dayCheckins.map { DayRow(checkin = it, skip = null) } +
                daySkips.map { DayRow(checkin = null, skip = it) }).sortedWith(
                compareBy(
                    { userNames[it.userId] ?: "" },
                    { itemRefs(it.comboItemId, combos, exerciseNames)?.sort ?: Int.MAX_VALUE },
                    { it.setIndex },
                ),
            )
            for (row in rows) {
                val ref = itemRefs(row.comboItemId, combos, exerciseNames)
                // 动作格：追加当天该训练者对该动作的动作备注
                val exerciseCell = ref?.let { r ->
                    exerciseNotes.find {
                        it.date == day && it.exerciseId == r.exerciseId && it.userId == row.userId
                    }?.let { note -> "${r.exerciseName}（备注：${note.note}）" } ?: r.exerciseName
                } ?: "?"
                // 完成次数列：打卡行记了重量显示「次数×重量」（重量沿用 TimeUtil.formatWeight，
                // 与动作库/组合/记录段同一格式，负数与小数保留原符号）；未记重量只写次数；逃组行固定「逃」。
                val repsCell = row.checkin?.let { c ->
                    c.reps?.let { reps ->
                        c.weight?.let { w -> "${reps}×${TimeUtil.formatWeight(w)}" } ?: reps.toString()
                    } ?: "只记完成"
                } ?: "逃"
                if (multiUser) {
                    appendLine(
                        "| ${userNames[row.userId] ?: "?"} | ${ref?.comboName ?: "?"} | $exerciseCell | " +
                            "${row.setIndex + 1} | $repsCell |"
                    )
                } else {
                    appendLine(
                        "| ${ref?.comboName ?: "?"} | $exerciseCell | ${row.setIndex + 1} | $repsCell |"
                    )
                }
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendBodyWeights(
        bodyWeights: List<BodyWeightEntity>,
        userNames: Map<Long, String>,
        unitIsLb: Boolean,
    ) {
        if (bodyWeights.isEmpty()) return
        appendLine("## 体重记录")
        appendLine()
        // 单训练者保持旧表结构；多训练者时追加「用户」列标注归属
        val multiUser = userNames.size > 1
        val unit = if (unitIsLb) "lb" else "kg"
        if (multiUser) {
            appendLine("| 日期 | 星期 | 用户 | 体重（$unit） |")
            appendLine("| --- | --- | --- | --- |")
        } else {
            appendLine("| 日期 | 星期 | 体重（$unit） |")
            appendLine("| --- | --- | --- |")
        }
        for (w in bodyWeights.sortedWith(compareBy({ it.date }, { userNames[it.userId] ?: "" }))) {
            val d = TimeUtil.fromEpochDay(w.date)
            if (multiUser) {
                appendLine(
                    "| ${TimeUtil.formatDate(d)} | ${TimeUtil.weekdayName(d)} | " +
                        "${userNames[w.userId] ?: "?"} | ${TimeUtil.formatWeight(w.weight)} |"
                )
            } else {
                appendLine(
                    "| ${TimeUtil.formatDate(d)} | ${TimeUtil.weekdayName(d)} | ${TimeUtil.formatWeight(w.weight)} |"
                )
            }
        }
        appendLine()
    }

    private fun itemRefs(
        comboItemId: Long,
        combos: List<ComboWithItems>,
        exerciseNames: Map<Long, String>,
    ): ItemRef? {
        combos.forEach { c ->
            val item = c.items.find { it.id == comboItemId }
            if (item != null) {
                return ItemRef(c.combo.name, exerciseNames[item.exerciseId] ?: "?", item.sort, item.exerciseId)
            }
        }
        return null
    }

    /** 打卡归属引用：组合名 + 动作名 + 条目序 + 动作 id（动作备注匹配用）。 */
    private data class ItemRef(
        val comboName: String,
        val exerciseName: String,
        val sort: Int,
        val exerciseId: Long,
    )

    private fun StringBuilder.appendLogs(
        entries: List<LogEntryWithSets>,
        exercises: List<ExerciseEntity>,
    ) {
        appendLine("## 训练记录")
        appendLine()
        if (entries.isEmpty()) {
            appendLine("（范围内无记录）")
            appendLine()
            return
        }
        val exerciseById = exercises.associateBy { it.id }
        for (day in entries.groupBy { it.entry.date }.toSortedMap()) {
            val d = TimeUtil.fromEpochDay(day.key)
            appendLine("### ${TimeUtil.formatDate(d)}（${TimeUtil.weekdayName(d)}）")
            appendLine()
            for (e in day.value.sortedBy { it.entry.sort }) {
                val ex = exerciseById[e.entry.exerciseId]
                val name = ex?.name ?: "?"
                appendLine("- **$name**")
                if (e.sets.isNotEmpty()) {
                    appendLine("  | 组 | 组数 | 次数 | 重量 | 时长 |")
                    appendLine("  | --- | --- | --- | --- | --- |")
                    e.sets.sortedBy { it.sort }.forEachIndexed { i, s ->
                        appendLine(
                            "  | ${i + 1} | ${s.sets ?: "-"} | ${s.reps ?: "-"} | " +
                                (s.weight?.let { TimeUtil.formatWeight(it) } ?: "-") + " | " +
                                (s.duration?.let { "$it 分钟" } ?: "-") + " |"
                        )
                    }
                }
                if (e.entry.note.isNotBlank()) {
                    appendLine("  - 备注：${e.entry.note}")
                }
            }
            appendLine()
        }
    }
}

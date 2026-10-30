package com.fitplan.app.export

import com.fitplan.app.data.db.BodyWeightEntity
import com.fitplan.app.data.db.ComboEntity
import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.ComboWithItems
import com.fitplan.app.data.db.DayNoteEntity
import com.fitplan.app.data.db.DayPlanEntity
import com.fitplan.app.data.db.ExerciseEntity
import com.fitplan.app.data.db.ExerciseNoteEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.util.TimeUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExporterTest {

    private fun data(
        checkins: List<SetCheckinEntity> = emptyList(),
        combos: List<ComboWithItems> = emptyList(),
        exercises: List<ExerciseEntity> = emptyList(),
        dayPlans: List<DayPlanEntity> = emptyList(),
        dayNotes: List<DayNoteEntity> = emptyList(),
        exerciseNotes: List<ExerciseNoteEntity> = emptyList(),
        bodyWeights: List<BodyWeightEntity> = emptyList(),
        skips: List<SetSkipEntity> = emptyList(),
    ) = ExportData(
        settings = SettingsEntity(),
        exercises = exercises,
        combos = combos,
        dayPlans = dayPlans,
        logEntries = emptyList(),
        checkins = checkins,
        comboNames = combos.associate { it.combo.id to it.combo.name },
        exerciseNames = exercises.associate { it.id to it.name },
        dayNotes = dayNotes,
        exerciseNotes = exerciseNotes,
        bodyWeights = bodyWeights,
        skips = skips,
    )

    private val request = ExportRequest(scopeLabel = "全部", rangeFrom = null, rangeTo = null)

    /** 日期列的期望文本直接由 TimeUtil 生成，测试只关注表格结构而非日期换算。 */
    private fun dateCells(epochDay: Long): String {
        val d = TimeUtil.fromEpochDay(epochDay)
        return "${TimeUtil.formatDate(d)} | ${TimeUtil.weekdayName(d)}"
    }

    /** 打卡段天标题的期望文本（与训练记录段同款）。 */
    private fun dateHeading(epochDay: Long): String {
        val d = TimeUtil.fromEpochDay(epochDay)
        return "### ${TimeUtil.formatDate(d)}（${TimeUtil.weekdayName(d)}）"
    }

    @Test
    fun `导出 - 打卡表格 含日期组合动作组次与只记完成`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "辅助引体向上")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = null),
                ),
            ),
            request,
        )
        assertTrue(out.contains("## 训练打卡"))
        assertTrue(out.contains(dateHeading(20657)))
        assertTrue(out.contains("用时：-")) // 无时刻的历史打卡不参与用时推导
        assertTrue(out.contains("| 组合 | 动作 | 组 | 完成次数 |"))
        assertFalse(out.contains("| 日期 | 星期 | 组合 | 动作 | 组 | 完成次数 |")) // 日期列下沉到天标题
        assertTrue(out.contains("| 拉背 | 辅助引体向上 | 1 | 8 |"))
        assertTrue(out.contains("| 拉背 | 辅助引体向上 | 2 | 只记完成 |"))
    }

    @Test
    fun `导出 - 打卡表格 有重量的组显示次数乘重量`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "卧推")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "胸"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 12, weight = 52.5),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = 12, weight = 52.5),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 2, reps = 10, weight = null),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 3, reps = 8, weight = 60.0),
                ),
            ),
            request,
        )
        assertTrue(out.contains(dateHeading(20657)))
        assertTrue(out.contains("用时：-"))
        assertTrue(out.contains("| 胸 | 卧推 | 1 | 12×52.5 |"))
        assertTrue(out.contains("| 胸 | 卧推 | 2 | 12×52.5 |"))
        assertTrue(out.contains("| 胸 | 卧推 | 3 | 10 |")) // 无重量的组只写次数
        assertTrue(out.contains("| 胸 | 卧推 | 4 | 8×60 |")) // 整数重量不带小数尾巴
        assertFalse(out.contains("52.5×")) // 重量只出现在次数之后
    }

    @Test
    fun `导出 - 打卡表格 无重量组只显示次数`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "辅助引体向上")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 12),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = 12),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 2, reps = 10),
                ),
            ),
            request,
        )
        assertTrue(out.contains(dateHeading(20657)))
        assertTrue(out.contains("用时：-"))
        assertTrue(out.contains("| 拉背 | 辅助引体向上 | 1 | 12 |"))
        assertTrue(out.contains("| 拉背 | 辅助引体向上 | 2 | 12 |"))
        assertTrue(out.contains("| 拉背 | 辅助引体向上 | 3 | 10 |"))
        assertFalse(out.contains("×")) // 全无重量时打卡段与旧格式完全一致
    }

    @Test
    fun `导出 - 无打卡时显示占位`() {
        val out = MarkdownExporter.export(data(), request)
        assertTrue(out.contains("## 训练打卡"))
        assertTrue(out.contains("（无打卡记录）"))
        assertFalse(out.contains("### ")) // 整体为空时不产出任何天标题
    }

    @Test
    fun `导出 - 负数与小数重量保留原始符号`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "辅助引体向上")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0, targetWeight = -20.0),
                            ComboItemEntity(id = 12, comboId = 10, exerciseId = 1, sort = 1, targetWeight = 12.5),
                            ComboItemEntity(id = 13, comboId = 10, exerciseId = 1, sort = 2, targetWeight = 0.0),
                        ),
                    ),
                ),
            ),
            request,
        )
        // 字节级关键点：负号原样保留、小数不丢、0 显示为自重
        assertTrue(out.contains("-20"))
        assertTrue(out.contains("12.5"))
        assertTrue(out.contains("| 0 |"))
        assertFalse(out.contains("-20.0")) // 整数重量不带小数尾巴
    }

    @Test
    fun `导出 - 动作库含默认重量列 负数保留符号`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(
                    ExerciseEntity(id = 1, name = "卧推", defaultWeight = 52.5),
                    ExerciseEntity(id = 2, name = "辅助引体向上", defaultWeight = -20.0),
                    ExerciseEntity(id = 3, name = "平板支撑", defaultWeight = null),
                ),
            ),
            request,
        )
        assertTrue(out.contains("| 卧推 | 未分类 | 52.5 |"))
        assertTrue(out.contains("| 辅助引体向上 | 未分类 | -20 |"))
        assertTrue(out.contains("| 平板支撑 | 未分类 | - |"))
    }

    @Test
    fun `导出 - 打卡按条目顺序排列 孤儿行排最后且显示问号`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "卧推"), ExerciseEntity(id = 2, name = "深蹲")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "胸腿日"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 1),
                            ComboItemEntity(id = 12, comboId = 10, exerciseId = 2, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8),
                    SetCheckinEntity(date = 20657, comboItemId = 12, setIndex = 0, reps = 10),
                    SetCheckinEntity(date = 20657, comboItemId = 99, setIndex = 0, reps = 5), // 孤儿
                ),
            ),
            request,
        )
        assertTrue(out.contains(dateHeading(20657)))
        val idxDeepSquat = out.indexOf("| 胸腿日 | 深蹲 |")
        val idxBench = out.indexOf("| 胸腿日 | 卧推 |")
        val idxOrphan = out.indexOf("| ? | ? |")
        assertTrue(idxDeepSquat in 0 until idxBench) // sort=0 的深蹲在前
        assertTrue(idxBench in 0 until idxOrphan)    // 孤儿行排最后
    }

    @Test
    fun `导出 - 周计划 天级逃标旧列被忽略 按训练日显示 用时为-`() {
        val out = MarkdownExporter.export(
            data(
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = emptyList(),
                    ),
                ),
                dayPlans = listOf(
                    DayPlanEntity(date = 20657, isRest = false, comboId = 10, skipped = true),
                ),
            ),
            request,
        )
        val cells = dateCells(20657)
        assertTrue(out.contains("## 周计划"))
        assertTrue(out.contains("| $cells | 训练日 | 拉背 | - |")) // skipped 分支删除，类型只写训练日
        assertFalse(out.contains("逃课"))
    }

    @Test
    fun `导出 - 周计划 普通训练日与休息日不受影响`() {
        val out = MarkdownExporter.export(
            data(
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = emptyList(),
                    ),
                ),
                dayPlans = listOf(
                    DayPlanEntity(date = 20657, isRest = false, comboId = 10, skipped = false),
                    DayPlanEntity(date = 20658, isRest = true, comboId = null, skipped = false),
                ),
            ),
            request,
        )
        val cells = dateCells(20657)
        val restCells = dateCells(20658)
        assertTrue(out.contains("| $cells | 训练日 | 拉背 |"))
        assertTrue(out.contains("| $restCells | 休息日 | - |"))
        assertFalse(out.contains("逃课"))
    }

    @Test
    fun `导出 - 打卡段按天分组 当日备注与用时并入当天段落`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "卧推")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "胸腿日"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8, checkedAt = 0L),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = 8, checkedAt = 1_800_000L),
                ),
                dayNotes = listOf(
                    DayNoteEntity(date = 20657, note = "状态好"),
                    DayNoteEntity(date = 20658, note = "出差停练"),
                ),
            ),
            request,
        )
        val idxDay1 = out.indexOf(dateHeading(20657))
        val idxDay2 = out.indexOf(dateHeading(20658))
        val idxTable = out.indexOf("| 组合 | 动作 | 组 | 完成次数 |")
        val idxPlaceholder = out.indexOf("（无打卡记录）") // 纯备注天没有表，表位写占位
        assertTrue(out.contains("当日备注：状态好"))
        assertTrue(out.contains("当日备注：出差停练"))
        assertTrue(out.contains("用时：30 分钟")) // 有时刻打卡的天推导用时
        assertTrue(out.contains("用时：-"))       // 纯备注天无用时数据
        assertTrue(out.contains("| 胸腿日 | 卧推 | 1 | 8 |"))
        // 段内顺序：打卡天（标题→备注→用时→表）在前，纯备注天在后且占位在其标题之后
        assertTrue(idxDay1 in 0 until idxTable)
        assertTrue(idxTable < idxDay2)
        assertTrue(idxDay2 < idxPlaceholder)
    }

    @Test
    fun `导出 - 逃组按组标注 打卡覆盖逃标不重复`() {
        val base = data(
            exercises = listOf(ExerciseEntity(id = 1, name = "辅助引体向上")),
            combos = listOf(
                ComboWithItems(
                    combo = ComboEntity(id = 10, name = "拉背"),
                    items = listOf(
                        ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                    ),
                ),
            ),
            skips = listOf(SetSkipEntity(date = 20657, comboItemId = 11, setIndex = 2)),
        )
        val onlySkip = MarkdownExporter.export(base, request)
        assertTrue(onlySkip.contains("| 拉背 | 辅助引体向上 | 3 | 逃 |")) // 组格 = setIndex + 1，完成次数格 = 逃

        val covered = MarkdownExporter.export(
            base.copy(
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 2, reps = 8),
                ),
            ),
            request,
        )
        assertFalse(covered.contains("| 拉背 | 辅助引体向上 | 3 | 逃 |")) // 打卡覆盖逃标，逃行消失
        assertTrue(covered.contains("| 拉背 | 辅助引体向上 | 3 | 8 |"))
    }

    @Test
    fun `导出 - 动作备注并入动作行`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "卧推")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "胸腿日"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8),
                    SetCheckinEntity(date = 20658, comboItemId = 11, setIndex = 0, reps = 10),
                ),
                exerciseNotes = listOf(
                    ExerciseNoteEntity(date = 20657, exerciseId = 1, userId = 1, note = "肩膀不舒服"),
                ),
            ),
            request,
        )
        assertTrue(out.contains("| 胸腿日 | 卧推（备注：肩膀不舒服） | 1 | 8 |"))
        assertTrue(out.contains("| 胸腿日 | 卧推 | 1 | 10 |")) // 无备注的天动作格保持原名
        assertFalse(out.contains("10（备注"))
    }

    @Test
    fun `导出 - 体重记录段 有数据显示 原值不换算`() {
        val out = MarkdownExporter.export(
            data(
                bodyWeights = listOf(
                    BodyWeightEntity(date = 20656, weight = 70.0),
                    BodyWeightEntity(date = 20657, weight = 52.5),
                ),
            ),
            request,
        )
        val idxCheckin = out.indexOf("## 训练打卡")
        val idxWeight = out.indexOf("## 体重记录")
        val idxLogs = out.indexOf("## 训练记录")
        assertTrue(out.contains("## 体重记录"))
        assertTrue(out.contains("| 日期 | 星期 | 体重（kg） |"))
        assertTrue(out.contains("| ${dateCells(20656)} | 70 |"))
        assertTrue(out.contains("| ${dateCells(20657)} | 52.5 |"))
        // 段位置：打卡段之后、训练记录段之前；行按日期升序
        assertTrue(idxCheckin in 0 until idxWeight)
        assertTrue(idxWeight < idxLogs)
        assertTrue(out.indexOf("70") < out.indexOf("52.5"))
    }

    @Test
    fun `导出 - 体重记录段 无数据整段省略`() {
        val out = MarkdownExporter.export(data(), request)
        assertFalse(out.contains("## 体重记录"))
    }

    @Test
    fun `导出 - 多训练者 体重段与打卡段含用户列`() {
        val out = MarkdownExporter.export(
            data(
                exercises = listOf(ExerciseEntity(id = 1, name = "辅助引体向上")),
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = listOf(
                            ComboItemEntity(id = 11, comboId = 10, exerciseId = 1, sort = 0),
                        ),
                    ),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8, userId = 1),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = 6, userId = 2),
                ),
                bodyWeights = listOf(
                    BodyWeightEntity(date = 20657, weight = 70.0, userId = 1),
                    BodyWeightEntity(date = 20658, weight = 52.5, userId = 2),
                ),
            ).copy(userNames = mapOf(1L to "阿明", 2L to "小红")),
            request,
        )
        assertTrue(out.contains("| 用户 | 组合 | 动作 | 组 | 完成次数 |"))
        assertTrue(out.contains("| 阿明 | 拉背 | 辅助引体向上 | 1 | 8 |"))
        assertTrue(out.contains("| 小红 | 拉背 | 辅助引体向上 | 2 | 6 |"))
        assertTrue(out.contains("| 日期 | 星期 | 用户 | 体重（kg） |"))
        assertTrue(out.contains("| 阿明 | 70 |"))
        assertTrue(out.contains("| 小红 | 52.5 |"))
    }

    @Test
    fun `导出 - 周计划用时列 有数据显示时长`() {
        val out = MarkdownExporter.export(
            data(
                combos = listOf(
                    ComboWithItems(
                        combo = ComboEntity(id = 10, name = "拉背"),
                        items = emptyList(),
                    ),
                ),
                dayPlans = listOf(
                    DayPlanEntity(date = 20657, isRest = false, comboId = 10),
                    DayPlanEntity(date = 20658, isRest = true, comboId = null),
                ),
                checkins = listOf(
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 0, reps = 8, checkedAt = 0L),
                    SetCheckinEntity(date = 20657, comboItemId = 11, setIndex = 1, reps = 8, checkedAt = 1_800_000L),
                ),
            ),
            request,
        )
        assertTrue(out.contains("## 周计划"))
        assertTrue(out.contains("| 日期 | 星期 | 类型 | 训练组合 | 用时 |"))
        assertTrue(out.contains("| ${dateCells(20657)} | 训练日 | 拉背 | 30 分钟 |"))
        assertTrue(out.contains("| ${dateCells(20658)} | 休息日 | - | - |")) // 无用时数据的天写 -
    }
}

package com.fitplan.app.data.model

import com.fitplan.app.data.db.DayPlanEntity

/**
 * 内置自动排程方案。生成结果写入指定周的 7 天，之后允许手动修改。
 * 所有方案的周起始均为周一。
 */
enum class SchemeType(val label: String, val description: String) {
    SPLIT3("三分化", "周一、周三、周五各安排一个组合，其余休息"),
    EVERY_OTHER_DAY("练一休一", "隔天训练，多个组合轮流循环，其余休息"),
}

data class Split3Binding(
    val slot1ComboId: Long? = null,
    val slot2ComboId: Long? = null,
    val slot3ComboId: Long? = null,
)

object Schemes {
    /** 三分化的固定槽位：周一 / 周三 / 周五。 */
    val SPLIT3_WEEKDAYS = listOf(1, 3, 5)

    /** 练一休一的训练日：周一、周三、周五、周日。 */
    val EVERY_OTHER_DAY_WEEKDAYS = listOf(1, 3, 5, 7)

    /**
     * 生成一周计划。weekday 为 1（周一）到 7（周日）。
     * 未绑定组合的槽位按休息日处理。
     */
    fun generate(
        type: SchemeType,
        mondayEpochDay: Long,
        split3: Split3Binding,
        cycleComboIds: List<Long>,
    ): List<DayPlanEntity> {
        val plans = mutableListOf<DayPlanEntity>()
        val comboForWeekday = mutableMapOf<Int, Long>()
        when (type) {
            SchemeType.SPLIT3 -> {
                SPLIT3_WEEKDAYS.forEachIndexed { i, wd ->
                    val id = listOf(split3.slot1ComboId, split3.slot2ComboId, split3.slot3ComboId)[i]
                    if (id != null) comboForWeekday[wd] = id
                }
            }
            SchemeType.EVERY_OTHER_DAY -> {
                if (cycleComboIds.isNotEmpty()) {
                    EVERY_OTHER_DAY_WEEKDAYS.forEachIndexed { i, wd ->
                        comboForWeekday[wd] = cycleComboIds[i % cycleComboIds.size]
                    }
                }
            }
        }
        for (wd in 1..7) {
            val comboId = comboForWeekday[wd]
            plans += DayPlanEntity(
                date = mondayEpochDay + (wd - 1),
                isRest = comboId == null,
                comboId = comboId,
            )
        }
        return plans
    }
}

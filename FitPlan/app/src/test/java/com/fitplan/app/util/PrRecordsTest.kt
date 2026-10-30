package com.fitplan.app.util

import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.SetCheckinEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** PR@n 汇总：每个次数档的最大每组真实重量与首达日期。 */
class PrRecordsTest {

    /** 两个动作条目：101 → 动作 1，201 → 动作 2。 */
    private fun items() = listOf(
        ComboItemEntity(id = 101, comboId = 10, exerciseId = 1),
        ComboItemEntity(id = 201, comboId = 10, exerciseId = 2),
    )

    private fun checkin(date: Long, comboItemId: Long, setIndex: Int, reps: Int?, weight: Double?) =
        SetCheckinEntity(date = date, comboItemId = comboItemId, setIndex = setIndex, reps = reps, weight = weight)

    @Test
    fun `PR - 同档取最大重量且首达日期取最早`() {
        // 动作 1、8 次档：100 日 60kg、101 日 80kg、102 日 80kg、103 日 50kg
        // → maxWeight = 80，且 80 在两天达成 → firstDate 取最早的 101（100 日的 60kg 不影响首达日期）
        val entries = buildPrEntries(
            listOf(
                checkin(100, 101, 0, 8, 60.0),
                checkin(101, 101, 0, 8, 80.0),
                checkin(102, 101, 0, 8, 80.0),
                checkin(103, 101, 0, 8, 50.0),
            ),
            items(),
        )
        assertEquals(
            listOf(PrEntry(exerciseId = 1, reps = 8, maxWeight = 80.0, firstDate = 101)),
            entries,
        )
    }

    @Test
    fun `PR - 排除无次数或无重量或无归属的组`() {
        val entries = buildPrEntries(
            listOf(
                checkin(100, 101, 0, reps = null, weight = 80.0), // 只记完成：无次数
                checkin(101, 101, 1, reps = 8, weight = null), // 旧数据：无重量
                checkin(102, 999, 0, reps = 8, weight = 70.0), // 条目已删：comboItemId 无归属
            ),
            items(),
        )
        assertEquals(emptyList<PrEntry>(), entries)
    }

    @Test
    fun `PR - 按动作与次数档升序排列`() {
        val entries = buildPrEntries(
            listOf(
                checkin(100, 101, 0, 8, 60.0), // 动作 1、8 次档
                checkin(101, 101, 0, 5, 70.0), // 动作 1、5 次档
                checkin(100, 201, 0, 10, 90.0), // 动作 2、10 次档
                checkin(101, 201, 0, 8, 100.0), // 动作 2、8 次档
            ),
            items(),
        )
        // 先按 exerciseId 升序、再按 reps 升序（灌数据顺序故意打乱；exerciseId 是 Long，须用 1L 才与 Pair<Long, Int> 相等）
        assertEquals(listOf(1L to 5, 1L to 8, 2L to 8, 2L to 10), entries.map { it.exerciseId to it.reps })
        assertEquals(listOf(70.0, 60.0, 100.0, 90.0), entries.map { it.maxWeight })
    }
}

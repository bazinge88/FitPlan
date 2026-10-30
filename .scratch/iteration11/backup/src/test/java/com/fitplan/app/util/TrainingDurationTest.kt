package com.fitplan.app.util

import com.fitplan.app.data.db.SetCheckinEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** 训练用时推导：按（天 + 训练者）从打卡时刻取首末差；口径与训练会话通知同源。 */
class TrainingDurationTest {

    /** 任取的基准时刻（epoch millis），只关心相对差。 */
    private val t0 = 1_800_000_000L
    private val minute = 60_000L

    private fun checkin(
        date: Long,
        checkedAt: Long?,
        userId: Long = 1,
        comboItemId: Long = 101,
        setIndex: Int = 0,
    ) = SetCheckinEntity(
        date = date, comboItemId = comboItemId, setIndex = setIndex,
        reps = 8, checkedAt = checkedAt, userId = userId,
    )

    @Test
    fun `用时 - 无打卡不产生`() {
        assertEquals(emptyList<TrainingDuration>(), buildTrainingDurations(emptyList()))
    }

    @Test
    fun `用时 - 单组约 0 分钟`() {
        val durations = buildTrainingDurations(listOf(checkin(date = 100, checkedAt = t0)))
        assertEquals(listOf(TrainingDuration(date = 100, userId = 1, durationMillis = 0)), durations)
        assertEquals("不到 1 分钟", durationText("用时", durations.single().durationMillis).removePrefix("用时"))
    }

    @Test
    fun `用时 - 首末时刻差即用时 跨小时 中途结束同样成立`() {
        // 100 日：三组跨 65 分钟 → 1 小时 5 分钟
        val d100 = buildTrainingDurations(
            listOf(
                checkin(date = 100, checkedAt = t0),
                checkin(date = 100, checkedAt = t0 + 30 * minute, setIndex = 1),
                checkin(date = 100, checkedAt = t0 + 65 * minute, setIndex = 2),
            ),
        )
        assertEquals(3_900_000L, d100.single().durationMillis)
        assertEquals("1 小时 5 分钟", durationText("用时", 3_900_000L).removePrefix("用时 "))

        // 101 日中途结束：只练两组就停，末组即终点，同样产生用时 20 分钟
        val d101 = buildTrainingDurations(
            listOf(
                checkin(date = 101, checkedAt = t0),
                checkin(date = 101, checkedAt = t0 + 20 * minute, setIndex = 1),
            ),
        )
        assertEquals(1_200_000L, d101.single().durationMillis)
    }

    @Test
    fun `用时 - 历史无时刻打卡不参与 不崩溃`() {
        // 整天都是无时刻的历史打卡：不产生用时
        assertEquals(
            emptyList<TrainingDuration>(),
            buildTrainingDurations(
                listOf(
                    checkin(date = 100, checkedAt = null),
                    checkin(date = 100, checkedAt = null, setIndex = 1),
                ),
            ),
        )
        // 混合：旧数据无时刻 + 新数据带时刻 → 只按带时刻的组推导
        val mixed = buildTrainingDurations(
            listOf(
                checkin(date = 101, checkedAt = null),
                checkin(date = 101, checkedAt = t0, setIndex = 1),
                checkin(date = 101, checkedAt = t0 + 10 * minute, setIndex = 2),
            ),
        )
        assertEquals(listOf(TrainingDuration(date = 101, userId = 1, durationMillis = 10 * minute)), mixed)
    }

    @Test
    fun `用时 - 多训练者互不干扰`() {
        val durations = buildTrainingDurations(
            listOf(
                checkin(date = 100, checkedAt = t0, userId = 2),
                checkin(date = 100, checkedAt = t0 + 5 * minute, userId = 2, setIndex = 1),
                checkin(date = 100, checkedAt = t0, userId = 1),
                checkin(date = 100, checkedAt = t0 + 30 * minute, userId = 1, setIndex = 1),
            ),
        )
        // 同一天各训练者各一条，首末差各自独立；结果按 date、userId 升序
        assertEquals(
            listOf(
                TrainingDuration(date = 100, userId = 1, durationMillis = 30 * minute),
                TrainingDuration(date = 100, userId = 2, durationMillis = 5 * minute),
            ),
            durations,
        )
    }

    @Test
    fun `durationsForUser - 按训练者投影为按天用时映射`() {
        val durations = listOf(
            TrainingDuration(date = 100, userId = 1, durationMillis = 30 * minute),
            TrainingDuration(date = 100, userId = 2, durationMillis = 5 * minute),
            TrainingDuration(date = 101, userId = 1, durationMillis = 0),
        )
        assertEquals(mapOf(100L to 30 * minute, 101L to 0L), durationsForUser(durations, userId = 1))
        assertEquals(mapOf(100L to 5 * minute), durationsForUser(durations, userId = 2))
    }

    @Test
    fun `durationText - 与通知用时口径同源`() {
        // < 1 分钟：前缀后不加空格（与通知「已进行不到 1 分钟」逐字一致）
        assertEquals("已进行不到 1 分钟", durationText("已进行", 30_000))
        // ≥ 1 分钟：前缀后加空格
        assertEquals("已进行 5 分钟", durationText("已进行", 5 * minute))
        assertEquals("已进行 1 小时 5 分钟", durationText("已进行", 65 * minute))
        // 周计划页前缀
        assertEquals("用时不到 1 分钟", durationText("用时", 59_999))
        assertEquals("用时 5 分钟", durationText("用时", 5 * minute))
        // 核心文案边界：分钟向下取整，整点小时带 0 分钟
        assertEquals("不到 1 分钟", durationWords(0))
        assertEquals("1 分钟", durationWords(minute))
        assertEquals("1 小时 0 分钟", durationWords(3_600_000L))
    }
}

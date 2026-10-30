package com.fitplan.app.util

import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 直接打卡的「下一组」递进逻辑：第一个既未打卡也未逃的组。 */
class QuickCheckInTest {

    /** 组合内两个动作：卧推（目标 3 组 × 8 次）、深蹲（目标 2 组、目标次数留空）。 */
    private fun items() = listOf(
        ComboItemEntity(id = 101, comboId = 10, exerciseId = 1, sort = 0, targetSets = 3, targetReps = 8),
        ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 1, targetSets = 2, targetReps = null),
    )

    private fun checkin(comboItemId: Long, setIndex: Int, reps: Int?) =
        SetCheckinEntity(date = 100, comboItemId = comboItemId, setIndex = setIndex, reps = reps)

    private fun skip(comboItemId: Long, setIndex: Int) =
        SetSkipEntity(date = 100, comboItemId = comboItemId, setIndex = setIndex)

    @Test
    fun `空清单与全部完成 - 返回 null`() {
        assertNull(QuickCheckIn.nextSet(emptyList(), emptyList(), emptyList()))
        assertNull(
            QuickCheckIn.nextSet(
                items(),
                listOf(
                    checkin(101L, 0, 8), checkin(101L, 1, 8), checkin(101L, 2, 8),
                    checkin(102L, 0, null), checkin(102L, 1, null),
                ),
                emptyList(),
            ),
        )
    }

    @Test
    fun `第一个未练完动作的下一组 - 次数取目标次数 跨动作递进`() {
        val items = items()
        // 全新清单：卧推第 0 组，目标次数 8
        assertEquals(QuickCheckIn.NextSet(101L, 0, 8), QuickCheckIn.nextSet(items, emptyList(), emptyList()))

        // 卧推打满 3 组 → 递进到深蹲第 0 组
        val benchDone = (0 until 3).map { checkin(101L, it, 8) }
        assertEquals(QuickCheckIn.NextSet(102L, 0, null), QuickCheckIn.nextSet(items, benchDone, emptyList()))
    }

    @Test
    fun `目标次数为空 - 沿用上一组实际次数 上一组只记完成则 null`() {
        val items = listOf(
            ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 0, targetSets = 3, targetReps = null),
        )
        // 无上一组 → null（只记完成）
        assertEquals(QuickCheckIn.NextSet(102L, 0, null), QuickCheckIn.nextSet(items, emptyList(), emptyList()))

        // 上一组实际 10 次 → 沿用 10
        assertEquals(
            QuickCheckIn.NextSet(102L, 1, 10),
            QuickCheckIn.nextSet(items, listOf(checkin(102L, 0, 10)), emptyList()),
        )

        // 上一组只记完成（reps 为 null）→ null
        assertEquals(
            QuickCheckIn.NextSet(102L, 1, null),
            QuickCheckIn.nextSet(items, listOf(checkin(102L, 0, null)), emptyList()),
        )
    }

    @Test
    fun `超出目标组数的位置也打过卡 - 行数增长后仍有下一组`() {
        // 卧推目标 3 组但第 0-3 组都打了（4 行，超出目标）→ 卧推已完，递进深蹲
        val overDone = (0 until 4).map { checkin(101L, it, 8) }
        assertEquals(QuickCheckIn.NextSet(102L, 0, null), QuickCheckIn.nextSet(items(), overDone, emptyList()))
    }

    @Test
    fun `已逃的组被跳过 - 推进到第一个既未打卡也未逃的组`() {
        // 全新清单但卧推第 1 组已标逃：指针仍落在第 0 组
        val skips = listOf(skip(101L, 1))
        assertEquals(QuickCheckIn.NextSet(101L, 0, 8), QuickCheckIn.nextSet(items(), emptyList(), skips))

        // 第 0 组打完 → 跳过已逃的第 1 组，推进到第 2 组（力竭跳组后训练继续）
        assertEquals(
            QuickCheckIn.NextSet(101L, 2, 8),
            QuickCheckIn.nextSet(items(), listOf(checkin(101L, 0, 8)), skips),
        )

        // 已逃的组被打卡覆盖后不再视为逃：0、1 都打了 → 指针同样到第 2 组
        assertEquals(
            QuickCheckIn.NextSet(101L, 2, 8),
            QuickCheckIn.nextSet(
                items(),
                listOf(checkin(101L, 0, 8), checkin(101L, 1, 8)),
                skips,
            ),
        )
    }

    @Test
    fun `全部组已打卡或已逃 - 跳到下一动作或返回 null`() {
        // 卧推 0、2 打卡且 1 逃 → 卧推无候选，递进深蹲
        val mixed = listOf(checkin(101L, 0, 8), checkin(101L, 2, 8))
        val benchSkips = listOf(skip(101L, 1))
        assertEquals(QuickCheckIn.NextSet(102L, 0, null), QuickCheckIn.nextSet(items(), mixed, benchSkips))

        // 深蹲两组全逃 → 整单无候选，返回 null
        val squatSkips = listOf(skip(102L, 0), skip(102L, 1))
        val benchAllDone = (0 until 3).map { checkin(101L, it, 8) }
        assertNull(QuickCheckIn.nextSet(items(), benchAllDone, benchSkips + squatSkips))
    }

    @Test
    fun `逃组之后的下一组 - 次数沿用最后已打卡组`() {
        // 深蹲（无目标次数）：第 0 组实际 10 次、第 1 组逃 → 第 2 组默认沿用 10
        val items = listOf(
            ComboItemEntity(id = 102, comboId = 10, exerciseId = 2, sort = 0, targetSets = 3, targetReps = null),
        )
        val skips = listOf(skip(102L, 1))
        assertEquals(
            QuickCheckIn.NextSet(102L, 2, 10),
            QuickCheckIn.nextSet(items, listOf(checkin(102L, 0, 10)), skips),
        )
    }
}

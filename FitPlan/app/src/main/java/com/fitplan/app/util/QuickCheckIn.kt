package com.fitplan.app.util

import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity

/** 直接打卡：在当天清单里找出下一个应打卡的组（第一个既未打卡也未逃的组）。 */
object QuickCheckIn {

    /** 下一组：动作条目 id、组序（0 起）、默认次数（目标次数 → 上一组实际 → 只记完成）。 */
    data class NextSet(val comboItemId: Long, val setIndex: Int, val reps: Int?)

    /** 全部练完（或已打卡/已逃占满，或清单为空）返回 null。行数与训练页一致：max(目标组数, 已打组数, 最大已逃组序+1, 1)。 */
    fun nextSet(
        items: List<ComboItemEntity>,
        checkins: List<SetCheckinEntity>,
        skips: List<SetSkipEntity>,
    ): NextSet? {
        val doneByItem = checkins.groupBy { it.comboItemId }
        val skipByItem = skips.groupBy { it.comboItemId }
        for (item in items.sortedBy { it.sort }) {
            val done = doneByItem[item.id].orEmpty()
            val skipped = skipByItem[item.id].orEmpty()
            // 已逃组序可稀疏（如缩组遗留的越界逃标），上界须盖住最大已逃组序而非逃标条数
            val rowCount = maxOf(
                item.targetSets ?: 0, done.size, (skipped.maxOfOrNull { it.setIndex } ?: -1) + 1, 1,
            )
            for (index in 0 until rowCount) {
                if (done.none { it.setIndex == index } && skipped.none { it.setIndex == index }) {
                    // 与 defaultRepsFor 同语义：上一组 = 已打卡组里组序最大的那行
                    val previous = done.maxByOrNull { it.setIndex }
                    val reps = item.targetReps ?: previous?.reps
                    return NextSet(item.id, index, reps)
                }
            }
        }
        return null
    }
}

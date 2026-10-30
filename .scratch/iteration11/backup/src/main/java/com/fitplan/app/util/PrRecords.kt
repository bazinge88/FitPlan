package com.fitplan.app.util

import com.fitplan.app.data.db.ComboItemEntity
import com.fitplan.app.data.db.SetCheckinEntity

/** 单条 PR（个人纪录）：某动作「以 reps 次完成一组」的历史最大每组真实重量及其首达日期。 */
data class PrEntry(
    val exerciseId: Long,
    val reps: Int,
    val maxWeight: Double,
    /** 首达日期：达成 maxWeight 的组中最早的 date（epoch day）。 */
    val firstDate: Long,
)

/**
 * 汇总 PR@n：某动作在「以 n 次完成一组」的历史打卡中的最大每组真实重量。
 *
 * 规则：
 * - 打卡按 comboItemId 关联条目得到 exerciseId，找不到对应条目的组跳过；
 * - 只计 reps 与 weight 都非空的组（旧数据 / 未记重量的组排除）；
 * - 每个 (exerciseId, reps) 次数档一条记录，maxWeight 取该档最大重量；
 * - firstDate 为达成 maxWeight 的组中最早 date；
 * - 结果按 exerciseId 升序、再按 reps 升序。
 */
fun buildPrEntries(checkins: List<SetCheckinEntity>, items: List<ComboItemEntity>): List<PrEntry> {
    val exerciseIdByItem = items.associate { it.id to it.exerciseId }
    return checkins
        .mapNotNull { checkin ->
            val exerciseId = exerciseIdByItem[checkin.comboItemId] ?: return@mapNotNull null
            val reps = checkin.reps ?: return@mapNotNull null
            val weight = checkin.weight ?: return@mapNotNull null
            ValidSet(exerciseId, reps, weight, checkin.date)
        }
        .groupBy { it.exerciseId to it.reps }
        .map { (key, sets) ->
            val maxWeight = sets.maxOf { it.weight }
            val firstDate = sets.filter { it.weight == maxWeight }.minOf { it.date }
            PrEntry(exerciseId = key.first, reps = key.second, maxWeight = maxWeight, firstDate = firstDate)
        }
        .sortedWith(compareBy({ it.exerciseId }, { it.reps }))
}

/** 参与汇总的有效组：次数、重量、归属三要素齐全。 */
private data class ValidSet(val exerciseId: Long, val reps: Int, val weight: Double, val date: Long)

package com.fitplan.app.util

import com.fitplan.app.data.db.SetCheckinEntity

/** 某训练者某天的训练用时：当天首组打卡到末组打卡的时间差（中途结束同样成立）。 */
data class TrainingDuration(
    val date: Long,
    val userId: Long,
    val durationMillis: Long,
)

/**
 * 按（天 + 训练者）从打卡时刻推导训练用时：
 * - 无打卡不产生；单组约 0 分钟；首末打卡时刻差即用时；
 * - 无时刻的历史打卡不参与、不报错；
 * - 结果按 date、userId 升序。
 */
fun buildTrainingDurations(checkins: List<SetCheckinEntity>): List<TrainingDuration> =
    checkins
        .mapNotNull { checkin ->
            checkin.checkedAt?.let { StampedCheckin(checkin.date, checkin.userId, it) }
        }
        .groupBy { it.date to it.userId }
        .map { (key, rows) ->
            TrainingDuration(
                date = key.first,
                userId = key.second,
                durationMillis = rows.maxOf { it.atMillis } - rows.minOf { it.atMillis },
            )
        }
        .sortedWith(compareBy({ it.date }, { it.userId }))

/** 某训练者的按天用时：date(epoch day) -> 用时毫秒（周计划页展示用；无打卡的天不出现）。 */
fun durationsForUser(durations: List<TrainingDuration>, userId: Long): Map<Long, Long> =
    durations.filter { it.userId == userId }.associate { it.date to it.durationMillis }

/**
 * 时长核心文案（分钟向下取整）：「不到 1 分钟」「X 分钟」「H 小时 M 分钟」。
 * 与训练会话通知的用时口径同源：通知与周计划页都经 [durationText] 组装。
 */
fun durationWords(millis: Long): String =
    when {
        millis < 60_000L -> "不到 1 分钟"
        millis < 3_600_000L -> "${millis / 60_000L} 分钟"
        else -> "${millis / 3_600_000L} 小时 ${(millis % 3_600_000L) / 60_000L} 分钟"
    }

/** 前缀 + 时长文案：< 1 分钟时前缀后不加空格（如「已进行不到 1 分钟」），否则加空格（如「用时 5 分钟」）。 */
fun durationText(prefix: String, millis: Long): String =
    if (millis < 60_000L) prefix + durationWords(millis) else "$prefix ${durationWords(millis)}"

/** 参与推导的打卡时刻：天、训练者、时刻三要素齐全（无时刻的历史打卡排除）。 */
private data class StampedCheckin(val date: Long, val userId: Long, val atMillis: Long)

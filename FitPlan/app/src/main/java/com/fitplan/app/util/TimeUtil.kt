package com.fitplan.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 周起始固定为周一。 */
object TimeUtil {
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val shortFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

    /** 1970-01-05 是 epoch day 4，恰好是周一，作为周页码基准。 */
    const val BASE_MONDAY: Long = 4L

    fun today(): LocalDate = LocalDate.now()

    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** 该日期所在周的周一。 */
    fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    fun mondayOfEpochDay(epochDay: Long): Long = mondayOf(fromEpochDay(epochDay)).toEpochDay()

    fun weekStartEpochDay(page: Int): Long = BASE_MONDAY + page * 7L

    fun pageFor(weekMondayEpochDay: Long): Int =
        ((weekMondayEpochDay - BASE_MONDAY) / 7L).toInt()

    fun formatDate(date: LocalDate): String = date.format(dateFmt)

    fun formatShort(date: LocalDate): String = date.format(shortFmt)

    fun weekdayName(date: LocalDate): String =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }

    fun isToday(epochDay: Long): Boolean = epochDay == today().toEpochDay()

    /** 重量格式化：保留正负号与小数，去掉多余的尾零。 */
    fun formatWeight(weight: Double): String =
        if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()
}

package com.fitplan.app.util

/**
 * 目标数值输入规则（组合目标与训练记录共用）。
 *
 * 整数类目标（组数/次数/时长）只允许数字并限定 1..上限，防止负数/0 脏数据进库；
 * 重量保持健身语义：正数=负重、0=自重、负数=辅助重量，允许小数。
 */
object InputRules {
    /** 目标组数上限。 */
    const val MAX_SETS = 50

    /** 目标次数上限。 */
    const val MAX_REPS = 999

    /** 目标时长上限（分钟，24 小时）。 */
    const val MAX_DURATION_MIN = 1440

    private val INT_INPUT = Regex("\\d*")
    private val WEIGHT_INPUT = Regex("-?\\d*(\\.\\d*)?")

    /** 整数输入过滤：只允许数字（空串允许，便于逐字输入）；负号与小数点在整数字段被拒绝。 */
    fun isIntInputAllowed(text: String): Boolean = text.matches(INT_INPUT)

    /** 重量输入过滤：允许负号与小数点（辅助重量/自重/小数），仅重量字段使用。 */
    fun isWeightInputAllowed(text: String): Boolean = text.isEmpty() || text.matches(WEIGHT_INPUT)

    /** 解析区间整数：空白返回 null（= 不设定）；非数字或越界同样返回 null（= 非法）。 */
    fun parseIntInRange(text: String, min: Int, max: Int): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toIntOrNull() ?: return null
        return if (value in min..max) value else null
    }

    /** 解析 1..max 的整数（目标组数/次数/时长）：空白、0、负数、非数字与越界均为 null。 */
    fun parsePositiveInt(text: String, max: Int): Int? = parseIntInRange(text, 1, max)

    /** 非法非空值判断（整数目标）：空串视为「不设定」不报错，越界/非数字报错。 */
    fun isInvalidPositiveInt(text: String, max: Int): Boolean =
        text.isNotBlank() && parsePositiveInt(text, max) == null

    /** 解析重量：空白返回 null（= 不设定/未记录）；保留原始符号，不做范围限制。 */
    fun parseWeightOrNull(text: String): Double? =
        text.trim().ifEmpty { return null }.toDoubleOrNull()

    /** 非法非空值判断（重量）：只拦「非数字」，负数/0/小数合法。 */
    fun isInvalidWeight(text: String): Boolean =
        text.isNotBlank() && parseWeightOrNull(text) == null
}

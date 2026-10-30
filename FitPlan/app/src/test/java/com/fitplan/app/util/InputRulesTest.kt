package com.fitplan.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 目标数值规则：整数拒绝负号/小数、重量放行负数、范围解析边界（组合目标与训练记录共用）。 */
class InputRulesTest {

    @Test
    fun `整数输入 - 拒绝负号与小数点`() {
        assertTrue(InputRules.isIntInputAllowed(""))
        assertTrue(InputRules.isIntInputAllowed("8"))
        assertTrue(InputRules.isIntInputAllowed("120"))
        assertFalse(InputRules.isIntInputAllowed("-3"))
        assertFalse(InputRules.isIntInputAllowed("3.5"))
        assertFalse(InputRules.isIntInputAllowed("+8"))
        assertFalse(InputRules.isIntInputAllowed("8a"))
        // 逐字输入中间态（先敲 1 再敲 2）必须放行，否则无法输入两位数
        assertTrue(InputRules.isIntInputAllowed("12"))
    }

    @Test
    fun `重量输入 - 允许负数 0 与小数`() {
        assertTrue(InputRules.isWeightInputAllowed(""))
        assertTrue(InputRules.isWeightInputAllowed("-20.5"))
        assertTrue(InputRules.isWeightInputAllowed("0"))
        assertTrue(InputRules.isWeightInputAllowed("52.5"))
        // 负号/小数点是逐字输入中间态：允许停留在 "-"、"-2." 这类半成品
        assertTrue(InputRules.isWeightInputAllowed("-"))
        assertTrue(InputRules.isWeightInputAllowed("-2."))
        assertFalse(InputRules.isWeightInputAllowed("--20"))
        assertFalse(InputRules.isWeightInputAllowed("2.5kg"))
    }

    @Test
    fun `区间解析 - 空白与越界返回 null`() {
        assertNull(InputRules.parseIntInRange("", 1, 50))
        assertNull(InputRules.parseIntInRange("   ", 1, 50))
        assertNull(InputRules.parsePositiveInt("0", InputRules.MAX_SETS))
        assertNull(InputRules.parsePositiveInt("-3", InputRules.MAX_SETS))
        assertNull(InputRules.parsePositiveInt("51", InputRules.MAX_SETS))
        assertNull(InputRules.parsePositiveInt("3.5", InputRules.MAX_SETS))
        assertNull(InputRules.parsePositiveInt("1441", InputRules.MAX_DURATION_MIN))
        assertNull(InputRules.parsePositiveInt("1000", InputRules.MAX_REPS))
    }

    @Test
    fun `区间解析 - 边界值与去除首尾空白`() {
        assertEquals(1, InputRules.parsePositiveInt("1", InputRules.MAX_SETS))
        assertEquals(50, InputRules.parsePositiveInt("50", InputRules.MAX_SETS))
        assertEquals(8, InputRules.parsePositiveInt("8", InputRules.MAX_REPS))
        assertEquals(999, InputRules.parsePositiveInt("999", InputRules.MAX_REPS))
        assertEquals(1440, InputRules.parsePositiveInt("1440", InputRules.MAX_DURATION_MIN))
        assertEquals(8, InputRules.parseIntInRange(" 8 ", 1, 999))
        assertEquals(20, InputRules.parseIntInRange("20", 20, 30))
    }

    @Test
    fun `非法非空判断 - 空串不算错 重量负数合法`() {
        assertFalse(InputRules.isInvalidPositiveInt("", InputRules.MAX_SETS))
        assertFalse(InputRules.isInvalidPositiveInt("30", InputRules.MAX_SETS))
        assertTrue(InputRules.isInvalidPositiveInt("-3", InputRules.MAX_SETS))
        assertTrue(InputRules.isInvalidPositiveInt("0", InputRules.MAX_SETS))
        assertFalse(InputRules.isInvalidWeight(""))
        assertFalse(InputRules.isInvalidWeight("-20.5"))
        assertFalse(InputRules.isInvalidWeight("0"))
        assertTrue(InputRules.isInvalidWeight("-"))
        assertTrue(InputRules.isInvalidWeight("abc"))
        assertNull(InputRules.parseWeightOrNull(""))
        assertEquals(-20.5, InputRules.parseWeightOrNull(" -20.5 ")!!, 0.0)
    }
}

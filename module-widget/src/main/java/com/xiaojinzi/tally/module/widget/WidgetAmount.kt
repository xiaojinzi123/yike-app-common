package com.xiaojinzi.tally.module.widget

import java.math.BigDecimal
import java.math.RoundingMode

/** Compact values always include a unit and an approximation mark; never truncate digits. */
internal fun formatWidgetAmount(cents: Long, maxCharacters: Int = 11, absolute: Boolean = false): String {
    val value = BigDecimal.valueOf(cents, 2).let { if (absolute) it.abs() else it }
    val full = "¥${value.toPlainString()}"
    if (full.length <= maxCharacters) return full
    val magnitude = value.abs()
    val (power, unit) = when {
        magnitude >= BigDecimal("1000000000000") -> 12 to "万亿"
        magnitude >= BigDecimal("100000000") -> 8 to "亿"
        else -> 4 to "万"
    }
    val scaled = value.movePointLeft(power)
    for (decimals in 2 downTo 0) {
        val compact = "≈¥${scaled.setScale(decimals, RoundingMode.HALF_UP).toPlainString()}$unit"
        if (compact.length <= maxCharacters) return compact
    }
    return "≈¥${scaled.setScale(0, RoundingMode.HALF_UP).toPlainString()}$unit"
}

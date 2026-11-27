package com.xiaojinzi.tally.module.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetAmountTest {
    @Test
    fun regularAmountsKeepCentsAndSign() {
        assertEquals("¥0.00", formatWidgetAmount(0))
        assertEquals("¥0.01", formatWidgetAmount(1))
        assertEquals("¥-0.01", formatWidgetAmount(-1))
        assertEquals("¥1234567.89", formatWidgetAmount(123_456_789))
        assertEquals("¥-1234.56", formatWidgetAmount(-123_456))
    }

    @Test
    fun fullAmountIsUsedUntilTheCharacterLimit() {
        assertEquals("¥1234567.89", formatWidgetAmount(123_456_789, maxCharacters = 11))
        assertEquals("≈¥123.46万", formatWidgetAmount(123_456_789, maxCharacters = 9))
        assertEquals("≈¥1234.57万", formatWidgetAmount(1_234_567_890))
    }

    @Test
    fun compactAmountsMarkRoundingAndUnits() {
        assertEquals("≈¥1.23亿", formatWidgetAmount(12_345_678_900))
        assertEquals("≈¥1.23万亿", formatWidgetAmount(123_456_789_000_000))
        assertEquals("≈¥-1.23亿", formatWidgetAmount(-12_345_678_900))
        assertEquals("≈¥1.23亿", formatWidgetAmount(-12_345_678_900, absolute = true))
        assertEquals("≈¥123.46万", formatWidgetAmount(123_455_000, maxCharacters = 9))
    }

    @Test
    fun extremeValuesDoNotOverflowOrLoseTheirSign() {
        val positive = formatWidgetAmount(Long.MAX_VALUE)
        val negative = formatWidgetAmount(Long.MIN_VALUE)
        val absolute = formatWidgetAmount(Long.MIN_VALUE, absolute = true)
        assertTrue(positive.startsWith("≈¥"))
        assertTrue(negative.startsWith("≈¥-"))
        assertTrue(positive.endsWith("万亿"))
        assertTrue(negative.endsWith("万亿"))
        assertEquals("≈¥-92234万亿", negative)
        assertEquals("≈¥92233.7万亿", absolute)
        assertFalse(absolute.contains("-"))
        assertTrue(positive.length <= 11)
        assertTrue(negative.length <= 11)
    }
}

package com.xiaojinzi.tally.module.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {
    @Test
    fun legacyShortAndNarrowWidgetsUseCompactLayout() {
        val layout = WidgetLayout.select(110f, 56f, 1f)
        assertTrue(layout.compact)
        assertFalse(layout.wide)
        assertFalse(layout.tall)
    }

    @Test
    fun wideWidgetKeepsMetricsUntilLargeTextRequiresLessContent() {
        val regular = WidgetLayout.select(300f, 160f, 1f)
        assertTrue(regular.wide)
        assertFalse(regular.compact)
        assertFalse(regular.largeText)
        val large = WidgetLayout.select(300f, 160f, 2f)
        assertTrue(large.compact)
        assertTrue(large.largeText)
        assertFalse(WidgetLayout.select(300f, 250f, 2f).compact)
    }

    @Test
    fun textFitNeverFallsBelowReadableMinimum() {
        assertEquals(11, fitWidgetTextSize(28) { false })
        assertEquals(17, fitWidgetTextSize(17) { true })
        assertEquals(14, fitWidgetTextSize(28) { it <= 14 })
    }
}

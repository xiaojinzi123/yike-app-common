package com.xiaojinzi.tally.module.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotTest {
    private fun ready() = WidgetSnapshot(
        status = WidgetStatus.Ready,
        amountsVisible = true,
        month = "2026-11",
        day = "2026-11-25",
        budget = 100_000,
        monthSpending = 32_000,
        monthIncome = 10_000,
    )

    @Test
    fun budgetShowsRemainingAndClampedProgress() {
        val snapshot = ready()
        assertEquals(68_000L, snapshot.remaining)
        assertEquals("¥680.00", snapshot.amount(snapshot.remaining))
        assertEquals(0.32f, snapshot.progress!!, 0.0001f)
        assertFalse(snapshot.overBudget)
        assertEquals("11月", snapshot.monthLabel)

        val exceeded = snapshot.copy(monthSpending = 120_000)
        assertTrue(exceeded.overBudget)
        assertEquals("已超支", exceeded.budgetLabel)
        assertEquals("¥200.00", exceeded.budgetAmount())
        assertEquals(-20_000L, exceeded.remaining)
        assertEquals("¥200.00", exceeded.amount(exceeded.remaining, absolute = true))
        assertEquals(1f, exceeded.progress!!, 0f)
        assertEquals(0f, snapshot.copy(monthSpending = -1).progress!!, 0f)
    }

    @Test
    fun privacyHidesAmountsProgressAndOverspending() {
        val hidden = ready().copy(amountsVisible = false, monthSpending = 120_000)
        assertEquals("••••", hidden.amount(hidden.remaining))
        assertEquals("••••", hidden.amount(Long.MIN_VALUE, absolute = true))
        assertEquals("••••", hidden.amount(null))
        assertFalse(hidden.overBudget)
        assertEquals("预算剩余", hidden.budgetLabel)
        assertEquals("••••", hidden.budgetAmount())
        assertNull(hidden.progress)
    }

    @Test
    fun absentOrNonPositiveBudgetDoesNotInventRemainingOrProgress() {
        listOf<Long?>(null, 0L, -1L).forEach { budget ->
            val snapshot = ready().copy(budget = budget)
            assertNull(snapshot.remaining)
            assertNull(snapshot.progress)
            assertFalse(snapshot.overBudget)
            assertEquals("—", snapshot.amount(snapshot.remaining))
        }
        val missingSpending = ready().copy(monthSpending = null)
        assertNull(missingSpending.remaining)
        assertNull(missingSpending.progress)
    }

    @Test
    fun incompleteStatusesNeverExposeStaleAmounts() {
        WidgetStatus.entries.filter { it != WidgetStatus.Ready }.forEach { status ->
            assertEquals("—", ready().copy(status = status).amount(12_345))
        }
        assertEquals("—", ready().amount(null))
        assertEquals("—月", ready().copy(month = "invalid").monthLabel)
    }
}

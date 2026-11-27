package com.xiaojinzi.tally.module.qianji.autorecord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPaymentNotificationsTest {
    @Test
    fun cancelsExpiredPersistedIdsAfterProcessRestart() {
        assertEquals(
            setOf("expired"),
            stalePaymentNotificationIds(
                persistedIds = setOf("expired", "active"),
                currentIds = emptySet(),
                retainedIds = setOf("active"),
            ),
        )
    }

    @Test
    fun cancelsDiscardedPersistedAndInMemoryIdsOnce() {
        assertEquals(
            setOf("expired", "discarded", "memoryOnly"),
            stalePaymentNotificationIds(
                persistedIds = setOf("expired", "discarded", "active"),
                currentIds = setOf("expired", "memoryOnly", "active"),
                retainedIds = setOf("active"),
            ),
        )
    }

    @Test
    fun keepsNotificationsForEveryRetainedId() {
        assertTrue(
            stalePaymentNotificationIds(
                persistedIds = setOf("first", "second"),
                currentIds = setOf("first"),
                retainedIds = setOf("first", "second"),
            ).isEmpty(),
        )
    }
}

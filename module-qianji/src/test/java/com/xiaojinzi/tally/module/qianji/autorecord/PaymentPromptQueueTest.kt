package com.xiaojinzi.tally.module.qianji.autorecord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentPromptQueueTest {
    @Test
    fun nextPreservesFifoAndKeepsCurrentUntilRemoved() {
        val queue = PaymentPromptQueue()
        listOf("first", "second", "third").forEach(queue::enqueue)

        assertEquals("first", queue.next())
        assertEquals("first", queue.next())
        assertEquals(2, queue.waitingCount)
        assertSequence(queue, "first", "second", "third")
    }

    @Test
    fun enqueueDeduplicatesCurrentAndWaitingIdsWithoutReordering() {
        val queue = PaymentPromptQueue()
        listOf("first", "second", "second", "third").forEach(queue::enqueue)
        assertEquals("first", queue.next())

        queue.enqueue("first")
        queue.enqueue("second")
        queue.enqueue("third")

        assertEquals(2, queue.waitingCount)
        listOf("first", "second", "third").forEach { assertTrue(queue.contains(it)) }
        assertFalse(queue.contains("unknown"))
        assertSequence(queue, "first", "second", "third")
    }

    @Test
    fun pauseWithoutReviewedIdBlocksDeliveryAndRestoresCurrentAtFront() {
        val queue = PaymentPromptQueue()
        listOf("first", "second", "third").forEach(queue::enqueue)
        assertEquals("first", queue.next())

        queue.pause(null)
        queue.pause(null)

        assertTrue(queue.paused)
        assertNull(queue.currentId)
        assertNull(queue.next())
        assertEquals(3, queue.waitingCount)
        queue.resume(setOf("third", "second", "first"))
        assertFalse(queue.paused)
        assertSequence(queue, "first", "second", "third")
    }

    @Test
    fun repeatedPauseForSameReviewedIdKeepsItAndNewArrivalsExactlyOnce() {
        val queue = PaymentPromptQueue()
        listOf("reviewed", "second", "third").forEach(queue::enqueue)
        assertEquals("reviewed", queue.next())

        queue.pause("reviewed")
        queue.enqueue("new")
        queue.enqueue("new")
        repeat(3) { queue.pause("reviewed") }

        assertTrue(queue.paused)
        assertNull(queue.currentId)
        assertNull(queue.next())
        assertEquals(3, queue.waitingCount)
        queue.resume(setOf("reviewed", "second", "third", "new"))
        assertSequence(queue, "reviewed", "second", "third", "new")
    }

    @Test
    fun leavingReviewWhileStillPendingRestoresReviewedIdBeforeInterruptedCurrent() {
        val queue = PaymentPromptQueue()
        listOf("current", "reviewed", "last").forEach(queue::enqueue)
        assertEquals("current", queue.next())

        queue.pause("reviewed")
        assertNull(queue.next())
        queue.resume(setOf("last", "current", "reviewed"))

        assertSequence(queue, "reviewed", "current", "last")
    }

    @Test
    fun savedReviewedIdMissingFromPendingDoesNotReturn() {
        val queue = PaymentPromptQueue()
        listOf("saved", "second", "third").forEach(queue::enqueue)
        assertEquals("saved", queue.next())

        queue.pause("saved")
        queue.resume(setOf("second", "third"))

        assertFalse(queue.contains("saved"))
        assertSequence(queue, "second", "third")
    }

    @Test
    fun explicitlyRemovedReviewedIdDoesNotReturnEvenWithStalePendingSnapshot() {
        val queue = PaymentPromptQueue()
        listOf("ignored", "next").forEach(queue::enqueue)
        assertEquals("ignored", queue.next())

        queue.pause("ignored")
        queue.remove("ignored")
        queue.remove("ignored")
        queue.resume(setOf("ignored", "next"))

        assertFalse(queue.contains("ignored"))
        assertSequence(queue, "next")
    }

    @Test
    fun retainDropsCurrentAndObsoleteWaitingIdsWithoutReorderingSurvivors() {
        val queue = PaymentPromptQueue()
        listOf("current", "second", "obsolete", "last").forEach(queue::enqueue)
        assertEquals("current", queue.next())

        queue.retain(setOf("last", "second"))

        assertNull(queue.currentId)
        assertFalse(queue.contains("current"))
        assertFalse(queue.contains("obsolete"))
        assertEquals(2, queue.waitingCount)
        assertSequence(queue, "second", "last")
    }

    @Test
    fun retainWhilePausedForgetsCompletedReviewBeforeLaterResume() {
        val queue = PaymentPromptQueue()
        listOf("reviewed", "next").forEach(queue::enqueue)
        assertEquals("reviewed", queue.next())
        queue.pause("reviewed")

        queue.retain(setOf("next"))

        assertTrue(queue.paused)
        assertNull(queue.next())
        queue.resume(setOf("reviewed", "next"))
        assertSequence(queue, "next")
    }

    @Test
    fun removeHandlesWaitingCurrentAndUnknownIdsIndependently() {
        val queue = PaymentPromptQueue()
        listOf("current", "removed", "last").forEach(queue::enqueue)
        assertEquals("current", queue.next())

        queue.remove("removed")
        queue.remove("unknown")
        assertEquals("current", queue.next())
        assertEquals(1, queue.waitingCount)
        assertFalse(queue.contains("removed"))

        queue.remove("current")
        assertNull(queue.currentId)
        assertFalse(queue.contains("current"))
        assertSequence(queue, "last")
    }

    @Test
    fun clearDiscardsCurrentAndWaitingAndAllowsSameIdToBeEnqueuedAgain() {
        val queue = PaymentPromptQueue()
        listOf("current", "waiting").forEach(queue::enqueue)
        assertEquals("current", queue.next())

        queue.clear()

        assertFalse(queue.paused)
        assertNull(queue.currentId)
        assertNull(queue.next())
        assertEquals(0, queue.waitingCount)
        assertFalse(queue.contains("current"))
        assertFalse(queue.contains("waiting"))
        queue.enqueue("current")
        assertSequence(queue, "current")
    }

    @Test
    fun clearWhilePausedAlsoDiscardsReviewedIdAndResetsPause() {
        val queue = PaymentPromptQueue()
        listOf("reviewed", "waiting").forEach(queue::enqueue)
        assertEquals("reviewed", queue.next())
        queue.pause("reviewed")

        queue.clear()
        queue.clear()

        assertFalse(queue.paused)
        assertNull(queue.currentId)
        assertEquals(0, queue.waitingCount)
        queue.resume(setOf("reviewed", "waiting"))
        assertNull(queue.next())
        queue.enqueue("waiting")
        queue.enqueue("reviewed")
        assertSequence(queue, "waiting", "reviewed")
    }

    @Test
    fun reviewingIdNeverQueuedDoesNotCreateAnExtraPrompt() {
        val queue = PaymentPromptQueue()
        listOf("current", "waiting").forEach(queue::enqueue)
        assertEquals("current", queue.next())

        queue.pause("external")
        queue.resume(setOf("external", "current", "waiting"))

        assertFalse(queue.contains("external"))
        assertSequence(queue, "current", "waiting")
    }

    private fun assertSequence(queue: PaymentPromptQueue, vararg expected: String) {
        expected.forEach { id ->
            assertEquals(id, queue.next())
            assertEquals(id, queue.currentId)
            queue.remove(id)
        }
        assertNull(queue.next())
        assertNull(queue.currentId)
        assertEquals(0, queue.waitingCount)
    }
}

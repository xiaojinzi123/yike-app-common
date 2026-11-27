package com.xiaojinzi.tally.module.qianji.authorization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationRequestGateTest {
    @Test
    fun pendingPermissionPreventsRootOrAnotherRequest() {
        val gate = AuthorizationRequestGate()
        val request = requireNotNull(gate.begin(waitForPermission = true))
        assertNull(gate.begin())
        assertNull(gate.begin(waitForPermission = true))
        assertTrue(gate.consumePermission(request))
        assertFalse(gate.consumePermission(request))
        assertNull(gate.begin())
        assertTrue(gate.finish(request))
        assertFalse(gate.isBusy())
    }

    @Test
    fun lateGrantOrDenialCannotFinishRootStartedAfterTimeout() {
        val gate = AuthorizationRequestGate()
        val expired = requireNotNull(gate.begin(waitForPermission = true))
        assertTrue(gate.expirePermission(expired))
        val root = requireNotNull(gate.begin())
        assertFalse(gate.consumePermission(expired))
        assertFalse(gate.finish(expired))
        assertFalse(gate.expirePermission(expired))
        assertTrue(gate.isActive(root))
        assertTrue(gate.isBusy())
    }

    @Test
    fun retryUsesDifferentCodeAndIgnoresOldCallbacks() {
        val gate = AuthorizationRequestGate()
        val first = requireNotNull(gate.begin(waitForPermission = true))
        assertTrue(gate.expirePermission(first))
        val retry = requireNotNull(gate.begin(waitForPermission = true))
        assertNotEquals(first, retry)
        assertFalse(gate.consumePermission(first))
        assertFalse(gate.finish(first))
        assertTrue(gate.consumePermission(retry))
        assertTrue(gate.finish(retry))
    }

    @Test
    fun grantAlreadyConsumedCannotBeExpiredDuringExecution() {
        val gate = AuthorizationRequestGate()
        val request = requireNotNull(gate.begin(waitForPermission = true))
        assertTrue(gate.consumePermission(request))
        assertFalse(gate.expirePermission(request))
        assertNull(gate.pendingPermission())
        assertTrue(gate.isActive(request))
    }

    @Test
    fun closedPageIgnoresResultsAndDoesNotShareIdsWithNewPage() {
        val old = AuthorizationRequestGate()
        val oldRequest = requireNotNull(old.begin(waitForPermission = true))
        old.close()
        assertFalse(old.consumePermission(oldRequest))
        assertFalse(old.finish(oldRequest))
        assertNull(old.begin())
        val current = AuthorizationRequestGate()
        val currentRequest = requireNotNull(current.begin(waitForPermission = true))
        assertNotEquals(oldRequest, currentRequest)
        assertFalse(current.consumePermission(oldRequest))
        assertTrue(current.isActive(currentRequest))
    }
}

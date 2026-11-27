package com.xiaojinzi.tally.module.qianji.authorization

import java.util.concurrent.atomic.AtomicInteger

/** 一个页面只保留一轮请求，过期或已销毁页面的回调不能执行授权。 */
internal class AuthorizationRequestGate {
    private var activeId: Int? = null
    private var waitingPermission = false
    private var closed = false

    @Synchronized
    fun begin(waitForPermission: Boolean = false): Int? {
        if (closed || activeId != null) return null
        return ids.incrementAndGet().also {
            activeId = it
            waitingPermission = waitForPermission
        }
    }

    @Synchronized
    fun consumePermission(id: Int): Boolean {
        if (closed || activeId != id || !waitingPermission) return false
        waitingPermission = false
        return true
    }

    @Synchronized
    fun expirePermission(id: Int): Boolean {
        if (!consumePermission(id)) return false
        activeId = null
        return true
    }

    @Synchronized
    fun finish(id: Int): Boolean {
        if (closed || activeId != id) return false
        activeId = null
        waitingPermission = false
        return true
    }

    @Synchronized
    fun pendingPermission(): Int? = activeId?.takeIf { waitingPermission && !closed }

    @Synchronized
    fun isActive(id: Int): Boolean = !closed && activeId == id

    @Synchronized
    fun isBusy(): Boolean = activeId != null

    @Synchronized
    fun close() {
        closed = true
        activeId = null
        waitingPermission = false
    }

    companion object {
        private val ids = AtomicInteger(2105)
    }
}

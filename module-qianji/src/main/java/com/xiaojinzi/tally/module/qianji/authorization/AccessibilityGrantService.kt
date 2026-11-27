package com.xiaojinzi.tally.module.qianji.authorization

import android.content.Context
import android.os.Binder
import android.os.Process
import android.util.Log
import androidx.annotation.Keep
import kotlin.system.exitProcess

/** 仅供本应用显式请求开启自身无障碍服务，不提供任意命令执行接口。 */
@Keep
class AccessibilityGrantService(private val context: Context) : IAccessibilityGrantService.Stub() {

    override fun enableOwnAccessibility(userId: Int): String {
        val callerUid = Binder.getCallingUid()
        requireOwnCaller(callerUid)
        require(userId == callerUid / 100_000) { "Invalid user" }
        val result = LocalCommandRunner.run(
            command = listOf("/system/bin/sh"),
            stdin = AccessibilityGrantCommand.script(context.packageName, userId),
            timeoutMillis = 10_000L,
        )
        val status = when {
            result.timedOut -> "TIMEOUT"
            result.exitCode == null -> "PROCESS_FAILED"
            result.exitCode != 0 -> "SETTINGS_${result.exitCode}"
            result.output.trim() == "QIANJI_ACCESSIBILITY_ENABLED" -> "OK"
            else -> "RESULT_UNCONFIRMED"
        }
        if (status != "OK") {
            Log.w("AccessibilityGrant", "status=$status outputBytes=${result.output.length}")
        }
        return status
    }

    override fun destroy() {
        val callerUid = Binder.getCallingUid()
        if (callerUid != Process.myUid()) requireOwnCaller(callerUid)
        exitProcess(0)
    }

    private fun requireOwnCaller(callerUid: Int) {
        if (callerUid != context.applicationInfo.uid) {
            throw SecurityException("Caller does not own this service")
        }
    }
}

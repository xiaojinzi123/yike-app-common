package com.xiaojinzi.tally.module.qianji.authorization

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.xiaojinzi.tally.module.qianji.QianjiAutoRecordService
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AccessibilityAuthorizationController(context: Context) {
    data class State(
        val busy: Boolean = false,
        val shizukuStatus: String = "正在检查本机服务…",
        val secureSettingsGranted: Boolean = false,
        val systemServiceEnabled: Boolean = false,
        val serviceConnected: Boolean = false,
        val message: String? = null,
        val error: Boolean = false,
    )

    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val requests = AuthorizationRequestGate()
    private var permissionTimeout: Job? = null
    private var binding: GrantBinding? = null
    private var closed = false

    private val receivedListener = Shizuku.OnBinderReceivedListener { scope.launch { refresh() } }
    private val deadListener = Shizuku.OnBinderDeadListener {
        scope.launch {
            requests.pendingPermission()?.let { id ->
                permissionTimeout?.cancel()
                finish(id, "Shizuku 服务已停止，请启动后重试", true)
            }
            binding?.remote?.completeExceptionally(IllegalStateException("Service stopped"))
            refresh()
        }
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, grant ->
        scope.launch {
            refresh()
            if (requests.consumePermission(code)) {
                permissionTimeout?.cancel()
                if (grant == PackageManager.PERMISSION_GRANTED) {
                    runShizuku(code)
                } else {
                    finish(code, "未获得 Shizuku 授权，可再次尝试或使用系统设置", true)
                }
            } else if (!requests.isBusy() && grant == PackageManager.PERMISSION_GRANTED) {
                // 超时后的授权仍可使用，但不会代替用户再次发起操作。
                mutableState.value = mutableState.value.copy(
                    message = "已获得 Shizuku 使用权限，点击授权即可开启无障碍服务", error = false,
                )
            }
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(receivedListener)
        Shizuku.addBinderDeadListener(deadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        scope.launch { AutoRecordState.status.collect { refresh() } }
        refresh()
    }

    fun refresh() {
        if (closed) return
        val status = runCatching {
            when {
                !Shizuku.pingBinder() -> "Shizuku 未运行或尚未连接"
                Shizuku.getVersion() < 13 -> "请更新 Shizuku 至支持 API 13 的版本"
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> "服务已连接，尚未授权本应用"
                else -> "已授权 · ${if (Shizuku.getUid() == 0) "Root" else "ADB"} 服务"
            }
        }.getOrDefault("Shizuku 未运行或尚未连接")
        mutableState.value = mutableState.value.copy(
            shizukuStatus = status,
            secureSettingsGranted = hasSecureSettingsPermission(),
            systemServiceEnabled = isSystemServiceEnabled(),
            serviceConnected = AutoRecordState.status.value.connected,
        )
    }

    fun adbGrantCommand(): String = AccessibilityGrantCommand.adbGrantCommand(
        app.packageName, Process.myUid() / 100_000,
    )

    fun recheck() {
        val id = requests.begin() ?: return
        mutableState.value = mutableState.value.copy(busy = true, message = "正在检查服务连接…", error = false)
        scope.launch { verifyConnection(id) }
    }

    /** This path is usable only after a real privileged grant; it never attempts to grant itself. */
    fun requestWithSecureSettings() {
        if (closed || requests.isBusy()) return
        refresh()
        if (!hasSecureSettingsPermission()) {
            showMessage("请先在电脑执行复制的 ADB 命令，再返回一键开启；也可使用系统设置、Root 或 Shizuku。", true)
            return
        }
        val id = requests.begin() ?: return
        mutableState.value = mutableState.value.copy(busy = true, message = "正在开启并检查服务连接…", error = false)
        scope.launch {
            executionMutex.withLock {
                if (!requests.isActive(id)) return@withLock
                try {
                    runInterruptible(Dispatchers.IO) {
                        check(hasSecureSettingsPermission())
                        // A failed query throws; it must never be converted into an empty list.
                        AccessibilityGrantCommand.mergeServices(readSecureSetting(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), app.packageName)
                        val merged = AccessibilityGrantCommand.mergeServices(
                            readSecureSetting(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), app.packageName,
                        )
                        check(!Thread.currentThread().isInterrupted)
                        check(Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, merged))
                        check(Settings.Secure.putInt(app.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1))
                        check(readSecureSetting(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) == merged)
                        check(readSecureSetting(Settings.Secure.ACCESSIBILITY_ENABLED) == "1")
                    }
                    verifyConnection(id, "ADB")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    finish(id, "无法确认设置结果，可能是权限被撤销、系统限制或设置被更新。请重新检测，或在系统设置中开启。", true)
                }
            }
        }
    }

    private fun hasSecureSettingsPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    private fun readSecureSetting(key: String): String? {
        val cursor = app.contentResolver.query(Settings.Secure.getUriFor(key), arrayOf("value"), null, null, null)
            ?: throw IllegalStateException("Settings query unavailable")
        return cursor.use {
            check(it.count <= 1) { "Ambiguous settings result" }
            if (!it.moveToFirst()) null else it.getString(it.getColumnIndexOrThrow("value"))
        }
    }

    private fun isSystemServiceEnabled(): Boolean = runCatching {
        val manager = app.getSystemService(AccessibilityManager::class.java)
        Settings.Secure.getInt(app.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 &&
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                it.resolveInfo.serviceInfo.packageName == app.packageName &&
                    it.resolveInfo.serviceInfo.name == QianjiAutoRecordService::class.java.name
            }
    }.getOrDefault(false)

    fun requestShizuku() {
        if (closed || requests.isBusy()) return
        refresh()
        var requestId: Int? = null
        try {
            if (!Shizuku.pingBinder()) {
                showMessage("请先在 Shizuku 中启动服务，再返回授权；ADB 模式可使用无线调试或电脑启动", true)
                return
            }
            if (Shizuku.getVersion() < 13) {
                showMessage("当前 Shizuku 版本不支持此授权方式，请更新后重试", true)
                return
            }
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            val id = requests.begin(waitForPermission = !granted) ?: return
            requestId = id
            mutableState.value = mutableState.value.copy(busy = true, message = null, error = false)
            if (granted) {
                scope.launch { runShizuku(id) }
            } else {
                permissionTimeout = scope.launch {
                    delay(60_000L)
                    if (requests.expirePermission(id)) {
                        showMessage("Shizuku 授权等待已结束，请再次点击重试", true)
                    }
                }
                Shizuku.requestPermission(id)
            }
        } catch (_: Exception) {
            permissionTimeout?.cancel()
            requestId?.let { requests.finish(it) }
            showMessage("无法请求 Shizuku 授权，请检查服务状态后重试", true)
        }
    }

    fun requestRoot() {
        val id = requests.begin() ?: return
        mutableState.value = mutableState.value.copy(
            busy = true, message = "请在 Root 管理器中确认本次请求…", error = false,
        )
        scope.launch {
            executionMutex.withLock {
                if (!requests.isActive(id)) return@withLock
                try {
                    val result = runInterruptible(Dispatchers.IO) {
                        LocalCommandRunner.run(
                            command = listOf("su"),
                            stdin = AccessibilityGrantCommand.script(app.packageName, Process.myUid() / 100_000),
                            timeoutMillis = 20_000L,
                        )
                    }
                    when {
                        result.timedOut -> finish(id, "Root 请求超时，请确认授权后重试", true)
                        result.exitCode != 0 || result.output.trim() != "QIANJI_ACCESSIBILITY_ENABLED" ->
                            finish(id, "Root 不可用、未获授权或系统拒绝修改；可使用 Shizuku 或系统设置", true)
                        else -> verifyConnection(id, "Root")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    finish(id, "未找到可用的 Root 授权，请使用 Shizuku 或系统设置", true)
                }
            }
        }
    }

    private suspend fun runShizuku(id: Int) = executionMutex.withLock {
        if (!requests.isActive(id)) return@withLock
        mutableState.value = mutableState.value.copy(busy = true, message = "正在通过 Shizuku 开启服务…", error = false)
        val current = GrantBinding(id)
        binding = current
        try {
            Shizuku.bindUserService(current.args, current)
            val service = withTimeoutOrNull(12_000L) { current.remote.await() }
            if (service == null) {
                finish(id, "Shizuku 辅助服务启动超时，请重启 Shizuku 后重试", true)
                return@withLock
            }
            // 同步 Binder 调用不响应线程中断；超时或退出页面时销毁对应的 UserService。
            val result = withTimeoutOrNull(15_000L) {
                suspendCancellableCoroutine<String> { continuation ->
                    Thread({
                        try {
                            val value = service.enableOwnAccessibility(Process.myUid() / 100_000)
                            continuation.resume(value)
                        } catch (failure: Exception) {
                            continuation.resumeWithException(failure)
                        }
                    }, "qianji-shizuku-request").apply { isDaemon = true }.start()
                }
            }
            when (result) {
                "OK" -> verifyConnection(id, "Shizuku")
                null, "TIMEOUT" -> finish(id, "系统设置操作超时，请重试或使用系统设置", true)
                "PROCESS_FAILED" -> finish(id, "Shizuku 辅助进程无法执行操作，请重启 Shizuku 后重试", true)
                "SETTINGS_10", "SETTINGS_11", "SETTINGS_30", "SETTINGS_33" ->
                    finish(id, "无法读取系统无障碍设置，请重启 Shizuku 或使用系统设置", true)
                "SETTINGS_31" -> finish(id, "无障碍设置刚被其他操作更新，请重新授权", true)
                "SETTINGS_20", "SETTINGS_21" ->
                    finish(id, "系统拒绝修改无障碍设置，请使用系统设置开启", true)
                else -> finish(id, "无法确认设置结果，请刷新状态或使用系统设置", true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            finish(id, "Shizuku 授权未完成，请检查服务、授权状态或使用系统设置", true)
        } finally {
            release(current)
            refresh()
        }
    }

    private inner class GrantBinding(id: Int) : ServiceConnection {
        val remote = CompletableDeferred<IAccessibilityGrantService>()
        val args = Shizuku.UserServiceArgs(ComponentName(app, AccessibilityGrantService::class.java))
            .daemon(false)
            .processNameSuffix("accessibility_grant")
            .tag("qianji_accessibility_grant_${Process.myUid()}_$id")
            .version(2)
        var released = false

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (released) return
            if (binder != null && binder.pingBinder()) remote.complete(IAccessibilityGrantService.Stub.asInterface(binder))
            else remote.completeExceptionally(IllegalStateException("Service not ready"))
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            remote.completeExceptionally(IllegalStateException("Service disconnected"))
        }
        override fun onBindingDied(name: ComponentName?) {
            remote.completeExceptionally(IllegalStateException("Binding died"))
        }
        override fun onNullBinding(name: ComponentName?) {
            remote.completeExceptionally(IllegalStateException("Empty binding"))
        }
    }

    private fun release(current: GrantBinding) {
        if (current.released) return
        current.released = true
        current.remote.cancel()
        if (binding === current) binding = null
        runCatching { Shizuku.unbindUserService(current.args, current, true) }
    }

    private suspend fun verifyConnection(id: Int, method: String? = null) {
        withTimeoutOrNull(6_000L) {
            AutoRecordState.status.first { it.connected }
            while (!isSystemServiceEnabled() || !AutoRecordState.status.value.connected) delay(200L)
        }
        refresh()
        val current = mutableState.value
        when {
            current.systemServiceEnabled && current.serviceConnected ->
                finish(id, if (method == null) "无障碍服务已开启并连接" else "$method 开启完成，无障碍服务已连接", false)
            current.systemServiceEnabled ->
                finish(id, "系统已开启，服务尚未连接。请在系统无障碍设置中关闭后重新开启，再检测连接。", true)
            else -> finish(id, "未检测到本应用的无障碍服务已开启，请在系统设置中确认后重试。", true)
        }
    }

    private fun finish(id: Int, message: String, error: Boolean) {
        if (requests.finish(id)) {
            refresh()
            showMessage(message, error)
        }
    }

    private fun showMessage(message: String, error: Boolean) {
        if (!closed) mutableState.value = mutableState.value.copy(busy = requests.isBusy(), message = message, error = error)
    }

    fun close() {
        if (closed) return
        closed = true
        requests.close()
        permissionTimeout?.cancel()
        binding?.let { release(it) }
        Shizuku.removeBinderReceivedListener(receivedListener)
        Shizuku.removeBinderDeadListener(deadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        scope.cancel()
    }

    companion object {
        private val executionMutex = Mutex()
    }
}

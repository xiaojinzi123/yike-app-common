package com.xiaojinzi.tally.module.qianji

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordPreferences
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordState
import com.xiaojinzi.tally.module.qianji.autorecord.PaymentNotifications
import com.xiaojinzi.tally.module.qianji.autorecord.PaymentOverlay
import com.xiaojinzi.tally.module.qianji.autorecord.PendingPayments
import com.xiaojinzi.tally.module.qianji.autorecord.RecognitionLabSession
import com.xiaojinzi.tally.module.qianji.recognition.DuplicatePaymentGate
import com.xiaojinzi.tally.module.qianji.recognition.PaymentNode
import com.xiaojinzi.tally.module.qianji.recognition.PaymentRecognitionResult
import com.xiaojinzi.tally.module.qianji.recognition.PaymentRecognizer
import com.xiaojinzi.tally.module.qianji.recognition.PaymentSnapshot

/** 无障碍节点采集与结果交付；识别规则独立为可离线测试的纯 Kotlin 内核。 */
class QianjiAutoRecordService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val recognizer = PaymentRecognizer()
    private val duplicateGate = DuplicatePaymentGate()
    private lateinit var overlay: PaymentOverlay
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled" || key == "overlay") {
            handler.post {
                if (!AutoRecordPreferences.enabled(this)) {
                    handler.removeCallbacks(sample)
                    scheduled = false
                    pendingPackage = null
                }
                if ((!AutoRecordPreferences.enabled(this) || !AutoRecordPreferences.overlay(this)) && ::overlay.isInitialized) {
                    overlay.dismiss()
                }
            }
        }
    }
    private val prunePending = object : Runnable {
        override fun run() {
            runCatching { PendingPayments.load(this@QianjiAutoRecordService) }
            handler.postDelayed(this, 60_000L)
        }
    }
    private var pendingPackage: String? = null
    private var pendingWindow = ""
    private var scheduled = false
    private val sample = Runnable {
        scheduled = false
        inspectWindow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (::overlay.isInitialized) overlay.close()
        PendingPayments.load(this)
        overlay = PaymentOverlay(this)
        AutoRecordPreferences.preferences(this).registerOnSharedPreferenceChangeListener(preferenceListener)
        handler.removeCallbacks(prunePending)
        handler.postDelayed(prunePending, 60_000L)
        // 自检包只用于用户主动打开的实验页；识别器仍只接受支持的真实支付包名。
        serviceInfo = serviceInfo.apply { packageNames = (WATCHED_PACKAGES + packageName).toTypedArray() }
        AutoRecordState.connected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return
        val source = event.packageName?.toString() ?: return
        val lab = source == packageName && RecognitionLabSession.current() != null
        if (!lab && (source !in WATCHED_PACKAGES || !AutoRecordPreferences.enabled(this))) return
        pendingPackage = source
        pendingWindow = event.className?.toString().orEmpty()
        if (!scheduled) {
            scheduled = true
            handler.postDelayed(sample, 350L)
        }
    }

    private fun inspectWindow() {
        val expectedPackage = pendingPackage ?: return
        val root = rootInActiveWindow ?: return
        try {
            if (root.packageName?.toString() != expectedPackage) return
            val lab = if (expectedPackage == packageName) RecognitionLabSession.current() else null
            if (expectedPackage == packageName && lab == null) return
            if (lab == null && !AutoRecordPreferences.enabled(this)) return
            val snapshot = PaymentSnapshot(
                packageName = lab?.packageName ?: expectedPackage,
                windowClass = pendingWindow,
                nodes = collectNodes(root),
                capturedAt = System.currentTimeMillis(),
            )
            when (val result = recognizer.recognize(snapshot)) {
                is PaymentRecognitionResult.Rejected -> AutoRecordState.result("未匹配：${result.reason}")
                is PaymentRecognitionResult.Matched -> {
                    if (!duplicateGate.tryAcquire(result.payment, SystemClock.elapsedRealtime())) {
                        AutoRecordState.result("已忽略同一支付页面的重复事件", duplicate = true)
                        return
                    }
                    val addition = try {
                        PendingPayments.enqueue(this, result.payment, synthetic = lab != null)
                    } catch (_: Exception) {
                        duplicateGate.release(result.payment)
                        AutoRecordState.result("本次记录未能落盘，请稍后重试；尚未加入待确认")
                        return
                    }
                    if (!addition.created) {
                        AutoRecordState.result("此笔记录已在待确认队列中，未重复提醒", duplicate = true)
                        return
                    }
                    val pending = addition.pending
                    val notificationAvailability = PaymentNotifications.availability(this)
                    val notificationPosted = PaymentNotifications.publish(this, pending)
                    val overlayDelivery = if (::overlay.isInitialized) {
                        overlay.show(pending)
                    } else AutoRecordState.OverlayDelivery.FAILED
                    AutoRecordState.delivery(
                        AutoRecordState.Delivery(
                            pendingId = pending.id,
                            notification = notificationAvailability,
                            notificationPosted = notificationPosted,
                            overlay = overlayDelivery,
                        ),
                    )
                    if (!notificationPosted && overlayDelivery in setOf(
                            AutoRecordState.OverlayDelivery.DISABLED,
                            AutoRecordState.OverlayDelivery.FAILED,
                        )
                    ) {
                        PaymentNotifications.pendingFeedback(this)
                    }
                    AutoRecordState.result(
                        when {
                            notificationPosted && overlayDelivery == AutoRecordState.OverlayDelivery.SHOWN ->
                                "已识别，悬浮卡与通知均已提醒，等待你核对"
                            notificationPosted && overlayDelivery == AutoRecordState.OverlayDelivery.QUEUED ->
                                "已识别，已发送通知；悬浮卡排队等待显示"
                            notificationPosted -> "已识别，已发送确认通知"
                            overlayDelivery == AutoRecordState.OverlayDelivery.SHOWN ->
                                "已识别，悬浮卡等待核对；系统通知不可用"
                            overlayDelivery == AutoRecordState.OverlayDelivery.QUEUED ->
                                "已识别，悬浮卡排队等待显示；系统通知不可用"
                            else -> "已加入待确认；提醒受限，请到自动记账页核对"
                        },
                        accepted = true,
                    )
                }
            }
        } catch (_: Exception) {
            AutoRecordState.result("页面已变化，本次采集已跳过")
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun collectNodes(root: AccessibilityNodeInfo): List<PaymentNode> {
        val nodes = ArrayList<PaymentNode>(80)
        var visited = 0
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (++visited > 300 || depth > 30 || nodes.size >= 200) return
            if (node.isVisibleToUser) {
                val text = node.text?.toString()?.trim().orEmpty().take(240)
                val description = node.contentDescription?.toString()?.trim().orEmpty().take(240)
                if (text.isNotBlank() || description.isNotBlank()) {
                    nodes += PaymentNode(text, node.viewIdResourceName.orEmpty(), description)
                }
            }
            for (index in 0 until minOf(node.childCount, 80)) {
                if (visited >= 300 || nodes.size >= 200) break
                val child = runCatching { node.getChild(index) }.getOrNull() ?: continue
                try { walk(child, depth + 1) } finally {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }
        walk(root, 0)
        return nodes
    }

    override fun onInterrupt() {
        handler.removeCallbacks(sample)
        scheduled = false
        pendingPackage = null
        if (::overlay.isInitialized) overlay.dismiss()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        AutoRecordPreferences.preferences(this).unregisterOnSharedPreferenceChangeListener(preferenceListener)
        if (::overlay.isInitialized) overlay.close()
        AutoRecordState.connected(false)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AMOUNT = "qj_amount"
        const val EXTRA_INCOME = "qj_income"
        const val EXTRA_NOTE = "qj_note"
        const val EXTRA_TIME = "qj_time"
        const val EXTRA_PACKAGE = "qj_pkg"
        val WATCHED_PACKAGES = setOf(
            "com.eg.android.AlipayGphone", "com.tencent.mm", "cn.gov.pbc.dcep", "com.unionpay",
        )
    }
}

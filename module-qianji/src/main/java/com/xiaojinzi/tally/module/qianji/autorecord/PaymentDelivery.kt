package com.xiaojinzi.tally.module.qianji.autorecord

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haooz.nexioui.theme.NexioDarkColorScheme
import com.haooz.nexioui.theme.NexioLightColorScheme
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.qianji.QianjiConfirmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

object PaymentNotifications {
    private const val CHANNEL = "payment_confirmation_v2"
    const val EXTRA_PENDING_ID = "pending_payment_id"

    enum class Availability { READY, PERMISSION_REQUIRED, APP_DISABLED, CHANNEL_DISABLED, CHANNEL_SILENT, UNAVAILABLE }

    fun intent(context: Context, id: String): Intent = Intent(context, QianjiConfirmActivity::class.java)
        .setData(Uri.Builder().scheme("xiaoya").authority("payment-confirmation").appendPath(id).build())
        .putExtra(EXTRA_PENDING_ID, id)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** 包括系统总开关、运行时权限及确认频道；可供设置页在 onResume 时刷新。 */
    fun availability(context: Context): Availability = runCatching {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "记账确认", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "支付结果识别后，由你核对并确认保存"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        })
        when {
            Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                Availability.PERMISSION_REQUIRED
            !NotificationManagerCompat.from(context).areNotificationsEnabled() -> Availability.APP_DISABLED
            manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE ->
                Availability.CHANNEL_DISABLED
            Build.VERSION.SDK_INT >= 28 && manager.getNotificationChannel(CHANNEL)?.group?.let {
                manager.getNotificationChannelGroup(it)?.isBlocked
            } == true -> Availability.CHANNEL_DISABLED
            (manager.getNotificationChannel(CHANNEL)?.importance ?: NotificationManager.IMPORTANCE_HIGH) <
                NotificationManager.IMPORTANCE_HIGH -> Availability.CHANNEL_SILENT
            else -> Availability.READY
        }
    }.getOrDefault(Availability.UNAVAILABLE)

    fun publish(context: Context, pending: PendingPayment): Boolean = runCatching {
        if (availability(context) !in setOf(Availability.READY, Availability.CHANNEL_SILENT)) return@runCatching false
        val open = PendingIntent.getActivity(
            context, 0, intent(context, pending.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payment = pending.payment
        val title = if (pending.synthetic) "${appName(context)} · 识别自检待确认" else "${appName(context)} · 支付待确认"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title)
            .setContentText("${if (payment.income) "收入" else "支出"} ¥${money(payment.amountFen)} · 点击核对")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_edit, "核对账单", open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .build()
        // tag 使用完整 ID，避免两笔记录的 hashCode 碰撞覆盖通知。
        context.getSystemService(NotificationManager::class.java).notify(pending.id, 0, notification)
        true
    }.getOrDefault(false)

    fun cancel(context: Context, id: String) {
        runCatching {
            context.getSystemService(NotificationManager::class.java).apply {
                cancel(id, 0)
                cancel(id.hashCode()) // 清理旧版本投递的通知。
            }
        }
    }

    fun pendingFeedback(context: Context) {
        Toast.makeText(context, "${appName(context)}：已识别支付，请到自动记账页核对保存", Toast.LENGTH_LONG).show()
    }

    fun appName(context: Context): String = runCatching { AppServices.appInfoSpi.appName }
        .getOrElse { context.applicationInfo.loadLabel(context.packageManager).toString() }

    fun money(amountFen: Long): String = BigDecimal.valueOf(amountFen, 2).toPlainString()
}

/** 主线程使用；只有新 pending 入队，同一 ID 不重置显示时间。 */
internal class PaymentPromptQueue {
    private val waiting = LinkedHashSet<String>()
    var currentId: String? = null
        private set
    val waitingCount: Int get() = waiting.size
    var paused: Boolean = false
        private set

    fun contains(id: String): Boolean = id == currentId || id in waiting

    fun enqueue(id: String) {
        if (id != currentId) waiting.add(id)
    }

    private var reviewedId: String? = null

    fun pause(reviewedId: String?) {
        if (paused && this.reviewedId == reviewedId) return
        this.reviewedId?.takeIf { it != reviewedId }?.let { waiting.add(it) }
        val resumeId = reviewedId?.takeIf(::contains)
        reviewedId?.let(::remove)
        this.reviewedId = resumeId
        currentId?.let { id ->
            val rest = waiting.toList()
            waiting.clear()
            waiting.add(id)
            waiting.addAll(rest)
        }
        currentId = null
        paused = true
    }

    fun resume(pendingIds: Set<String>) {
        retain(pendingIds)
        paused = false
        val reviewed = reviewedId
        reviewedId = null
        if (reviewed != null) {
            waiting.remove(reviewed)
            val rest = waiting.toList()
            waiting.clear()
            waiting.add(reviewed)
            waiting.addAll(rest)
        }
    }

    fun next(): String? {
        if (paused) return null
        if (currentId == null) {
            currentId = waiting.firstOrNull()?.also { waiting.remove(it) }
        }
        return currentId
    }

    fun remove(id: String) {
        waiting.remove(id)
        if (currentId == id) currentId = null
        if (reviewedId == id) reviewedId = null
    }

    fun retain(ids: Set<String>) {
        waiting.retainAll(ids)
        if (currentId !in ids) currentId = null
        if (reviewedId !in ids) reviewedId = null
    }

    fun clear() {
        waiting.clear()
        currentId = null
        reviewedId = null
        paused = false
    }
}

/** 无障碍服务内的触摸确认卡；只在用户点击核对时打开 Activity，不直接保存账单。 */
class PaymentOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = PaymentPromptQueue()
    private var view: View? = null
    private var timeout: Runnable? = null
    private var dark = service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    init {
        scope.launch {
            PendingPayments.items.collect { records ->
                val previous = queue.currentId
                queue.retain(records.map { it.id }.toSet())
                if (previous != queue.currentId) removeView()
                showNext()
            }
        }
        scope.launch {
            AutoRecordState.confirmationId.collect { id ->
                if (id != null) {
                    removeView()
                    queue.pause(id)
                    updateState()
                } else {
                    queue.resume(PendingPayments.items.value.map { it.id }.toSet())
                    showNext()
                }
            }
        }
        scope.launch {
            AppServices.appInfoSpi.isDarkThemeStateOb.collect { dark = it }
        }
    }

    fun show(pending: PendingPayment): AutoRecordState.OverlayDelivery {
        if (!AutoRecordPreferences.overlay(service)) return AutoRecordState.OverlayDelivery.DISABLED
        if (PendingPayments.items.value.none { it.id == pending.id }) return AutoRecordState.OverlayDelivery.FAILED
        queue.enqueue(pending.id)
        showNext()
        return when {
            queue.currentId == pending.id && view != null -> AutoRecordState.OverlayDelivery.SHOWN
            queue.contains(pending.id) -> AutoRecordState.OverlayDelivery.QUEUED
            else -> AutoRecordState.OverlayDelivery.FAILED
        }
    }

    private fun showNext() {
        if (!AutoRecordPreferences.overlay(service)) {
            dismiss()
            return
        }
        AutoRecordState.confirmationId.value?.let { id ->
            removeView()
            queue.pause(id)
            updateState()
            return
        }
        if (queue.paused) queue.resume(PendingPayments.items.value.map { it.id }.toSet())
        if (view != null) {
            updateState()
            return
        }
        while (true) {
            val id = queue.next() ?: break
            val pending = PendingPayments.items.value.find { it.id == id }
            if (pending == null) {
                queue.remove(id)
                continue
            }
            if (attach(pending)) {
                AutoRecordState.overlayDelivery(id, AutoRecordState.OverlayDelivery.SHOWN)
                break
            }
            queue.remove(id)
            AutoRecordState.overlayDelivery(id, AutoRecordState.OverlayDelivery.FAILED)
            AutoRecordState.feedback("悬浮提醒未能显示，记录已保留，请到自动记账页核对")
            PaymentNotifications.pendingFeedback(service)
        }
        updateState()
    }

    private fun attach(pending: PendingPayment): Boolean = runCatching {
        val colors = if (dark) NexioDarkColorScheme else NexioLightColorScheme
        val foreground = colors.onSurfaceContainer.toArgb()
        val muted = colors.onSurfaceVariantSummary.toArgb()
        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            background = GradientDrawable().apply {
                setColor(colors.surfaceContainer.toArgb())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), colors.onSurfaceVariantActions.copy(alpha = 0.1f).toArgb())
            }
        }
        fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(service).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(label(
            "${PaymentNotifications.appName(service)} · ${if (pending.synthetic) "识别自检待确认" else "支付待确认"}", 13f, muted,
        ))
        card.addView(label("${if (pending.payment.income) "+" else "−"} ¥${PaymentNotifications.money(pending.payment.amountFen)}", 24f, foreground, true).apply {
            setPadding(0, dp(4), 0, dp(4))
        })
        card.addView(label(pending.payment.merchant.ifBlank { "商户待补充" }.take(60), 14f, foreground).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        card.addView(label("核对账本、分类和账户后保存", 13f, muted).apply { setPadding(0, dp(4), 0, dp(8)) })
        val actions = LinearLayout(service).apply { gravity = Gravity.END }
        fun button(title: String, primary: Boolean = false, onClick: () -> Unit) =
            label(title, 14f, if (primary) colors.onPrimary.toArgb() else foreground, true).apply {
                gravity = Gravity.CENTER
                minHeight = dp(48)
                minWidth = dp(64)
                setPadding(dp(12), 0, dp(12), 0)
                background = RippleDrawable(
                    ColorStateList.valueOf(colors.onSurfaceVariantActions.copy(alpha = 0.16f).toArgb()),
                    GradientDrawable().apply {
                        setColor(if (primary) colors.primary.toArgb() else colors.onSurfaceVariantActions.copy(alpha = 0.06f).toArgb())
                        cornerRadius = dp(100).toFloat()
                    }, null,
                )
                setOnClickListener { if (queue.currentId == pending.id) onClick() }
            }
        actions.addView(button("忽略") {
            runCatching { PendingPayments.remove(service, pending.id) }.onSuccess {
                dismissCurrent(pending.id)
            }.onFailure {
                AutoRecordState.feedback("忽略失败，待确认记录已保留")
                Toast.makeText(service, "暂未能忽略，请稍后重试", Toast.LENGTH_SHORT).show()
            }
        })
        actions.addView(button("稍后") { dismissCurrent(pending.id) }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = dp(8); marginEnd = dp(8) })
        actions.addView(button("核对账单", primary = true) {
            AutoRecordState.confirmationStarted(pending.id)
            runCatching { service.startActivity(PaymentNotifications.intent(service, pending.id)) }.onFailure {
                AutoRecordState.confirmationFinished(pending.id)
                AutoRecordState.feedback("核对页暂未打开，记录已保留，请到自动记账页查看")
                Toast.makeText(service, "核对页暂未打开，请稍后重试", Toast.LENGTH_SHORT).show()
            }
        })
        card.addView(actions)
        val container = ScrollView(service).apply { addView(card) }
        val available = (service.resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(160))
        val params = WindowManager.LayoutParams(
            minOf(available, dp(380)), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(12)
        }
        windowManager.addView(container, params)
        view = container
        timeout = Runnable { if (queue.currentId == pending.id) dismissCurrent(pending.id) }
            .also { handler.postDelayed(it, 15_000L) }
        true
    }.getOrDefault(false)

    private fun dismissCurrent(id: String, delayNext: Boolean = false) {
        if (queue.currentId != id) return
        removeView()
        queue.remove(id)
        updateState()
        if (delayNext) {
            handler.postDelayed({ showNext() }, 15_000L)
        } else {
            showNext()
        }
    }

    private fun updateState() { AutoRecordState.overlay(queue.currentId, queue.waitingCount) }

    private fun removeView() {
        timeout?.let { handler.removeCallbacks(it) }
        timeout = null
        view?.let { runCatching { windowManager.removeViewImmediate(it) } }
        view = null
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        queue.clear()
        removeView()
        updateState()
    }

    fun close() {
        scope.cancel()
        dismiss()
    }

    private fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()
}

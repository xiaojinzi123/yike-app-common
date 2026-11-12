package com.xiaojinzi.tally.module.qianji

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 钱迹式无障碍自动记账（移植版）。
 *
 * 只监听支付类 App 的窗口变化，用通用节点扫描抽取金额/商户/类型，
 * 写入 yike Room。确认 UI 交给 yike Activity，不复用钱迹浮窗资源。
 */
class QianjiAutoRecordService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handled = LinkedHashMap<String, Long>(32)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in WATCHED_PACKAGES) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return
        val root = rootInActiveWindow ?: return
        val texts = collectTexts(root)
        val signal = PayPageParser.parse(pkg, texts) ?: return
        val key = "$pkg|${signal.amount}|${signal.note}|${signal.timeMs / 1500}"
        val now = System.currentTimeMillis()
        synchronized(handled) {
            val last = handled[key]
            if (last != null && now - last < 15_000) return
            handled[key] = now
            if (handled.size > 32) handled.remove(handled.keys.first())
        }
        // 交给确认页；通过广播发也可以，这里启动一个轻量确认 Activity
        val intent = Intent(this, QianjiConfirmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_AMOUNT, signal.amount)
            .putExtra(EXTRA_INCOME, signal.income)
            .putExtra(EXTRA_NOTE, signal.note)
            .putExtra(EXTRA_TIME, signal.timeMs)
            .putExtra(EXTRA_PACKAGE, pkg)
        startActivity(intent)
    }

    override fun onInterrupt() {}

    private fun collectTexts(node: AccessibilityNodeInfo?): List<String> {
        if (node == null) return emptyList()
        val out = ArrayList<String>(64)
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40 || out.size > 300) return
            n.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
            n.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
            for (i in 0 until n.childCount) {
                val child = try { n.getChild(i) } catch (e: Exception) { null } ?: continue
                walk(child, depth + 1)
            }
        }
        walk(node, 0)
        return out
    }

    companion object {
        const val EXTRA_AMOUNT = "qj_amount"
        const val EXTRA_INCOME = "qj_income"
        const val EXTRA_NOTE = "qj_note"
        const val EXTRA_TIME = "qj_time"
        const val EXTRA_PACKAGE = "qj_pkg"

        val WATCHED_PACKAGES = setOf(
            "com.eg.android.AlipayGphone",
            "com.tencent.mm",
            "cn.gov.pbc.dcep",
            "com.unionpay",
        )
    }
}

data class PaySignal(
    val amount: Double,
    val income: Boolean,
    val note: String,
    val timeMs: Long,
)

/** 通用支付成功页解析：不追求覆盖钱迹上百条规则，先覆盖核心四件套。 */
object PayPageParser {
    private val successWords = listOf("支付成功", "付款成功", "交易成功", "转账成功", "已完成", "支付完成")
    private val moneyRegex = Regex("(?:[¥￥]\\s*)?(-?\\d+(?:\\.\\d{1,2})?)")
    private val incomeWords = listOf("收款", "已收钱", "收入", "转入", "退款")
    private val exclude = listOf("余额", "红包", "优惠券", "积分", "手续费", "查看账单", "订单详情", "账单详情")

    fun parse(pkg: String, texts: List<String>): PaySignal? {
        if (texts.isEmpty()) return null
        val joined = texts.joinToString(" ")
        val isSuccess = successWords.any { joined.contains(it) }
        val classNameLike = joined.contains("成功") || joined.contains("完成")
        if (!isSuccess && !classNameLike) return null
        val amountText = texts.asSequence()
            .filter { it.length <= 18 && moneyRegex.containsMatchIn(it) }
            .filterNot { t -> exclude.any { t.contains(it) } }
            .mapNotNull { t ->
                moneyRegex.findAll(t)
                    .map { it.groupValues[1].toDoubleOrNull() }
                    .filterNotNull()
                    .map { kotlin.math.abs(it) }
                    .maxOrNull()
            }
            .filter { it > 0 && it < 1_000_000 }
            .maxOrNull()
            ?: return null
        val income = incomeWords.any { joined.contains(it) }
        val note = texts.firstOrNull {
            it.length in 2..40 && !exclude.any { e -> it.contains(e) } && !moneyRegex.matches(it)
        }.orEmpty()
        return PaySignal(
            amount = amountText,
            income = income,
            note = note,
            timeMs = System.currentTimeMillis(),
        )
    }
}

package com.xiaojinzi.tally.module.qianji.autorecord

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.xiaojinzi.tally.module.qianji.recognition.DuplicatePaymentGate
import com.xiaojinzi.tally.module.qianji.recognition.RecognizedPayment
import com.xiaojinzi.tally.module.qianji.recognition.normalizePaymentText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object AutoRecordState {
    enum class OverlayDelivery { SHOWN, QUEUED, DISABLED, FAILED }

    private val mutableConfirmationId = MutableStateFlow<String?>(null)
    val confirmationId = mutableConfirmationId.asStateFlow()

    /** Confirm 页进入前调用；只暂停悬浮提示，不改变 pending 内容。 */
    fun confirmationStarted(pendingId: String) {
        if (pendingId.isNotBlank()) mutableConfirmationId.value = pendingId
    }

    /** Confirm 页离开时调用；允许队列继续显示仍存在的 pending。 */
    fun confirmationFinished(pendingId: String? = null) {
        if (pendingId == null || mutableConfirmationId.value == pendingId) {
            mutableConfirmationId.value = null
        }
    }

    data class Delivery(
        val pendingId: String,
        val notification: PaymentNotifications.Availability,
        val notificationPosted: Boolean,
        val overlay: OverlayDelivery,
    )

    data class Status(
        val connected: Boolean = false,
        val sampled: Int = 0,
        val matched: Int = 0,
        val duplicate: Int = 0,
        val lastResult: String = "等待无障碍服务连接",
        val pendingCount: Int = 0,
        val overlayId: String? = null,
        val overlayQueueCount: Int = 0,
        val lastDelivery: Delivery? = null,
    )

    private val mutableStatus = MutableStateFlow(Status())
    val status = mutableStatus.asStateFlow()

    fun connected(value: Boolean) {
        mutableStatus.update { it.copy(
            connected = value,
            lastResult = if (value) "服务已连接，等待支付结果页" else "服务已断开",
        ) }
    }

    fun delivery(value: Delivery) {
        mutableStatus.update { it.copy(lastDelivery = value) }
    }

    fun overlayDelivery(id: String, value: OverlayDelivery) {
        mutableStatus.update { old ->
            old.copy(lastDelivery = old.lastDelivery?.let {
                if (it.pendingId == id) it.copy(overlay = value) else it
            })
        }
    }

    fun pendingCount(value: Int) {
        mutableStatus.update { it.copy(pendingCount = value) }
    }

    fun overlay(id: String?, queued: Int) {
        mutableStatus.update { it.copy(overlayId = id, overlayQueueCount = queued) }
    }

    fun feedback(message: String) {
        mutableStatus.update { it.copy(lastResult = message) }
    }

    fun result(message: String, accepted: Boolean = false, duplicate: Boolean = false) {
        mutableStatus.update { old -> old.copy(
            sampled = old.sampled + 1,
            matched = old.matched + if (accepted) 1 else 0,
            duplicate = old.duplicate + if (duplicate) 1 else 0,
            lastResult = message,
        ) }
    }
}

object AutoRecordPreferences {
    private const val FILE = "auto_record_preferences_v2"
    fun preferences(context: Context): SharedPreferences = context.getSharedPreferences(FILE, 0)
    fun enabled(context: Context) = preferences(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, 0).edit().putBoolean("enabled", value).apply()
    }
    fun overlay(context: Context) = context.getSharedPreferences(FILE, 0).getBoolean("overlay", true)
    fun setOverlay(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, 0).edit().putBoolean("overlay", value).apply()
    }
}

/** 只有正在显示的自检页可以授权本进程内的合成页面；不接收外部 Intent 的包名覆盖。 */
object RecognitionLabSession {
    data class Active(val packageName: String, val expiresAt: Long)
    @Volatile private var active: Active? = null

    fun begin(packageName: String) {
        active = Active(packageName, SystemClock.elapsedRealtime() + 120_000L)
    }
    fun end() { active = null }
    fun current(): Active? = active?.takeIf { SystemClock.elapsedRealtime() < it.expiresAt }
}

data class PendingPayment(
    val id: String,
    val payment: RecognizedPayment,
    val synthetic: Boolean,
    val orderDigest: String? = null,
    val lastSeenAt: Long = payment.occurredAt,
)

internal fun stalePaymentNotificationIds(
    persistedIds: Set<String>,
    currentIds: Set<String>,
    retainedIds: Set<String>,
): Set<String> = (persistedIds + currentIds) - retainedIds

/** 只保留确认所需字段和去重摘要，不存支付页面节点、订单原文或截图。 */
object PendingPayments {
    private const val FILE = "pending_payments_v2"
    private const val KEY = "records"
    private const val MAX_AGE = 24 * 60 * 60 * 1000L
    private val mutableItems = MutableStateFlow<List<PendingPayment>>(emptyList())
    val items = mutableItems.asStateFlow()

    @Synchronized
    fun load(context: Context): List<PendingPayment> {
        val now = System.currentTimeMillis()
        val json = context.getSharedPreferences(FILE, 0).getString(KEY, "[]").orEmpty()
        val persistedIds = mutableSetOf<String>()
        val records = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    val item = array.getJSONObject(index)
                    item.optString("id").takeIf { it.isNotBlank() }?.let(persistedIds::add)
                    val occurred = item.getLong("occurredAt")
                    if (now - occurred > MAX_AGE || occurred > now + 60_000L) return@runCatching null
                    if (item.getLong("amountFen") <= 0L || item.getString("id").isBlank()) return@runCatching null
                    PendingPayment(
                        id = item.getString("id"),
                        synthetic = item.optBoolean("synthetic"),
                        orderDigest = item.optString("orderDigest").takeIf { it.matches(Regex("[0-9a-f]{64}")) },
                        lastSeenAt = item.optLong("lastSeenAt", occurred).coerceIn(occurred, now + 60_000L),
                        payment = RecognizedPayment(
                            amountFen = item.getLong("amountFen"),
                            income = item.getBoolean("income"),
                            merchant = item.optString("merchant"),
                            sourcePackage = item.getString("sourcePackage"),
                            occurredAt = occurred,
                            ruleId = item.getString("ruleId"),
                            orderReference = null,
                        ),
                    )
                }.getOrNull()
            }.takeLast(32)
        }.getOrDefault(emptyList())
        val retainedIds = records.map { it.id }.toSet()
        stalePaymentNotificationIds(persistedIds, mutableItems.value.map { it.id }.toSet(), retainedIds)
            .forEach { PaymentNotifications.cancel(context, it) }
        mutableItems.value = records
        AutoRecordState.pendingCount(records.size)
        return records
    }

    data class Addition(val pending: PendingPayment, val created: Boolean)

    @Synchronized
    fun enqueue(context: Context, payment: RecognizedPayment, synthetic: Boolean): Addition {
        val records = load(context)
        val orderDigest = orderDigest(payment)
        val existing = records.lastOrNull { matches(it, payment, synthetic, orderDigest) }
        if (existing != null) {
            val updated = existing.copy(
                payment = existing.payment.copy(merchant = payment.merchant.ifBlank { existing.payment.merchant }),
                orderDigest = orderDigest ?: existing.orderDigest,
                lastSeenAt = maxOf(existing.lastSeenAt, payment.occurredAt),
            )
            persist(context, records.map { if (it.id == existing.id) updated else it })
            return Addition(updated, created = false)
        }
        val pending = PendingPayment(
            UUID.randomUUID().toString(), payment.copy(orderReference = null), synthetic, orderDigest,
        )
        persist(context, (records + pending).takeLast(32))
        return Addition(pending, created = true)
    }

    fun add(context: Context, payment: RecognizedPayment, synthetic: Boolean): PendingPayment =
        enqueue(context, payment, synthetic).pending

    internal fun orderDigest(payment: RecognizedPayment): String? = payment.orderReference
        ?.takeIf { normalizePaymentText(it).isNotEmpty() }
        ?.let {
            DuplicatePaymentGate.fingerprint(payment.copy(merchant = "", occurredAt = 0, ruleId = ""))
        }

    internal fun matches(
        pending: PendingPayment,
        payment: RecognizedPayment,
        synthetic: Boolean,
        orderDigest: String? = orderDigest(payment),
    ): Boolean {
        val old = pending.payment
        if (pending.synthetic != synthetic || old.sourcePackage != payment.sourcePackage ||
            old.amountFen != payment.amountFen || old.income != payment.income
        ) return false
        val merchant = normalizePaymentText(payment.merchant)
        val oldMerchant = normalizePaymentText(old.merchant)
        if (merchant.isNotEmpty() && oldMerchant.isNotEmpty() && merchant != oldMerchant) return false
        if (orderDigest != null && pending.orderDigest != null) return orderDigest == pending.orderDigest
        return payment.occurredAt - pending.lastSeenAt in 0L until 5 * 60_000L
    }

    @Synchronized
    fun find(context: Context, id: String): PendingPayment? = load(context).find { it.id == id }

    @Synchronized
    fun remove(context: Context, id: String) = persist(context, load(context).filterNot { it.id == id })

    private fun persist(context: Context, records: List<PendingPayment>) {
        val array = JSONArray()
        records.forEach { pending ->
            val payment = pending.payment
            array.put(JSONObject().apply {
                put("id", pending.id)
                put("synthetic", pending.synthetic)
                put("amountFen", payment.amountFen)
                put("income", payment.income)
                put("merchant", payment.merchant.take(120))
                put("sourcePackage", payment.sourcePackage)
                put("occurredAt", payment.occurredAt)
                put("ruleId", payment.ruleId)
                pending.orderDigest?.let { put("orderDigest", it) }
                put("lastSeenAt", pending.lastSeenAt)
            })
        }
        check(context.getSharedPreferences(FILE, 0).edit().putString(KEY, array.toString()).commit()) {
            "Unable to persist pending payments"
        }
        val retained = records.map { it.id }.toSet()
        mutableItems.value.filterNot { it.id in retained }.forEach { PaymentNotifications.cancel(context, it.id) }
        mutableItems.value = records
        AutoRecordState.pendingCount(records.size)
    }
}

package com.xiaojinzi.tally.module.qianji.recognition

import java.security.MessageDigest

/**
 * 单调时钟驱动的进程内去重。对同一指纹的每次观察（包括被拒绝的事件）都会刷新 TTL。
 * 因此只要成功页持续发出事件，就不会因为跨越固定时间桶而再次放行。
 *
 * debounceMillis 用于区分短促抖动和稍晚的重复事件，两者均不放行；TTL 从最后观察计算。
 * 缺失商户/订单号的观察与包/金额/方向相同且已知字段不冲突的观察保守合并。
 * 商户或订单号后续补全不会重复放行；两个明确不同的商户或订单号仍可分别放行。
 * 缓存按最后访问的 LRU 淘汰；进程重启、TTL 到期或容量淘汰后不再保证去重。
 * 缺商户/订单号时可能合并 TTL 内真实的新交易，调用方应接受此保守限制。
 */
class DuplicatePaymentGate(
    val debounceMillis: Long = 1_500L,
    val ttlMillis: Long = 5 * 60_000L,
    val maxEntries: Int = 256,
) {
    enum class Decision { ACCEPTED, DEBOUNCED, DUPLICATE }

    private data class Entry(
        val baseFingerprint: String,
        val merchant: String,
        val orderReference: String?,
        val lastSeen: Long,
    )

    private val entries = LinkedHashMap<String, Entry>()
    private var lastElapsedMillis: Long? = null

    init {
        require(debounceMillis >= 0L) { "debounceMillis must be non-negative" }
        require(ttlMillis > 0L && ttlMillis >= debounceMillis) {
            "ttlMillis must be positive and at least debounceMillis"
        }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    val size: Int
        @Synchronized get() = entries.size

    fun tryAcquire(payment: RecognizedPayment, elapsedMillis: Long): Boolean =
        evaluate(payment, elapsedMillis) == Decision.ACCEPTED

    /** 在下游持久化失败时撤销刚刚接受的观察，允许后续事件重试。 */
    @Synchronized
    fun release(payment: RecognizedPayment) {
        val base = fingerprint(payment.copy(merchant = "", orderReference = null))
        val merchant = normalizePaymentText(payment.merchant)
        val order = normalizedOrder(payment.orderReference)
        entries.entries.removeAll { (_, entry) ->
            entry.baseFingerprint == base &&
                (merchant.isEmpty() || entry.merchant.isEmpty() || merchant == entry.merchant) &&
                (order == null || entry.orderReference == null || order == entry.orderReference)
        }
    }

    /** 时间必须非负且不倒退；违规会抛 IllegalArgumentException，缓存保持不变。 */
    @Synchronized
    fun evaluate(payment: RecognizedPayment, elapsedMillis: Long): Decision {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative" }
        require(lastElapsedMillis == null || elapsedMillis >= lastElapsedMillis!!) {
            "elapsedMillis must be monotonic; use a new gate after a clock reset"
        }
        require(payment.amountFen > 0L) { "payment must have a positive amount" }
        lastElapsedMillis = elapsedMillis

        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            if (elapsedMillis - iterator.next().value.lastSeen >= ttlMillis) iterator.remove()
        }

        val key = fingerprint(payment)
        val base = fingerprint(payment.copy(merchant = "", orderReference = null))
        val merchant = normalizePaymentText(payment.merchant)
        val order = normalizedOrder(payment.orderReference)
        val matches = entries.entries.filter {
            it.value.baseFingerprint == base &&
                (merchant.isEmpty() || it.value.merchant.isEmpty() || merchant == it.value.merchant) &&
                (order == null || it.value.orderReference == null || order == it.value.orderReference)
        }.map { it.key to it.value }
        if (matches.isNotEmpty()) {
            val lastSeen = matches.maxOf { it.second.lastSeen }
            // 先移除再插到末尾刷新 LRU；仅补全未知字段，不抹去已经观察到的商户/订单号。
            matches.forEach { entries.remove(it.first) }
            for ((_, oldEntry) in matches) {
                val mergedMerchant = merchant.ifEmpty { oldEntry.merchant }
                val mergedOrder = order ?: oldEntry.orderReference
                val newKey = fingerprint(payment.copy(merchant = mergedMerchant, orderReference = mergedOrder))
                entries[newKey] = oldEntry.copy(
                    merchant = mergedMerchant,
                    orderReference = mergedOrder,
                    lastSeen = elapsedMillis,
                )
            }
            return if (elapsedMillis - lastSeen < debounceMillis) {
                Decision.DEBOUNCED
            } else {
                Decision.DUPLICATE
            }
        }

        entries[key] = Entry(base, merchant, order, elapsedMillis)
        if (entries.size > maxEntries) {
            val eldest = entries.entries.iterator()
            eldest.next()
            eldest.remove()
        }
        return Decision.ACCEPTED
    }

    companion object {
        /**
         * 长度前缀编码后 SHA-256；包含包/分/方向/规范化商户/可选订单号。
         * 不含 occurredAt、ruleId 或任何时间桶，避免同一页面事件时刻改变指纹。
         */
        @JvmStatic
        fun fingerprint(payment: RecognizedPayment): String {
            val fields = listOf(
                payment.sourcePackage,
                payment.amountFen.toString(),
                if (payment.income) "income" else "expense",
                normalizePaymentText(payment.merchant),
                normalizedOrder(payment.orderReference).orEmpty(),
            )
            val encoded = fields.joinToString("") { "${it.length}:$it" }
            val digest = MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray(Charsets.UTF_8))
            val hex = "0123456789abcdef"
            return buildString(digest.size * 2) {
                for (byte in digest) {
                    val value = byte.toInt() and 0xff
                    append(hex[value ushr 4])
                    append(hex[value and 0x0f])
                }
            }
        }

        private fun normalizedOrder(value: String?): String? =
            value?.let(::normalizePaymentText)?.takeIf { it.isNotEmpty() }
    }
}

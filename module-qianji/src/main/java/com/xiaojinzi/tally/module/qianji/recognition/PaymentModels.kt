package com.xiaojinzi.tally.module.qianji.recognition

/** 单个可访问性节点。内核不信任 viewId 或窗口类名来证明交易成功。 */
data class PaymentNode(
    val text: String,
    val viewId: String = "",
    val description: String = "",
)

/**
 * nodes 应保持同一当前窗口的视觉/树遍历顺序，不能拼接通知、历史事件或多个窗口。
 * 显式标签和值只在同一节点或紧邻节点内配对；有限成功模板另允许唯一的独立币符金额。
 * 未提供节点类型，不能据此重建 TextView 距离或父子关系；capturedAt 是调用方提供的墙钟时间。
 */
data class PaymentSnapshot(
    val packageName: String,
    val windowClass: String = "",
    val nodes: List<PaymentNode>,
    val capturedAt: Long,
)

/** merchant 为空表示缺少可证明的商户信息，交给 UI 待补全；不会用临近文字伪造。 */
data class RecognizedPayment(
    val amountFen: Long,
    val income: Boolean,
    val merchant: String,
    val sourcePackage: String,
    val occurredAt: Long,
    val ruleId: String,
    val orderReference: String? = null,
)

sealed class PaymentRecognitionResult {
    data class Matched(val payment: RecognizedPayment) : PaymentRecognitionResult()
    data class Rejected(val reason: String) : PaymentRecognitionResult()
}

/** 稳定的机器可读拒绝原因，不包含支付页面原文。 */
object PaymentRejectionReasons {
    const val UNSUPPORTED_PACKAGE = "unsupported_package"
    const val INVALID_SNAPSHOT = "invalid_snapshot"
    const val UNSUPPORTED_TEMPLATE = "unsupported_success_template"
    const val MISSING_SUCCESS = "missing_success_evidence"
    const val CONFLICTING_STATUS = "conflicting_transaction_status"
    const val AMBIGUOUS_DIRECTION = "ambiguous_direction"
    const val MISSING_AMOUNT = "missing_labeled_amount"
    const val INVALID_AMOUNT = "invalid_labeled_amount"
    const val AMBIGUOUS_AMOUNT = "ambiguous_amount"
    const val MISSING_MERCHANT = "missing_labeled_merchant"
    const val INVALID_MERCHANT = "invalid_labeled_merchant"
    const val AMBIGUOUS_MERCHANT = "ambiguous_merchant"
    const val INVALID_ORDER_REFERENCE = "invalid_order_reference"
    const val AMBIGUOUS_ORDER_REFERENCE = "ambiguous_order_reference"
}

internal fun normalizePaymentText(value: String): String = value
    .replace('\u00a0', ' ')
    .replace('\u202f', ' ')
    .replace('\u3000', ' ')
    .replace('\uff1a', ':')
    .trim()
    .replace(Regex("\\s+"), " ")

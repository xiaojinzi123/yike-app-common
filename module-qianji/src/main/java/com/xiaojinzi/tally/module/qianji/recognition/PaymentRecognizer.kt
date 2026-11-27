package com.xiaojinzi.tally.module.qianji.recognition

import java.math.BigDecimal

/**
 * 保守的人民币支付识别内核，仅覆盖下面显式列出的文本规则。
 * 当前规则由合成 fixtures 验证，未经实盘页面捕获验证，不代表钱迹的完整规则库。
 *
 * 金额标签优先；仅支付宝/微信的精确成功模板可使用独立币符金额兜底，并要求候选分值唯一。
 * 依据 j9/e.java:39-49、u9/e.java:6、ba/f.java:21-24、r9/e.java:90-98 的有限文本规则。
 * PaymentNode 缺少节点类型/父子关系，不能重现“后 5 个 TextView”的裸数字定位；仍拒绝裸数字兜底。
 * 不靠窗口类名、viewId、商户中的“收款”或泛化“成功/已完成”猜测；无明确商户时返回空字符串。
 * 账单详情同样必须提供精确成功状态；occurredAt 保留 capturedAt，不猜测交易时间。
 */
class PaymentRecognizer {
    fun recognize(snapshot: PaymentSnapshot): PaymentRecognitionResult {
        val rule = rules[snapshot.packageName]
            ?: return rejected(PaymentRejectionReasons.UNSUPPORTED_PACKAGE)
        if (snapshot.capturedAt < 0L || snapshot.nodes.isEmpty() || snapshot.nodes.size > 512 ||
            snapshot.nodes.any { it.text.length > 4096 || it.description.length > 4096 }
        ) return rejected(PaymentRejectionReasons.INVALID_SNAPSHOT)

        val cells = snapshot.nodes.map { node ->
            (node.text.lineSequence() + node.description.lineSequence())
                .map(::normalizePaymentText).filter { it.isNotEmpty() }.distinct().toList()
        }
        val visibleTexts = cells.flatten().toSet()
        if (rule.id == "wechat" &&
            (("首页" in visibleTexts && "点单" in visibleTexts) ||
                ("支付成功" in visibleTexts && "去下单" in visibleTexts))
        ) return rejected(PaymentRejectionReasons.UNSUPPORTED_TEMPLATE)

        val fields = extractFields(cells, rule.id in setOf("alipay", "wechat"))
        val boundNodes = fields.flatMap { it.boundNodes }.toSet()
        val statusFields = fields.filter { it.label in statusLabels }
        val statusValues = statusFields.flatMap { it.values }
        val standalone = cells.flatMapIndexed { index, values ->
            if (index in boundNodes) emptyList() else values
        }
        // u9/e 的合并节点模板还要求“完成”；这里只接受精确人民币格式，不扩展为包含匹配。
        val combinedAmounts = if (rule.id == "alipay" && "完成" in standalone) {
            standalone.mapNotNull { combinedSuccessPattern.matchEntire(it)?.groupValues?.get(1) }
        } else {
            emptyList()
        }
        val statusEvidence = standalone + statusValues + if (combinedAmounts.isNotEmpty()) {
            listOf("支付成功")
        } else {
            emptyList()
        }
        val fallbackTemplate = when (rule.id) {
            "alipay" -> "支付成功" in standalone || combinedAmounts.isNotEmpty()
            "wechat" -> "支付成功" in standalone || "充值成功" in standalone
            else -> false
        }
        if (statusEvidence.any { it in blockedStatuses }) {
            return rejected(PaymentRejectionReasons.CONFLICTING_STATUS)
        }
        val successes = statusEvidence.mapNotNull { rule.successes[it] }.toSet()
        if (successes.isEmpty()) return rejected(PaymentRejectionReasons.MISSING_SUCCESS)
        if (statusFields.any { field ->
                field.values.isEmpty() || field.values.any { value ->
                    val statusKind = rule.successes[value]
                    statusKind == null || when (field.label) {
                        "退款状态" -> statusKind != Kind.REFUND
                        "收款状态" -> statusKind != Kind.INCOME
                        "支付状态", "付款状态" -> statusKind !in setOf(Kind.EXPENSE, Kind.UNSPECIFIED)
                        else -> false
                    }
                }
            }
        ) return rejected(PaymentRejectionReasons.CONFLICTING_STATUS)
        val concreteSuccesses = successes - Kind.UNSPECIFIED
        if (concreteSuccesses.size > 1) return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)

        val directionFields = fields.filter { it.label in directionLabels }
        val explicitDirections = directionFields.flatMap { it.values }.map { directionValuesToKind[it] }
        if (directionFields.any { it.values.isEmpty() } || explicitDirections.any { it == null } ||
            explicitDirections.filterNotNull().distinct().size > 1
        ) {
            return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
        }
        val explicitDirection = explicitDirections.filterNotNull().firstOrNull()
        val amountFields = fields.filter { it.label in amountLabels }
        val amountDirections = amountFields.mapNotNull { amountLabels.getValue(it.label).kind }.toSet()
        val successKind = concreteSuccesses.singleOrNull()
        val kind = if (successKind == Kind.REFUND) {
            if (explicitDirection != null && explicitDirection !in setOf(Kind.INCOME, Kind.REFUND)) {
                return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
            }
            Kind.REFUND
        } else {
            if (Kind.REFUND in amountDirections || explicitDirection == Kind.REFUND) {
                // “交易成功”不能证明退款已经到账。
                return rejected(PaymentRejectionReasons.MISSING_SUCCESS)
            }
            val directions = (amountDirections + listOfNotNull(successKind, explicitDirection)).toSet()
            if (directions.size != 1) return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
            directions.single()
        }

        val applicableAmounts = amountFields.filter {
            val labelKind = amountLabels.getValue(it.label).kind
            if (kind == Kind.REFUND) labelKind == Kind.REFUND else labelKind == null || labelKind == kind
        }
        val usedCurrencyFallback = applicableAmounts.isEmpty()
        val parsedAmounts = ArrayList<LabeledAmount>()
        if (usedCurrencyFallback) {
            if (!fallbackTemplate) return rejected(PaymentRejectionReasons.MISSING_AMOUNT)
            val looseTexts = standalone.filterNot { combinedSuccessPattern.matches(it) } + combinedAmounts
            val currencyTexts = looseTexts.filter { '¥' in it || '￥' in it }
            if (currencyTexts.isEmpty()) return rejected(PaymentRejectionReasons.MISSING_AMOUNT)
            // 无明确金额标签时，订单/优惠/余额等字段会破坏局部关联，不能忽略后猜剩下的数字。
            if (cells.flatten().any { fallbackInterference.containsMatchIn(it) }) {
                return rejected(PaymentRejectionReasons.AMBIGUOUS_AMOUNT)
            }
            val amounts = currencyTexts.map { value ->
                if (!currencyAmountPattern.matches(value)) return rejected(PaymentRejectionReasons.INVALID_AMOUNT)
                parseMoney(value) ?: return rejected(PaymentRejectionReasons.INVALID_AMOUNT)
            }
            if (amounts.any { (kind == Kind.EXPENSE && it.sign == '+') || (kind != Kind.EXPENSE && it.sign == '-') }) {
                return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
            }
            val distinctAmounts = amounts.map { it.fen }.distinct()
            if (distinctAmounts.size != 1) return rejected(PaymentRejectionReasons.AMBIGUOUS_AMOUNT)
            val fallbackFen = distinctAmounts.single()
            // 无币符的数字不作为金额候选；仅用来否决与已确定金额冲突的数值/日期/订单片段。
            for (text in looseTexts.filterNot { it in currencyTexts }) {
                val otherMoney = parseMoney(text)
                if (otherMoney != null &&
                    ((kind == Kind.EXPENSE && otherMoney.sign == '+') || (kind != Kind.EXPENSE && otherMoney.sign == '-'))
                ) return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
                if ((otherMoney != null && otherMoney.fen != fallbackFen) ||
                    (numericLookingPattern.matches(text) && otherMoney == null)
                ) return rejected(PaymentRejectionReasons.AMBIGUOUS_AMOUNT)
            }
            parsedAmounts += LabeledAmount(1, fallbackFen)
        }
        for (field in applicableAmounts) {
            if (field.values.isEmpty()) return rejected(PaymentRejectionReasons.INVALID_AMOUNT)
            for (value in field.values) {
                val amount = parseMoney(value) ?: return rejected(PaymentRejectionReasons.INVALID_AMOUNT)
                if ((kind == Kind.EXPENSE && amount.sign == '+') ||
                    (kind != Kind.EXPENSE && amount.sign == '-')
                ) return rejected(PaymentRejectionReasons.AMBIGUOUS_DIRECTION)
                parsedAmounts += LabeledAmount(amountLabels.getValue(field.label).priority, amount.fen)
            }
        }
        val bestPriority = parsedAmounts.minOf { it.priority }
        val bestAmounts = parsedAmounts.filter { it.priority == bestPriority }.map { it.fen }.distinct()
        if (bestAmounts.size != 1) return rejected(PaymentRejectionReasons.AMBIGUOUS_AMOUNT)
        val amountFen = bestAmounts.single()
        // 实付和付款金额都表示实际扣款，二者冲突不能猜。交易金额可为优惠前金额。
        if (parsedAmounts.any { it.priority < 2 && it.fen != amountFen } ||
            parsedAmounts.filter { it.priority == 2 }.map { it.fen }.distinct().size > 1 ||
            parsedAmounts.any { it.priority == 2 && it.fen < amountFen }
        ) return rejected(PaymentRejectionReasons.AMBIGUOUS_AMOUNT)

        val merchantLabels = neutralMerchantLabels + if (kind == Kind.EXPENSE) {
            payeeLabels
        } else {
            payerLabels
        }
        val merchantFields = fields.filter { it.label in merchantLabels }
        // j9/e 的 maybeRemark 和 r9/e 的标题只能作备注，不能伪造为已确认商户。
        if (merchantFields.isEmpty() && !fallbackTemplate) return rejected(PaymentRejectionReasons.MISSING_MERCHANT)
        if (merchantFields.any { it.values.isEmpty() || it.values.any { value -> !isMerchant(value) } }) {
            return rejected(PaymentRejectionReasons.INVALID_MERCHANT)
        }
        val merchants = merchantFields.flatMap { it.values }.distinct()
        if (merchants.size > 1) return rejected(PaymentRejectionReasons.AMBIGUOUS_MERCHANT)

        val orderFields = fields.filter { it.label in orderLabels }
        if (orderFields.any { it.values.isEmpty() || it.values.any { value -> !orderPattern.matches(value) } }) {
            return rejected(PaymentRejectionReasons.INVALID_ORDER_REFERENCE)
        }
        // 平台交易单号优先于商户订单号；同类单号冲突则拒绝。
        if (orderFields.groupBy { orderLabels.getValue(it.label) }.values.any { group ->
                group.flatMap { it.values }.distinct().size > 1
            }
        ) return rejected(PaymentRejectionReasons.AMBIGUOUS_ORDER_REFERENCE)
        val order = orderFields.minByOrNull { orderLabels.getValue(it.label) }?.values?.firstOrNull()

        return PaymentRecognitionResult.Matched(
            RecognizedPayment(
                amountFen = amountFen,
                income = kind != Kind.EXPENSE,
                merchant = merchants.singleOrNull().orEmpty(),
                sourcePackage = snapshot.packageName,
                occurredAt = snapshot.capturedAt,
                ruleId = if (usedCurrencyFallback) "${rule.id}.${kind.name.lowercase()}.currency.v1" else
                    "${rule.id}.${kind.name.lowercase()}.v1",
                orderReference = order,
            ),
        )
    }

    private data class Field(val label: String, val values: List<String>, val boundNodes: Set<Int>)
    private data class LabelMatch(val label: String, val inlineValue: String)
    private data class Money(val fen: Long, val sign: Char?)
    private data class LabeledAmount(val priority: Int, val fen: Long)
    private data class AmountLabel(val kind: Kind?, val priority: Int)
    private data class Rule(val id: String, val successes: Map<String, Kind>)
    private enum class Kind { EXPENSE, INCOME, REFUND, UNSPECIFIED }

    private fun extractFields(cells: List<List<String>>, allowSuccessAmountLabels: Boolean): List<Field> = buildList {
        val valueNodes = HashSet<Int>()
        cells.forEachIndexed { index, values ->
            // 备注等字段的值即使看起来像另一个标签，也不能被再次解析成状态或金额。
            if (index in valueNodes) return@forEachIndexed
            val opaqueNode = values.any { matchLabel(it)?.label in ignoredLabels }
            for (cell in values) {
                val match = matchLabel(cell) ?: continue
                if (match.label in successAmountLabels) {
                    if (!allowSuccessAmountLabels) continue
                    // “已支付”本身也可为状态，不能把后面的任意文本强行绑定为其金额。
                    if (match.inlineValue.isEmpty() &&
                        (values.filter { it != cell } + cells.getOrNull(index + 1).orEmpty()).none { parseMoney(it) != null }
                    ) continue
                }
                if (opaqueNode && match.label !in ignoredLabels) continue
                if (match.inlineValue.isNotEmpty()) {
                    val otherValues = values.filter { it != cell && matchLabel(it) == null }
                    add(Field(match.label, (listOf(match.inlineValue) + otherValues).distinct(), setOf(index)))
                } else {
                    val sameNodeValues = values.filter { it != cell }.mapNotNull { other ->
                        val otherMatch = matchLabel(other)
                        if (otherMatch?.label == match.label) {
                            otherMatch.inlineValue.takeIf { it.isNotEmpty() }
                        } else {
                            other
                        }
                    }
                    if (sameNodeValues.isNotEmpty()) {
                        add(Field(match.label, sameNodeValues, setOf(index)))
                    } else {
                        // 不越过空节点、其他标签或中间文本寻找“看起来像金额”的数字。
                        add(Field(match.label, cells.getOrNull(index + 1).orEmpty(), setOf(index, index + 1)))
                        valueNodes += index + 1
                    }
                }
            }
        }
    }

    private fun matchLabel(value: String): LabelMatch? {
        for (label in allLabels) {
            if (!value.startsWith(label)) continue
            val suffix = value.substring(label.length)
            if (suffix.isEmpty()) return LabelMatch(label, "")
            if (suffix.first() == ':' || suffix.first().isWhitespace()) {
                return LabelMatch(label, suffix.trimStart().removePrefix(":").trim())
            }
            // r9/e:95 的组合金额允许标签后直接接币符/数字，或使用中文/英文逗号。
            if (label in joinedAmountLabels &&
                (suffix.first() in ",，¥￥+-" || suffix.first().isDigit())
            ) return LabelMatch(label, suffix.removePrefix(",").removePrefix("，").trim())
        }
        return null
    }

    private fun parseMoney(value: String): Money? {
        if (value.length > 64) return null
        val match = moneyPattern.matchEntire(value) ?: return null
        val firstSign = match.groupValues[1]
        val secondSign = match.groupValues[3]
        if (firstSign.isNotEmpty() && secondSign.isNotEmpty()) return null
        val fen = try {
            BigDecimal(match.groupValues[4].replace(",", "")).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        } catch (_: NumberFormatException) {
            return null
        }
        if (fen <= 0L) return null
        return Money(fen, (firstSign + secondSign).firstOrNull())
    }

    private fun isMerchant(value: String): Boolean = value.length in 1..100 &&
        value !in nonMerchantTexts && matchLabel(value) == null && !moneyPattern.matches(value) &&
        value.any { it.isLetter() } &&
        !value.contains('¥') && !value.contains('￥') &&
        !value.any { Character.isISOControl(it) }

    private fun rejected(reason: String) = PaymentRecognitionResult.Rejected(reason)

    private companion object {
        // 白名单按应用独立列出，不能把某应用的成功提示泛化给其他应用。
        val rules = mapOf(
            "com.eg.android.AlipayGphone" to Rule("alipay", mapOf(
                "支付成功" to Kind.EXPENSE,
                "付款成功" to Kind.EXPENSE,
                "交易成功" to Kind.UNSPECIFIED,
                "收款成功" to Kind.INCOME,
                "退款成功" to Kind.REFUND,
                "退款已到账" to Kind.REFUND,
            )),
            "com.tencent.mm" to Rule("wechat", mapOf(
                "支付成功" to Kind.EXPENSE,
                "充值成功" to Kind.EXPENSE,
                "付款成功" to Kind.EXPENSE,
                "已支付" to Kind.EXPENSE,
                "已收钱" to Kind.INCOME,
                "收款成功" to Kind.INCOME,
                "退款成功" to Kind.REFUND,
                "已退款" to Kind.REFUND,
            )),
            "cn.gov.pbc.dcep" to Rule("dcep", mapOf(
                "支付成功" to Kind.EXPENSE,
                "付款成功" to Kind.EXPENSE,
                "收款成功" to Kind.INCOME,
                "退款成功" to Kind.REFUND,
                "退款已到账" to Kind.REFUND,
            )),
            "com.unionpay" to Rule("unionpay", mapOf(
                "支付成功" to Kind.EXPENSE,
                "付款成功" to Kind.EXPENSE,
                "交易成功" to Kind.UNSPECIFIED,
                "收款成功" to Kind.INCOME,
                "退款成功" to Kind.REFUND,
                "退款已到账" to Kind.REFUND,
            )),
        )
        val statusLabels = setOf("交易状态", "支付状态", "付款状态", "订单状态", "退款状态", "收款状态", "转账状态")
        val directionLabels = setOf("收支类型", "收支方向", "交易类型")
        val directionValuesToKind = mapOf(
            "支出" to Kind.EXPENSE, "消费" to Kind.EXPENSE, "付款" to Kind.EXPENSE,
            "收入" to Kind.INCOME, "收款" to Kind.INCOME, "退款" to Kind.REFUND,
        )
        val successAmountLabels = setOf("支出", "收入", "已支付")
        val joinedAmountLabels = successAmountLabels + setOf("实付金额", "实际支付")
        val amountLabels = mapOf(
            "支出" to AmountLabel(Kind.EXPENSE, 1),
            "收入" to AmountLabel(Kind.INCOME, 1),
            "已支付" to AmountLabel(Kind.EXPENSE, 1),
            "实付" to AmountLabel(Kind.EXPENSE, 0),
            "实付金额" to AmountLabel(Kind.EXPENSE, 0),
            "实际支付" to AmountLabel(Kind.EXPENSE, 0),
            "实际支付金额" to AmountLabel(Kind.EXPENSE, 0),
            "付款金额" to AmountLabel(Kind.EXPENSE, 1),
            "支付金额" to AmountLabel(Kind.EXPENSE, 1),
            "实收金额" to AmountLabel(Kind.INCOME, 0),
            "到账金额" to AmountLabel(Kind.INCOME, 0),
            "收款金额" to AmountLabel(Kind.INCOME, 1),
            "退款金额" to AmountLabel(Kind.REFUND, 0),
            "退回金额" to AmountLabel(Kind.REFUND, 0),
            "交易金额" to AmountLabel(null, 2),
        )
        val neutralMerchantLabels = setOf("商户名称", "商家名称", "商户", "商家", "交易对方")
        val payeeLabels = setOf("收款方", "收款人", "收款商户", "对方户名")
        val payerLabels = setOf("付款方", "付款人", "付款商户", "退款方")
        val orderLabels = mapOf(
            "交易单号" to 0, "交易号" to 0, "支付单号" to 0,
            "订单号" to 1, "订单编号" to 1,
            "商户单号" to 2, "商户订单号" to 2,
        )
        val ignoredLabels = setOf(
            "余额", "账户余额", "可用余额", "优惠", "优惠金额", "优惠券", "优惠券金额",
            "红包", "红包抵扣", "立减", "积分", "手续费", "服务费", "原价", "原金额", "原支付金额",
            "订单金额", "商品金额", "商品总额", "支付时间", "付款时间", "交易时间", "创建时间",
            "订单时间", "完成时间", "退款时间", "备注", "商品说明", "商品", "商品名称", "支付方式", "付款方式",
        )
        val allLabels = (statusLabels + directionLabels + amountLabels.keys + neutralMerchantLabels +
            payeeLabels + payerLabels + orderLabels.keys + ignoredLabels).sortedByDescending { it.length }
        val blockedStatuses = setOf(
            "支付失败", "付款失败", "交易失败", "交易关闭", "订单关闭", "交易取消", "已取消",
            "支付中", "付款中", "处理中", "待支付", "待付款", "等待付款", "待收款", "等待收款",
            "等待对方收款", "转账中", "收款失败", "退款中", "退款处理中", "退款申请中", "退款失败",
        )
        val nonMerchantTexts = rules.values.flatMap { it.successes.keys }.toSet() + blockedStatuses +
            setOf("已完成", "成功", "完成", "账单详情", "交易详情", "订单详情", "查看账单", "收入", "支出")
        val moneyPattern = Regex(
            "^([+-]?) *(?:(¥|￥|CNY|RMB|人民币) *)?([+-]?) *" +
                "((?:0|[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?) *(元)?$",
        )
        val currencyAmountPattern = Regex(
            "^[+-]? *[¥￥] *[+-]? *(?:0|[1-9][0-9]{0,8}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})" +
                "(?:\\.[0-9]{1,2})? *(?:元)?$",
        )
        val combinedSuccessPattern = Regex("^支付成功(￥(?:0|[1-9][0-9]{0,8})\\.[0-9]{1,2})$")
        val numericLookingPattern = Regex("^[+-]?[0-9][0-9 ,.:/eE+-]*(?:元)?$")
        val fallbackInterference = Regex("订单|单号|交易号|余额|优惠|红包|积分|立减|折扣|抵扣|原价|手续费|服务费|时间|日期|人民币|CNY|RMB|美元|港币|日元|欧元|英镑|USD|HKD|JPY|EUR|GBP|[$€£]", RegexOption.IGNORE_CASE)
        val orderPattern = Regex("[A-Za-z0-9][A-Za-z0-9_-]{3,95}")
    }
}

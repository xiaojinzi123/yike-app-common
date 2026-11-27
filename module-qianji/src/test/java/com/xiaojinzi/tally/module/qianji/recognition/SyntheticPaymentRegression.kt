package com.xiaojinzi.tally.module.qianji.recognition

/**
 * 所有页面均为手工合成 fixtures，不是支付宝/微信/数字人民币/云闪付的实盘捕获。
 * JUnit4 与下面的 main 共用断言；main 只依赖 Kotlin/JVM，无需 JUnit 或 Android。
 */
object SyntheticPaymentRegression {
    private const val ALIPAY = "com.eg.android.AlipayGphone"
    private const val WECHAT = "com.tencent.mm"
    private const val DCEP = "cn.gov.pbc.dcep"
    private const val UNIONPAY = "com.unionpay"
    private const val CAPTURED_AT = 1_800_000_000_123L

    sealed class Expected {
        data class Payment(
            val fen: Long,
            val income: Boolean = false,
            val merchant: String = "合成咖啡店",
            val order: String? = null,
            val kind: String = if (income) "income" else "expense",
            val currencyFallback: Boolean = false,
        ) : Expected()
        data class Rejection(val reason: String) : Expected()
    }

    data class RecognitionCase(val name: String, val snapshot: PaymentSnapshot, val expected: Expected) {
        override fun toString(): String = name
    }

    data class GateCase(val name: String, val verify: () -> Unit) {
        override fun toString(): String = name
    }

    private fun snapshot(pkg: String = ALIPAY, vararg texts: String): PaymentSnapshot = PaymentSnapshot(
        packageName = pkg,
        nodes = texts.map { PaymentNode(it) },
        capturedAt = CAPTURED_AT,
    )

    private fun payPage(
        amount: String = "12.30",
        pkg: String = ALIPAY,
        status: String = "支付成功",
        vararg extra: String,
    ): PaymentSnapshot = snapshot(pkg, status, "实付金额", amount, "收款方", "合成咖啡店", *extra)

    private fun accepted(name: String, page: PaymentSnapshot, expected: Expected.Payment) =
        RecognitionCase(name, page, expected)

    private fun rejected(name: String, page: PaymentSnapshot, reason: String) =
        RecognitionCase(name, page, Expected.Rejection(reason))

    val recognitionCases: List<RecognitionCase> = listOf(
        accepted("支付宝明确实付，收款方不导致收入", payPage(), Expected.Payment(1230)),
        accepted(
            "微信描述节点中的已支付账单",
            PaymentSnapshot(WECHAT, nodes = listOf(
                PaymentNode("账单详情"), PaymentNode("交易状态", description = "交易状态"),
                PaymentNode("", description = "已支付"), PaymentNode("付款金额：￥ 28.80"),
                PaymentNode("商户名称", description = "商户名称"), PaymentNode("合成咖啡店"),
            ), capturedAt = CAPTURED_AT), Expected.Payment(2880),
        ),
        accepted(
            "数字人民币独立成功标志和人民币后缀",
            snapshot(DCEP, "付款成功", "支付金额", "人民币 19.80 元", "商户名称", "合成咖啡店"),
            Expected.Payment(1980),
        ),
        accepted(
            "云闪付交易成功由支出方向消除歧义",
            snapshot(UNIONPAY, "交易状态：交易成功", "收支类型：支出", "交易金额：8.00", "商户：合成咖啡店"),
            Expected.Payment(800),
        ),
        accepted(
            "实付优先于优惠前交易金额，不读取余额优惠积分订单数字",
            snapshot(ALIPAY, "支付成功", "交易金额", "100.00", "优惠金额", "12.00", "实付", "88.00",
                "余额", "999999.99", "积分", "9000", "订单号", "202611141234567890123456",
                "交易时间", "2026-11-14 12:34:56", "收款方", "合成咖啡店"),
            Expected.Payment(8800, order = "202611141234567890123456"),
        ),
        accepted(
            "精确分值避免浮点误差",
            payPage("0.29"), Expected.Payment(29),
        ),
        accepted("最小正金额一分", payPage("0.01"), Expected.Payment(1)),
        accepted("合法千分位", payPage("￥1,234.56"), Expected.Payment(123456)),
        accepted("整数金额", payPage("12"), Expected.Payment(1200)),
        accepted("负号支出不翻转方向", payPage("-￥12.30"), Expected.Payment(1230)),
        accepted(
            "微信已收钱配付款方才是收入",
            snapshot(WECHAT, "已收钱", "收款金额", "+￥6.60", "付款方", "合成付款人"),
            Expected.Payment(660, income = true, merchant = "合成付款人"),
        ),
        accepted(
            "已完成退款使用退款金额且忽略原支付金额",
            snapshot(ALIPAY, "退款成功", "退款金额", "3.50", "原支付金额", "50.00", "退款方", "合成咖啡店"),
            Expected.Payment(350, income = true, kind = "refund"),
        ),
        accepted(
            "重复可访问性标签及相同候选不产生假歧义",
            PaymentSnapshot(ALIPAY, nodes = listOf(
                PaymentNode("支付成功", description = "支付成功"),
                PaymentNode("实付金额", description = "实付金额"), PaymentNode("12.30", description = "12.30"),
                PaymentNode("付款金额：12.30"), PaymentNode("商户：合成咖啡店"),
            ), capturedAt = CAPTURED_AT), Expected.Payment(1230),
        ),
        accepted(
            "单节点多行标签值",
            snapshot(ALIPAY, "支付成功", "实付金额\n12.30", "收款方\n合成咖啡店"),
            Expected.Payment(1230),
        ),
        accepted("Long分值上界", payPage("92233720368547758.07"), Expected.Payment(Long.MAX_VALUE)),
        accepted(
            "平台单号和商户单号分层提取",
            payPage("12.30", ALIPAY, "支付成功", "交易单号", "ALI202611140001", "商户订单号", "SHOP202611140002"),
            Expected.Payment(1230, order = "ALI202611140001"),
        ),
        // 以下是依据四个原版 Java 规则构造的合成模板，并非真实页面捕获。
        accepted(
            "支付宝精确成功和独立币符金额允许空商户",
            snapshot(ALIPAY, "支付成功", "￥12.30"),
            Expected.Payment(1230, merchant = "", currencyFallback = true),
        ),
        accepted(
            "金额前文字仅为maybeRemark不冒充商户",
            snapshot(ALIPAY, "合成咖啡店", "￥12.30", "支付成功"),
            Expected.Payment(1230, merchant = "", currencyFallback = true),
        ),
        accepted(
            "微信充值成功使用唯一币符金额",
            snapshot(WECHAT, "充值成功", "¥15.00"),
            Expected.Payment(1500, merchant = "", currencyFallback = true),
        ),
        accepted(
            "币符候选也可来自独立description",
            PaymentSnapshot(WECHAT, nodes = listOf(
                PaymentNode("", description = "支付成功"), PaymentNode("", description = "￥0.10"),
            ), capturedAt = CAPTURED_AT), Expected.Payment(10, merchant = "", currencyFallback = true),
        ),
        accepted(
            "支付宝合并成功金额节点还须完成按钮",
            snapshot(ALIPAY, "支付成功￥12.30", "完成"),
            Expected.Payment(1230, merchant = "", currencyFallback = true),
        ),
        accepted(
            "同分值多个币符候选及裸金额镜像可合并",
            snapshot(ALIPAY, "支付成功", "￥12.30", "¥12.3", "12.30"),
            Expected.Payment(1230, merchant = "", currencyFallback = true),
        ),
        accepted(
            "微信实际支付组合不要求冒号",
            snapshot(WECHAT, "支付成功", "实际支付￥12.30"), Expected.Payment(1230, merchant = ""),
        ),
        accepted(
            "组合实付支持中文逗号和合法千分位",
            snapshot(WECHAT, "支付成功", "实付金额，￥1,234.56"), Expected.Payment(123456, merchant = ""),
        ),
        accepted(
            "成功页支出组合是明确金额标签",
            snapshot(WECHAT, "支付成功", "支出12.30元"), Expected.Payment(1230, merchant = ""),
        ),
        accepted(
            "收入组合仍须独立收入成功证据",
            snapshot(WECHAT, "已收钱", "收入￥8.00", "付款方：合成付款人"),
            Expected.Payment(800, income = true, merchant = "合成付款人"),
        ),
        accepted(
            "支付宝已支付组合金额",
            snapshot(ALIPAY, "支付成功", "已支付￥12.30"), Expected.Payment(1230, merchant = ""),
        ),
        accepted(
            "有明确实付时不会被余额订单数字替代，商户可空",
            snapshot(ALIPAY, "支付成功", "实际支付", "12.30", "余额", "999.00", "订单号", "ALIPAY202611140001"),
            Expected.Payment(1230, merchant = "", order = "ALIPAY202611140001"),
        ),
        accepted(
            "明确实付优先于交易金额和独立展示的原价金额",
            snapshot(WECHAT, "支付成功", "实付金额：8.00", "交易金额：10.00", "优惠金额：2.00", "￥10.00"),
            Expected.Payment(800, merchant = ""),
        ),
        accepted(
            "微信下单排除规则不泛化给支付宝",
            snapshot(ALIPAY, "支付成功", "￥12.30", "去下单"),
            Expected.Payment(1230, merchant = "", currencyFallback = true),
        ),
        rejected(
            "币符候选不同金额不能取max",
            snapshot(ALIPAY, "支付成功", "￥12.30", "￥999.00"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "币符金额与CNY文字金额不同仍拒绝",
            snapshot(ALIPAY, "支付成功", "￥12.30", "CNY 99.00"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "币符兜底出现外币标记不能假定人民币",
            snapshot(ALIPAY, "支付成功", "￥12.30", "JPY"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "同金额裸正号与付款方向冲突仍拒绝",
            snapshot(ALIPAY, "支付成功", "￥12.30", "+12.30"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected(
            "独立币符text和description金额冲突拒绝",
            PaymentSnapshot(ALIPAY, nodes = listOf(
                PaymentNode("支付成功"), PaymentNode("￥12.30", description = "￥99.00"),
            ), capturedAt = CAPTURED_AT), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "币符金额旁另一个裸数字只能否决不能猜选",
            snapshot(ALIPAY, "支付成功", "￥12.30", "202611140001"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "币符金额旁独立时间增加歧义",
            snapshot(ALIPAY, "支付成功", "￥12.30", "12:30"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "无标签兜底存在订单号字段仍拒绝",
            snapshot(ALIPAY, "支付成功", "￥12.30", "订单号", "ALIPAY202611140001"),
            PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "无分隔符订单文字也会阻止无标签兜底",
            snapshot(WECHAT, "支付成功", "￥12.30", "订单编号202611140001"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "无标签兜底与优惠字段共存拒绝",
            snapshot(ALIPAY, "支付成功", "￥12.30", "优惠金额", "￥2.00"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "无标签兜底与余额字段共存拒绝",
            snapshot(WECHAT, "支付成功", "￥12.30", "余额：999.00"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "只有优惠金额不能作为币符兜底",
            snapshot(ALIPAY, "支付成功", "优惠金额", "￥12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "只有备注里的币符金额不能作为兜底",
            snapshot(ALIPAY, "支付成功", "备注", "￥12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "缺TextView结构时支付宝裸两位小数仍拒绝",
            snapshot(ALIPAY, "支付成功", "12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "微信裸数字同样不能代替币符或明确标签",
            snapshot(WECHAT, "支付成功", "12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "支付宝付款成功不擅自扩展支付成功兜底模板",
            snapshot(ALIPAY, "付款成功", "￥12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "数字人民币未提供原版依据不扩展兜底",
            snapshot(DCEP, "支付成功", "￥12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "云闪付未提供原版依据不扩展兜底",
            snapshot(UNIONPAY, "支付成功", "￥12.30"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "微信首页点单组合即使有明确实付也排除",
            snapshot(WECHAT, "首页", "点单", "支付成功", "实付金额：12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.UNSUPPORTED_TEMPLATE,
        ),
        rejected(
            "微信支付成功去下单组合排除",
            snapshot(WECHAT, "支付成功", "￥12.30", "去下单"), PaymentRejectionReasons.UNSUPPORTED_TEMPLATE,
        ),
        rejected(
            "微信充值成功首页点单组合同样排除",
            snapshot(WECHAT, "充值成功", "￥12.30", "首页", "点单"), PaymentRejectionReasons.UNSUPPORTED_TEMPLATE,
        ),
        rejected(
            "支付宝合并模板缺少完成节点拒绝",
            snapshot(ALIPAY, "支付成功￥12.30"), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "合并模板也不接受三位小数",
            snapshot(ALIPAY, "支付成功￥12.301", "完成"), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "独立币符模板超过两位小数拒绝",
            snapshot(ALIPAY, "支付成功", "￥12.301"), PaymentRejectionReasons.INVALID_AMOUNT,
        ),
        rejected(
            "币符模板长数字超出原版九位整数范围拒绝",
            snapshot(ALIPAY, "支付成功", "￥202611140001"), PaymentRejectionReasons.INVALID_AMOUNT,
        ),
        rejected(
            "金额标签值错误不能改用旁边币符救回",
            snapshot(ALIPAY, "支付成功", "实付金额：12.301", "￥12.30"), PaymentRejectionReasons.INVALID_AMOUNT,
        ),
        rejected(
            "币符兜底也不能放过损坏的商户标签",
            snapshot(ALIPAY, "支付成功", "￥12.30", "商户"), PaymentRejectionReasons.INVALID_MERCHANT,
        ),
        rejected(
            "微信支付成功与收入组合冲突仍拒绝",
            snapshot(WECHAT, "支付成功", "收入￥12.30"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected("非监测包拒绝，即使页面完全相同", payPage(pkg = "com.example.lab"), PaymentRejectionReasons.UNSUPPORTED_PACKAGE),
        rejected("包名前缀伪装拒绝", payPage(pkg = "$ALIPAY.fake"), PaymentRejectionReasons.UNSUPPORTED_PACKAGE),
        rejected("泛成功字样拒绝", payPage(status = "成功"), PaymentRejectionReasons.MISSING_SUCCESS),
        rejected("已完成不证明支付成功", payPage(status = "已完成"), PaymentRejectionReasons.MISSING_SUCCESS),
        rejected("成功提示的包含匹配拒绝", payPage(status = "支付成功后可查看订单"), PaymentRejectionReasons.MISSING_SUCCESS),
        rejected(
            "仅窗口类名和viewId包含成功仍拒绝",
            PaymentSnapshot(ALIPAY, windowClass = "PaymentSuccessActivity", nodes = listOf(
                PaymentNode("", viewId = "payment_success"), PaymentNode("实付金额：12.30"), PaymentNode("商户：合成咖啡店"),
            ), capturedAt = CAPTURED_AT), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected("微信的已支付不泛化给支付宝", payPage(status = "已支付"), PaymentRejectionReasons.MISSING_SUCCESS),
        rejected(
            "账单标题和已完成仍不足",
            snapshot(ALIPAY, "账单详情", "交易状态：已完成", "收支类型：支出", "交易金额：12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "通用交易成功仍须证明方向",
            snapshot(ALIPAY, "交易成功", "交易金额：12.30", "收款方：合成咖啡店"),
            PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected(
            "商户文本不能伪造成功证据",
            snapshot(ALIPAY, "商户名称", "支付成功", "实付金额", "12.30"), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "备注不能伪造成功证据",
            snapshot(ALIPAY, "备注", "支付成功", "实付金额", "12.30", "商户", "合成咖啡店"), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "备注中的标签样式成功文字不再次解析",
            snapshot(ALIPAY, "备注", "交易状态：支付成功", "实付金额：12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "同一备注节点内的标签样式成功文字不再次解析",
            snapshot(ALIPAY, "备注\n交易状态：支付成功", "实付金额：12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "失败状态与成功标题并存拒绝",
            payPage("12.30", ALIPAY, "支付成功", "交易状态：支付失败"), PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "已出现但未知的状态不能由成功标题掩盖",
            payPage("12.30", ALIPAY, "支付成功", "交易状态：银行处理中（稍后更新）"),
            PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "状态标签缺值拒绝",
            payPage("12.30", ALIPAY, "支付成功", "交易状态"), PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "退款状态不能使用付款成功证据",
            snapshot(ALIPAY, "退款状态：支付成功", "付款金额：12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "方向标签缺值拒绝",
            payPage("12.30", ALIPAY, "支付成功", "收支类型"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected(
            "待收款不证明转账到账",
            snapshot(WECHAT, "转账成功", "转账状态：等待对方收款", "付款金额：12.30", "收款方：合成咖啡店"),
            PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "仅原订单成功不能确认退款",
            snapshot(ALIPAY, "交易成功", "退款金额", "3.50", "商户", "合成咖啡店"), PaymentRejectionReasons.MISSING_SUCCESS,
        ),
        rejected(
            "退款处理中拒绝，即便原支付成功仍可见",
            snapshot(ALIPAY, "支付成功", "退款状态：退款处理中", "退款金额：3.50", "商户：合成咖啡店"),
            PaymentRejectionReasons.CONFLICTING_STATUS,
        ),
        rejected(
            "成功退款缺少退款金额不读取原实付",
            snapshot(ALIPAY, "退款成功", "实付金额：50.00", "退款方：合成咖啡店"), PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "支出成功和收入方向冲突",
            payPage("12.30", ALIPAY, "支付成功", "收支类型：收入"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected(
            "收款成功与付款金额冲突",
            snapshot(ALIPAY, "收款成功", "付款金额：12.30", "付款方：合成付款人"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        rejected(
            "不从多个无标签数字中选最大",
            snapshot(ALIPAY, "支付成功", "12.30", "99999.00", "20261114", "收款方：合成咖啡店"),
            PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "余额优惠和订单号不能充当交易金额",
            snapshot(ALIPAY, "支付成功", "余额", "999.00", "优惠金额", "10.00", "订单号", "202611140001", "商户", "合成咖啡店"),
            PaymentRejectionReasons.MISSING_AMOUNT,
        ),
        rejected(
            "相同优先级不同金额拒绝",
            payPage("12.30", ALIPAY, "支付成功", "实付金额：25.60"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "实付和付款金额冲突拒绝",
            payPage("12.30", ALIPAY, "支付成功", "付款金额：25.60"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "多个交易金额仍属歧义",
            snapshot(ALIPAY, "支付成功", "交易金额：12.30", "交易金额：25.60", "商户：合成咖啡店"),
            PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "实付高于交易金额拒绝",
            payPage("12.30", ALIPAY, "支付成功", "交易金额：10.00"), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "不越过优惠标签抓取后续数字",
            snapshot(ALIPAY, "支付成功", "实付金额", "优惠金额", "2.00", "12.30", "商户：合成咖啡店"),
            PaymentRejectionReasons.INVALID_AMOUNT,
        ),
        rejected(
            "不越过空节点猜相邻值",
            snapshot(ALIPAY, "支付成功", "实付金额", "", "12.30", "商户：合成咖啡店"), PaymentRejectionReasons.INVALID_AMOUNT,
        ),
        rejected("两金额拼接不可提取局部", payPage("12.30 / 25.60"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("金额混有优惠不可取首数字", payPage("12.30 已优惠2.00"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("三位小数不可四舍五入", payPage("1.001"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("零金额拒绝", payPage("0.00"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("非法千分位拒绝", payPage("1,23.45"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("科学计数法拒绝", payPage("1e3"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("非人民币币种拒绝", payPage("USD 12.30"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("时间不可作金额", payPage("12:30"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("日期不可作金额", payPage("2026-11-14"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("重复正负号拒绝", payPage("-￥-12.30"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("金额分值溢出拒绝", payPage("92233720368547758.08"), PaymentRejectionReasons.INVALID_AMOUNT),
        rejected("正号和支出冲突拒绝", payPage("+12.30"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION),
        rejected(
            "收入负号冲突拒绝",
            snapshot(WECHAT, "已收钱", "收款金额：-6.60", "付款方：合成付款人"), PaymentRejectionReasons.AMBIGUOUS_DIRECTION,
        ),
        accepted(
            "成功模板缺商户标签时留空而不猜第一个词",
            snapshot(ALIPAY, "支付成功", "合成咖啡店", "实付金额：12.30"), Expected.Payment(1230, merchant = ""),
        ),
        rejected(
            "商户标签后金额不是商户",
            snapshot(ALIPAY, "支付成功", "实付金额：12.30", "收款方", "12.30"), PaymentRejectionReasons.INVALID_MERCHANT,
        ),
        rejected(
            "含币种文字的金额也不能冒充商户",
            snapshot(ALIPAY, "支付成功", "实付金额：12.30", "收款方", "CNY 12.30"), PaymentRejectionReasons.INVALID_MERCHANT,
        ),
        rejected(
            "两个商户冲突拒绝",
            payPage("12.30", ALIPAY, "支付成功", "商户：另一合成店"), PaymentRejectionReasons.AMBIGUOUS_MERCHANT,
        ),
        rejected(
            "收入页不能把收款方自己当交易对方",
            snapshot(WECHAT, "已收钱", "收款金额：6.60", "收款方：合成本人"), PaymentRejectionReasons.MISSING_MERCHANT,
        ),
        rejected(
            "两个订单号冲突拒绝",
            payPage("12.30", ALIPAY, "支付成功", "订单号：A202611140001", "订单号：A202611140002"),
            PaymentRejectionReasons.AMBIGUOUS_ORDER_REFERENCE,
        ),
        rejected(
            "已出现但被截断的订单号拒绝",
            payPage("12.30", ALIPAY, "支付成功", "订单号：2026****0001"), PaymentRejectionReasons.INVALID_ORDER_REFERENCE,
        ),
        rejected(
            "text和description金额冲突拒绝",
            PaymentSnapshot(ALIPAY, nodes = listOf(
                PaymentNode("支付成功"), PaymentNode("实付金额"), PaymentNode("12.30", description = "25.60"),
                PaymentNode("商户：合成咖啡店"),
            ), capturedAt = CAPTURED_AT), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        rejected(
            "inline金额和同节点description冲突拒绝",
            PaymentSnapshot(ALIPAY, nodes = listOf(
                PaymentNode("支付成功"), PaymentNode("实付金额：12.30", description = "25.60"),
                PaymentNode("商户：合成咖啡店"),
            ), capturedAt = CAPTURED_AT), PaymentRejectionReasons.AMBIGUOUS_AMOUNT,
        ),
        accepted(
            "inline金额和同节点相同description可以去重",
            PaymentSnapshot(ALIPAY, nodes = listOf(
                PaymentNode("支付成功"), PaymentNode("实付金额：12.30", description = "12.30"),
                PaymentNode("商户：合成咖啡店"),
            ), capturedAt = CAPTURED_AT), Expected.Payment(1230),
        ),
        rejected("空快照拒绝", snapshot(ALIPAY), PaymentRejectionReasons.INVALID_SNAPSHOT),
        rejected("负的捕获时刻拒绝", payPage().copy(capturedAt = -1), PaymentRejectionReasons.INVALID_SNAPSHOT),
        rejected(
            "过量节点拒绝而非只截取部分猜测",
            payPage().copy(nodes = payPage().nodes + List(513) { PaymentNode("填充") }), PaymentRejectionReasons.INVALID_SNAPSHOT,
        ),
        rejected(
            "过长节点拒绝",
            payPage().copy(nodes = payPage().nodes + PaymentNode("x".repeat(4097))), PaymentRejectionReasons.INVALID_SNAPSHOT,
        ),
    )

    fun verify(case: RecognitionCase) {
        val actual = PaymentRecognizer().recognize(case.snapshot)
        when (val expected = case.expected) {
            is Expected.Payment -> {
                check(actual is PaymentRecognitionResult.Matched) { "${case.name}: expected matched, got $actual" }
                val payment = actual.payment
                check(payment.amountFen == expected.fen) { "${case.name}: amount ${payment.amountFen} != ${expected.fen}" }
                check(payment.income == expected.income) { "${case.name}: wrong direction" }
                check(payment.merchant == expected.merchant) { "${case.name}: wrong merchant ${payment.merchant}" }
                check(payment.orderReference == expected.order) { "${case.name}: wrong order ${payment.orderReference}" }
                check(payment.sourcePackage == case.snapshot.packageName) { "${case.name}: source changed" }
                check(payment.occurredAt == case.snapshot.capturedAt) { "${case.name}: capturedAt changed" }
                val appId = mapOf(ALIPAY to "alipay", WECHAT to "wechat", DCEP to "dcep", UNIONPAY to "unionpay")
                val ruleSuffix = if (expected.currencyFallback) ".currency.v1" else ".v1"
                check(payment.ruleId == "${appId.getValue(case.snapshot.packageName)}.${expected.kind}$ruleSuffix") {
                    "${case.name}: wrong rule ${payment.ruleId}"
                }
            }
            is Expected.Rejection -> check(actual == PaymentRecognitionResult.Rejected(expected.reason)) {
                "${case.name}: expected ${expected.reason}, got $actual"
            }
        }
    }

    private fun payment(order: String? = null): RecognizedPayment = RecognizedPayment(
        amountFen = 1230, income = false, merchant = "合成咖啡店", sourcePackage = ALIPAY,
        occurredAt = CAPTURED_AT, ruleId = "alipay.expense.v1", orderReference = order,
    )

    private fun expectIllegalArgument(action: () -> Unit) {
        try {
            action()
        } catch (_: IllegalArgumentException) {
            return
        }
        error("Expected IllegalArgumentException")
    }

    val gateCases: List<GateCase> = listOf(
        GateCase("首次放行且同一时刻重复被防抖") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment(), 0))
            check(gate.evaluate(payment(), 0) == DuplicatePaymentGate.Decision.DEBOUNCED)
            check(gate.size == 1)
        },
        GateCase("跨1.5秒时间桶不能绕过去重") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment(), 1499))
            check(!gate.tryAcquire(payment().copy(occurredAt = CAPTURED_AT + 2), 1501))
            check(gate.evaluate(payment(), 3001) == DuplicatePaymentGate.Decision.DUPLICATE)
        },
        GateCase("持续事件超过初次TTL仍不重复，停止后才过期") {
            val gate = DuplicatePaymentGate(debounceMillis = 100, ttlMillis = 5000)
            check(gate.tryAcquire(payment(), 0))
            for (time in 2000L..20000L step 2000L) check(!gate.tryAcquire(payment(), time))
            check(gate.tryAcquire(payment(), 25000))
        },
        GateCase("TTL边界从最后观察计算") {
            val gate = DuplicatePaymentGate(debounceMillis = 100, ttlMillis = 5000)
            check(gate.tryAcquire(payment(), 0))
            check(!gate.tryAcquire(payment(), 4999))
            check(!gate.tryAcquire(payment(), 5000))
            check(gate.tryAcquire(payment(), 10000))
        },
        GateCase("指纹不依赖墙钟规则或商户边界空白") {
            val first = payment().copy(merchant = " 合成 咖啡店 ")
            val second = first.copy(merchant = "合成\u00a0咖啡店", occurredAt = 1, ruleId = "other.v2")
            check(DuplicatePaymentGate.fingerprint(first) == DuplicatePaymentGate.fingerprint(second))
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(first, 100))
            check(!gate.tryAcquire(second, 200))
        },
        GateCase("空商户识别结果可去重且后续商户补全不重放") {
            val gate = DuplicatePaymentGate()
            val empty = payment().copy(merchant = "")
            check(gate.tryAcquire(empty, 0))
            check(!gate.tryAcquire(empty.copy(merchant = "合成咖啡店"), 2000))
            check(!gate.tryAcquire(empty, 3000))
            check(gate.size == 1)
        },
        GateCase("缺商户时明确不同订单仍可分别放行") {
            val gate = DuplicatePaymentGate()
            val first = payment("ORDER-1").copy(merchant = "")
            val second = payment("ORDER-2").copy(merchant = "")
            check(gate.tryAcquire(first, 0))
            check(gate.tryAcquire(second, 1))
            check(!gate.tryAcquire(first, 2))
            check(gate.size == 2)
        },
        GateCase("不同金额方向商户来源分别放行") {
            val base = payment()
            val variants = listOf(base, base.copy(amountFen = 1231), base.copy(income = true),
                base.copy(merchant = "另一合成店"), base.copy(sourcePackage = WECHAT))
            val gate = DuplicatePaymentGate()
            variants.forEachIndexed { index, value -> check(gate.tryAcquire(value, index.toLong())) }
            check(variants.map(DuplicatePaymentGate::fingerprint).distinct().size == variants.size)
        },
        GateCase("同额同商户但明确不同订单分别放行") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment("ORDER-1"), 0))
            check(gate.tryAcquire(payment("ORDER-2"), 1))
            check(!gate.tryAcquire(payment("ORDER-1"), 2))
            check(gate.size == 2)
        },
        GateCase("订单号晚出现合并为原事件") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment(), 0))
            check(!gate.tryAcquire(payment("ORDER-1"), 1))
            check(!gate.tryAcquire(payment("ORDER-1"), 2000))
            check(gate.tryAcquire(payment("ORDER-2"), 2001))
            check(gate.size == 2)
        },
        GateCase("已知订单号暂时消失也不能再次放行") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment("ORDER-1"), 0))
            check(!gate.tryAcquire(payment(), 2000))
            check(!gate.tryAcquire(payment("ORDER-1"), 4000))
            check(gate.size == 1)
        },
        GateCase("无订单号刷新所有兼容记录而不增大缓存") {
            val gate = DuplicatePaymentGate(debounceMillis = 100, ttlMillis = 5000)
            check(gate.tryAcquire(payment("ORDER-1"), 0))
            check(gate.tryAcquire(payment("ORDER-2"), 1))
            check(!gate.tryAcquire(payment(), 4999))
            check(!gate.tryAcquire(payment("ORDER-1"), 5001))
            check(!gate.tryAcquire(payment("ORDER-2"), 5002))
            check(gate.size == 2)
        },
        GateCase("LRU重复访问保护活跃页且容量始终有界") {
            val gate = DuplicatePaymentGate(maxEntries = 2)
            check(gate.tryAcquire(payment("ORDER-A"), 0))
            check(gate.tryAcquire(payment("ORDER-B"), 1))
            check(!gate.tryAcquire(payment("ORDER-A"), 2))
            check(gate.tryAcquire(payment("ORDER-C"), 3))
            check(gate.size == 2)
            check(!gate.tryAcquire(payment("ORDER-A"), 4))
            check(gate.tryAcquire(payment("ORDER-B"), 5))
            check(gate.size == 2)
        },
        GateCase("过期记录在新交易到达时清理") {
            val gate = DuplicatePaymentGate(debounceMillis = 0, ttlMillis = 100)
            check(gate.tryAcquire(payment("ORDER-A"), 0))
            check(gate.tryAcquire(payment("ORDER-B"), 1))
            check(gate.tryAcquire(payment("ORDER-C"), 101))
            check(gate.size == 1)
        },
        GateCase("单调时钟倒退或负值报错且不改变缓存") {
            val gate = DuplicatePaymentGate()
            check(gate.tryAcquire(payment(), 100))
            expectIllegalArgument { gate.tryAcquire(payment(), 99) }
            expectIllegalArgument { gate.tryAcquire(payment(), -1) }
            check(!gate.tryAcquire(payment(), 101))
            check(gate.size == 1)
        },
        GateCase("时钟接近Long上界不发生加法溢出") {
            val gate = DuplicatePaymentGate(debounceMillis = 0, ttlMillis = 10)
            check(gate.tryAcquire(payment(), Long.MAX_VALUE - 10))
            check(gate.tryAcquire(payment(), Long.MAX_VALUE))
        },
        GateCase("非法配置及非法付款不污染门状态") {
            expectIllegalArgument { DuplicatePaymentGate(debounceMillis = -1) }
            expectIllegalArgument { DuplicatePaymentGate(ttlMillis = 0) }
            expectIllegalArgument { DuplicatePaymentGate(ttlMillis = 1) }
            expectIllegalArgument { DuplicatePaymentGate(maxEntries = 0) }
            val gate = DuplicatePaymentGate()
            expectIllegalArgument { gate.tryAcquire(payment().copy(amountFen = 0), 100) }
            expectIllegalArgument { gate.tryAcquire(payment().copy(amountFen = -1), 100) }
            check(gate.tryAcquire(payment(), 0))
        },
        GateCase("指纹长度编码避免字段分隔符碰撞") {
            val a = payment("B|C").copy(merchant = "合成|A")
            val b = payment("A|B|C").copy(merchant = "合成")
            check(DuplicatePaymentGate.fingerprint(a) != DuplicatePaymentGate.fingerprint(b))
            check(DuplicatePaymentGate.fingerprint(a).matches(Regex("[0-9a-f]{64}")))
            check(DuplicatePaymentGate.fingerprint(payment()) == DuplicatePaymentGate.fingerprint(payment("  ")))
        },
        GateCase("并发同一交易只放行一次") {
            val gate = DuplicatePaymentGate()
            val start = java.util.concurrent.CountDownLatch(1)
            val accepted = java.util.concurrent.atomic.AtomicInteger()
            val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
            val threads = List(12) {
                Thread {
                    try {
                        start.await()
                        if (gate.tryAcquire(payment(), 0)) accepted.incrementAndGet()
                    } catch (error: Throwable) {
                        failures.add(error)
                    }
                }.apply { start() }
            }
            start.countDown()
            threads.forEach { it.join(5000) }
            check(threads.none { it.isAlive })
            check(failures.isEmpty()) { failures.toString() }
            check(accepted.get() == 1)
        },
    )

    @JvmStatic
    fun main(args: Array<String>) {
        recognitionCases.forEach(::verify)
        gateCases.forEach { case ->
            try {
                case.verify()
            } catch (failure: Throwable) {
                throw AssertionError(case.name, failure)
            }
        }
        println("PASS: ${recognitionCases.size} synthetic recognition cases + ${gateCases.size} duplicate gate cases; no live captures")
    }
}

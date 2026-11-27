package com.xiaojinzi.tally.module.main.module.main.bill.view

import com.xiaojinzi.tally.module.main.module.main.bill.domain.CategoryBudgetProgress
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal fun formatBillAmount(fen: Long?): String {
    if (fen == null) return "—"
    return NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(fen, 2))
}

/** 隐藏模式先截断敏感数据，界面不能再根据金额推断预算状态。 */
internal data class BillBudgetSummaryVo(
    val amountText: String,
    val detailText: String,
    val progress: Float? = null,
    val isOverBudget: Boolean = false,
    val categoryStatus: String? = null,
) {
    companion object {
        fun create(
            amountsVisible: Boolean,
            budgetFen: Long?,
            spendingFen: Long?,
            categoryProgress: List<CategoryBudgetProgress>?,
        ): BillBudgetSummaryVo {
            if (!amountsVisible) {
                return BillBudgetSummaryVo(
                    amountText = "••••",
                    detailText = "预算金额与进度已隐藏",
                )
            }
            if (budgetFen == null || spendingFen == null) {
                return BillBudgetSummaryVo(
                    amountText = "—",
                    detailText = "正在加载预算",
                )
            }
            val overdueCount = categoryProgress.orEmpty().count { it.isOver }
            val categoryStatus = if (overdueCount > 0) "${overdueCount} 项分类预算已超支" else null
            if (budgetFen <= 0L) {
                return BillBudgetSummaryVo(
                    amountText = "未设置",
                    detailText = "设置月预算，让开支有计划",
                    categoryStatus = categoryStatus,
                )
            }
            val isOverBudget = spendingFen > budgetFen
            return BillBudgetSummaryVo(
                amountText = "¥ ${formatBillAmount(budgetFen)}",
                detailText = if (isOverBudget) {
                    "已超支 ¥ ${formatBillAmount(spendingFen - budgetFen)}"
                } else {
                    "剩余 ¥ ${formatBillAmount(budgetFen - spendingFen)}"
                },
                progress = (spendingFen.toDouble() / budgetFen).coerceIn(0.0, 1.0).toFloat(),
                isOverBudget = isOverBudget,
                categoryStatus = categoryStatus,
            )
        }
    }
}

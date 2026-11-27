package com.xiaojinzi.tally.module.widget

import java.time.LocalDate
import java.time.YearMonth

internal enum class WidgetStatus { Loading, OpenApp, NoBook, Ready, Error }

/** A single book/date/privacy revision: never combine amounts from different books or months. */
internal data class WidgetSnapshot(
    val status: WidgetStatus = WidgetStatus.Loading,
    val bookId: String? = null,
    val bookName: String = "",
    val month: String = YearMonth.now().toString(),
    val day: String = LocalDate.now().toString(),
    val amountsVisible: Boolean = false,
    val budget: Long? = null,
    val monthSpending: Long? = null,
    val monthIncome: Long? = null,
    val daySpending: Long? = null,
    val dayIncome: Long? = null,
) {
    val remaining: Long?
        get() = budget?.takeIf { it > 0L }?.let { total -> monthSpending?.let { total - it } }

    val overBudget: Boolean
        get() = amountsVisible && (remaining ?: 0L) < 0L

    val progress: Float?
        get() = if (amountsVisible && (budget ?: 0L) > 0L && monthSpending != null) {
            (monthSpending.toDouble() / budget!!).coerceIn(0.0, 1.0).toFloat()
        } else null

    val budgetLabel: String
        get() = when {
            !amountsVisible -> "预算剩余"
            (budget ?: 0L) <= 0L -> "未设预算"
            overBudget -> "已超支"
            else -> "预算剩余"
        }

    fun budgetAmount(maxCharacters: Int = 11): String =
        if (status == WidgetStatus.Ready && amountsVisible && (budget ?: 0L) <= 0L) "点此设置"
        else amount(remaining, maxCharacters = maxCharacters, absolute = true)

    val monthLabel: String
        get() = "${month.substringAfter('-').toIntOrNull() ?: "—"}月"

    fun amount(cents: Long?, maxCharacters: Int = 11, absolute: Boolean = false): String = when {
        status != WidgetStatus.Ready -> "—"
        !amountsVisible -> "••••"
        cents == null -> "—"
        else -> formatWidgetAmount(cents, maxCharacters, absolute)
    }

    val statusText: String
        get() = when (status) {
            WidgetStatus.Loading -> "正在读取账本…"
            WidgetStatus.OpenApp -> "请打开应用加载账本"
            WidgetStatus.NoBook -> "请在应用中选择账本"
            WidgetStatus.Error -> "读取失败，打开应用重试"
            WidgetStatus.Ready -> ""
        }
}

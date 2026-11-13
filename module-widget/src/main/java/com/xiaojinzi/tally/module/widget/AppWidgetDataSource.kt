package com.xiaojinzi.tally.module.widget

import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.SharedStateFlow
import com.xiaojinzi.support.ktx.getDayInterval
import com.xiaojinzi.support.ktx.getMonthInterval
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyStatisticsHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

object AppWidgetDataSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribe(
        timeRange: Pair<Long, Long>,
        isSpending: Boolean,
    ): SharedStateFlow<MoneyFen?> {
        return AppServices
            .tallyDataSourceInitSpi
            .isInitStateOb
            .flatMapLatest { isDatasourceInit ->
                if (isDatasourceInit) {
                    AppServices
                        .tallyDataSourceSpi
                        .selectedBookStateOb
                        .flatMapLatest { book ->
                            book?.let { bookInfo ->
                                if (isSpending) {
                                    TallyStatisticsHelper
                                        .subscribeTimeRangeSpending(
                                            bookIdList = listOf(bookInfo.id),
                                            startTime = timeRange.first,
                                            endTime = timeRange.second,
                                        )
                                } else {
                                    TallyStatisticsHelper
                                        .subscribeTimeRangeIncome(
                                            bookIdList = listOf(bookInfo.id),
                                            startTime = timeRange.first,
                                            endTime = timeRange.second,
                                        )
                                }.map { it1 ->
                                    it1.transform { it.absoluteValue }
                                }
                            } ?: flowOf(value = MoneyFen())
                        }
                } else {
                    flowOf(value = null)
                }
            }
            .sharedStateIn(
                scope = AppScope,
            )
    }

    /**
     * 当天支出
     */
    @StateHotObservable
    val currentDaySpendingStateOb = subscribe(
        timeRange = getDayInterval(
            timeStamp = System.currentTimeMillis(),
        ),
        isSpending = true,
    )

    /**
     * 当天收入
     */
    @StateHotObservable
    val currentDayIncomeStateOb = subscribe(
        timeRange = getDayInterval(
            timeStamp = System.currentTimeMillis(),
        ),
        isSpending = false,
    )

    /**
     * 当月支出
     */
    @StateHotObservable
    val currentMonthSpendingStateOb = subscribe(
        timeRange = getMonthInterval(
            timeStamp = System.currentTimeMillis(),
        ),
        isSpending = true,
    )

    /**
     * 当月收入
     */
    @StateHotObservable
    val currentMonthIncomeStateOb = subscribe(
        timeRange = getMonthInterval(
            timeStamp = System.currentTimeMillis(),
        ),
        isSpending = false,
    )

    /**
     * 当月预算（钱迹口径），未设置时为 0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @StateHotObservable
    val currentMonthBudgetStateOb = AppServices
        .tallyDataSourceInitSpi
        .isInitStateOb
        .flatMapLatest { isDatasourceInit ->
            if (isDatasourceInit) {
                AppServices
                    .tallyDataSourceSpi
                    .selectedBookStateOb
                    .flatMapLatest { book ->
                        book?.let { bookInfo ->
                            AppServices
                                .tallyDataSourceSpi
                                .subscribeMonthTotalBudget(
                                    bookId = bookInfo.id,
                                    typeInfo = currentMonthTypeInfo(),
                                )
                        } ?: flowOf(value = MoneyFen())
                    }
            } else {
                flowOf(value = null)
            }
        }
        .sharedStateIn(
            scope = AppScope,
        )

    private fun currentMonthTypeInfo(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            java.util.Locale.CHINA,
            "%04d-%02d",
            calendar[java.util.Calendar.YEAR],
            calendar[java.util.Calendar.MONTH] + 1,
        )
    }

}
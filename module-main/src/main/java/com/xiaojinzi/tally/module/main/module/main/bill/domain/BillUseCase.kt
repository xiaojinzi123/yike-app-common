package com.xiaojinzi.tally.module.main.module.main.bill.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.getMonthInterval
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCase
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCaseImpl
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyStatisticsHelper
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCaseImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Calendar
import kotlin.math.absoluteValue

sealed class BillIntent {

    data object Submit : BillIntent()

    data class BookSelect(
        @UiContext val context: Context,
    ) : BillIntent()

}

@ViewModelLayer
interface BillUseCase : BusinessMVIUseCase {

    val billQueryConditionUseCase: CommonBillQueryConditionUseCase

    val timeSelectUseCase: TimeSelectUseCase

    @StateHotObservable
    val timeRangeStateOb: Flow<Pair<Long, Long>>

    /**
     * 肯定是 > 0
     */
    @StateHotObservable
    val currentMonthSpendingStateOb: Flow<MoneyFen>

    /**
     * 肯定是 > 0
     */
    @StateHotObservable
    val currentMonthIncomeStateOb: Flow<MoneyFen>

    /**
     * 有正负的情况
     */
    @StateHotObservable
    val currentMonthBalanceStateOb: Flow<MoneyFen>

}

@ViewModelLayer
class BillUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val billQueryConditionUseCase: CommonBillQueryConditionUseCase = CommonBillQueryConditionUseCaseImpl(),
    override val timeSelectUseCase: TimeSelectUseCase = TimeSelectUseCaseImpl(
        commonUseCase = commonUseCase,
    ),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillUseCase {

    override val timeRangeStateOb = combine(
        timeSelectUseCase.selectedYearStateOb,
        timeSelectUseCase.selectedMonthStateOb,
    ) { year, month ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 0
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        getMonthInterval(
            timeStamp = calendar.timeInMillis,
        )
    }.sharedStateIn(
        scope = scope,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentMonthSpendingStateOb = timeRangeStateOb
        .flatMapLatest { pair ->
            AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb
                .flatMapLatest { book ->
                    book?.let { bookInfo ->
                        TallyStatisticsHelper
                            .subscribeTimeRangeSpending(
                                bookIdList = listOf(bookInfo.id),
                                startTime = pair.first,
                                endTime = pair.second,
                            )
                            .map { it1 ->
                                it1.transform { it.absoluteValue }
                            }
                    } ?: flowOf(value = MoneyFen())
                }
        }
        .sharedStateIn(
            scope = scope,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentMonthIncomeStateOb = timeRangeStateOb
        .flatMapLatest { pair ->
            AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb
                .flatMapLatest { book ->
                    book?.let {
                        TallyStatisticsHelper
                            .subscribeTimeRangeIncome(
                                bookIdList = listOf(it.id),
                                startTime = pair.first,
                                endTime = pair.second,
                            )
                    } ?: flowOf(value = MoneyFen())
                }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val currentMonthBalanceStateOb = combine(
        currentMonthSpendingStateOb,
        currentMonthIncomeStateOb
    ) { spending, income ->
        income - spending
    }

    @IntentProcess
    private suspend fun bookSelect(intent: BillIntent.BookSelect) {
        AppRouterCoreApi::class
            .routeApi()
            .toBookSwitch(
                context = intent.context,
            )
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: BillIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
        billQueryConditionUseCase.destroy()
        timeSelectUseCase.destroy()
    }

    init {
        combine(
            timeRangeStateOb,
            AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb,
        ) { timePair, bookSelected ->
            bookSelected?.let {
                TallyDataSourceSpi.Companion.BillQueryConditionDto(
                    bookIdList = listOf(it.id),
                    startTimeInclude = timePair.first,
                    endTimeInclude = timePair.second,
                )
            }
        }.onEach {
            billQueryConditionUseCase
                .queryConditionStateOb
                .emit(
                    value = it,
                )
        }.launchIn(scope = scope)
    }

}
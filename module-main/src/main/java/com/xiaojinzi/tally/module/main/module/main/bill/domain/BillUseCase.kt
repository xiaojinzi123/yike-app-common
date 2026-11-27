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
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
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
import java.util.Locale
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

    /**
     * 当月预算总额（钱迹口径，yyyy-MM），未设置为 0
     */
    @StateHotObservable
    val currentMonthBudgetStateOb: Flow<MoneyFen>

    /**
     * 当月分类预算与已花（首页超支提示用）
     */
    @StateHotObservable
    val currentMonthCategoryBudgetStateOb: Flow<List<CategoryBudgetProgress>>

}

/** 分类预算进度，金额单位：分 */
data class CategoryBudgetProgress(
    val categoryId: String,
    val budgetFen: Long,
    val spentFen: Long,
) {
    val isOver: Boolean get() = spentFen > budgetFen
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentMonthBudgetStateOb = combine(
        timeSelectUseCase.selectedYearStateOb,
        timeSelectUseCase.selectedMonthStateOb,
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
    ) { year, month, book ->
        Triple(year, month, book)
    }.flatMapLatest { (year, month, book) ->
        if (book == null) {
            flowOf(value = MoneyFen())
        } else {
            AppServices
                .tallyDataSourceSpi
                .subscribeMonthTotalBudget(
                    bookId = book.id,
                    typeInfo = String.format(Locale.CHINA, "%04d-%02d", year, month + 1),
                )
        }
    }.sharedStateIn(
        scope = scope,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentMonthCategoryBudgetStateOb = combine(
        timeRangeStateOb,
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
    ) { timeRange, book ->
        timeRange to book
    }.flatMapLatest { (timeRange, book) ->
        if (book == null) {
            flowOf(emptyList())
        } else {
            combine(
                AppServices.tallyDataSourceSpi.subscribeMonthCategoryBudgets(
                    bookId = book.id,
                    typeInfo = monthTypeInfo(timeMs = timeRange.first),
                ),
                AppServices.tallyDataSourceSpi.subscribeAllCategory(bookIdList = listOf(book.id)),
            ) { budgets, categories -> budgets to categories }
                .flatMapLatest { (budgets, categories) ->
                    if (budgets.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(
                            budgets.map { budget ->
                                AppServices
                                    .tallyDataSourceSpi
                                    .subscribeBillAmountByCondition(
                                        queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                            typeList = listOf(
                                                TallyBillDto.Type.NORMAL,
                                                TallyBillDto.Type.REFUND,
                                            ),
                                            bookIdList = listOf(book.id),
                                            categoryIdList = (listOf(budget.categoryId) + categories
                                                .filter { it.parentId == budget.categoryId }
                                                .map { it.id }).distinct(),
                                            startTimeInclude = timeRange.first,
                                            endTimeInclude = timeRange.second - 1,
                                            amountLessThanZero = true,
                                            isNotCalculate = false,
                                        )
                                    )
                                    .map { amount ->
                                        CategoryBudgetProgress(
                                            categoryId = budget.categoryId,
                                            budgetFen = budget.amount,
                                            spentFen = amount.value.absoluteValue,
                                        )
                                    }
                            }
                        ) { progress ->
                            progress.toList()
                        }
                    }
                }
        }
    }.sharedStateIn(
        scope = scope,
    )

    private fun monthTypeInfo(timeMs: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMs
        return String.format(
            Locale.CHINA,
            "%04d-%02d",
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH] + 1,
        )
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
package com.xiaojinzi.tally.module.main.module.main.statistics.domain

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.DAY_MS
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.commonTimeFormat3
import com.xiaojinzi.support.ktx.commonTimeFormat4
import com.xiaojinzi.support.ktx.getDayInterval
import com.xiaojinzi.support.ktx.getDayOfMonth
import com.xiaojinzi.support.ktx.getMonthInterval
import com.xiaojinzi.support.ktx.getYearInterval
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCaseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.math.absoluteValue

@Keep
data class StatisticsTendencyItemUseCaseDto(
    val timeRange: Pair<Long, Long>,
    val timeStr: String,
    // 金额(单位：分), 为正数 可以表示收入或者支出
    val amount: MoneyFen,
)

@Keep
data class StatisticsCategoryItemUseCaseDto(
    val categoryIdList: List<String>,
    val categoryName: StringItemDto?,
    val icon: LocalImageItemDto?,
    val amount: MoneyYuan,
    val percent: Float,
    val percentForView: Float,
)

@Keep
data class StatisticsLabelItemUseCaseDto(
    val labelId: String,
    val billIdListForQuery: List<String>?,
    val labelName: StringItemDto?,
    val amount: MoneyYuan,
    val percent: Float,
    val percentForView: Float,
)

sealed class StatisticsIntent {

    data object Submit : StatisticsIntent()

    data class CategoryTypeChange(
        val categoryType: StatisticsUseCase.CategoryType,
    ) : StatisticsIntent()

    data class YearOrMonthAdjust(
        val value: Int,
    ) : StatisticsIntent()

    data class ToBillList(
        @UiContext val context: Context,
    ) : StatisticsIntent()

}

@ViewModelLayer
interface StatisticsUseCase : BusinessMVIUseCase {

    enum class StatisticsType {
        Spending, Income,
    }

    enum class CategoryType {
        Big, Small,
    }

    enum class Tab(
        val pageIndex: Int,
    ) {
        Spending(pageIndex = 0), Income(pageIndex = 1),
    }

    enum class TimeType(
        val index: Int,
    ) {
        Month(index = 0), Year(index = 1),
    }

    companion object {
        const val CATEGORY_MIN_COUNT = 3
        const val LABEL_MIN_COUNT = 3
        val Tabs = listOf(
            Tab.Spending,
            Tab.Income,
        )
        val CategoryTypeList = listOf(
            CategoryType.Big,
            CategoryType.Small,
        )
    }

    /**
     * 时间选择的逻辑类
     */
    val timeSelectUseCase: TimeSelectUseCase

    /**
     * tab 的选择
     */
    @StateHotObservable
    val tabSelectedStateOb: MutableSharedStateFlow<Tab>

    /**
     * 时间类型的选择
     */
    @StateHotObservable
    val timeTypeSelectedStateOb: MutableSharedStateFlow<TimeType>

    /**
     * 时间区间
     */
    @StateHotObservable
    val timeRangeStateOb: Flow<Pair<Long, Long>>

    /**
     * 时间段内的天数
     */
    @StateHotObservable
    val dayCountInTimeRangeStateOb: Flow<Long>

    /**
     * 支出的数据
     */
    @StateHotObservable
    val spendingStatisticsStateOb: Flow<MoneyYuan?>

    /**
     * 收入的数据
     */
    @StateHotObservable
    val incomeStatisticsStateOb: Flow<MoneyYuan?>

    /**
     * 日均支出
     */
    @StateHotObservable
    val spendingDayAverageStatisticsStateOb: Flow<MoneyYuan?>

    /**
     * 日均收入
     */
    @StateHotObservable
    val incomeDayAverageStatisticsStateOb: Flow<MoneyYuan?>

    /**
     * 支出的趋势图的数据
     */
    @StateHotObservable
    val tendencyChatSpendingStatisticsStateOb: Flow<List<StatisticsTendencyItemUseCaseDto>>

    /**
     * 收入的趋势图的数据
     */
    @StateHotObservable
    val tendencyChatIncomeStatisticsStateOb: Flow<List<StatisticsTendencyItemUseCaseDto>>

    /**
     * 趋势图的底部的数字列表
     */
    @StateHotObservable
    val tendencyChatBottomNumberListStateOb: Flow<List<String>>

    /**
     * 类别类型
     */
    @StateHotObservable
    val categoryTypeStateOb: Flow<CategoryType>

    /**
     * 类别支出的数据
     */
    @StateHotObservable
    val allCategorySpendingStatisticsStateOb: Flow<List<StatisticsCategoryItemUseCaseDto>>

    /**
     * 类别收入的数据
     */
    @StateHotObservable
    val allCategoryIncomeStatisticsStateOb: Flow<List<StatisticsCategoryItemUseCaseDto>>

    /**
     * 标签支出的数据
     */
    @StateHotObservable
    val allLabelSpendingStatisticsStateOb: Flow<List<StatisticsLabelItemUseCaseDto>>

    /**
     * 标签收入的数据
     */
    @StateHotObservable
    val allLabelIncomeStatisticsStateOb: Flow<List<StatisticsLabelItemUseCaseDto>>

}

@ViewModelLayer
class StatisticsUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val timeSelectUseCase: TimeSelectUseCase = TimeSelectUseCaseImpl(
        maxTime = System.currentTimeMillis(),
        commonUseCase = commonUseCase,
    ),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), StatisticsUseCase {

    override val tabSelectedStateOb = MutableSharedStateFlow(
        initValue = StatisticsUseCase.Tab.Spending,
    )

    override val timeTypeSelectedStateOb = MutableSharedStateFlow(
        initValue = StatisticsUseCase.TimeType.Month,
    )

    override val timeRangeStateOb = combine(
        timeTypeSelectedStateOb,
        timeSelectUseCase.selectedYearStateOb,
        timeSelectUseCase.selectedMonthStateOb,
    ) { timeType, year, month ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 0
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        when (timeType) {
            StatisticsUseCase.TimeType.Year -> getYearInterval(
                timeStamp = calendar.timeInMillis,
            )

            StatisticsUseCase.TimeType.Month -> getMonthInterval(
                timeStamp = calendar.timeInMillis,
            )
        }
    }

    override val dayCountInTimeRangeStateOb = timeRangeStateOb
        .map { pair ->
            val startTimeStamp = pair.first
            val endTimeStamp = minOf(
                a = pair.second,
                System.currentTimeMillis(),
            )
            // 计算两个时间戳之间的天数
            (endTimeStamp - startTimeStamp) / DAY_MS + 1
        }

    private fun spendingOrIncomeStatistics(type: StatisticsUseCase.StatisticsType): Flow<MoneyYuan?> {
        return combine(
            AppServices.tallyDataSourceSpi.selectedBookStateOb,
            timeRangeStateOb,
            AppServices
                .tallyDataSourceSpi
                .subscribeDataBaseTableChangedOb(
                    TallyTable.Bill, emitOneWhileSubscribe = true,
                )
        ) { currentBookInfo, timePair, _ ->
            currentBookInfo?.let {
                AppServices
                    .tallyDataSourceSpi
                    .getBillAmountByCondition(
                        queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                            typeList = listOf(
                                TallyBillDto.Type.NORMAL,
                                TallyBillDto.Type.REFUND,
                            ),
                            bookIdList = listOf(
                                currentBookInfo.id,
                            ),
                            startTimeInclude = timePair.first,
                            endTimeInclude = timePair.second,
                            amountMoreThanZero = when (type) {
                                StatisticsUseCase.StatisticsType.Income -> true
                                else -> null
                            },
                            amountLessThanZero = when (type) {
                                StatisticsUseCase.StatisticsType.Spending -> true
                                else -> null
                            },
                            isNotCalculate = false,
                        ),
                    ).transform { it.absoluteValue }.toYuan()
            }
        }.flowOn(
            context = Dispatchers.IO,
        ).sharedStateIn(
            scope = scope,
            initValue = null,
        )
    }

    override val spendingStatisticsStateOb = spendingOrIncomeStatistics(
        type = StatisticsUseCase.StatisticsType.Spending,
    )

    override val incomeStatisticsStateOb = spendingOrIncomeStatistics(
        type = StatisticsUseCase.StatisticsType.Income,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val spendingDayAverageStatisticsStateOb = dayCountInTimeRangeStateOb
        .flatMapLatest { dayCount ->
            delay(200)
            spendingStatisticsStateOb
                .map { amount ->
                    amount?.value?.div(other = dayCount.toFloat())?.let {
                        MoneyYuan(
                            value = it,
                        )
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val incomeDayAverageStatisticsStateOb = dayCountInTimeRangeStateOb
        .flatMapLatest { dayCount ->
            delay(200)
            incomeStatisticsStateOb
                .map { amount ->
                    amount?.value?.div(other = dayCount.toFloat())?.let {
                        MoneyYuan(
                            value = it,
                        )
                    }
                }
        }

    private fun tendencyChatStatistics(
        statisticsType: StatisticsUseCase.StatisticsType,
    ): Flow<List<StatisticsTendencyItemUseCaseDto>> {
        return combine(
            AppServices
                .tallyDataSourceSpi
                .subscribeDataBaseTableChangedOb(
                    TallyTable.Bill, emitOneWhileSubscribe = true,
                ),
            AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb,
            timeTypeSelectedStateOb,
            timeSelectUseCase.currentTimeStateOb,
        ) { _, selectedBook, timeType, currentSelectTime ->
            if (selectedBook == null) {
                emptyList()
            } else // 站位
            {
                when (timeType) {
                    StatisticsUseCase.TimeType.Year -> {
                        getYearInterval(timeStamp = currentSelectTime).run {
                            (0..11).map { month ->
                                getMonthInterval(
                                    timeStamp = this.first,
                                    monthOffset = month,
                                )
                            }
                        }
                    }

                    StatisticsUseCase.TimeType.Month -> {
                        getMonthInterval(timeStamp = currentSelectTime).run {
                            // 这个月的天数
                            val dayCountOfThisMonth = getDayOfMonth(
                                timeStamp = getMonthInterval(
                                    timeStamp = currentSelectTime,
                                ).second,
                            )
                            (0 until dayCountOfThisMonth).map { dayItem ->
                                getDayInterval(
                                    timeStamp = this.first,
                                    dayOffset = dayItem,
                                )
                            }
                        }
                    }
                }.map { timeRange ->
                    StatisticsTendencyItemUseCaseDto(
                        timeRange = timeRange,
                        timeStr = when (timeType) {
                            StatisticsUseCase.TimeType.Year -> {
                                timeRange.first.commonTimeFormat4()
                            }

                            StatisticsUseCase.TimeType.Month -> {
                                timeRange.first.commonTimeFormat3()
                            }
                        },
                        amount = AppServices
                            .tallyDataSourceSpi
                            .getBillAmountByCondition(
                                queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                    typeList = listOf(
                                        TallyBillDto.Type.NORMAL,
                                        TallyBillDto.Type.REFUND,
                                    ),
                                    bookIdList = listOf(selectedBook.id),
                                    startTimeInclude = timeRange.first,
                                    endTimeInclude = timeRange.second,
                                    amountMoreThanZero = when (statisticsType) {
                                        StatisticsUseCase.StatisticsType.Income -> true
                                        else -> null
                                    },
                                    amountLessThanZero = when (statisticsType) {
                                        StatisticsUseCase.StatisticsType.Spending -> true
                                        else -> null
                                    },
                                    isNotCalculate = false,
                                ),
                            ).transform { it.absoluteValue }
                    )
                }
            }
        }.sharedStateIn(
            scope = scope,
        )
    }

    override val tendencyChatSpendingStatisticsStateOb = tendencyChatStatistics(
        statisticsType = StatisticsUseCase.StatisticsType.Spending,
    )

    override val tendencyChatIncomeStatisticsStateOb = tendencyChatStatistics(
        statisticsType = StatisticsUseCase.StatisticsType.Income,
    )

    override val tendencyChatBottomNumberListStateOb = combine(
        timeTypeSelectedStateOb,
        timeSelectUseCase.currentTimeStateOb,
    ) { timeType, currentSelectTime ->
        when (timeType) {
            StatisticsUseCase.TimeType.Year -> {
                (1..12).map {
                    if (it < 10) {
                        "0$it"
                    } else {
                        it.toString()
                    }
                }
            }

            StatisticsUseCase.TimeType.Month -> {
                val dayCountOfThisMonth = getDayOfMonth(
                    timeStamp = getMonthInterval(
                        timeStamp = currentSelectTime,
                    ).second,
                )
                (1..dayCountOfThisMonth)
                    .filter {
                        it == dayCountOfThisMonth || it % 3 == 1
                    }
                    .map {
                        if (it < 10) {
                            "0$it"
                        } else {
                            it.toString()
                        }
                    }
            }
        }
    }

    override val categoryTypeStateOb = MutableSharedStateFlow(
        initValue = StatisticsUseCase.CategoryType.Big,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun categoryStatistics(
        type: StatisticsUseCase.StatisticsType,
        categoryType: StatisticsUseCase.CategoryType,
    ): Flow<List<StatisticsCategoryItemUseCaseDto>> {
        return combine(
            AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb
                .flatMapLatest { currentBookInfo ->
                    currentBookInfo?.let {
                        AppServices
                            .tallyDataSourceSpi
                            .subscribeAllCategory(
                                bookIdList = listOf(
                                    currentBookInfo.id,
                                ),
                            )
                            .map { categoryLIst ->
                                when (categoryType) {
                                    StatisticsUseCase.CategoryType.Big -> {
                                        categoryLIst
                                            // 拿到大类别的
                                            .filter { item1 ->
                                                item1.parentId.isNullOrEmpty()
                                            }.map { item1 ->
                                                categoryLIst.filter { item2 ->
                                                    item1.id == item2.parentId
                                                }.toMutableList().apply {
                                                    this.add(
                                                        index = 0, element = item1,
                                                    )
                                                }
                                            }
                                    }

                                    StatisticsUseCase.CategoryType.Small -> {
                                        categoryLIst
                                            // 拿到大类别的
                                            .filter { item ->
                                                !item.parentId.isNullOrEmpty()
                                            }.map {
                                                listOf(it)
                                            }
                                    }
                                }
                            }
                    } ?: flowOf(value = emptyList())
                },
            // 选择的时间段
            timeRangeStateOb,
            // 账单改变的一个触发 Flow
            AppServices
                .tallyDataSourceSpi
                .subscribeDataBaseTableChangedOb(
                    TallyTable.Bill, emitOneWhileSubscribe = true,
                )
        ) { categoryListList, timePair, _ ->
            val categoryAmountList = categoryListList
                .map { categoryList ->
                    // categoryList 中至少是有一个的, 如果是全部或者小类. 那么这个里面就是唯一的一个对应的类别
                    // 如果是大类, 那么这个里面是有多个类别的, 第一个是大类别
                    categoryList to AppServices
                        .tallyDataSourceSpi
                        .getBillAmountByCondition(
                            queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                startTimeInclude = timePair.first,
                                endTimeInclude = timePair.second,
                                categoryIdList = categoryList.map { it.id },
                                amountMoreThanZero = when (type) {
                                    StatisticsUseCase.StatisticsType.Income -> true
                                    else -> null
                                },
                                amountLessThanZero = when (type) {
                                    StatisticsUseCase.StatisticsType.Spending -> true
                                    else -> null
                                },
                                isNotCalculate = false,
                            ),
                        )
                }
                .map { pairItem ->
                    pairItem.copy(
                        second = pairItem.second.transform {
                            it.absoluteValue
                        }
                    )
                }
                // 降序排列
                .sortedByDescending {
                    it.second.value
                }
            // 这个时间段的总和
            val amountTotal: MoneyFen =
                categoryAmountList.map { it.second }.reduceOrNull { acc, item ->
                    acc + item
                } ?: MoneyFen(value = 0)
            val amountTotalFloat = amountTotal.value.toFloat()

            if (categoryAmountList.isEmpty() || amountTotalFloat == 0f) {
                emptyList()
            } else {
                val maxItemValue: Float =
                    categoryAmountList.first().second.value.toFloat()
                categoryAmountList
                    .map { pairItem ->
                        val categoryOrBigCategory = pairItem.first.first()
                        StatisticsCategoryItemUseCaseDto(
                            categoryIdList = pairItem.first.map { it.id },
                            categoryName = categoryOrBigCategory.name?.toStringItemDto(),
                            icon = AppServices.iconMappingSpi[categoryOrBigCategory.iconName]?.toLocalImageItemDto(),
                            amount = pairItem.second.toYuan(),
                            percent = pairItem.second.value / amountTotalFloat,
                            percentForView = pairItem.second.value / maxItemValue,
                        )
                    }
                    .filter { it.amount.value != 0f }
            }
        }.flowOn(
            context = Dispatchers.IO
        ).sharedStateIn(
            scope = scope,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategorySpendingStatisticsStateOb = categoryTypeStateOb
        .flatMapLatest { categoryType ->
            categoryStatistics(
                type = StatisticsUseCase.StatisticsType.Spending,
                categoryType = categoryType,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategoryIncomeStatisticsStateOb = categoryTypeStateOb
        .flatMapLatest { categoryType ->
            categoryStatistics(
                type = StatisticsUseCase.StatisticsType.Income,
                categoryType = categoryType
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun labelStatistics(
        type: StatisticsUseCase.StatisticsType,
    ): Flow<List<StatisticsLabelItemUseCaseDto>> {
        return AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb
            .flatMapLatest { currentBookInfo ->
                currentBookInfo?.let { currentBookInfo1 ->
                    combine(
                        AppServices
                            .tallyDataSourceSpi
                            .subscribeAllLabel(
                                bookId = currentBookInfo1.id,
                            ),
                        // 选择的时间段
                        timeRangeStateOb,
                        // 账单改变的一个触发 Flow
                        AppServices
                            .tallyDataSourceSpi
                            .subscribeDataBaseTableChangedOb(
                                TallyTable.Bill, emitOneWhileSubscribe = true,
                            )
                    ) { allLabels, timePair, _ ->
                        val amountList = allLabels
                            .map { item ->
                                val billIdList = AppServices
                                    .tallyDataSourceSpi
                                    .getBillIdListByLabelId(
                                        bookId = currentBookInfo1.id,
                                        labelId = item.id,
                                        isExcludeDeleted = true,
                                    )
                                val amount = if (billIdList.isEmpty()) {
                                    MoneyFen()
                                } else // 占位
                                {
                                    AppServices
                                        .tallyDataSourceSpi
                                        .getBillAmountByCondition(
                                            queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                                idList = billIdList,
                                                startTimeInclude = timePair.first,
                                                endTimeInclude = timePair.second,
                                                amountMoreThanZero = when (type) {
                                                    StatisticsUseCase.StatisticsType.Income -> true
                                                    else -> null
                                                },
                                                amountLessThanZero = when (type) {
                                                    StatisticsUseCase.StatisticsType.Spending -> true
                                                    else -> null
                                                },
                                                isNotCalculate = false,
                                            ),
                                        )
                                }
                                Triple(
                                    first = item,
                                    second = amount,
                                    third = billIdList,
                                )
                            }
                            .map { pairItem ->
                                pairItem.copy(
                                    second = pairItem.second.transform {
                                        it.absoluteValue
                                    }
                                )
                            }
                            // 降序排列
                            .sortedByDescending {
                                it.second.value
                            }
                        // 这个时间段的总和
                        val amountTotal: MoneyFen =
                            amountList.map { it.second }.reduceOrNull { acc, item ->
                                acc + item
                            } ?: MoneyFen(value = 0)
                        val amountTotalFloat = amountTotal.value.toFloat()
                        if (amountList.isEmpty() || amountTotalFloat == 0f) {
                            emptyList()
                        } else {
                            val maxItemValue: Float =
                                amountList.first().second.value.toFloat()
                            amountList
                                .map { pairItem ->
                                    StatisticsLabelItemUseCaseDto(
                                        labelId = pairItem.first.id,
                                        billIdListForQuery = pairItem.third.orNull(),
                                        labelName = pairItem.first.name?.toStringItemDto(),
                                        amount = pairItem.second.toYuan(),
                                        percent = pairItem.second.value / amountTotalFloat,
                                        percentForView = pairItem.second.value / maxItemValue,
                                    )
                                }
                                .filter { it.amount.value != 0f }
                        }
                    }
                } ?: flowOf(value = emptyList())
            }.flowOn(
                context = Dispatchers.IO
            ).sharedStateIn(
                scope = scope,
            )
    }

    override val allLabelSpendingStatisticsStateOb = labelStatistics(
        type = StatisticsUseCase.StatisticsType.Spending,
    ).sharedStateIn(
        scope = scope,
    )

    override val allLabelIncomeStatisticsStateOb = labelStatistics(
        type = StatisticsUseCase.StatisticsType.Income,
    ).sharedStateIn(
        scope = scope,
    )

    @IntentProcess
    private suspend fun yearOrMonthAdjust(intent: StatisticsIntent.YearOrMonthAdjust) {

        when (timeTypeSelectedStateOb.value) {
            StatisticsUseCase.TimeType.Year -> {
                timeSelectUseCase.addIntent(
                    intent = TimeSelectUseCase.Intent.YearAdjust(
                        value = intent.value,
                    )
                )
            }

            StatisticsUseCase.TimeType.Month -> {
                timeSelectUseCase.addIntent(
                    intent = TimeSelectUseCase.Intent.MonthAdjust(
                        value = intent.value,
                    )
                )
            }
        }
    }

    @IntentProcess
    private suspend fun categoryTypeChange(intent: StatisticsIntent.CategoryTypeChange) {
        categoryTypeStateOb.emit(
            value = intent.categoryType,
        )
    }

    @IntentProcess
    private suspend fun toBillList(intent: StatisticsIntent.ToBillList) {
        val currentBookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        val selectTime = timeSelectUseCase.currentTimeStateOb.first()
        val selectYear = timeSelectUseCase.selectedYearStateOb.first()
        val selectMonth = timeSelectUseCase.selectedMonthStateOb.first()
        val timeTypeSelected = timeTypeSelectedStateOb.first()
        val title = when (timeTypeSelected) {
            StatisticsUseCase.TimeType.Month -> {
                "${selectYear}年${selectMonth.plus(1)}月"
            }

            StatisticsUseCase.TimeType.Year -> {
                "${selectYear}年"
            }
        }
        val timeRange = when (timeTypeSelected) {
            StatisticsUseCase.TimeType.Month -> {
                getMonthInterval(
                    timeStamp = selectTime,
                )
            }

            StatisticsUseCase.TimeType.Year -> {
                getYearInterval(
                    timeStamp = selectTime,
                )
            }
        }
        AppRouterCoreApi::class
            .routeApi()
            .toBillListView(
                context = intent.context,
                title = title.toStringItemDto(),
                question = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                    bookIdList = listOf(
                        currentBookInfo.id,
                    ),
                    startTimeInclude = timeRange.first,
                    endTimeInclude = timeRange.second,
                ),
            )
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: StatisticsIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
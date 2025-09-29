package com.xiaojinzi.tally.module.main.module.main.calendar.domain

import androidx.annotation.Keep
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getDayOfMonth
import com.xiaojinzi.support.ktx.getMonthInterval
import com.xiaojinzi.support.ktx.isSameDay
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCase
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCaseImpl
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCase
import com.xiaojinzi.tally.module.base.usecase.TimeSelectUseCaseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone

@Keep
data class DayInMonthItemData(
    val isToday: Boolean,
    val year: Int,
    val month: Int,
    // 从 1 开始
    val dayOfWeek: Int,
    // 从 1 开始的
    val dayOfMonth: Int,
    val amount: MoneyYuan,
)

@Keep
data class MonthPageListItemData(
    val year: Int,
    // 从 0 开始的
    val month: Int,
    val isCurrentYearAndMonth: Boolean,
    // 从开始时间到目标时间段的月份 index
    val monthIndex: Int,
)

@Keep
data class MonthPageListData(
    // 默认的月份 index
    val defaultMonthIndex: Int,
    // 月份的数据
    val monthPageList: List<MonthPageListItemData>,
)

sealed class CalendarIntent {

    data object Submit : CalendarIntent()

}

@ViewModelLayer
interface CalendarUseCase : BusinessMVIUseCase {

    companion object {
        val CalendarTitleList = listOf(
            "日", "一", "二", "三", "四", "五", "六",
        )
    }

    val billQueryConditionUseCase: CommonBillQueryConditionUseCase

    val timeSelectUseCase: TimeSelectUseCase

    /**
     * 所有月份信息
     */
    @StateHotObservable
    val monthPageListDataStateOb: Flow<MonthPageListData>

    /**
     * 订阅某一年的某一个月的日历数据
     */
    @StateHotObservable
    fun subscribeMonthDayList(
        year: Int,
        month: Int,
    ): Flow<List<DayInMonthItemData?>>

}

@ViewModelLayer
class CalendarUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val billQueryConditionUseCase: CommonBillQueryConditionUseCase = CommonBillQueryConditionUseCaseImpl(),
    override val timeSelectUseCase: TimeSelectUseCase = TimeSelectUseCaseImpl(
        commonUseCase = commonUseCase,
    ),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CalendarUseCase {

    override val monthPageListDataStateOb = MutableSharedStateFlow(
        initValue = run {
            val previousMonthCount = 10 * 12
            val nextMonthCount = 10 * 12
            val totalMonthCount = previousMonthCount + nextMonthCount
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            calendar.timeInMillis = 0
            calendar.set(Calendar.YEAR, currentYear - 10)

            val monthPageList = (0 until totalMonthCount).map { index ->
                val itemYear = calendar.get(Calendar.YEAR)
                val itemMonth = calendar.get(Calendar.MONTH)
                MonthPageListItemData(
                    year = itemYear,
                    month = itemMonth,
                    isCurrentYearAndMonth = itemYear == currentYear && itemMonth == currentMonth,
                    monthIndex = index,
                ).apply {
                    calendar.add(Calendar.MONTH, 1)
                }
            }
            MonthPageListData(
                defaultMonthIndex = monthPageList
                    .indexOfFirst { it.isCurrentYearAndMonth }
                    .coerceAtLeast(
                        minimumValue = 0,
                    ),
                monthPageList = monthPageList,
            )
        }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribeMonthDayList(
        year: Int,
        month: Int,
    ): Flow<List<DayInMonthItemData?>> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 0
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        val (startTime, endTime) = getMonthInterval(
            timeStamp = calendar.timeInMillis,
        )
        calendar.timeInMillis = startTime
        val dayStart = calendar[Calendar.DAY_OF_MONTH]
        calendar.timeInMillis = endTime
        val dayEnd = calendar[Calendar.DAY_OF_MONTH]
        return AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb
            .flatMapLatest { bookSelected ->
                if (bookSelected == null) {
                    flowOf(value = emptyList())
                } else {
                    AppServices
                        .tallyDataSourceSpi
                        .subscribeBillDetailList(
                            queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                bookIdList = listOf(bookSelected.id),
                                startTimeInclude = startTime,
                                endTimeInclude = endTime,
                                isNotCalculate = false,
                            ),
                        )
                }
            }
            .map { billDetailList ->
                billDetailList
                    .groupBy {
                        getDayOfMonth(
                            timeStamp = it.core.time,
                        )
                    }.run {
                        // 确保每天都是有数据的
                        this.toMutableMap().let { mutableMap ->
                            (dayStart..dayEnd).forEach { item ->
                                mutableMap.getOrPut(key = item) {
                                    emptyList()
                                }
                            }
                            mutableMap
                        }
                    }
                    .map { entity ->
                        calendar.set(Calendar.DAY_OF_MONTH, entity.key)
                        DayInMonthItemData(
                            isToday = isSameDay(
                                timeStamp1 = calendar.timeInMillis,
                                System.currentTimeMillis(),
                            ),
                            year = year,
                            month = month,
                            dayOfWeek = run {
                                calendar.get(Calendar.DAY_OF_WEEK)
                            },
                            dayOfMonth = entity.key,
                            amount = (
                                    entity.value.map {
                                        when (it.core.type) {
                                            TallyBillDto.Type.NORMAL -> {
                                                it.core.amount
                                            }

                                            else -> MoneyFen()
                                        }

                                    }.reduceOrNull { acc, moneyFen ->
                                        acc + moneyFen
                                    } ?: MoneyFen(value = 0)
                                    ).toYuan(),
                        )
                    }
                    .sortedBy { it.dayOfMonth }
                    .run {
                        val firstDayOfWeek = this.first().dayOfWeek
                        val resultList =
                            ArrayList<DayInMonthItemData?>(this.size + firstDayOfWeek - 1)
                        (1 until firstDayOfWeek).forEach { _ ->
                            resultList.add(element = null)
                        }
                        resultList.addAll(
                            elements = this,
                        )
                        resultList
                    }
            }
            .flowOn(context = Dispatchers.IO)
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: CalendarIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
        billQueryConditionUseCase.destroy()
        timeSelectUseCase.destroy()
    }

}
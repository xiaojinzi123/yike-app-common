package com.xiaojinzi.tally.module.base.module.common_bill_list.view

import com.xiaojinzi.reactive.domain.BaseUseCase
import com.xiaojinzi.reactive.domain.BaseUseCaseImpl
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.DAY_MS
import com.xiaojinzi.support.ktx.getDayInterval
import com.xiaojinzi.support.ktx.getDayOfWeek
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillListUseCase
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillListUseCaseImpl
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

interface CommonBillListViewUseCase : BaseUseCase {

    val commonBillListUseCase: CommonBillListUseCase

    /**
     * 账单列表的数据
     */
    @StateHotObservable
    val billListStateObVo: Flow<List<CommonBillListItemVo>>

}

class CommonBillListViewUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    private val billQueryConditionUseCase: CommonBillQueryConditionUseCase,
    override val commonBillListUseCase: CommonBillListUseCase = CommonBillListUseCaseImpl(
        commonUseCase = commonUseCase,
        commonBillQueryConditionUseCase = billQueryConditionUseCase,
    ),
) : BaseUseCaseImpl(), CommonBillListViewUseCase {

    @StateHotObservable
    override val billListStateObVo = commonBillListUseCase
        .billListStateOb
        .map { list ->
            list
                .groupBy { getDayInterval(timeStamp = it.core.time).first }
                .map { entity ->
                    val dayIncomeCost = entity
                        .value
                        .asSequence()
                        .filter {
                            (it.core.type == TallyBillDto.Type.NORMAL ||
                                    it.core.type == TallyBillDto.Type.REFUND) &&
                                    !it.core.isNotCalculate &&
                                    it.core.amount.value > 0f
                        }
                        .map { it.core.amount }
                        .reduceOrNull { acc, value -> acc + value } ?: MoneyFen(value = 0)
                    val daySpendingCost = entity
                        .value
                        .asSequence()
                        .filter {
                            (it.core.type == TallyBillDto.Type.NORMAL ||
                                    it.core.type == TallyBillDto.Type.REFUND) &&
                                    !it.core.isNotCalculate &&
                                    it.core.amount.value < 0f
                        }
                        .map { it.core.amount }
                        .reduceOrNull { acc, value -> acc + value } ?: MoneyFen(value = 0)
                    val billDetailList = entity.value
                    val itemList = billDetailList
                        .mapIndexed<TallyBillDetailDto, CommonBillListItemVo> { index, item ->
                            item.toCommonBillListNormalItemVo(
                                clipType = index.toCommonBillListItemClipType(
                                    size = billDetailList.size,
                                ),
                            )
                        }
                    ArrayList<CommonBillListItemVo>(
                        1 + itemList.size
                    ).apply {
                        val currentTime = System.currentTimeMillis()
                        val (currentDayStartTime, _) = getDayInterval(
                            timeStamp = currentTime,
                        )
                        // 和当前时间的凌晨时间的差值.
                        val diffTime = entity.key - currentDayStartTime
                        val timeStr = when (diffTime) {
                            in (0 until DAY_MS - 1) -> "今天"
                            in (-DAY_MS until 0) -> "昨天"
                            else -> SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            ).format(Date(entity.key))
                        }.toStringItemDto()
                        this.add(
                            element = CommonBillListHeaderItemVo(
                                timeStr = timeStr,
                                dayOfWeekStr = when (getDayOfWeek(
                                    timeStamp = entity.key,
                                )) {
                                    Calendar.SUNDAY -> "周日"
                                    Calendar.MONDAY -> "周一"
                                    Calendar.TUESDAY -> "周二"
                                    Calendar.WEDNESDAY -> "周三"
                                    Calendar.THURSDAY -> "周四"
                                    Calendar.FRIDAY -> "周五"
                                    Calendar.SATURDAY -> "周六"
                                    else -> ""
                                }.toStringItemDto(),
                                income = dayIncomeCost.toYuan().transform { it.absoluteValue },
                                spending = daySpendingCost.toYuan().transform { it.absoluteValue },
                            )
                        )
                        this.addAll(
                            elements = itemList,
                        )
                    }
                }
                .flatten()
        }
        .flowOn(context = Dispatchers.IO)
        .sharedStateIn(
            scope = scope,
            initValue = emptyList(),
        )

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
        billQueryConditionUseCase.destroy()
        commonBillListUseCase.destroy()
    }

}
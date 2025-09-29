package com.xiaojinzi.tally.module.base.usecase

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.ktx.HotStateFlow
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.commonTimeFormat3
import com.xiaojinzi.support.ktx.getDayOfMonth
import com.xiaojinzi.support.ktx.getMonthByTimeStamp
import com.xiaojinzi.support.ktx.getYearByTimeStamp
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

interface TimeSelectUseCase : BusinessMVIUseCase {

    sealed class Intent {

        data class DateTimeSelect(
            @UiContext val context: Context,
            val dateTimeType: DateTimeType,
        )

        data class YearAndMonthSet(
            val year: Int,
            val month: Int,
        ) : Intent()

        /**
         * value 可以为负数
         * 每次调用会产生一个偏移
         */
        data class YearAdjust(
            val value: Int,
        ) : Intent()

        /**
         * value 可以为负数
         * 每次调用会产生一个偏移
         */
        data class MonthAdjust(
            val value: Int,
        ) : Intent()

        /**
         * 重置时间
         */
        data object ResetTime : Intent()

    }

    val currentTimeStateOb: HotStateFlow<Long>

    val selectedYearStateOb: HotStateFlow<Int>

    val selectedMonthStateOb: HotStateFlow<Int>

    val selectedDayOfMonthStateOb: HotStateFlow<Int>

}

class TimeSelectUseCaseImpl(
    private val maxTime: Long? = null,
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), TimeSelectUseCase {

    override val currentTimeStateOb = MutableSharedStateFlow(
        initValue = System.currentTimeMillis(),
    )

    override val selectedYearStateOb = currentTimeStateOb
        .map {
            getYearByTimeStamp(timeStamp = it)
        }

    override val selectedMonthStateOb = currentTimeStateOb
        .map {
            getMonthByTimeStamp(timeStamp = it)
        }

    override val selectedDayOfMonthStateOb = currentTimeStateOb
        .map {
            getDayOfMonth(timeStamp = it)
        }

    @IntentProcess
    private suspend fun dateTimeSelect(
        intent: TimeSelectUseCase.Intent.DateTimeSelect,
    ) {
        ParameterSupport.getLong(
            intent = AppRouterBaseApi::class
                .routeApi()
                .dateTimeSelectBySuspend(
                    context = intent.context,
                    type = intent.dateTimeType,
                    time = currentTimeStateOb.first(),
                ),
            key = "data"
        )?.let {
            currentTimeStateOb.emit(
                value = if (maxTime == null) {
                    it
                } else {
                    it.coerceAtMost(maximumValue = maxTime).apply {
                        if (this != it) {
                            tip(
                                content = "最大时间不超过：${
                                    maxTime.commonTimeFormat3()
                                }".toStringItemDto(),
                            )
                        }
                    }
                },
            )
        }
    }

    @IntentProcess
    private suspend fun yearAndMonthSet(
        intent: TimeSelectUseCase.Intent.YearAndMonthSet,
    ) {
        val currentTime = currentTimeStateOb.first()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(Calendar.YEAR, intent.year)
        calendar.set(Calendar.MONTH, intent.month)
        currentTimeStateOb.emit(
            value = calendar.timeInMillis,
        )
    }

    @IntentProcess
    private suspend fun yearAdjust(
        intent: TimeSelectUseCase.Intent.YearAdjust,
    ) {
        val currentTime = currentTimeStateOb.first()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.add(Calendar.YEAR, intent.value)
        currentTimeStateOb.emit(
            value = calendar.timeInMillis,
        )
    }

    @IntentProcess
    private suspend fun monthAdjust(
        intent: TimeSelectUseCase.Intent.MonthAdjust,
    ) {
        val currentTime = currentTimeStateOb.first()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.add(Calendar.MONTH, intent.value)
        currentTimeStateOb.emit(
            value = calendar.timeInMillis,
        )
    }

    @IntentProcess
    private suspend fun resetTime(
        intent: TimeSelectUseCase.Intent.ResetTime,
    ) {

        currentTimeStateOb.emit(
            value = System.currentTimeMillis(),
        )

    }

}
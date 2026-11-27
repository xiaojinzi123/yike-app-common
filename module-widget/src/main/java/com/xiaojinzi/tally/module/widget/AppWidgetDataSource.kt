package com.xiaojinzi.tally.module.widget

import com.xiaojinzi.support.ktx.getDayInterval
import com.xiaojinzi.support.ktx.getMonthInterval
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyStatisticsHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.absoluteValue

internal object AppWidgetDataSource {
    private val clockChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun invalidateDate() {
        clockChanged.tryEmit(Unit)
    }

    private data class Period(
        val day: String,
        val month: String,
        val dayRange: Pair<Long, Long>,
        val monthRange: Pair<Long, Long>,
        val zone: String,
    )

    private fun period(): Period {
        val now = System.currentTimeMillis()
        return Period(
            day = LocalDate.now().toString(),
            month = YearMonth.now().toString(),
            dayRange = getDayInterval(timeStamp = now),
            monthRange = getMonthInterval(timeStamp = now),
            zone = ZoneId.systemDefault().id,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WidgetSnapshot> = flow {
        // New subscriptions always calculate the current date. The ticker also handles a
        // process kept alive across midnight; DATE/TIME/TIMEZONE broadcasts invalidate it.
        val periods = merge(
            flow { while (true) { emit(Unit); delay(60_000L) } },
            clockChanged,
        ).map { period() }.distinctUntilChanged()
        val snapshots = combine(
            AppServices.tallyDataSourceInitSpi.isInitStateOb,
            AppServices.appConfigSpi.isHomeBillStatVisibleStateOb,
            periods,
        ) { ready, visible, period -> Triple(ready, visible, period) }
            .flatMapLatest { (ready, visible, period) ->
                val base = WidgetSnapshot(
                    status = if (ready) WidgetStatus.Loading else WidgetStatus.OpenApp,
                    month = period.month, day = period.day, amountsVisible = visible,
                )
                if (!ready) {
                    flowOf(base)
                } else {
                    // Obtaining this SPI before init completes constructs a data source on a
                    // missing Room DB. Keep all access inside the ready branch.
                    val source = AppServices.tallyDataSourceSpi
                    source.selectedBookStateOb.flatMapLatest { book ->
                        if (book == null) flowOf(base.copy(status = WidgetStatus.NoBook))
                        else {
                            val selected = base.copy(bookId = book.id, bookName = book.name.orEmpty().ifBlank { "当前账本" })
                            combine(
                                source.subscribeMonthTotalBudget(bookId = book.id, typeInfo = period.month),
                                TallyStatisticsHelper.subscribeTimeRangeSpending(
                                    bookIdList = listOf(book.id), startTime = period.monthRange.first, endTime = period.monthRange.second,
                                ),
                                TallyStatisticsHelper.subscribeTimeRangeIncome(
                                    bookIdList = listOf(book.id), startTime = period.monthRange.first, endTime = period.monthRange.second,
                                ),
                                TallyStatisticsHelper.subscribeTimeRangeSpending(
                                    bookIdList = listOf(book.id), startTime = period.dayRange.first, endTime = period.dayRange.second,
                                ),
                                TallyStatisticsHelper.subscribeTimeRangeIncome(
                                    bookIdList = listOf(book.id), startTime = period.dayRange.first, endTime = period.dayRange.second,
                                ),
                            ) { budget, monthSpent, monthIncome, daySpent, dayIncome ->
                                selected.copy(
                                    status = WidgetStatus.Ready,
                                    // Redact before the view consumes the model: no hidden progress,
                                    // exceeded-budget color or accessibility text can leak amounts.
                                    budget = budget.value.takeIf { visible },
                                    monthSpending = monthSpent.value.absoluteValue.takeIf { visible },
                                    monthIncome = monthIncome.value.takeIf { visible },
                                    daySpending = daySpent.value.absoluteValue.takeIf { visible },
                                    dayIncome = dayIncome.value.takeIf { visible },
                                )
                            }.onStart { emit(selected) }
                        }
                    }.onStart { emit(base) }
                }
            }
        snapshots.collect { emit(it) }
    }.retryWhen { failure, attempt ->
        if (failure is CancellationException) throw failure
        emit(WidgetSnapshot(status = WidgetStatus.Error))
        // Keep the subscription alive after transient local database failures.
        delay(minOf(60_000L, 1_000L shl attempt.coerceAtMost(6L).toInt()))
        true
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun readSnapshot(): WidgetSnapshot = withTimeoutOrNull(8_000L) {
        observe().first { it.status != WidgetStatus.Loading }
    } ?: WidgetSnapshot(status = WidgetStatus.OpenApp)
}

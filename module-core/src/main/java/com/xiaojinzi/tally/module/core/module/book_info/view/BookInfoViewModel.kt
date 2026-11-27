package com.xiaojinzi.tally.module.core.module.book_info.view

import com.xiaojinzi.reactive.view.BaseViewModel
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.core.module.book_info.domain.BookInfoUseCase
import com.xiaojinzi.tally.module.core.module.book_info.domain.BookInfoUseCaseImpl
import kotlinx.coroutines.flow.combine

@ViewLayer
class BookInfoViewModel(
    private val useCase: BookInfoUseCase = BookInfoUseCaseImpl(),
) : BaseViewModel(),
    BookInfoUseCase by useCase {

    @StateHotObservable
    val bookListStateObVo = combine(
        AppServices
            .tallyDataSourceSpi
            .allBookStateOb,
        AppServices.tallyDataSourceSpi.subscribeDataBaseTableChangedOb(
            TallyTable.Bill,
            emitOneWhileSubscribe = true,
        )
    ) { bookList, _ ->
        bookList.sortedBy { it.timeCreate }.map { item ->
            val queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                bookIdList = listOf(item.id),
            )
            BookInfoItemVo(
                bookId = item.id,
                icon = AppServices.iconMappingSpi[item.iconName]?.toLocalImageItemDto(),
                bookName = item.name?.toStringItemDto(),
                totalSpending = AppServices.tallyDataSourceSpi.getBillAmountByCondition(
                    queryCondition = queryCondition.copy(
                        typeList = listOf(
                            TallyBillDto.Type.NORMAL,
                            TallyBillDto.Type.REFUND,
                        ),
                        amountLessThanZero = true,
                        isNotCalculate = false,
                    ),
                ).toYuan(),
                totalIncome = AppServices.tallyDataSourceSpi.getBillAmountByCondition(
                    queryCondition = queryCondition.copy(
                        typeList = listOf(
                            TallyBillDto.Type.NORMAL,
                            TallyBillDto.Type.REFUND,
                        ),
                        amountMoreThanZero = true,
                        isNotCalculate = false,
                    ),
                ).toYuan(),
                billCount = AppServices.tallyDataSourceSpi.getBillCountByCondition(
                    queryCondition = queryCondition,
                ),
                timeCreate = item.timeCreate,
            )
        }
    }

}
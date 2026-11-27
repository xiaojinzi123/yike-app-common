package com.xiaojinzi.tally.module.core.module.bill_detail.view

import com.xiaojinzi.reactive.view.BaseViewModel
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.core.module.bill_detail.domain.BillDetailUseCase
import com.xiaojinzi.tally.module.core.module.bill_detail.domain.BillDetailUseCaseImpl
import kotlinx.coroutines.flow.map

@ViewLayer
class BillDetailViewModel(
    private val useCase: BillDetailUseCase = BillDetailUseCaseImpl(),
) : BaseViewModel(),
    BillDetailUseCase by useCase {

    @StateHotObservable
    val billDetailStateObVo = useCase
        .billDetailStateOb
        .map { item ->
            BillDetailVo(
                billId = item?.core?.id,
                type = item?.core?.type,
                bookName = item?.book?.name?.toStringItemDto(),
                categoryImage = AppServices.iconMappingSpi[item?.categoryAdapter?.iconName]?.toLocalImageItemDto(),
                categoryName = item?.categoryAdapter?.name?.toStringItemDto(),
                accountName = item?.account?.getAdapter?.name?.toStringItemDto(),
                transferTargetAccountName = item?.transferTargetAccount?.getAdapter?.name?.toStringItemDto(),
                amount = item?.core?.amount?.toYuan(),
                time = item?.core?.time,
                labelList = (item?.labelList ?: emptyList()).filter {
                    !it.isDeleted
                },
                note = item?.core?.note,
            )
        }

}
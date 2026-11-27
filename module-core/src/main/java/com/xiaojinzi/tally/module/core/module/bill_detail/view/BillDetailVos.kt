package com.xiaojinzi.tally.module.core.module.bill_detail.view

import androidx.annotation.Keep
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto

@Keep
data class BillDetailVo(
    val billId: String?,
    val type: TallyBillDto.Type?,
    val bookName: StringItemDto?,
    val categoryImage: LocalImageItemDto?,
    val categoryName: StringItemDto?,
    val accountName: StringItemDto?,
    val transferTargetAccountName: StringItemDto?,
    val amount: MoneyYuan?,
    val time: Long?,
    val labelList: List<TallyLabelDto>,
    val note: String?,
)
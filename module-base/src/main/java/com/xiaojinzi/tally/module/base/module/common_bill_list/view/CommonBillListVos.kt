package com.xiaojinzi.tally.module.base.module.common_bill_list.view

import androidx.annotation.Keep
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlin.math.absoluteValue


enum class CommonBillListItemClipType {
    None,
    Top,
    Bottom,
    TopAndBottom,
}

@Keep
sealed class CommonBillListItemVo

@Keep
data class CommonBillListNormalItemVo(
    val billId: String,
    val type: TallyBillDto.Type,
    val clipType: CommonBillListItemClipType,
    val bookName: StringItemDto?,
    val categoryIcon: LocalImageItemDto?,
    val categoryName: StringItemDto?,
    val accountName: StringItemDto?,
    val transferTargetAccountName: StringItemDto?,
    val labelNameList: List<String>,
    val amount: Float,
    val isNotCalculate: Boolean,
) : CommonBillListItemVo()

@Keep
data class CommonBillListHeaderItemVo(
    val timeStr: StringItemDto,
    val dayOfWeekStr: StringItemDto,
    val income: MoneyYuan,
    val spending: MoneyYuan,
) : CommonBillListItemVo()

fun TallyBillDetailDto.toCommonBillListNormalItemVo(
    clipType: CommonBillListItemClipType,
): CommonBillListNormalItemVo {
    return CommonBillListNormalItemVo(
        billId = this.core.id,
        type = this.core.type,
        clipType = clipType,
        bookName = this.book?.name?.toStringItemDto(),
        categoryIcon = AppServices.iconMappingSpi[this.categoryAdapter?.iconName]?.toLocalImageItemDto(),
        categoryName = categoryAdapter?.name?.toStringItemDto(),
        accountName = this.account?.getAdapter?.name?.toStringItemDto(),
        transferTargetAccountName = this.transferTargetAccount?.getAdapter?.name?.toStringItemDto(),
        labelNameList = this.labelList.filter { !it.isDeleted }.mapNotNull { it.name.orNull() },
        amount = when (this.core.type) {
            TallyBillDto.Type.TRANSFER -> this.core.amount.toYuan().value.absoluteValue
            else -> this.core.amount.toYuan().value
        },
        isNotCalculate = this.core.isNotCalculate,
    )
}

fun Int.toCommonBillListItemClipType(size: Int): CommonBillListItemClipType {
    return when {
        size == 1 -> CommonBillListItemClipType.TopAndBottom
        this == 0 -> CommonBillListItemClipType.Top
        this == size - 1 -> CommonBillListItemClipType.Bottom
        else -> CommonBillListItemClipType.None
    }
}
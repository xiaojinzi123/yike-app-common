package com.xiaojinzi.tally.module.core.module.book_info.view

import androidx.annotation.Keep
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan

@Keep
data class BookInfoItemVo(
    val bookId: String,
    val icon: LocalImageItemDto?,
    val bookName: StringItemDto?,
    val totalSpending: MoneyYuan,
    val totalIncome: MoneyYuan,
    val billCount: Long,
    val timeCreate: Long,
)
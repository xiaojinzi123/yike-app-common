package com.xiaojinzi.tally.module.core.module.book_select.view

import com.xiaojinzi.reactive.view.BaseViewModel
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.core.module.book_select.domain.BookSelectUseCase
import com.xiaojinzi.tally.module.core.module.book_select.domain.BookSelectUseCaseImpl
import kotlinx.coroutines.flow.map

@ViewLayer
class BookSwitchViewModel(
    private val useCase: BookSelectUseCase = BookSelectUseCaseImpl(),
) : BaseViewModel(),
    BookSelectUseCase by useCase {

    @StateHotObservable
    val allBookStateObVo = useCase.allBookStateOb
        .map { bookList ->
            bookList.map { item ->
                BookSelectItemVo(
                    bookId = item.id,
                    bookIcon = AppServices.iconMappingSpi[item.iconName]?.toLocalImageItemDto(),
                    bookName = item.name?.toStringItemDto(),
                )
            }
        }

}
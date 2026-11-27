package com.xiaojinzi.tally.module.core.module.book_crud.view

import com.xiaojinzi.tally.module.core.module.book_crud.domain.BookCrudUseCase
import com.xiaojinzi.tally.module.core.module.book_crud.domain.BookCrudUseCaseImpl
import com.xiaojinzi.reactive.view.BaseViewModel
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.tally.module.core.module.book_crud.domain.BookTemplate
import kotlinx.coroutines.flow.Flow

@ViewLayer
class BookCrudViewModel(
    private val useCase: BookCrudUseCase = BookCrudUseCaseImpl(),
): BaseViewModel(),
    BookCrudUseCase by useCase{

    override val bookTypeListStateOb: Flow<List<BookTemplate>> = useCase.bookTypeListStateOb

    override val bookTypeSelectedStateOb: Flow<BookTemplate?> = useCase.bookTypeSelectedStateOb

}
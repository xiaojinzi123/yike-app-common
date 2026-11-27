package com.xiaojinzi.tally.module.core.module.book_info.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.support.MenuItemLevel
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class BookInfoIntent {

    data class BookSwitch(val id: String) : BookInfoIntent()

    data class OnMoreClick(
        @UiContext val context: Context,
        val bookId: String,
    ) : BookInfoIntent()

    data class DeleteBook(
        val bookId: String,
    ) : BookInfoIntent()

}

@ViewModelLayer
interface BookInfoUseCase : BusinessMVIUseCase {
}

@ViewModelLayer
class BookInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookInfoUseCase {

    @IntentProcess
    private suspend fun onMoreClick(intent: BookInfoIntent.OnMoreClick) {
        val bookInfo = AppServices.tallyDataSourceSpi.getBookById(
            id = intent.bookId,
        ) ?: return
        val menuList = listOf(
            MenuItem(content = "查看账单".toStringItemDto()),
            MenuItem(content = "编辑账本".toStringItemDto()),
            MenuItem(
                content = "删除账本".toStringItemDto(),
                level = MenuItemLevel.Danger,
            ),
        )
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelect(
                context = intent.context,
                items = ArrayList(menuList),
            )
        when (selectIndex) {
            0 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillListView(
                        context = intent.context,
                        title = bookInfo.name.orEmpty().toStringItemDto(),
                        question = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                            bookIdList = listOf(intent.bookId),
                        )
                    )
            }

            1 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBookCrudView(
                        context = intent.context,
                        bookId = intent.bookId,
                    )
            }

            2 -> {
                addIntent(
                    intent = BookInfoIntent.DeleteBook(
                        bookId = intent.bookId,
                    )
                )
            }
        }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun deleteBook(intent: BookInfoIntent.DeleteBook) {
        val dataSource = AppServices.tallyDataSourceSpi
        val bookInfo = dataSource.getBookById(id = intent.bookId) ?: return
        if (withContext(Dispatchers.IO) { dataSource.getAllBookList().size } <= 1) {
            tip(content = "请至少保留一个账本".toStringItemDto())
            return
        }
        confirmDialogOrError(
            content = "删除「${bookInfo.name.orEmpty()}」会永久删除其中的全部本地数据，是否确定删除？".toStringItemDto(),
        )
        val books = withContext(Dispatchers.IO) { dataSource.getAllBookList() }
        if (books.none { it.id == bookInfo.id }) return
        val nextBook = books.filter { it.id != bookInfo.id }.minByOrNull { it.timeCreate }
        if (nextBook == null) {
            tip(content = "请至少保留一个账本".toStringItemDto())
            return
        }
        if (dataSource.selectedBookStateOb.first()?.id == bookInfo.id) {
            dataSource.switchBook(bookId = nextBook.id)
        }
        dataSource.clearAllDataByBookId(bookId = bookInfo.id)
        tip(content = "账本已删除".toStringItemDto())
    }

    @IntentProcess
    private suspend fun bookSwitch(intent: BookInfoIntent.BookSwitch) {
        if (AppServices.tallyDataSourceSpi.getBookById(id = intent.id) == null) {
            tip(content = "账本不存在".toStringItemDto())
            return
        }
        AppServices
            .tallyDataSourceSpi
            .switchBook(
                bookId = intent.id,
                isTipAfterSwitch = true,
            )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
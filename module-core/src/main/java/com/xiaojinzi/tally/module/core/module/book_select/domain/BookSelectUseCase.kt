package com.xiaojinzi.tally.module.core.module.book_select.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class BookSelectIntent {

    data class ParameterInit(
        val maxCount: Int? = null,
        val bookIdSelectList: Set<String>,
    ) : BookSelectIntent()

    data class SwitchBook(
        val bookId: String,
    ) : BookSelectIntent()

    data class SelectBook(
        val bookId: String,
    ) : BookSelectIntent()

    data class ReturnSelectList(
        @UiContext val context: Context,
    ) : BookSelectIntent()

}

@ViewModelLayer
interface BookSelectUseCase : BusinessMVIUseCase {

    /**
     * 最大选择数量
     */
    @StateHotObservable
    val maxSelectCountStateOb: Flow<Int?>

    /**
     * 所有的账本
     */
    @StateHotObservable
    val allBookStateOb: Flow<List<TallyBookDto>>

    /**
     * 选择的账本的 id 列表
     */
    @StateHotObservable
    val bookIdSelectListStateOb: Flow<Set<String>>

}

@ViewModelLayer
class BookSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookSelectUseCase {

    override val maxSelectCountStateOb = MutableSharedStateFlow<Int?>(
        initValue = null,
    )

    override val allBookStateOb = AppServices.tallyDataSourceSpi.allBookStateOb.map { bookList ->
        bookList.sortedBy { it.timeCreate }
    }

    override val bookIdSelectListStateOb = MutableSharedStateFlow<Set<String>>(
        initValue = emptySet(),
    )

    @IntentProcess
    private suspend fun parameterInit(intent: BookSelectIntent.ParameterInit) {
        val maxCount = intent.maxCount?.coerceAtLeast(1)
        val bookIds = withContext(Dispatchers.IO) {
            AppServices.tallyDataSourceSpi.getAllBookList().map { it.id }.toSet()
        }
        val selectedIds = intent.bookIdSelectList.intersect(bookIds)
        maxSelectCountStateOb.emit(value = maxCount)
        bookIdSelectListStateOb.emit(
            value = if (maxCount == null) selectedIds else selectedIds.take(maxCount).toSet(),
        )
    }

    @IntentProcess
    private suspend fun selectBook(intent: BookSelectIntent.SelectBook) {
        if (AppServices.tallyDataSourceSpi.getBookById(id = intent.bookId) == null) return
        val maxCount = maxSelectCountStateOb.firstOrNull()
        bookIdSelectListStateOb.emit(
            value = bookIdSelectListStateOb.first().toMutableSet().apply {
                if (contains(element = intent.bookId)) {
                    this.remove(element = intent.bookId)
                } else {
                    if (maxCount != null && maxCount <= this.size) {
                        if (maxCount == 1) {
                            this.clear()
                            this.add(element = intent.bookId)
                        } else {
                            tip(
                                content = "最多只能选择 $maxCount 个账本".toStringItemDto(),
                            )
                        }
                    } else {
                        this.add(element = intent.bookId)
                    }
                }
            },
        )
    }

    @IntentProcess
    private suspend fun switchBook(intent: BookSelectIntent.SwitchBook) {
        if (AppServices.tallyDataSourceSpi.getBookById(id = intent.bookId) == null) {
            tip(content = "账本不存在".toStringItemDto())
            return
        }
        AppServices
            .tallyDataSourceSpi
            .switchBook(
                bookId = intent.bookId,
                isTipAfterSwitch = true,
            )
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun returnSelectList(intent: BookSelectIntent.ReturnSelectList) {
        val bookIds = withContext(Dispatchers.IO) {
            AppServices.tallyDataSourceSpi.getAllBookList().map { it.id }.toSet()
        }
        val selectedIds = bookIdSelectListStateOb.first().intersect(bookIds)
        intent
            .context
            .getActivity()?.run {
                this.setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        this.putStringArrayListExtra(
                            "data", ArrayList(
                                selectedIds,
                            )
                        )
                    }
                )
                this.finish()
            }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
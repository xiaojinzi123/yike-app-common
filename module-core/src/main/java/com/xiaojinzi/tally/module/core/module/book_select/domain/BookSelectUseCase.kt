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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

sealed class BookSelectIntent {

    data object Submit : BookSelectIntent()

    data class ParameterInit(
        val maxCount: Int? = null,
        val bookIdSelectList: Set<String>,
    )

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

    override val allBookStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        AppServices
            .tallyDataSourceSpi
            .allBookStateOb,
    ) { userId, bookList ->
        val (list1, list2) = bookList.partition {
            it.userId == userId
        }
        list1.sortedBy { it.timeCreate } + list2.sortedBy { it.timeCreate }
    }

    override val bookIdSelectListStateOb = MutableSharedStateFlow<Set<String>>(
        initValue = emptySet(),
    )

    @IntentProcess
    private suspend fun parameterInit(intent: BookSelectIntent.ParameterInit) {
        maxSelectCountStateOb.emit(
            value = intent.maxCount,
        )
        bookIdSelectListStateOb.emit(
            value = intent.bookIdSelectList,
        )
    }

    @IntentProcess
    private suspend fun selectBook(intent: BookSelectIntent.SelectBook) {
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
        intent
            .context
            .getActivity()?.run {
                this.setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        this.putStringArrayListExtra(
                            "data", ArrayList(
                                bookIdSelectListStateOb.first(),
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
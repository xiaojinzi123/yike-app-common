package com.xiaojinzi.tally.module.core.module.label_select.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class LabelSelectIntent {

    data object Submit : LabelSelectIntent()

    data class ParameterInit(
        val bookId: String? = null,
        val idList: Set<String>
    ) : LabelSelectIntent()

    data class ItemSelect(
        val id: String,
    ) : LabelSelectIntent()

}

@ViewModelLayer
interface LabelSelectUseCase : BusinessMVIUseCase {

    /**
     * 账本 Id
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

    /**
     * 选择的 Id 列表
     */
    @StateHotObservable
    val selectIdSetStateOb: Flow<Set<String>>

    /**
     * 标签列表, 当前账本的
     */
    @StateHotObservable
    val labelListStateOb: Flow<List<TallyLabelDto>>

}

@ViewModelLayer
class LabelSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), LabelSelectUseCase {

    override val bookIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val selectIdSetStateOb = MutableSharedStateFlow<Set<String>>(
        initValue = emptySet(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val labelListStateOb = bookIdStateOb
        .flatMapLatest { bookId ->
            bookId.orNull()?.let { bookId1 ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeAllLabel(
                        bookId = bookId1,
                    )
                    .map { list ->
                        list.filter {
                            !it.isDeleted
                        }
                    }
            } ?: flowOf(value = emptyList())
        }
        .sharedStateIn(
            scope = scope,
        )

    @IntentProcess
    private suspend fun parameterInit(intent: LabelSelectIntent.ParameterInit) {
        bookIdStateOb.emit(
            value = intent.bookId ?: AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb
                .firstOrNull()?.id,
        )
        selectIdSetStateOb.emit(
            value = intent.idList,
        )
    }

    @IntentProcess
    private suspend fun itemSelect(intent: LabelSelectIntent.ItemSelect) {
        selectIdSetStateOb.emit(
            value = selectIdSetStateOb.first().toMutableSet().apply {
                if (contains(element = intent.id)) {
                    remove(element = intent.id)
                } else {
                    add(element = intent.id)
                }
            }
        )
    }

    @IntentProcess
    private suspend fun submit(intent: LabelSelectIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
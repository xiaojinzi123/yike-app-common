package com.xiaojinzi.tally.module.core.module.category_select1.domain

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
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed class CategorySelect1Intent {

    data class ParameterInit(
        val bookId: String,
        val categoryIdSet: Set<String>,
    ) : CategorySelect1Intent()

    data class CheckToggle(
        val categoryId: String,
    ) : CategorySelect1Intent()

    data object Submit : CategorySelect1Intent()

    data class ReturnData(
        @UiContext val context: Context,
    ) : CategorySelect1Intent()

}

@ViewModelLayer
interface CategorySelect1UseCase : BusinessMVIUseCase {

    /**
     * 类别列表
     */
    @StateHotObservable
    val categoryListStateOb: Flow<List<Pair<TallyCategoryDto, List<TallyCategoryDto>>>>

    /**
     * 类别选择的 id List
     */
    @StateHotObservable
    val categoryIdSelectSetStateOb: Flow<Set<String>>

}

@ViewModelLayer
class CategorySelect1UseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CategorySelect1UseCase {

    override val categoryListStateOb =
        MutableSharedStateFlow<List<Pair<TallyCategoryDto, List<TallyCategoryDto>>>>(
            initValue = emptyList()
        )

    override val categoryIdSelectSetStateOb = MutableSharedStateFlow<Set<String>>(
        initValue = emptySet()
    )

    @IntentProcess
    private suspend fun parameterInit(intent: CategorySelect1Intent.ParameterInit) {
        categoryIdSelectSetStateOb.emit(
            value = intent.categoryIdSet,
        )
        val categoryList = AppServices
            .tallyDataSourceSpi
            .getCategoryByBookId(
                bookId = intent.bookId,
            )
        categoryListStateOb.emit(
            value = categoryList
                .filter {
                    it.parentId.isNullOrEmpty()
                }
                .map { parentItem ->
                    parentItem to categoryList
                        .filter {
                            it.parentId == parentItem.id
                        }
                }
        )
    }

    @IntentProcess
    private suspend fun checkToggle(intent: CategorySelect1Intent.CheckToggle) {
        categoryIdSelectSetStateOb.emit(
            value = categoryIdSelectSetStateOb.first().toMutableSet().apply {
                if (contains(element = intent.categoryId)) {
                    remove(element = intent.categoryId)
                } else {
                    add(element = intent.categoryId)
                }
            }
        )
    }

    @IntentProcess
    private suspend fun returnData(intent: CategorySelect1Intent.ReturnData) {
        intent.context.getActivity()?.run {
            this.setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    this.putStringArrayListExtra(
                        "data",
                        ArrayList(
                            categoryIdSelectSetStateOb.first()
                        ),
                    )
                },
            )
            this.finish()
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
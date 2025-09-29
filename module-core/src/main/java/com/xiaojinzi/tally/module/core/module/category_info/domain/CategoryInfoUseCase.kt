package com.xiaojinzi.tally.module.core.module.category_info.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.mutableSharedStateIn
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyDataSourceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed class CategoryInfoIntent {

    data object Submit : CategoryInfoIntent()

    data class ChangeSort(
        val pageIndex: Int,
        val cateId: String,
        val isUp: Boolean,
    ) : CategoryInfoIntent()

}

@ViewModelLayer
interface CategoryInfoUseCase : BusinessMVIUseCase {

    companion object {
        const val TAG = "CategoryInfoUseCase"
    }

    /**
     * 所有类别
     */
    @StateHotObservable
    val allCategoryListStateOb: Flow<List<TallyCategoryDto>>

    /**
     * 支出的类别数据
     */
    @StateHotObservable
    val spendingCategoryListStateOb: Flow<List<Pair<TallyCategoryDto, List<TallyCategoryDto>>>>

    /**
     * 收入的类别数据
     */
    @StateHotObservable
    val incomeCategoryListStateOb: Flow<List<Pair<TallyCategoryDto, List<TallyCategoryDto>>>>

    /**
     * 是否是排序状态
     */
    @StateHotObservable
    val isSortStateOb: MutableSharedStateFlow<Boolean>

}

@ViewModelLayer
class CategoryInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CategoryInfoUseCase {

    private fun List<TallyCategoryDto>.filterTargetTypeList(
        type: TallyCategoryDto.Companion.TallyCategoryType,
    ): Map<TallyCategoryDto, List<TallyCategoryDto>> {
        val map = mutableMapOf<TallyCategoryDto, MutableList<TallyCategoryDto>>()
        this
            .filter { it.type == type }
            .forEach { item ->
                if (item.parentId.isNullOrEmpty()) {
                    map.getOrPut(
                        key = item,
                    ) {
                        mutableListOf()
                    }
                } else {
                    this.find { it.id == item.parentId }?.let { parent ->
                        map.getOrPut(
                            key = parent,
                        ) {
                            mutableListOf()
                        }.add(
                            element = item
                        )
                    }
                }
            }
        return map.map { entity ->
            entity.key to entity.value.sortedByDescending { it.sort }
        }.toMap()
    }

    override val allCategoryListStateOb = TallyDataSourceHelper
        .subscribeCurrentBookCategoryList()
        .map { list ->
            list.filter { !it.isDeleted }
        }
        .sharedStateIn(
             scope = scope,
        )

    override val spendingCategoryListStateOb = allCategoryListStateOb
        .map { categoryList ->
            categoryList
                .filterTargetTypeList(
                    type = TallyCategoryDto.Companion.TallyCategoryType.SPENDING,
                )
                .toList()
                .sortedByDescending { it.first.sort }
        }
        .mutableSharedStateIn(
            scope = scope,
        )

    override val incomeCategoryListStateOb = allCategoryListStateOb
        .map { categoryList ->
            categoryList
                .filterTargetTypeList(
                    type = TallyCategoryDto.Companion.TallyCategoryType.INCOME,
                )
                .toList()
                .sortedByDescending { it.first.sort }
        }
        .mutableSharedStateIn(
            scope = scope,
        )

    override val isSortStateOb = MutableSharedStateFlow(
        initValue = false,
    )

    @IntentProcess
    private suspend fun changeSort(intent: CategoryInfoIntent.ChangeSort) {
        val cateList = when (intent.pageIndex) {
            0 -> spendingCategoryListStateOb.first()
            1 -> incomeCategoryListStateOb.first()
            else -> notSupportError()
        }.map { it.first }
            .sortedByDescending { it.sort }
            .toMutableList()
        cateList.indexOfFirst {
            it.id == intent.cateId
        }.let { itemIndex ->
            val indices = cateList.indices
            val tempItem = cateList.removeAt(index = itemIndex)
            val targetMovedItemIndex = if (intent.isUp) {
                itemIndex - 1
            } else {
                itemIndex + 1
            }.coerceIn(
                minimumValue = indices.first,
                maximumValue = indices.last,
            )
            cateList.add(
                index = targetMovedItemIndex,
                element = tempItem,
            )
            val currentTime = System.currentTimeMillis()
            AppServices
                .tallyDataSourceSpi
                .updateCategoryList(
                    targetList = cateList.mapIndexed { index, item ->
                        item.copy(
                            sort = currentTime - index,
                        )
                    },
                )
        }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: CategoryInfoIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
package com.xiaojinzi.tally.module.core.module.category_select.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class CategorySelectIntent {

    data class ParameterInit(
        val bookId: String?,
        val categoryId: String?,
    ) : CategorySelectIntent()

    data class BookSwitch(
        @UiContext val context: Context,
    ) : CategorySelectIntent()

    data class CategoryGroupSelect(
        val id: String?,
    ) : CategorySelectIntent()

    data class CategoryItemSelect(
        val id: String?,
    ) : CategorySelectIntent()

    data object CategorySetNull : CategorySelectIntent()

    data class ReturnData(
        @UiContext val context: Context,
    ) : CategorySelectIntent()

}

@ViewModelLayer
interface CategorySelectUseCase : BusinessMVIUseCase {

    /**
     * 账本 Id
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

    /**
     * 所有类别的数据
     */
    @StateHotObservable
    val allCategoryListStateOb: Flow<List<TallyCategoryDto>>

    /**
     * 类别的支出的数据
     */
    @StateHotObservable
    val categorySpendingMapStateOb: Flow<Map<TallyCategoryDto, List<TallyCategoryDto>>>

    /**
     * 类别的收入的数据
     */
    @StateHotObservable
    val categoryIncomeMapStateOb: Flow<Map<TallyCategoryDto, List<TallyCategoryDto>>>

    /**
     * 选中的类别组
     */
    @StateHotObservable
    val categoryGroupSelectedStateOb: Flow<TallyCategoryDto?>

    /**
     * 选中的类别 Item
     */
    @StateHotObservable
    val categoryItemSelectedStateOb: Flow<TallyCategoryDto?>

    /**
     * 切换账本
     */
    suspend fun setBookId(
        bookId: String?,
    )

    /**
     * 切换账本
     */
    suspend fun intentForBookSwitch(
        intent: CategorySelectIntent.BookSwitch,
    ): Boolean

    /**
     * 类别组的选择
     */
    suspend fun intentForCategoryGroupSelect(
        intent: CategorySelectIntent.CategoryGroupSelect,
    )

    /**
     * 类别 Item 的选择
     */
    suspend fun intentForCategoryItemSelect(
        intent: CategorySelectIntent.CategoryItemSelect,
    )

    /**
     * 雷贝设置为 null
     */
    suspend fun intentForCategorySetNull(
        intent: CategorySelectIntent.CategorySetNull,
    )

}

@ViewModelLayer
class CategorySelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CategorySelectUseCase {

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
        return map
    }

    override val bookIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategoryListStateOb = bookIdStateOb
        .flatMapLatest { bookId ->
            bookId.orNull()?.let { bokSelected1 ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeAllCategory(
                        bookIdList = listOf(bokSelected1),
                    )
            } ?: flowOf(value = emptyList())
        }
        .map { list ->
            list.filter { !it.isDeleted }
        }
        .sharedStateIn(
            scope = AppScope,
        )

    override val categorySpendingMapStateOb = allCategoryListStateOb
        .map { categoryList ->
            categoryList
                .filterTargetTypeList(
                    type = TallyCategoryDto.Companion.TallyCategoryType.SPENDING,
                )
        }
        .sharedStateIn(
            scope = scope,
        )

    override val categoryIncomeMapStateOb = allCategoryListStateOb
        .map { categoryList ->
            categoryList
                .filterTargetTypeList(
                    type = TallyCategoryDto.Companion.TallyCategoryType.INCOME,
                )
        }
        .sharedStateIn(
            scope = scope,
        )

    override val categoryGroupSelectedStateOb = MutableSharedStateFlow<TallyCategoryDto?>(
        initValue = null,
    )

    override val categoryItemSelectedStateOb = MutableSharedStateFlow<TallyCategoryDto?>(
        initValue = null,
    )

    override suspend fun setBookId(bookId: String?) {
        bookIdStateOb.emit(
            value = bookId,
        )
    }

    @IntentProcess
    private suspend fun intentForParameterInit(
        intent: CategorySelectIntent.ParameterInit,
    ) {
        val initBookId = intent.bookId.orNull() ?: AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb
            .firstOrNull()
            ?.id
        setBookId(
            bookId = initBookId,
        )
        initBookId?.let { bookId ->
            intent.categoryId?.let { categoryId ->
                AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                    id = categoryId,
                    bookId = bookId,
                )?.let { categoryInfo ->
                    if (categoryInfo.parentId.orNull() == null) {
                        categoryGroupSelectedStateOb.emit(
                            value = categoryInfo,
                        )
                    } else {
                        AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                            id = categoryInfo.parentId.orEmpty(),
                            bookId = initBookId,
                        )?.let { parentCategoryInfo ->
                            categoryGroupSelectedStateOb.emit(
                                value = parentCategoryInfo,
                            )
                            categoryItemSelectedStateOb.emit(
                                value = categoryInfo,
                            )
                        }
                    }
                    categoryInfo
                }
            }
        }
    }

    @IntentProcess
    override suspend fun intentForBookSwitch(
        intent: CategorySelectIntent.BookSwitch,
    ): Boolean {
        return ParameterSupport
            .getStringArrayList(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .bookSelect1SuspendForResult(
                        context = intent.context,
                        maxCount = 1,
                    ),
                key = "data",
            )?.firstOrNull()
            .orNull()?.let { bookId ->
                // 全局的 bookId 也切换, 因为去创建标签、账户的时候也要统一的
                AppServices
                    .tallyDataSourceSpi
                    .switchBook(
                        bookId = bookId,
                    )
                // 切换账本, 会重置一些和账本相关的选择
                bookIdStateOb.emit(
                    value = bookId,
                )
                true
            } ?: false
    }

    @IntentProcess
    override suspend fun intentForCategoryGroupSelect(
        intent: CategorySelectIntent.CategoryGroupSelect,
    ) {
        val targetCategoryGroup = intent.id.orNull()?.let { categoryId ->
            AppServices.tallyDataSourceSpi.getCategoryById(
                id = categoryId,
            )
        }
        categoryGroupSelectedStateOb.emit(
            value = targetCategoryGroup,
        )
        categoryItemSelectedStateOb.emit(
            value = null,
        )
    }

    @IntentProcess
    override suspend fun intentForCategoryItemSelect(
        intent: CategorySelectIntent.CategoryItemSelect,
    ) {
        val targetCategoryItem = intent.id.orNull()?.let { categoryId ->
            AppServices.tallyDataSourceSpi.getCategoryById(
                id = categoryId,
            )
        }
        categoryItemSelectedStateOb.emit(
            value = targetCategoryItem,
        )
    }

    @IntentProcess
    override suspend fun intentForCategorySetNull(
        intent: CategorySelectIntent.CategorySetNull,
    ) {
        categoryItemSelectedStateOb.emit(
            value = null,
        )
        categoryGroupSelectedStateOb.emit(
            value = null,
        )
    }

    @IntentProcess
    private suspend fun returnData(
        intent: CategorySelectIntent.ReturnData,
    ) {
        val targetCategory =
            categoryItemSelectedStateOb.firstOrNull() ?: categoryGroupSelectedStateOb.firstOrNull()
        intent.context.getActivity()?.run {
            this.setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    this.putExtra(
                        "data", targetCategory?.id,
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
package com.xiaojinzi.tally.module.core.module.category_sub_info.domain

import androidx.annotation.Keep
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
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.lib.res.model.user.UserInfoCacheDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Keep
data class CategorySubInfoCategoryUseCaseDto(
    val id: String,
    val userId: String,
    val name: String?,
    val iconName: String?,
    val userInfoCache: UserInfoCacheDto?,
    val sort: Long,
    // 是否有删除的权限
    val canDelete: Boolean,
)

sealed class CategorySubInfoIntent {

    data object Submit : CategorySubInfoIntent()

    data class ParameterInit(
        val parentId: String,
    ) : CategorySubInfoIntent()

    data class ChangeSort(
        val cateId: String,
        val isUp: Boolean,
    ) : CategorySubInfoIntent()

    data class ItemDelete(
        val categoryId: String,
    ) : CategorySubInfoIntent()

}

@ViewModelLayer
interface CategorySubInfoUseCase : BusinessMVIUseCase {

    /**
     * 初始化的 id
     * 不支持对应类别的 parentId 有值的情况. 也就是说只支持一级类别的 id
     */
    val parentIdStateOb: Flow<String?>

    /**
     * 对应的类别
     */
    @StateHotObservable
    val parentCategoryInfoStateOb: Flow<CategorySubInfoCategoryUseCaseDto?>

    /**
     * 子类别的列表
     */
    @StateHotObservable
    val subCategoryListStateOb: Flow<List<CategorySubInfoCategoryUseCaseDto>>

    /**
     * 是否是排序状态
     */
    @StateHotObservable
    val isSortStateOb: MutableSharedStateFlow<Boolean>

}

@ViewModelLayer
class CategorySubInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CategorySubInfoUseCase {

    override val parentIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val parentCategoryInfoStateOb = combine(
        AppServices.tallyDataSourceSpi.subscribeDataBaseTableChangedOb(
            TallyTable.Category,
            emitOneWhileSubscribe = true,
        ),
        AppServices.userSpi.latestUserIdStateOb,
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
        parentIdStateOb,
    ) { _, currentUserId, bookInfo, id ->
        bookInfo?.let {
            id.orNull()?.let {
                AppServices
                    .tallyDataSourceSpi
                    .getCategoryByIdAndBookId(
                        id = it,
                        bookId = bookInfo.id,
                    )
                    ?.getAdapter?.run {
                        CategorySubInfoCategoryUseCaseDto(
                            id = this.id,
                            userId = this.userId,
                            name = this.name,
                            iconName = this.iconName,
                            userInfoCache = AppServices
                                .tallyDataSourceSpi
                                .getCacheUserInfo(
                                    userId = this.userId,
                                ),
                            sort = this.sort,
                            canDelete = currentUserId != null && currentUserId == this.userId,
                        )
                    }
            }
        }
    }.sharedStateIn(
        scope = scope,
        initValue = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val subCategoryListStateOb = parentCategoryInfoStateOb
        .flatMapLatest { categoryInfo ->
            categoryInfo?.let { categoryInfo1 ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeDataBaseTableChangedOb(
                        TallyTable.Category, emitOneWhileSubscribe = true,
                    ).map {
                        val currentUserId = AppServices.userSpi.latestUserIdStateOb.firstOrNull()
                        AppServices
                            .tallyDataSourceSpi
                            .getCategoryByParentId(
                                parentId = categoryInfo1.id,
                            )
                            .mapNotNull { it.getAdapter }
                            .sortedByDescending { it.sort }
                            .map { item ->
                                CategorySubInfoCategoryUseCaseDto(
                                    id = item.id,
                                    userId = item.userId,
                                    name = item.name,
                                    iconName = item.iconName,
                                    userInfoCache = AppServices
                                        .tallyDataSourceSpi
                                        .getCacheUserInfo(
                                            userId = item.userId,
                                        ),
                                    sort = item.sort,
                                    canDelete = currentUserId != null && currentUserId == item.userId,
                                )
                            }
                    }
            } ?: flowOf(value = emptyList())
        }
        .sharedStateIn(
            scope = scope,
        )

    override val isSortStateOb = MutableSharedStateFlow(
        initValue = false,
    )

    @IntentProcess
    private suspend fun parameterInit(intent: CategorySubInfoIntent.ParameterInit) {
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        val parentCategoryInfo = AppServices
            .tallyDataSourceSpi
            .getCategoryByIdAndBookId(
                id = intent.parentId,
                bookId = bookInfo.id,
            )
            ?.getAdapter
        if (parentCategoryInfo == null || parentCategoryInfo.parentId.orNull() != null) {
            tip(
                content = "类别不合法".toStringItemDto(),
            )
            return
        }
        parentIdStateOb.emit(
            value = intent.parentId,
        )
    }

    @IntentProcess
    private suspend fun changeSort(intent: CategorySubInfoIntent.ChangeSort) {
        val currentBookInfo = AppServices.tallyDataSourceSpi.requiredSelectedBookInfo()
        val cateList = subCategoryListStateOb
            .first()
            .mapNotNull {
                AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                    id = it.id,
                    bookId = currentBookInfo.id,
                )
            }
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
    private suspend fun itemDelete(intent: CategorySubInfoIntent.ItemDelete) {
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        confirmDialogOrError(
            content = "确定删除这个类别吗?".toStringItemDto(),
        )
        AppServices
            .tallyDataSourceSpi
            .apply {
                this.getCategoryByIdAndBookId(
                    id = intent.categoryId,
                    bookId = bookInfo.id,
                )?.let { target ->
                    this.updateCategory(
                        target = target.copy(
                            isDeleted = true,
                        )
                    )
                }
            }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: CategorySubInfoIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
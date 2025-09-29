package com.xiaojinzi.tally.module.core.module.category_crud.domain

import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.ui.text.input.TextFieldValue
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
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
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryInsertDto
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

sealed class CategoryCrudIntent {

    data object Submit : CategoryCrudIntent()

    data object Delete : CategoryCrudIntent()

    data class IconSelect(
        @UiContext val context: Context,
    ) : CategoryCrudIntent()

    data class ParameterInit(
        val id: String?,
        val parentId: String?,
        val categoryType: TallyCategoryDto.Companion.TallyCategoryType?,
    ) : CategoryCrudIntent()

}

@ViewModelLayer
interface CategoryCrudUseCase : BusinessMVIUseCase {

    /**
     * 编辑的 Id
     */
    @StateHotObservable
    val editIdStateOb: Flow<String?>

    /**
     * 编辑的 Id
     */
    @StateHotObservable
    val editInfoStateOb: Flow<TallyCategoryDto?>

    /**
     * 是否是编辑
     */
    @StateHotObservable
    val isEditStateOb: Flow<Boolean>

    /**
     * 是否可以删除
     */
    @StateHotObservable
    val canDeleteStateOb: Flow<Boolean>

    /**
     * 父容器的 Id
     */
    @StateHotObservable
    val parentIdStateOb: Flow<String?>

    /**
     * 类别
     */
    @StateHotObservable
    val typeStateOb: Flow<TallyCategoryDto.Companion.TallyCategoryType>

    /**
     * Icon 的名字
     */
    @StateHotObservable
    val iconNameStateOb: Flow<String?>

    /**
     * Icon 的资源 Id
     */
    @StateHotObservable
    val iconRsdStateOb: Flow<Int?>

    /**
     * 账户名
     */
    @StateHotObservable
    val nameStateOb: MutableStateFlow<TextFieldValue>

}

@ViewModelLayer
class CategoryCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CategoryCrudUseCase {

    override val editIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val editInfoStateOb = combine(
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
        editIdStateOb,
    ) { bookInfo, editId ->
        bookInfo?.let {
            editId?.let {
                AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                    id = editId,
                    bookId = bookInfo.id,
                )
            }
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val isEditStateOb = editInfoStateOb
        .map { it != null }

    override val canDeleteStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        editInfoStateOb,
    ) { selfUserId, categoryInfo ->
        selfUserId != null && selfUserId == categoryInfo?.userId
    }

    override val parentIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val typeStateOb = MutableSharedStateFlow(
        initValue = TallyCategoryDto.Companion.TallyCategoryType.SPENDING,
    )

    override val iconNameStateOb = MutableStateFlow<String?>(value = null)

    override val iconRsdStateOb = iconNameStateOb.map { iconName ->
        AppServices.iconMappingSpi.get(
            iconName = iconName,
        )
    }

    override val nameStateOb = MutableStateFlow(value = TextFieldValue())

    @IntentProcess
    private suspend fun parameterInit(intent: CategoryCrudIntent.ParameterInit) {
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        if (intent.id.isNullOrEmpty()) {
            if (intent.parentId.isNullOrEmpty()) {
                if (intent.categoryType == null) {
                    tip(
                        content = "类别类型不能为空".toStringItemDto(),
                    )
                    postActivityFinishEvent()
                } else {
                    typeStateOb.emit(
                        value = intent.categoryType,
                    )
                }
            } else {
                AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                    id = intent.parentId,
                    bookId = bookInfo.id,
                )?.let { categoryInfo ->
                    parentIdStateOb.emit(
                        value = categoryInfo.id,
                    )
                    typeStateOb.emit(
                        value = categoryInfo.type,
                    )
                } ?: run {
                    tip(
                        content = "父类别不存在".toStringItemDto(),
                    )
                    postActivityFinishEvent()
                }
            }
        } else {
            AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                id = intent.id,
                bookId = bookInfo.id,
            )?.let { categoryInfo ->
                editIdStateOb.emit(
                    value = categoryInfo.id,
                )
                iconNameStateOb.emit(
                    value = categoryInfo.iconName,
                )
                nameStateOb.emit(
                    value = TextFieldValue(
                        text = categoryInfo.name.orEmpty(),
                    )
                )
            } ?: run {
                tip(
                    content = "没有可编辑的类别".toStringItemDto(),
                )
                postActivityFinishEvent()
            }
        }
    }

    @IntentProcess
    private suspend fun delete(intent: CategoryCrudIntent.Delete) {
        val editInfo = editInfoStateOb.first() ?: return
        confirmDialogOrError(
            content = "确定删除吗？".toStringItemDto(),
        )
        AppServices
            .tallyDataSourceSpi
            .updateCategory(
                target = editInfo.copy(
                    isDeleted = true,
                )
            )
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun iconSelect(intent: CategoryCrudIntent.IconSelect) {
        val iconName = ParameterSupport.getString(
            intent = AppRouterCoreApi::class
                .routeApi()
                .iconSelectBySuspend(
                    context = intent.context,
                ),
            key = "data",
        )
        iconNameStateOb.emit(
            value = iconName,
        )
    }

    @IntentProcess
    private suspend fun submit(intent: CategoryCrudIntent.Submit) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val bookInfo = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        val iconName = iconNameStateOb.firstOrNull()
        if (iconName.orNull() == null) {
            tip(
                content = "请选择 Icon".toStringItemDto(),
            )
            return
        }
        val parentId = parentIdStateOb.first()
        val type = typeStateOb.first()
        val name = nameStateOb.firstOrNull()?.text.orNull()
        if (name.isNullOrEmpty()) {
            tip(
                content = "请输入类别名".toStringItemDto(),
            )
            return
        }
        val editInfo = editInfoStateOb.first()
        AppServices.tallyDataSourceSpi.run {
            if (editInfo == null) {
                this.insertCategory(
                    target = TallyCategoryInsertDto(
                        userId = userId,
                        bookId = bookInfo.id,
                        parentId = parentId,
                        type = type.dbStr,
                        name = name,
                        iconName = iconName,
                    )
                )
            } else {
                this.updateCategory(
                    target = editInfo.copy(
                        name = name,
                        iconName = iconName,
                    )
                )
            }
        }
        tip(
            content = "提交成功".toStringItemDto(),
        )
        postActivityFinishEvent()
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
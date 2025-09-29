package com.xiaojinzi.tally.module.core.module.label_crud.domain

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

sealed class LabelCrudIntent {

    data object Submit : LabelCrudIntent()

    data object Delete : LabelCrudIntent()

    data class ParameterInit(
        val labelId: String? = null,
    ) : LabelCrudIntent()

}

@ViewModelLayer
interface LabelCrudUseCase : BusinessMVIUseCase {

    /**
     * 标签的数量
     */
    @StateHotObservable
    val labelCountStateOb: Flow<Int?>

    /**
     * 标签名称
     */
    @StateHotObservable
    val labelNameStateOb: MutableSharedStateFlow<TextFieldValue>

    /**
     * 编辑的标签的 id
     */
    @StateHotObservable
    val editLabelIdStateOb: Flow<String?>

    /**
     * 编辑的对象
     */
    @StateHotObservable
    val editLabelInfoStateOb: Flow<TallyLabelDto?>

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

}

@ViewModelLayer
class LabelCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), LabelCrudUseCase {

    override val labelCountStateOb = combine(
        AppServices
            .tallyDataSourceSpi
            .subscribeDataBaseTableChangedOb(
                TallyTable.Label, emitOneWhileSubscribe = true,
            ),
        AppServices
            .tallyDataSourceSpi
            .selectedBookStateOb,
    ) { _, currentBookInfo ->
        currentBookInfo?.let {
            AppServices
                .tallyDataSourceSpi
                .getAllLabelUnderBook(
                    bookId = currentBookInfo.id,
                    isExcludeDeleted = true,
                )
                .count()
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val labelNameStateOb = MutableSharedStateFlow(
        initValue = TextFieldValue("")
    )

    override val editLabelIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val editLabelInfoStateOb = combine(
        AppServices.tallyDataSourceSpi.selectedBookStateOb,
        editLabelIdStateOb,
    ) { bookSelected, editLabelId ->
        bookSelected?.let {
            editLabelId?.let {
                AppServices
                    .tallyDataSourceSpi
                    .getLabelUnderBook(
                        bookId = bookSelected.id,
                        id = editLabelId,
                    )
            }
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val isEditStateOb = editLabelInfoStateOb.map {
        it != null
    }

    override val canDeleteStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        editLabelInfoStateOb,
    ) { selfUserId, labelInfo ->
        selfUserId != null && selfUserId == labelInfo?.userId
    }

    @IntentProcess
    private suspend fun parameterInit(intent: LabelCrudIntent.ParameterInit) {
        val book = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        intent.labelId.orNull()?.let { id ->
            val editLabel = AppServices
                .tallyDataSourceSpi
                .getLabelUnderBook(
                    bookId = book.id,
                    id = id,
                )
            val labelName = editLabel?.name.orEmpty()
            editLabelIdStateOb.emit(
                value = editLabel?.id,
            )
            labelNameStateOb.emit(
                value = TextFieldValue(
                    labelName,
                    selection = TextRange(
                        index = labelName.length,
                    ),
                )
            )
        }
    }

    @IntentProcess
    private suspend fun submit(intent: LabelCrudIntent.Delete) {
        editLabelInfoStateOb.first()?.let { editLabelInfo ->
            confirmDialogOrError(
                content = "确定要删除吗?".toStringItemDto(),
            )
            AppServices.tallyDataSourceSpi.updateLabel(
                target = editLabelInfo.copy(
                    isDeleted = true,
                )
            )
            tip(
                content = "删除成功".toStringItemDto(),
            )
            postActivityFinishEvent()
        }
    }

    @IntentProcess
    private suspend fun submit(intent: LabelCrudIntent.Submit) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val book = AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()
            ?: throw NoBookSelectException()
        val labelName = labelNameStateOb.first().text
        val editLabelInfo = editLabelInfoStateOb.firstOrNull()
        // 是插入
        if (editLabelInfo == null) {
            AppServices.tallyDataSourceSpi.insertLabel(
                target = TallyLabelInsertDto(
                    userId = userId,
                    bookId = book.id,
                    name = labelName,
                )
            )
        } else {
            AppServices.tallyDataSourceSpi.updateLabel(
                target = editLabelInfo.copy(
                    name = labelName,
                )
            )
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
package com.xiaojinzi.tally.module.core.module.book_crud.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.reactive.template.domain.DialogUseCase
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyRemoteBookTypeResDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

sealed class BookCrudIntent {

    data object Submit : BookCrudIntent()

    data class ItemSelect(
        val item: TallyRemoteBookTypeResDto,
    ) : BookCrudIntent()

}

@ViewModelLayer
interface BookCrudUseCase : BusinessMVIUseCase {

    /**
     * 账本名字
     */
    @StateHotObservable
    val bookNameStateOb: MutableSharedStateFlow<String>

    /**
     * 账本列表
     */
    @StateHotObservable
    val bookTypeListStateOb: Flow<List<TallyRemoteBookTypeResDto>>

    /**
     * 选择的账本类型
     */
    @StateHotObservable
    val bookTypeSelectedStateOb: Flow<TallyRemoteBookTypeResDto?>

}

@ViewModelLayer
class BookCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookCrudUseCase {

    override val bookNameStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    override val bookTypeListStateOb = MutableSharedStateFlow<List<TallyRemoteBookTypeResDto>>(
        initValue = emptyList(),
    )

    override val bookTypeSelectedStateOb = MutableSharedStateFlow<TallyRemoteBookTypeResDto?>(
        initValue = null,
    )

    override suspend fun initData() {
        super.initData()
        bookTypeListStateOb.emit(
            value = AppServices.appNetworkSpi.getBookTypeList(),
        )
    }

    @IntentProcess
    private suspend fun itemSelect(intent: BookCrudIntent.ItemSelect) {
        bookTypeSelectedStateOb.emit(
            value = intent.item,
        )
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: BookCrudIntent.Submit) {
        val typeItem = bookTypeSelectedStateOb.firstOrNull()
        val name = bookNameStateOb.firstOrNull().orNull()
        if (typeItem == null) {
            tip(content = "请选择账本类型".toStringItemDto())
            return
        }
        val targetBookName = name.orNull() ?: run {
            bookNameStateOb.emit(
                value = typeItem.name.orEmpty()
            )
            typeItem.name.orNull()
        }
        if (targetBookName == null) {
            tip(content = "请填写账本名称".toStringItemDto())
            return
        }

        // 创建账本
        val bookNecessaryInfo = AppServices
            .appNetworkSpi
            .createBook(
                type = typeItem.type,
                name = targetBookName,
            )
        // 插入到数据库
        AppServices
            .tallyDataSourceSpi
            .insertBookNecessaryInfo(
                bookNecessaryInfo = bookNecessaryInfo,
            )
        tip(content = " 创建成功".toStringItemDto())
        // 询问是否切换到新账本
        when (
            confirmDialog(
                content = "是否切换到新创建的账本?".toStringItemDto(),
                positive = "切换".toStringItemDto(),
                negative = "不切换".toStringItemDto(),
            )
        ) {
            DialogUseCase.ConfirmDialogResultType.CONFIRM -> {
                AppServices
                    .tallyDataSourceSpi
                    .switchBook(
                        bookId = bookNecessaryInfo.book.id,
                        isTipAfterSwitch = true,
                    )
            }

            else -> {}
        }
        // 销毁界面
        postActivityFinishEvent()
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
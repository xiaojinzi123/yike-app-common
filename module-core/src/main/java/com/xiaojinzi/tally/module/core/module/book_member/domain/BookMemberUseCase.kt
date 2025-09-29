package com.xiaojinzi.tally.module.core.module.book_member.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableInitOnceData
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.exception.NotLoggedInException
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookMemberResDto
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

sealed class BookMemberIntent {

    data object Submit : BookMemberIntent()

    data class ToInvite(
        @UiContext val context: Context,
    ) : BookMemberIntent()

    data class ToRemoveOther(
        val targetUserId: String,
    ) : BookMemberIntent()

}

@ViewModelLayer
interface BookMemberUseCase : BusinessMVIUseCase {

    /**
     * 账本 Id 的初始化数据
     */
    val bookIdInitData: MutableInitOnceData<String>

    /**
     * 账本信息
     */
    @StateHotObservable
    val bookInfoStateOb: Flow<TallyBookDto?>

    /**
     * 是否可分享
     */
    @StateHotObservable
    val canShareStateOb: Flow<Boolean>

    /**
     * 成员列表
     */
    @StateHotObservable
    val memberListStateOb: Flow<List<TallyBookMemberResDto>>

}

@ViewModelLayer
class BookMemberUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookMemberUseCase {

    override val bookIdInitData = MutableInitOnceData<String>()

    override val bookInfoStateOb = bookIdInitData
        .valueStateFlow
        .map {
            AppServices.tallyDataSourceSpi.getBookById(id = it)
        }
        .sharedStateIn(
            scope = scope,
        )

    override val canShareStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        bookInfoStateOb,
    ) { selfUserId, bookInfo ->
        !selfUserId.isNullOrEmpty() && selfUserId == bookInfo?.userId
    }

    override val memberListStateOb = MutableSharedStateFlow<List<TallyBookMemberResDto>>(
        initValue = emptyList(),
    )

    @IntentProcess
    private suspend fun toInvite(intent: BookMemberIntent.ToInvite) {
        val bookId = bookIdInitData.awaitValue()
        AppRouterCoreApi::class
            .routeApi()
            .toBookInviteView(
                context = intent.context,
                bookId = bookId,
            )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun toRemoveOther(intent: BookMemberIntent.ToRemoveOther) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        if (userId != intent.targetUserId) {
            confirmDialogOrError(
                content = "移除之前, 请确保对方未同步的数据已经同步到远程\n确定要移除吗?".toStringItemDto(),
            )
            // 清退别人
            AppServices
                .appNetworkSpi
                .exitBook(
                    bookId = bookIdInitData.awaitValue(),
                    targetUserId = intent.targetUserId,
                )
            // 删除这个 item
            memberListStateOb.emit(
                value = memberListStateOb.first().filter {
                    it.userInfo.id != intent.targetUserId
                },
            )
        }
    }

    override suspend fun initData() {
        super.initData()
        val bookId = bookIdInitData.awaitValue()
        memberListStateOb.emit(
            value = AppServices.appNetworkSpi.getBookMemberList(
                bookId = bookId,
            ).partition {
                it.isOwner
            }.run {
                this.first + this.second
            }
        )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
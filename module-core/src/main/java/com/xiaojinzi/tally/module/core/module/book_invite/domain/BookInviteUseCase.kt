package com.xiaojinzi.tally.module.core.module.book_invite.domain

import android.graphics.Bitmap
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableInitOnceData
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.toJson
import com.xiaojinzi.tally.lib.res.model.tally.BookInviteShareResDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class BookInviteIntent {

    data object Submit : BookInviteIntent()

}

@ViewModelLayer
interface BookInviteUseCase : BusinessMVIUseCase {

    /**
     * 账本 Id 的初始化数据
     */
    val bookIdInitData: MutableInitOnceData<String>

    /**
     * 账本邀请信息
     */
    @StateHotObservable
    val bookInviteInfoStateOb: Flow<BookInviteShareResDto?>

    /**
     * 二维码的 bitmap
     */
    @StateHotObservable
    val qrcodeBitmapStateOb: Flow<Bitmap?>

    /**
     * 过期时间
     */
    @StateHotObservable
    val expiredTimeStateOb: Flow<Long?>

}

@ViewModelLayer
class BookInviteUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BookInviteUseCase {

    override val bookIdInitData = MutableInitOnceData<String>()

    override val bookInviteInfoStateOb = MutableSharedStateFlow<BookInviteShareResDto?>(
        initValue = null,
    )

    override val qrcodeBitmapStateOb = MutableSharedStateFlow<Bitmap?>(
        initValue = null,
    )

    override val expiredTimeStateOb = bookInviteInfoStateOb
        .map {
            it?.expiredTime
        }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: BookInviteIntent.Submit) {
        // TODO
    }

    override suspend fun initData() {
        super.initData()
        // 获取分享二维码信息
        val shareInfo = AppServices.appNetworkSpi.createBookShareInfo(
            bookId = bookIdInitData.awaitValue(),
        )
        bookInviteInfoStateOb.emit(
            value = shareInfo,
        )
        val shareInfoJson = buildMap {
            this["code"] = shareInfo.code
            this["bookId"] = shareInfo.bookId
        }.toJson()
        qrcodeBitmapStateOb.emit(
            value = AppServices.qrCodeSpi?.createQRCode(
                content = shareInfoJson,
                width = 400, height = 400,
            )
        )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
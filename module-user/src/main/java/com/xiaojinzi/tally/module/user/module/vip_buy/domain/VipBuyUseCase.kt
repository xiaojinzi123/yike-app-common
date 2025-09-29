package com.xiaojinzi.tally.module.user.module.vip_buy.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.module.common.base.support.CommonServices
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.user.VipItemResDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

sealed class VipBuyIntent {

    data class Submit(
        @UiContext val context: Context,
        val itemId: String,
    ) : VipBuyIntent()

}

@ViewModelLayer
interface VipBuyUseCase : BusinessMVIUseCase {

    /**
     * 可购买项目的列表
     */
    @StateHotObservable
    val itemsStateOb: Flow<List<VipItemResDto>>

}

@ViewModelLayer
class VipBuyUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), VipBuyUseCase {

    override val itemsStateOb = MutableSharedStateFlow<List<VipItemResDto>>(
        initValue = emptyList()
    )

    override suspend fun initData() {
        super.initData()
        itemsStateOb.emit(
            value = AppServices
                .appNetworkSpi
                .getVipItems()
                .sortedByDescending { it.sort }
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: VipBuyIntent.Submit) {

        val alipayOrderNo = AppServices
            .appNetworkSpi
            .createAlipayVipOrder(
                itemId = intent.itemId
            )

        CommonServices
            .alipaySpi
            ?.pay(
                context = intent.context,
                orderInfo = alipayOrderNo,
            )

        tip(
            content = "支付成功".toStringItemDto(),
        )

        // 充值成功的话, 下次快到期了就还是要提醒
        AppServices
            .appConfigSpi
            .switchTipBeforeVipExpire(
                b = true,
            )

        // 为了让服务端能收到支付的回调, 然后更新会员信息
        delay(800)

        // 刷新 vip 信息
        AppServices
            .userSpi
            .updateVipInfoAction()
            .awaitIgnoreException()

        // 触发一次全部账本的同步
        AppServices
            .tallyDataSyncSpi
            ?.trySync(
                bookIdList = null,
            )

        // 结束界面
        commonUseCase.postActivityFinishEvent()

    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
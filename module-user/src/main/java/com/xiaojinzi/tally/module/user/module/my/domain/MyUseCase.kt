package com.xiaojinzi.tally.module.user.module.my.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.timeAtLeast
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.QQ_GROUP_LINK
import com.xiaojinzi.tally.lib.res.model.support.DayMillisecond
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class MyIntent {

    data object Submit : MyIntent()

    data object VipRefresh : MyIntent()

    data class Feedback(
        @UiContext val context: Context,
    ) : MyIntent()

}

@ViewModelLayer
interface MyUseCase : BusinessMVIUseCase {

    /**
     * 记账的天数
     */
    @StateHotObservable
    val totalDayStateOb: Flow<Long?>

    /**
     * 记账总笔数
     */
    @StateHotObservable
    val totalBillCountStateOb: Flow<Long>

}

@ViewModelLayer
class MyUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), MyUseCase {

    override val totalDayStateOb = AppServices
        .userSpi
        .userInfoStateOb
        .map { userInfo ->
            userInfo?.let { userInfo1 ->
                (System.currentTimeMillis() - userInfo1.timeCreate) / DayMillisecond + 1
            }
        }

    override val totalBillCountStateOb = AppServices
        .tallyDataSourceSpi
        .subscribeBillCount(
            queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(),
        )

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun vipRefresh(intent: MyIntent.VipRefresh) {
        timeAtLeast (
            timeMillis = 800L,
        ){
            AppServices
                .userSpi
                .updateVipInfoAction()
                .awaitIgnoreException()
        }
    }

    @IntentProcess
    private suspend fun feedback(intent: MyIntent.Feedback) {
        confirmDialogOrError(
            content = "需要进入 QQ 群进行反馈\n是否继续?".toStringItemDto(),
        )
        Router
            .with(
                context = intent.context,
            ).url(
                url = QQ_GROUP_LINK,
            ).forward()
    }

    @IntentProcess
    private suspend fun submit(intent: MyIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
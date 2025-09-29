package com.xiaojinzi.tally.module.main.module.setting.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.BusinessUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.timeAtLeast
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.QQ_GROUP_LINK
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices

sealed class SettingIntent {

    data object Submit : SettingIntent()

    data class CheckUpdate(
        @UiContext val context: Context,
    ) : SettingIntent()

    data class Feedback(
        @UiContext val context: Context,
    ) : SettingIntent()

    data class ToLoginOut(
        @UiContext val context: Context,
    ) : SettingIntent()

    data class ToLogOff(
        @UiContext val context: Context,
    ) : SettingIntent()

}

@ViewModelLayer
interface SettingUseCase : BusinessMVIUseCase

@ViewModelLayer
class SettingUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), SettingUseCase {

    @IntentProcess
    private suspend fun checkUpdate(intent: SettingIntent.CheckUpdate) {
        AppRouterMainApi::class
            .routeApi()
            .toAppUpdateView(
                context = intent.context,
                isTip = true,
            )
    }

    @IntentProcess
    private suspend fun feedback(intent: SettingIntent.Feedback) {
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
    private suspend fun submit(intent: SettingIntent.Submit) {
        // TODO
    }

    @IntentProcess
    private suspend fun toLoginOut(intent: SettingIntent.ToLoginOut) {
        confirmDialogOrError(
            content = "退出后不会删除任何历史数据, 下次登录依然可以使用本账号".toStringItemDto(),
        )
        showLoading()
        kotlin.runCatching {
            AppServices.userSpi.logoutForBusinessLogic()
        }
        hideLoading()
    }

    @IntentProcess
    private suspend fun toLogOff(intent: SettingIntent.ToLogOff) {
        // 去确认注销登录
        AppRouterUserApi::class
            .routeApi()
            .signOutConfirmBySuspend(context = intent.context)
        // 再次弹出一个框框进行确认
        confirmDialogOrError(
            title = "警告".toStringItemDto(),
            content = "注销后将不能恢复, 确认注销吗?".toStringItemDto(),
        )
        showLoading()
        try {
            timeAtLeast {
                // 调用注销登录的接口
                AppServices.appNetworkSpi.logOff()
            }
            tip(
                content = "注销成功".toStringItemDto(),
            )
            // 退出登录
            AppServices.userSpi.logoutForBusinessLogic()
        } finally {
            hideLoading()
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
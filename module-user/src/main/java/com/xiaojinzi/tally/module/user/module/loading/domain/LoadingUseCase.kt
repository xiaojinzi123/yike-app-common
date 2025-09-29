package com.xiaojinzi.tally.module.user.module.loading.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.ui.APP_ACTIVITY_FLAG_MAIN
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.finishAppAllTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

sealed class LoadingIntent {

    data class GO(
        @UiContext val context: Context,
    ) : LoadingIntent()

}

@ViewModelLayer
interface LoadingUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class LoadingUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), LoadingUseCase {

    private suspend fun goNext(
        @UiContext context: Context,
    ) {
        val currentUserInfo = AppServices
            .userSpi
            .userInfoStateOb
            .firstOrNull()
        val latestUserId = AppServices
            .userSpi
            .latestUserIdStateOb
            .firstOrNull()
        // 没登录过
        if (latestUserId == null) {
            AppRouterUserApi::class
                .routeApi()
                .toLoginView(
                    context = context,
                ) {
                    context.tryFinishActivity()
                }
        } else {
            currentUserInfo?.let {
                // 更新 Token 信息
                AppServices
                    .userSpi
                    .updateTokenInfoAction()
                    .awaitIgnoreException()
            }
            // 如果主界面存在, 就关闭当前界面, 否则就启动一个
            val isMainViewExist = ActivityStack.any {
                it.hasFlag(
                    flag = APP_ACTIVITY_FLAG_MAIN,
                )
            }
            if (isMainViewExist) {
                // 不然太快了, 会闪
                delay(800)
                context.tryFinishActivity()
            } else {
                // 去主界面
                AppRouterMainApi::class
                    .routeApi()
                    .toMainView(
                        context = context,
                    ) {
                        context.tryFinishActivity()
                    }
            }
        }
    }

    @IntentProcess
    private suspend fun go(
        intent: LoadingIntent.GO,
    ) {
        val forOpenSource = AppServices
            .appInfoSpi
            .forOpenSource
        val isAgreedPrivacyAgreement =
            AppServices.appConfigSpi.isAgreedPrivacyAgreementStateOb.first()
        try {
            if (!forOpenSource) {
                if (!isAgreedPrivacyAgreement) {
                    AppRouterUserApi::class
                        .routeApi()
                        .privacyAgreementBySuspend(
                            context = intent.context,
                        )
                    // 进行本地写入
                    AppServices.appConfigSpi.isAgreedPrivacyAgreementStateOb.emit(
                        value = true
                    )
                }
            }
            goNext(
                context = intent.context,
            )
        } catch (e: Exception) {
            finishAppAllTask()
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
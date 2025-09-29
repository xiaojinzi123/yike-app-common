package com.xiaojinzi.tally.module.main.module.theme_select.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.timeAtLeast
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelectSimple
import kotlinx.coroutines.flow.first

sealed class ThemeSelectIntent {

    data object Submit : ThemeSelectIntent()

    data class ThemeColorSet(
        @UiContext val context: Context,
        val isNeedVip: Boolean,
        val themeName: String,
    ) : ThemeSelectIntent()

    data class OnMoreClick(
        @UiContext val context: Context,
    ) : ThemeSelectIntent()

}

/**
 * https://www.sohu.com/a/580697550_121124712
 * https://zhuanlan.zhihu.com/p/551176430
 */
@ViewModelLayer
interface ThemeSelectUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class ThemeSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), ThemeSelectUseCase {

    @IntentProcess
    private suspend fun submit(intent: ThemeSelectIntent.Submit) {
        // TODO
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun themeColorSet(intent: ThemeSelectIntent.ThemeColorSet) {
        if (intent.isNeedVip) {
            val isVip = AppServices.userSpi.isVipStateOb.first()
            if (!isVip) {
                confirmDialogOrError(
                    content = "此配色需要开通会员\n去开通会员".toStringItemDto(),
                )
                AppRouterUserApi::class
                    .routeApi()
                    .toVipBuyView(
                        context = intent.context,
                    )
                return
            }
        }
        timeAtLeast(timeMillis = 600) {
            AppServices.appInfoSpi.switchTheme(
                themeName = intent.themeName,
            )
        }
    }

    @IntentProcess
    private suspend fun onMoreClick(intent: ThemeSelectIntent.OnMoreClick) {

        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "亮色",
                    "暗色",
                    "跟随系统",
                ).map { it.toStringItemDto() },
            )

        val themeIndex = when (selectIndex) {
            0 -> 1
            1 -> 2
            else -> 0
        }

        showLoading()

        timeAtLeast(timeMillis = 1000) {
            AppServices.appInfoSpi.switchTheme(
                themeIndex = themeIndex,
            )
        }

        hideLoading()

    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
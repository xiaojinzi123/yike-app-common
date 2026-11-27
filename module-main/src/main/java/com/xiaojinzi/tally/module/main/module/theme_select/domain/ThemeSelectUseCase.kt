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
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelectSimple

sealed class ThemeSelectIntent {

    data class ThemeColorSet(
        val themeName: String,
    ) : ThemeSelectIntent()

    data class OnMoreClick(
        @UiContext val context: Context,
    ) : ThemeSelectIntent()

}

@ViewModelLayer
interface ThemeSelectUseCase : BusinessMVIUseCase

@ViewModelLayer
class ThemeSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), ThemeSelectUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun themeColorSet(intent: ThemeSelectIntent.ThemeColorSet) {
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

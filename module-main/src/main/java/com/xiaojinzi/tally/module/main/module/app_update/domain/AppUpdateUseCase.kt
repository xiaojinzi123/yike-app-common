package com.xiaojinzi.tally.module.main.module.app_update.domain

import com.xiaojinzi.module.common.base.support.CommonServices
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableInitOnceData
import com.xiaojinzi.tally.lib.res.model.app_update.AppUpdateResDto

sealed class AppUpdateIntent {

    data object Submit : AppUpdateIntent()

    data object IgnoreThisVersion : AppUpdateIntent()

}

@ViewModelLayer
interface AppUpdateUseCase : BusinessMVIUseCase {

    /**
     * 更新的信息
     */
    val appInfoInitData: MutableInitOnceData<AppUpdateResDto>

}

@ViewModelLayer
class AppUpdateUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AppUpdateUseCase {

    override val appInfoInitData = MutableInitOnceData<AppUpdateResDto>()

    @IntentProcess
    private suspend fun ignoreThisVersion(intent: AppUpdateIntent.IgnoreThisVersion) {
        val appInfo = appInfoInitData.awaitValue()
        CommonServices.spService
            ?.putBool(
                "appUpdateIgnore",
                key = "version: ${appInfo.versionCode}",
                value = true,
            )
        postActivityFinishEvent()
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
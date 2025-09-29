package com.xiaojinzi.tally.module.main.module.app_share.domain

import com.xiaojinzi.component.impl.service.ServiceManager
import com.xiaojinzi.lib.common.res.share.PlatformShareInfoDto
import com.xiaojinzi.lib.common.res.share.ShareInfoDto
import com.xiaojinzi.lib.common.res.share.ShareType
import com.xiaojinzi.module.common.base.spi.ShareSpi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppServices

sealed class AppShareIntent {

    data object Submit : AppShareIntent()

    data object ShareAppForWxChat : AppShareIntent()

    data object ShareAppForWxSTATE : AppShareIntent()

    data object CopyToClipboard : AppShareIntent()

}

@ViewModelLayer
interface AppShareUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class AppShareUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AppShareUseCase {

    @IntentProcess
    private suspend fun shareAppForWxChat(intent: AppShareIntent.ShareAppForWxChat) {
        ServiceManager
            .get(
                tClass = ShareSpi::class, name = PlatformShareInfoDto.PLATFORM_WX_CHAT,
            )
            ?.run {
                this.share(
                    shareInfo = PlatformShareInfoDto(
                        platform = PlatformShareInfoDto.PLATFORM_WX_CHAT,
                        core = ShareInfoDto(
                            shareType = ShareType.Link,
                            title = "一刻记账".toStringItemDto(),
                            description = "一个简约而不简单的记账 App".toStringItemDto(),
                            link = AppServices.appInfoSpi.officialUrl,
                            thumbImageRsd = AppServices.appInfoSpi.appLauncherIconRsd,
                        ),
                    )
                )
            }
    }

    @IntentProcess
    private suspend fun shareAppForWxSTATE(intent: AppShareIntent.ShareAppForWxSTATE) {
        ServiceManager
            .get(
                tClass = ShareSpi::class, name = PlatformShareInfoDto.PLATFORM_WX_CHAT,
            )
            ?.run {
                this.share(
                    shareInfo = PlatformShareInfoDto(
                        platform = PlatformShareInfoDto.PLATFORM_WX_STATE,
                        core = ShareInfoDto(
                            shareType = ShareType.Link,
                            title = "一刻记账".toStringItemDto(),
                            description = "一个简约而不简单的记账 App".toStringItemDto(),
                            link = AppServices.appInfoSpi.officialUrl,
                            thumbImageRsd = AppServices.appInfoSpi.appLauncherIconRsd,
                        ),
                    )
                )
            }
    }

    @IntentProcess
    private suspend fun copyToClipboard(intent: AppShareIntent.CopyToClipboard) {
        AppServices
            .systemSpi
            ?.copyToClipboard(
                content = AppServices.appInfoSpi.officialUrl,
            )
        tip(
            content = "复制成功".toStringItemDto(),
        )
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun submit(intent: AppShareIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
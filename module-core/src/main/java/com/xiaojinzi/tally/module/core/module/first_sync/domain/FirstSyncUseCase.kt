package com.xiaojinzi.tally.module.core.module.first_sync.domain

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
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.timeAtLeast
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.model.exception.NotLoggedInException
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.DevelopHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

sealed class FirstSyncIntent {

    data class StartInit(
        @UiContext val context: Context,
    ) : FirstSyncIntent()

}

@ViewModelLayer
interface FirstSyncUseCase : BusinessMVIUseCase {

    enum class FirstSyncState {
        // 正在同步
        SYNCING,

        // 同步成功
        SYNC_SUCCESS,

        // 同步失败
        SYNC_FAIL,
    }

    @StateHotObservable
    val syncStateStateOb: Flow<FirstSyncState>

}

@ViewModelLayer
class FirstSyncUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), FirstSyncUseCase {

    override val syncStateStateOb = MutableSharedStateFlow(
        initValue = FirstSyncUseCase.FirstSyncState.SYNCING,
    )

    @IntentProcess
    private suspend fun startInit(intent: FirstSyncIntent.StartInit) {
        syncStateStateOb.emit(
            value = FirstSyncUseCase.FirstSyncState.SYNCING,
        )
        val result = runCatching {
            timeAtLeast(timeMillis = 3000) {
                val userInfo =
                    AppServices.userSpi.userInfoStateOb.firstOrNull() ?: throw NotLoggedInException()
                // 进行数据同步
                AppServices
                    .tallyDataSourceSpi
                    .syncFirstData(
                        userId = userInfo.id,
                    )
            }
        }
        if (DevelopHelper.isDevelop) {
            result.exceptionOrNull()?.printStackTrace()
        }
        if (result.isSuccess) {
            tip(content = "同步成功".toStringItemDto())
            // 去主界面
            AppRouterMainApi::class
                .routeApi()
                .toMainView(
                    context = intent.context,
                ) {
                    intent.context.tryFinishActivity()
                }
        } else {
            syncStateStateOb.emit(
                value = FirstSyncUseCase.FirstSyncState.SYNC_FAIL,
            )
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
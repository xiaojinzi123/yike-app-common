package com.xiaojinzi.tally.module.core.module.sync_log.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class SyncLogIntent {

    data object Submit : SyncLogIntent()

}

@ViewModelLayer
interface SyncLogUseCase : BusinessMVIUseCase {

}

@ViewModelLayer
class SyncLogUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), SyncLogUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: SyncLogIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
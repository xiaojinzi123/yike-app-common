package com.xiaojinzi.tally.module.core.module.bill_cycle_crud.sub_module.repeat_count.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class RepeatCountIntent {

    data object Submit : RepeatCountIntent()

}

@ViewModelLayer
interface RepeatCountUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class RepeatCountUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), RepeatCountUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: RepeatCountIntent.Submit) {
        // TODO
    }
    
    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
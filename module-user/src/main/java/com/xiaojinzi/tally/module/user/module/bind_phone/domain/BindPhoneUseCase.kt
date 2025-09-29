package com.xiaojinzi.tally.module.user.module.bind_phone.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class BindPhoneIntent {

    data object Submit : BindPhoneIntent()

}

@ViewModelLayer
interface BindPhoneUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class BindPhoneUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BindPhoneUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: BindPhoneIntent.Submit) {
        // TODO
    }
    
    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
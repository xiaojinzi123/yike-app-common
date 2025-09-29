package com.xiaojinzi.tally.module.user.module.log_off_confirm.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class SignOutConfirmIntent {

    data object Submit : SignOutConfirmIntent()

}

@ViewModelLayer
interface SignOutConfirmUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class SignOutConfirmUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), SignOutConfirmUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: SignOutConfirmIntent.Submit) {
        // TODO
    }
    
    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
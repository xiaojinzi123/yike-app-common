package com.xiaojinzi.tally.module.user.module.vip_expire_remind.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class VipExpireRemindIntent {

    data object Submit : VipExpireRemindIntent()

}

@ViewModelLayer
interface VipExpireRemindUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class VipExpireRemindUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), VipExpireRemindUseCase {

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: VipExpireRemindIntent.Submit) {
        // TODO
    }
    
    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
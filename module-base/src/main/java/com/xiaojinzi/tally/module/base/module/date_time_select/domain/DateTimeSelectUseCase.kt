package com.xiaojinzi.tally.module.base.module.date_time_select.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

sealed class DateTimeSelectIntent {

    data object Submit : DateTimeSelectIntent()

}

@ViewModelLayer
interface DateTimeSelectUseCase : BusinessMVIUseCase {
    // TODO
}

@ViewModelLayer
class DateTimeSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), DateTimeSelectUseCase {

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: DateTimeSelectIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
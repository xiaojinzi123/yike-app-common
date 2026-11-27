package com.xiaojinzi.tally.module.main.module.setting.domain

import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer

@ViewModelLayer
interface SettingUseCase : BusinessMVIUseCase

@ViewModelLayer
class SettingUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), SettingUseCase {

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}

package com.xiaojinzi.tally.module.base.module.center_menu_select.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.HotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.model.support.MenuItem

@ViewModelLayer
interface CenterMenuSelectUseCase : BusinessMVIUseCase {

    /**
     * 显示的数据
     */
    @HotObservable(HotObservable.Pattern.BEHAVIOR, isShared = true)
    val dataListObservableDto: MutableSharedStateFlow<List<MenuItem>>

    /**
     * 返回数据
     */
    fun returnData(context: Context, index: Int?)

}

@ViewModelLayer
class CenterMenuSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CenterMenuSelectUseCase {

    override val dataListObservableDto =
        MutableSharedStateFlow<List<MenuItem>>(initValue = emptyList())

    override fun returnData(context: Context, index: Int?) {
        if (index != null) {
            context.getActivity()?.let { act ->
                act.setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        this.putExtra("data", index)
                    }
                )
            }
        }
        context.tryFinishActivity()
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
package com.xiaojinzi.tally.module.core.module.bill_cycle.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.compose.StateBar
import com.xiaojinzi.support.ktx.initOnceUseViewModel
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.view.BaseBusinessAct
import com.xiaojinzi.tally.module.base.support.interceptor.OpenSourceNotSupportRouterInterceptor
import kotlinx.coroutines.InternalCoroutinesApi

@RouterAnno(
    hostAndPath = AppRouterConfig.CORE_BILL_CYCLE,
    interceptors = [
        OpenSourceNotSupportRouterInterceptor::class,
    ],
)
@ViewLayer
class BillCycleAct : BaseBusinessAct<BillCycleViewModel>() {

    override fun getViewModelClass(): Class<BillCycleViewModel> {
        return BillCycleViewModel::class.java
    }

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        initOnceUseViewModel {
        }

        setContent {
            AppTheme {
                StateBar {
                    BillCycleViewWrap()
                }
            }
        }

    }

}
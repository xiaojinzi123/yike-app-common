package com.xiaojinzi.tally.module.core.module.account_crud.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xiaojinzi.component.anno.AttrValueAutowiredAnno
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.compose.StateBar
import com.xiaojinzi.support.ktx.initOnceUseViewModel
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.view.BaseBusinessAct
import com.xiaojinzi.tally.module.core.module.account_crud.domain.AccountCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * 1. 创建账户
 * 2. 编辑账户的功能
 */
@RouterAnno(
    hostAndPath = AppRouterConfig.CORE_ACCOUNT_CRUD,
)
@ViewLayer
class AccountCrudAct : BaseBusinessAct<AccountCrudViewModel>() {

    @AttrValueAutowiredAnno("accountId")
    var accountId: String? = null

    override fun getViewModelClass(): Class<AccountCrudViewModel> {
        return AccountCrudViewModel::class.java
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
            requiredViewModel().addIntent(
                intent = AccountCrudIntent.DataInit(
                    accountId = accountId,
                )
            )
        }

        setContent {
            AppTheme {
                StateBar {
                    AccountCrudViewWrap()
                }
            }
        }

    }

}
package com.xiaojinzi.tally.module.core.module.bill_image_crud.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import com.xiaojinzi.component.anno.AttrValueAutowiredAnno
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.view.BaseBusinessAct
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.support.annotation.ViewLayer
import com.xiaojinzi.support.compose.StateBar
import com.xiaojinzi.support.ktx.initOnceUseViewModel
import com.xiaojinzi.support.ktx.translateStatusBar
import com.xiaojinzi.tally.module.core.module.bill_image_crud.domain.BillImageCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

@RouterAnno(
    hostAndPath = AppRouterConfig.CORE_BILL_IMAGE_CURD,
)
@ViewLayer
class BillImageCrudAct : BaseBusinessAct<BillImageCrudViewModel>() {

    @AttrValueAutowiredAnno("imageUrlList")
    var imageUrlList: ArrayList<String> = arrayListOf()

    override fun getViewModelClass(): Class<BillImageCrudViewModel> {
        return BillImageCrudViewModel::class.java
    }

    @OptIn(
        InternalCoroutinesApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.translateStatusBar()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        initOnceUseViewModel {
            requiredViewModel().addIntent(
                intent = BillImageCrudIntent.ParameterInit(
                    imageUrlList = imageUrlList,
                )
            )
        }

        setContent {
            AppTheme {
                StateBar {
                    BillImageCrudViewWrap()
                }
            }
        }

    }

}
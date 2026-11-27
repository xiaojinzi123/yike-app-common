package com.xiaojinzi.tally.module.core.module.category_crud.view

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
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.view.BaseBusinessAct
import com.xiaojinzi.tally.module.core.module.category_crud.domain.CategoryCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

@RouterAnno(
    hostAndPath = AppRouterConfig.CORE_CATEGORY_CRUD,
)
@ViewLayer
class CategoryCrudAct : BaseBusinessAct<CategoryCrudViewModel>() {

    @AttrValueAutowiredAnno("id")
    var id: String? = null

    @AttrValueAutowiredAnno("parentId")
    var parentId: String? = null

    @AttrValueAutowiredAnno("categoryType")
    var categoryType: TallyCategoryDto.Companion.TallyCategoryType? = null

    override fun getViewModelClass(): Class<CategoryCrudViewModel> {
        return CategoryCrudViewModel::class.java
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
                intent = CategoryCrudIntent.ParameterInit(
                    id = id,
                    parentId = parentId,
                    categoryType = categoryType,
                )
            )
        }

        setContent {
            AppTheme {
                StateBar {
                    CategoryCrudViewWrap()
                }
            }
        }

    }

}
package com.xiaojinzi.tally.module.core.module.bill_crud.view

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
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.view.BaseBusinessAct
import com.xiaojinzi.tally.module.core.module.bill_crud.domain.BillCrudIntent
import kotlinx.coroutines.InternalCoroutinesApi

@RouterAnno(
    hostAndPath = AppRouterConfig.CORE_BILL_CURD,
)
@ViewLayer
class BillCrudAct : BaseBusinessAct<BillCrudViewModel>() {

    // 编辑的时候用
    @AttrValueAutowiredAnno("billId")
    var billId: String? = null

    // 如果 billId 为 null, refundBillId 不为 null, 则表示生成一个退款单
    // billId 如果有值, 则表示一定是编辑账单, 其他参数基本上都是无效的, 包括 refundBillId
    @AttrValueAutowiredAnno("associatedRefundBillId")
    var associatedRefundBillId: String? = null

    @AttrValueAutowiredAnno("billType")
    var initBillType: TallyBillDto.Type = TallyBillDto.Type.NORMAL

    @AttrValueAutowiredAnno("initBookId")
    var initBookId: String? = null

    @AttrValueAutowiredAnno("initCategoryId")
    var initCategoryId: String? = null

    @AttrValueAutowiredAnno("initAccountId")
    var initAccountId: String? = null

    @AttrValueAutowiredAnno("initTransferAccountId")
    var initTransferAccountId: String? = null

    @AttrValueAutowiredAnno("initTransferTargetAccountId")
    var initTransferTargetAccountId: String? = null

    @AttrValueAutowiredAnno("initLabelIdList")
    var initLabelIdList: ArrayList<String>? = null

    @AttrValueAutowiredAnno("initImageUrlList")
    var initImageUrlList: ArrayList<String>? = null

    @AttrValueAutowiredAnno("initTime")
    var initTime: Long? = null

    @AttrValueAutowiredAnno("initAmount")
    var initAmount: Long? = null

    override fun getViewModelClass(): Class<BillCrudViewModel> {
        return BillCrudViewModel::class.java
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
                intent = BillCrudIntent.ParameterInit(
                    billId = billId,
                    associatedRefundBillId = associatedRefundBillId,
                    bookId = initBookId,
                    billType = initBillType,
                    categoryId = initCategoryId,
                    accountId = initAccountId,
                    transferAccountId = initTransferAccountId,
                    transferTargetAccountId = initTransferTargetAccountId,
                    labelIdSet = initLabelIdList?.toSet() ?: emptySet(),
                    imageUrlList = initImageUrlList ?: emptyList(),
                    time = initTime,
                    amount = initAmount,
                )
            )
        }

        setContent {
            AppTheme {
                StateBar {
                    BillCrudViewWrap()
                }
            }
        }

    }

}
package com.xiaojinzi.tally.module.core.module.bill_detail.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.HotStateFlow
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillImageDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelectSimple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

sealed class BillDetailIntent {

    data class InitParameter(
        val billId: String,
    ) : BillDetailIntent()

    data object ToDeleteBill : BillDetailIntent()

    data class ToEditBill(
        @UiContext val context: Context,
    ) : BillDetailIntent()

    data class ToSelectMenu(
        @UiContext val context: Context,
    ) : BillDetailIntent()

    data class ToOriginBillDetailView(
        @UiContext val context: Context,
    ) : BillDetailIntent()

    data class ToAssociatedRefundBillDetailListView(
        @UiContext val context: Context,
    ) : BillDetailIntent()

    data object Submit : BillDetailIntent()

}

@ViewModelLayer
interface BillDetailUseCase : BusinessMVIUseCase {

    /**
     * 账单 Id
     */
    val billIdStateOb: HotStateFlow<String?>

    /**
     * 账单详情
     */
    val billDetailStateOb: HotStateFlow<TallyBillDetailDto?>

    /**
     * 退款关联账单
     */
    val associatedRefundBillDetailListCountState: HotStateFlow<Long>

    /**
     * 是否可编辑
     */
    val canEditStateOb: HotStateFlow<Boolean>

    /**
     * 是否支持退款
     */
    val isSupportRefundState: HotStateFlow<Boolean>

    /**
     * 账单的图片列表
     */
    val billImageListStateOb: HotStateFlow<List<TallyBillImageDto>>

}

@ViewModelLayer
class BillDetailUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillDetailUseCase {

    override val billIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val billDetailStateOb = billIdStateOb
        .flatMapLatest { billId ->
            billId.orNull()?.let { billId1 ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeBillDetailById(
                        id = billId1,
                    )
            } ?: flowOf(value = null)
        }
        .sharedStateIn(
            scope = scope,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val associatedRefundBillDetailListCountState = billDetailStateOb
        .flatMapLatest { billDetail ->
            billDetail?.core?.let { billCore ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeAssociatedRefundBillListCount(
                        billId = billCore.id,
                    )
            } ?: flowOf(value = 0L)
        }
        .sharedStateIn(
            scope = scope,
        )

    override val canEditStateOb = combine(
        AppServices.userSpi.latestUserIdStateOb,
        billDetailStateOb,
    ) { userId, billDetail ->
        userId != null && userId == billDetail?.core?.userId
    }

    override val isSupportRefundState = combine(
        AppServices.userSpi.latestUserIdStateOb,
        billDetailStateOb,
    ) { userId, billDetail ->
        if (userId .isNullOrEmpty()) {
            false
        } else {
            billDetail?.core?.userCanRefund(targetUserId = userId)?: false
        }
    }

    override val billImageListStateOb = combine(
        AppServices.tallyDataSourceSpi.subscribeDataBaseTableChangedOb(
            TallyTable.BillImage,
            emitOneWhileSubscribe = true,
        ),
        billDetailStateOb,
    ) { _, billDetailInfo ->
        billDetailInfo?.let { billDetailInfo1 ->
            billDetailInfo.book?.let { bookInfo ->
                AppServices
                    .tallyDataSourceSpi
                    .getBillImageListByBillId(
                        bookId = bookInfo.id,
                        billId = billDetailInfo1.core.id,
                    )
                    .filter {
                        !it.isDeleted
                    }
            }
        } ?: emptyList()
    }.sharedStateIn(
        scope = scope,
    )

    @IntentProcess
    private suspend fun initParameter(
        intent: BillDetailIntent.InitParameter,
    ) {
        billIdStateOb.emit(
            value = intent.billId,
        )
    }

    @IntentProcess
    private suspend fun toDeleteBill(
        intent: BillDetailIntent.ToDeleteBill,
    ) {
        val billId = billIdStateOb.firstOrNull().orNull() ?: return
        confirmDialogOrError(
            title = "⚠️".toStringItemDto(),
            content = "您确定要删除这个账单吗?".toStringItemDto(),
        )
        AppServices
            .tallyDataSourceSpi
            .getBillDetailById(
                id = billId,
            )?.core?.let { billInfo ->
                AppServices
                    .tallyDataSourceSpi
                    .updateBill(
                        target = billInfo.copy(
                            isDeleted = true,
                        ),
                    )
            }
        tip(content = "删除成功".toStringItemDto())
        postActivityFinishEvent()
    }

    @IntentProcess
    private suspend fun toEditBill(intent: BillDetailIntent.ToEditBill) {
        val billId = billIdStateOb.firstOrNull().orNull() ?: return
        AppRouterCoreApi::class
            .routeApi()
            .toBillCrudView(
                context = intent.context,
                billId = billId,
            )
    }

    @IntentProcess
    private suspend fun toSelectMenu(intent: BillDetailIntent.ToSelectMenu) {
        val billDetail = billDetailStateOb.firstOrNull() ?: return
        val isSupportRefund = isSupportRefundState.firstOrNull() ?: false
        if (!isSupportRefund) {
            return
        }
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "退款",
                ).map { it.toStringItemDto() }
            )
        when (selectIndex) {
            0 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillCrudView(
                        context = intent.context,
                        associatedRefundBillId = billDetail.core.id,
                    )
            }
        }
    }

    @IntentProcess
    private suspend fun toOriginBillDetailView(intent: BillDetailIntent.ToOriginBillDetailView) {
        val billDetail = billDetailStateOb.firstOrNull() ?: return
        val originBillId = billDetail.core.originBillId
        if (originBillId.isNullOrEmpty()) {
            commonUseCase.tip(
                content = "原始订单未关联".toStringItemDto(),
            )
        } else {
            AppRouterCoreApi::class
                .routeApi()
                .toBillDetailView(
                    context = intent.context,
                    billId = originBillId,
                )
        }
    }

    @IntentProcess
    private suspend fun toAssociatedRefundBillDetailListView(
        intent: BillDetailIntent.ToAssociatedRefundBillDetailListView,
    ) {
        val billDetail = billDetailStateOb.firstOrNull() ?: return
        AppRouterCoreApi::class
            .routeApi()
            .toBillListView(
                context = intent.context,
                title = "关联退款账单".toStringItemDto(),
                question = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                    originBillIdList = listOf(
                        billDetail.core.id,
                    ),
                )
            )
    }

    @IntentProcess
    private suspend fun submit(intent: BillDetailIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
package com.xiaojinzi.tally.module.base.module.common_bill_list.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.reactive.template.domain.DialogUseCase
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.mutableSharedStateIn
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.support.MenuItemLevel
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.centerMenuSelect
import com.xiaojinzi.tally.module.base.support.copyBillToBillCrudView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import okio.withLock
import java.util.concurrent.locks.ReentrantLock

private enum class PopMenu {
    COPY,
    REFUND,
    EDIT,
    DELETE,
}

sealed class CommonBillListIntent {

    data object LoadMore : CommonBillListIntent()

    data class ToBillDetail(
        @param:UiContext val context: Context,
        val billId: String,
    ) : CommonBillListIntent()

    data class PopMenu(
        @param:UiContext val context: Context,
        val billId: String,
    ) : CommonBillListIntent()

}

interface CommonBillQueryConditionUseCase : com.xiaojinzi.reactive.domain.BaseUseCase {

    /**
     * 查询条件
     * 默认是 null 的, 记得赋值
     * null 表示不展示任何数据
     */
    @StateHotObservable
    val queryConditionStateOb: MutableSharedStateFlow<TallyDataSourceSpi.Companion.BillQueryConditionDto?>

}

class CommonBillQueryConditionUseCaseImpl :
    com.xiaojinzi.reactive.domain.BaseUseCaseImpl(), CommonBillQueryConditionUseCase {

    override val queryConditionStateOb =
        MutableSharedStateFlow<TallyDataSourceSpi.Companion.BillQueryConditionDto?>(
            initValue = null,
        )

}

/**
 * 这个通用的列表类. 正常工作需要的条件
 * 1. 一个查询的条件对象 [TallyDataSourceSpi.Companion.BillQueryConditionDto]
 */
interface CommonBillListUseCase : BusinessMVIUseCase {

    companion object {
        const val TAG = "CommonBillListUseCase"
    }

    /**
     * 第几页
     */
    @StateHotObservable
    val pageStateOb: Flow<Int>

    /**
     * 要显示的账单.
     * 会根据上面的时间
     */
    @StateHotObservable
    val billListStateOb: Flow<List<TallyBillDetailDto>>

}

class CommonBillListUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    private val commonBillQueryConditionUseCase: CommonBillQueryConditionUseCase,
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), CommonBillListUseCase {

    private val loadMoreLock = ReentrantLock()

    override val pageStateOb = commonBillQueryConditionUseCase
        .queryConditionStateOb
        .map { _ ->
            1
        }
        .mutableSharedStateIn(
            scope = scope,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val billListStateOb = commonBillQueryConditionUseCase
        .queryConditionStateOb
        // 先根据条件查询 day 的时间段
        .flatMapLatest { queryCondition ->
            LogSupport.d(
                tag = CommonBillListUseCase.TAG,
                content = "billListStateOb flatMapLatest queryCondition = $queryCondition",
            )
            if (queryCondition == null) {
                flowOf(value = emptyList())
            } else {
                pageStateOb
                    .distinctUntilChanged()
                    .flatMapLatest { page ->
                        LogSupport.d(
                            tag = CommonBillListUseCase.TAG,
                            content = "billListStateOb flatMapLatest page = $page",
                        )
                        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
                        tallyDataSourceSpi
                            .subscribeDataBaseTableChangedOb(
                                TallyTable.Bill,
                                TallyTable.BillLabel,
                                TallyTable.Account,
                                TallyTable.Category,
                                emitOneWhileSubscribe = true,
                            )
                            // 计算出起始时间
                            .map {
                                val dayTimeList = tallyDataSourceSpi.getBillDayTimeListByCondition(
                                    queryCondition = queryCondition.copy(
                                        typeList = emptyList(),
                                        pageInfo = TallyDataSourceSpi.Companion.PageInfo(
                                            pageStartIndex = 0,
                                            pageSize = page * 10,
                                        ),
                                    ),
                                )
                                val startTime = dayTimeList.minByOrNull { it.value }?.toTimeStamp()
                                if (startTime == null) {
                                    emptyList()
                                } else // 占位
                                {
                                    AppServices
                                        .tallyDataSourceSpi
                                        .getBillDetailListByCondition(
                                            queryCondition = queryCondition.copy(
                                                startTimeInclude = maxOf(
                                                    a = startTime.value,
                                                    b = queryCondition.startTimeInclude
                                                        ?: Long.MIN_VALUE,
                                                )
                                            ),
                                        )
                                }
                            }
                            .onEach {
                                LogSupport.d(
                                    tag = CommonBillListUseCase.TAG,
                                    content = "billListStateOb resultList.size = ${it.size}",
                                )
                            }
                    }
            }
        }
        .sharedStateIn(
            scope = scope,
            initValue = emptyList(),
        )

    @IntentProcess
    private suspend fun popMenu(
        intent: CommonBillListIntent.ToBillDetail,
    ) {
        AppRouterCoreApi::class
            .routeApi()
            .toBillDetailView(
                context = intent.context,
                billId = intent.billId,
            )
    }

    @IntentProcess
    private suspend fun popMenu(
        intent: CommonBillListIntent.PopMenu,
    ) {
        val billDetail = AppServices.tallyDataSourceSpi.getBillDetailById(
            id = intent.billId,
        ) ?: return
        val currentUserId = AppServices.userSpi.requiredLastUserId()
        // 是否是我的订单
        val isMineBill = billDetail.core.userId == currentUserId
        // 是否支持退款
        val isSupportRefund = billDetail.core.userCanRefund(
            targetUserId = currentUserId,
        )
        val selectItemList = buildList {
            if (
            // 只有是我的账单或者是普通账单或者是转账账单才支持复制
                isMineBill || billDetail.core.type in listOf(
                    TallyBillDto.Type.NORMAL,
                    TallyBillDto.Type.TRANSFER
                )
            )
                this.add(
                    element = MenuItem(
                        content = "复制".toStringItemDto(),
                        flag = PopMenu.COPY.name,
                    ),
                )
            if (isSupportRefund) {
                this.add(
                    element = MenuItem(
                        content = "退款".toStringItemDto(),
                        flag = PopMenu.REFUND.name,
                    ),
                )
            }
            if (isMineBill) {
                this.add(
                    element = MenuItem(
                        content = "编辑".toStringItemDto(),
                        flag = PopMenu.EDIT.name,
                    ),
                )
                this.add(
                    element = MenuItem(
                        content = "删除".toStringItemDto(),
                        level = MenuItemLevel.Danger,
                        flag = PopMenu.DELETE.name,
                    ),
                )
            }
        }
        // 如果没有可用的菜单就返回
        if (selectItemList.isEmpty()) {
            return
        }
        val menuIndex = AppRouterBaseApi::class
            .routeApi()
            .centerMenuSelect(
                context = intent.context,
                items = selectItemList,
            )

        when (
            selectItemList.getOrNull(index = menuIndex)?.flag
        ) {
            PopMenu.COPY.name -> {
                val billTime = when (
                    confirmDialog(
                        content = "是否使用当前使用".toStringItemDto(),
                        positive = "当前时间".toStringItemDto(),
                        negative = "账单时间".toStringItemDto(),
                    )
                ) {
                    DialogUseCase.ConfirmDialogResultType.CONFIRM -> {
                        null
                    }

                    DialogUseCase.ConfirmDialogResultType.CANCEL -> {
                        billDetail.core.time
                    }
                }
                intent.billId.orNull()?.copyBillToBillCrudView(
                    context = intent.context,
                    billTime = billTime,
                )
            }

            PopMenu.REFUND.name -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillCrudView(
                        context = intent.context,
                        associatedRefundBillId = billDetail.core.id,
                    )
            }

            PopMenu.EDIT.name -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillCrudView(
                        context = intent.context,
                        billId = billDetail.core.id,
                    )
            }

            PopMenu.DELETE.name -> {
                if (isMineBill) {
                    confirmDialogOrError(
                        content = "确定删除这条账单吗?".toStringItemDto(),
                    )
                    AppServices.tallyDataSourceSpi.updateBill(
                        target = billDetail.core.copy(
                            isDeleted = true,
                        )
                    )
                }
            }
        }

    }

    @IntentProcess
    private suspend fun loadMore(
        intent: CommonBillListIntent.LoadMore,
    ) {
        kotlin.runCatching {
            loadMoreLock.withLock {
                val nextPage = (pageStateOb.first() + 1)
                val queryCondition =
                    commonBillQueryConditionUseCase.queryConditionStateOb.first() ?: return
                val dayTimeList = AppServices.tallyDataSourceSpi.getBillDayTimeListByCondition(
                    queryCondition = queryCondition.copy(
                        pageInfo = TallyDataSourceSpi.Companion.PageInfo(
                            pageStartIndex = 0,
                            pageSize = nextPage * 10,
                        ),
                    ),
                )
                // 如果列表的数量大于 (nextPage - 1) * pageSize, 说明下一页是存在的
                if (dayTimeList.size > (nextPage - 1) * 10) {
                    LogSupport.d(
                        tag = CommonBillListUseCase.TAG,
                        content = "loadMore page = $nextPage",
                    )
                    pageStateOb.emit(
                        value = nextPage,
                    )
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
        commonBillQueryConditionUseCase.destroy()
    }

}
package com.xiaojinzi.tally.module.core.module.bill_cycle.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.EventPublisher
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.APP_EVENT_REFRESH_BILL_CYCLE
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.support.MenuItemLevel
import com.xiaojinzi.tally.lib.res.model.tally.BillCycleResDto
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

sealed class BillCycleIntent {

    data object Submit : BillCycleIntent()

    data class StateToggle(
        val id: Long,
    ) : BillCycleIntent()

    data class RunOnce(
        val id: Long,
    ) : BillCycleIntent()

    data class DeleteTask(
        val id: Long,
    ) : BillCycleIntent()

    data class ShowItemMenu(
        @UiContext val context: Context,
        val id: Long,
    ) : BillCycleIntent()

}

@ViewModelLayer
interface BillCycleUseCase : BusinessMVIUseCase {

    /**
     * 周期任务
     */
    @StateHotObservable
    val cycleListStateOb: Flow<List<BillCycleResDto>>

}

private const val s = "行成功\n稍后账单会同步到"

@ViewModelLayer
class BillCycleUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillCycleUseCase {

    override val cycleListStateOb = MutableSharedStateFlow<List<BillCycleResDto>>(
        initValue = emptyList(),
    )

    override suspend fun initData() {
        super.initData()
        cycleListStateOb.emit(
            value = AppServices
                .appNetworkSpi
                .getBillCycleList(),
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun stateToggle(intent: BillCycleIntent.StateToggle) {
        val dataList = cycleListStateOb.first()
        dataList.find { it.id == intent.id }?.let { targetItem ->
            AppServices
                .appNetworkSpi
                .setBillCycleState(
                    id = intent.id,
                    state = when (targetItem.state) {
                        BillCycleResDto.STATE_RUNNING -> BillCycleResDto.STATE_STOPPED
                        BillCycleResDto.STATE_STOPPED -> BillCycleResDto.STATE_RUNNING
                        else -> BillCycleResDto.STATE_STOPPED
                    },
                ).let { billItemResItem ->
                    cycleListStateOb.emit(
                        value = dataList.map { item ->
                            if (item.id == billItemResItem.id) {
                                billItemResItem
                            } else {
                                item
                            }
                        }
                    )
                }
        }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun runOnce(intent: BillCycleIntent.RunOnce) {
        val dataList = cycleListStateOb.first()
        val billItemResItem = AppServices
            .appNetworkSpi
            .runBillCycleOnce(
                id = intent.id,
            )
        cycleListStateOb.emit(
            value = dataList.map { item ->
                if (item.id == billItemResItem.id) {
                    billItemResItem
                } else {
                    item
                }
            }
        )
        AppServices
            .tallyDataSyncSpi
            ?.trySync(
                bookIdList = listOf(
                    billItemResItem.bookId,
                )
            )
        confirmDialog(
            content = "执行成功\n稍后账单会同步到本地".toStringItemDto(),
            negative = null,
        )
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun deleteTask(intent: BillCycleIntent.DeleteTask) {
        confirmDialogOrError(
            content = "确定要删除这个任务吗?".toStringItemDto(),
        )
        AppServices
            .appNetworkSpi
            .deleteBillCycleById(
                id = intent.id,
            )
        tip(
            content = "删除成功".toStringItemDto(),
        )
        retryInit()
    }

    @IntentProcess
    private suspend fun showItemMenu(intent: BillCycleIntent.ShowItemMenu) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelect(
                context = intent.context,
                items = listOf(
                    MenuItem(
                        content = "运行一次".toStringItemDto(),
                    ),
                    MenuItem(
                        content = "编辑".toStringItemDto(),
                    ),
                    MenuItem(
                        content = "删除".toStringItemDto(),
                        level = MenuItemLevel.Danger,
                    ),
                )
            )
        when (selectIndex) {
            0 -> {
                addIntent(
                    intent = BillCycleIntent.RunOnce(
                        id = intent.id,
                    ),
                )
            }

            1 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillCycleCrudView(
                        context = intent.context,
                        editId = intent.id,
                    )
            }

            2 -> {
                addIntent(
                    intent = BillCycleIntent.DeleteTask(
                        id = intent.id,
                    ),
                )
            }
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

    init {
        EventPublisher
            .eventObservable
            .filter {
                it == APP_EVENT_REFRESH_BILL_CYCLE
            }
            .onEach {
                retryInit()
            }.launchIn(scope = scope)
    }

}
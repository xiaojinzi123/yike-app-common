package com.xiaojinzi.tally.module.core.module.label_info.domain

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.MenuItem
import com.xiaojinzi.tally.lib.res.model.support.MenuItemLevel
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.lib.res.model.user.UserInfoCacheDto
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class LabelInfoIntent {

    data object Submit : LabelInfoIntent()

    data class MenuMore(
        @UiContext val context: Context,
        val labelId: String,
        val labelName: String?,
        val billIdList: List<String>,
    ) : LabelInfoIntent()

}

@Keep
data class LabelDetailItemUseCaseDto(
    val labelId: String,
    val labelName: String?,
    val userCacheInfo: UserInfoCacheDto?,
    val amount: MoneyYuan,
    val billCount: Long,
    val billIdList: List<String>,
)

@ViewModelLayer
interface LabelInfoUseCase : BusinessMVIUseCase {

    /**
     * 标签列表, 当前账本的
     */
    @StateHotObservable
    val labelListStateOb: Flow<List<TallyLabelDto>>

    /**
     * 标签列表
     */
    @StateHotObservable
    val labelDetailListStateOb: Flow<List<LabelDetailItemUseCaseDto>>

}

@ViewModelLayer
class LabelInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), LabelInfoUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val labelListStateOb = AppServices
        .tallyDataSourceSpi
        .selectedBookStateOb
        .flatMapLatest { bookInfo ->
            bookInfo?.let { bookInfo1 ->
                AppServices
                    .tallyDataSourceSpi
                    .subscribeAllLabel(
                        bookId = bookInfo1.id,
                    )
                    .map { list ->
                        list.filter {
                            !it.isDeleted
                        }
                    }
            } ?: flowOf(value = emptyList())
        }
        .sharedStateIn(
            scope = scope,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val labelDetailListStateOb = labelListStateOb
        .flatMapLatest { labelList ->
            AppServices
                .tallyDataSourceSpi
                .subscribeDataBaseTableChangedOb(
                    TallyTable.Bill, emitOneWhileSubscribe = true,
                )
                .map {
                    labelList.map { labelItem ->
                        val billIdList = AppServices
                            .tallyDataSourceSpi
                            .getBillIdListByLabelId(
                                bookId = labelItem.bookId,
                                labelId = labelItem.id,
                                isExcludeDeleted = true,
                            )
                        LabelDetailItemUseCaseDto(
                            labelId = labelItem.id,
                            labelName = labelItem.name.orNull(),
                            userCacheInfo = AppServices
                                .tallyDataSourceSpi
                                .getCacheUserInfo(
                                    userId = labelItem.userId,
                                ),
                            amount = if (billIdList.isEmpty()) {
                                MoneyYuan()
                            } else {
                                AppServices.tallyDataSourceSpi.getBillAmountByCondition(
                                    queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                        typeList = listOf(
                                            TallyBillDto.Type.NORMAL,
                                            TallyBillDto.Type.REFUND,
                                        ),
                                        idList = billIdList,
                                    ),
                                ).toYuan()
                            },
                            billCount = if (billIdList.isEmpty()) {
                                0
                            } else {
                                AppServices.tallyDataSourceSpi.getBillCountByCondition(
                                    queryCondition = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                        idList = billIdList,
                                    ),
                                )
                            },
                            billIdList = billIdList,
                        )
                    }

                }
        }
        .sharedStateIn(
            scope = scope,
        )

    @IntentProcess
    private suspend fun menuMore(intent: LabelInfoIntent.MenuMore) {

        val currentUserInfo = AppServices
            .userSpi
            .requiredUserInfo()

        val currentBookInfo = AppServices
            .tallyDataSourceSpi
            .requiredSelectedBookInfo()

        val targetLabelInfo = AppServices
            .tallyDataSourceSpi
            .getLabelUnderBook(
                bookId = currentBookInfo.id,
                id = intent.labelId,
            ) ?: notSupportError("标签不存在")

        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelect(
                context = intent.context,
                items = buildList {
                    this.add(
                        element = MenuItem(
                            content = "记一笔".toStringItemDto(),
                        )
                    )
                    this.add(
                        element = MenuItem(
                            content = "查看账单".toStringItemDto(),
                        )
                    )
                    if (targetLabelInfo.userId == currentUserInfo.id) {
                        this.add(
                            element = MenuItem(
                                content = "删除".toStringItemDto(),
                                level = MenuItemLevel.Danger,
                            ),
                        )
                    }
                },
            )

        when (selectIndex) {
            0 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillCrudView(
                        context = intent.context,
                        initLabelIdList = arrayListOf(intent.labelId),
                    )
            }

            1 -> {
                AppRouterCoreApi::class
                    .routeApi()
                    .toBillListView(
                        context = intent.context,
                        title = intent.labelName?.toStringItemDto(),
                        question = if (intent.billIdList.isEmpty()) {
                            null
                        } else {
                            TallyDataSourceSpi.Companion.BillQueryConditionDto(
                                idList = intent.billIdList,
                            )
                        }
                    )
            }

            2 -> {
                if (currentUserInfo.id == targetLabelInfo.userId) {
                    confirmDialogOrError(
                        content = "确定要删除 '${intent.labelName.orEmpty()}' 标签吗?".toStringItemDto(),
                    )
                    AppServices.tallyDataSourceSpi.updateLabel(
                        target = targetLabelInfo.copy(
                            isDeleted = true,
                        )
                    )
                    tip(
                        content = "删除成功".toStringItemDto(),
                    )
                }
            }
        }

    }

    @IntentProcess
    private suspend fun submit(intent: LabelInfoIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
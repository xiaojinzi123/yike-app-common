package com.xiaojinzi.tally.module.core.module.bill_cycle_crud.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.EventPublisher
import com.xiaojinzi.support.ktx.InitOnceData
import com.xiaojinzi.support.ktx.MutableInitOnceData
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.getHourOfDayByTimeStamp
import com.xiaojinzi.support.ktx.notSupportError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.APP_EVENT_REFRESH_BILL_CYCLE
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.bottomMenuSelectSimple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.math.roundToLong

enum class BillCycleCrudCycleType(
    val str: String,
) {
    DAILY(str = "day"),
    WEEKLY(str = "week"),
    MONTHLY(str = "month"),
}

enum class BillCycleCrudCycleWeekType(
    val dayOfWeek: Int,
    val str: String,
) // 占位
{
    Monday(
        dayOfWeek = 2,
        str = "周一",
    ),
    Tuesday(
        dayOfWeek = 3,
        str = "周二",
    ),
    Wednesday(
        dayOfWeek = 4,
        str = "周三",
    ),
    Thursday(
        dayOfWeek = 5,
        str = "周四",
    ),
    Friday(
        dayOfWeek = 6,
        str = "周五",
    ),
    Saturday(
        dayOfWeek = 7,
        str = "周六",
    ),
    Sunday(
        dayOfWeek = 1,
        str = "周日",
    ),
}

enum class BillCycleCrudEndType {
    NEVER,
    COUNT,
}

sealed class BillCycleCrudIntent {

    data object Delete : BillCycleCrudIntent()

    data object Submit : BillCycleCrudIntent()

    data class ParameterInit(
        val editId: Long? = null,
    ) : BillCycleCrudIntent()

    data class CycleTypeSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class CycleWeekTypeSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class CycleMonthTypeSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class CycleEndTypeSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class CycleCountSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class HourSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillAmountSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillTypeSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillBookSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillCategorySelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillAccountSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillTransferFromAccountSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillTransferToAccountSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

    data class BillNoteSelect(
        @UiContext val context: Context,
    ) : BillCycleCrudIntent()

}

@ViewModelLayer
interface BillCycleCrudUseCase : BusinessMVIUseCase {

    /**
     * 编辑的 Id
     */
    val editIdInitData: InitOnceData<Long?>

    /**
     * 是否编辑
     */
    @StateHotObservable
    val isEditStateOb: Flow<Boolean>

    /**
     * 重复的类型
     */
    @StateHotObservable
    val cycleTypeStateOb: Flow<BillCycleCrudCycleType>

    /**
     * 一周重复的类型
     */
    @StateHotObservable
    val cycleWeekTypeStateOb: Flow<BillCycleCrudCycleWeekType>

    /**
     * 月里面的几号
     */
    @StateHotObservable
    val cycleMonthValueStateOb: Flow<Int>

    /**
     * 结束重复的类型
     */
    @StateHotObservable
    val cycleEndTypeStateOb: Flow<BillCycleCrudEndType>

    /**
     * 循环的次数
     */
    @StateHotObservable
    val cycleCountStateOb: Flow<Int>

    /**
     * 时间: 小时
     */
    @StateHotObservable
    val hourStateOb: Flow<Int>

    /**
     * 账单金额
     */
    @StateHotObservable
    val billAmountStateOb: Flow<Float>

    /**
     * 账单类型
     */
    @StateHotObservable
    val billTypeStateOb: Flow<TallyBillDto.Type>

    /**
     * 账本 Id
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

    /**
     * 账本
     */
    @StateHotObservable
    val bookInfoStateOb: Flow<TallyBookDto?>

    /**
     * 类别 Id
     */
    @StateHotObservable
    val categoryIdStateOb: Flow<String?>

    /**
     * 类别
     */
    @StateHotObservable
    val categoryInfoStateOb: Flow<TallyCategoryDto?>

    /**
     * 账户 Id
     */
    @StateHotObservable
    val accountIdStateOb: Flow<String?>

    /**
     * 账户
     */
    @StateHotObservable
    val accountInfoStateOb: Flow<TallyAccountDto?>

    /**
     *  转账 from 账户 Id
     */
    @StateHotObservable
    val transferFromAccountIdStateOb: Flow<String?>

    /**
     * 转账 from 账户
     */
    @StateHotObservable
    val transferFromAccountInfoStateOb: Flow<TallyAccountDto?>

    /**
     *  转账 to 账户 Id
     */
    @StateHotObservable
    val transferToAccountIdStateOb: Flow<String?>

    /**
     * 转账 to 账户
     */
    @StateHotObservable
    val transferToAccountInfoStateOb: Flow<TallyAccountDto?>

    /**
     * 备注
     */
    @StateHotObservable
    val noteStateOb: Flow<String>

}

@ViewModelLayer
class BillCycleCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillCycleCrudUseCase {

    override val editIdInitData = MutableInitOnceData<Long?>()

    override val isEditStateOb = editIdInitData
        .valueStateFlow
        .map {
            it != null
        }

    override val cycleTypeStateOb = MutableSharedStateFlow(
        initValue = BillCycleCrudCycleType.DAILY,
    )

    override val cycleWeekTypeStateOb = MutableSharedStateFlow(
        initValue = BillCycleCrudCycleWeekType.Monday,
    )

    override val cycleMonthValueStateOb = MutableSharedStateFlow(
        initValue = 1,
    )

    override val cycleEndTypeStateOb = MutableSharedStateFlow(
        initValue = BillCycleCrudEndType.NEVER,
    )

    override val cycleCountStateOb = MutableSharedStateFlow(
        initValue = 0,
    )

    override val hourStateOb = MutableSharedStateFlow(
        initValue = getHourOfDayByTimeStamp(
            timeStamp = System.currentTimeMillis(),
        ),
    )

    override val billAmountStateOb = MutableSharedStateFlow(
        initValue = 0f,
    )

    override val billTypeStateOb = MutableSharedStateFlow(
        initValue = TallyBillDto.Type.NORMAL,
    )

    override val bookIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val bookInfoStateOb = bookIdStateOb
        .map { bookId ->
            bookId.orNull()?.let { bookId1 ->
                AppServices.tallyDataSourceSpi.getBookById(
                    id = bookId1,
                )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val categoryIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val categoryInfoStateOb = categoryIdStateOb
        .map { categoryId ->
            categoryId.orNull()?.let { categoryId1 ->
                AppServices.tallyDataSourceSpi.getCategoryById(
                    id = categoryId1,
                )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val accountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val accountInfoStateOb = accountIdStateOb
        .map { accountId ->
            accountId.orNull()?.let { accountId1 ->
                AppServices.tallyDataSourceSpi.getAccountById(
                    id = accountId1,
                )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val transferFromAccountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val transferFromAccountInfoStateOb = transferFromAccountIdStateOb
        .map { accountId ->
            accountId.orNull()?.let { accountId1 ->
                AppServices.tallyDataSourceSpi.getAccountById(
                    id = accountId1,
                )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val transferToAccountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val transferToAccountInfoStateOb = transferToAccountIdStateOb
        .map { accountId ->
            accountId.orNull()?.let { accountId1 ->
                AppServices.tallyDataSourceSpi.getAccountById(
                    id = accountId1,
                )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val noteStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    override suspend fun initData() {
        super.initData()
        val editId = editIdInitData.awaitValue()
        if (editId == null) {
            bookIdStateOb.emit(
                value = AppServices
                    .tallyDataSourceSpi
                    .selectedBookStateOb
                    .firstOrNull()?.id,
            )
        } else {
            val targetBillCycleInfo = AppServices.appNetworkSpi.getBillCycleById(
                id = editId
            )
            cycleTypeStateOb.emit(
                value = when (targetBillCycleInfo.cycleType) {
                    BillCycleCrudCycleType.MONTHLY.str -> BillCycleCrudCycleType.MONTHLY
                    BillCycleCrudCycleType.WEEKLY.str -> BillCycleCrudCycleType.WEEKLY
                    BillCycleCrudCycleType.DAILY.str -> BillCycleCrudCycleType.DAILY
                    else -> notSupportError()
                }
            )
            BillCycleCrudCycleWeekType.entries.find {
                it.dayOfWeek == targetBillCycleInfo.dayOfWeek
            }?.let {
                cycleWeekTypeStateOb.emit(
                    value = it,
                )
            }
            if (targetBillCycleInfo.dayOfMonth in (1..28)) {
                cycleMonthValueStateOb.emit(
                    value = targetBillCycleInfo.dayOfMonth,
                )
            }
            cycleEndTypeStateOb.emit(
                value = when (targetBillCycleInfo.loopCount) {
                    -1 -> BillCycleCrudEndType.NEVER
                    else -> BillCycleCrudEndType.COUNT
                }
            )
            cycleCountStateOb.emit(
                value = targetBillCycleInfo.loopCount
            )
            if (targetBillCycleInfo.hour in (1..23)) {
                hourStateOb.emit(
                    value = targetBillCycleInfo.hour,
                )
            }
            billAmountStateOb.emit(
                value = targetBillCycleInfo.amount.toYuan().value,
            )
            val billType = TallyBillDto.Type.from(
                value = targetBillCycleInfo.billType,
            )
            billTypeStateOb.emit(
                value = billType,
            )
            bookIdStateOb.emit(
                value = targetBillCycleInfo.bookId,
            )
            categoryIdStateOb.emit(
                value = targetBillCycleInfo.categoryId,
            )
            when (billType) {
                TallyBillDto.Type.NORMAL -> {
                    accountIdStateOb.emit(
                        targetBillCycleInfo.accountId,
                    )
                }

                TallyBillDto.Type.TRANSFER -> {
                    transferFromAccountIdStateOb.emit(
                        value = targetBillCycleInfo.accountId,
                    )
                    transferToAccountIdStateOb.emit(
                        value = targetBillCycleInfo.transferTargetAccountId,
                    )
                }

                else -> {}
            }
            noteStateOb.emit(
                value = targetBillCycleInfo.note,
            )
        }
    }

    @IntentProcess
    private suspend fun parameterInit(intent: BillCycleCrudIntent.ParameterInit) {
        editIdInitData.value = intent.editId
    }

    @IntentProcess
    private suspend fun cycleTypeSelect(intent: BillCycleCrudIntent.CycleTypeSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "每天",
                    "每周",
                    "每月",
                ).map { it.toStringItemDto() }
            )
        cycleTypeStateOb.emit(
            value = when (selectIndex) {
                0 -> BillCycleCrudCycleType.DAILY
                1 -> BillCycleCrudCycleType.WEEKLY
                2 -> BillCycleCrudCycleType.MONTHLY
                else -> notSupportError()
            },
        )
    }

    @IntentProcess
    private suspend fun cycleWeekTypeSelect(intent: BillCycleCrudIntent.CycleWeekTypeSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "周一",
                    "周二",
                    "周三",
                    "周四",
                    "周五",
                    "周六",
                    "周日",
                ).map { it.toStringItemDto() }
            )
        BillCycleCrudCycleWeekType
            .entries
            .find { it.dayOfWeek == (selectIndex + 2) % 7 }
            ?.let {
                cycleWeekTypeStateOb.emit(
                    value = it,
                )
            }
    }

    @IntentProcess
    private suspend fun cycleMonthTypeSelect(intent: BillCycleCrudIntent.CycleMonthTypeSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = (1..28).map { "${it}号".toStringItemDto() }
            )
        if (selectIndex in (0..27)) {
            cycleMonthValueStateOb.emit(
                value = selectIndex + 1,
            )
        }
    }

    @IntentProcess
    private suspend fun cycleEndTypeSelect(intent: BillCycleCrudIntent.CycleEndTypeSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "永不结束",
                    "按次数结束重复",
                ).map { it.toStringItemDto() }
            )
        when (selectIndex) {
            0 -> {
                cycleEndTypeStateOb.emit(
                    value = BillCycleCrudEndType.NEVER
                )
            }

            1 -> {
                ParameterSupport.getInt(
                    intent = AppRouterCoreApi::class
                        .routeApi()
                        .billCycleRepeatCountBySuspend(
                            context = intent.context,
                        ),
                    key = "data",
                )?.let { count ->
                    cycleEndTypeStateOb.emit(
                        value = BillCycleCrudEndType.COUNT
                    )
                    cycleCountStateOb.emit(
                        value = count
                    )
                }
            }
        }
    }

    @IntentProcess
    private suspend fun cycleCountSelect(intent: BillCycleCrudIntent.CycleCountSelect) {
        if (cycleEndTypeStateOb.first() == BillCycleCrudEndType.COUNT) {
            ParameterSupport.getInt(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .billCycleRepeatCountBySuspend(
                        context = intent.context,
                    ),
                key = "data",
            )?.let { count ->
                cycleCountStateOb.emit(
                    value = count
                )
            }
        }
    }

    @IntentProcess
    private suspend fun hourSelect(intent: BillCycleCrudIntent.HourSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = (1..23).map { "${it}:00".toStringItemDto() }
            )
        if (selectIndex in (0..22)) {
            hourStateOb.emit(
                value = selectIndex + 1,
            )
        }
    }

    @IntentProcess
    private suspend fun billAmountSelect(intent: BillCycleCrudIntent.BillAmountSelect) {
        val oldAmount = billAmountStateOb.first()
        val amount = AppRouterCoreApi::class
            .routeApi()
            .priceCalculateViewSuspend(
                value = oldAmount.format2f(),
                context = intent.context,
            )
            .getDoubleExtra("data", 0.0)
            .toFloat()
        billAmountStateOb.emit(
            value = amount,
        )
    }

    @IntentProcess
    private suspend fun billTypeSelect(intent: BillCycleCrudIntent.BillTypeSelect) {
        val selectIndex = AppRouterBaseApi::class
            .routeApi()
            .bottomMenuSelectSimple(
                context = intent.context,
                items = listOf(
                    "普通账单",
                    "转账",
                ).map { it.toStringItemDto() }
            )
        when (selectIndex) {
            0 -> billTypeStateOb.emit(
                value = TallyBillDto.Type.NORMAL
            )

            1 -> billTypeStateOb.emit(
                value = TallyBillDto.Type.TRANSFER
            )
        }
    }

    @IntentProcess
    private suspend fun billBookSelect(intent: BillCycleCrudIntent.BillBookSelect) {
        ParameterSupport.getStringArrayList(
            intent = AppRouterCoreApi::class
                .routeApi()
                .bookSelect1SuspendForResult(
                    context = intent.context,
                    maxCount = 1,
                    bookIdList = bookIdStateOb
                        .firstOrNull()
                        .orNull()
                        ?.let {
                            arrayListOf(it)
                        } ?: arrayListOf(),
                ),
            key = "data",
        ).let { categoryIdList ->
            bookIdStateOb.emit(
                value = categoryIdList?.firstOrNull(),
            )
            categoryIdStateOb.emit(
                value = null,
            )
            accountIdStateOb.emit(
                value = null,
            )
            transferFromAccountIdStateOb.emit(
                value = null,
            )
            transferToAccountIdStateOb.emit(
                value = null,
            )
        }
    }

    @IntentProcess
    private suspend fun billCategorySelect(intent: BillCycleCrudIntent.BillCategorySelect) {
        if (bookIdStateOb.firstOrNull().orNull() == null) {
            tip(
                content = "请先选择账本".toStringItemDto(),
            )
        } else {
            ParameterSupport.getString(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .categorySelectBySuspend(
                        context = intent.context,
                        bookId = bookIdStateOb.firstOrNull(),
                        categoryId = categoryIdStateOb.firstOrNull(),
                    ),
                key = "data",
            ).orNull().let { categoryId ->
                categoryIdStateOb.emit(
                    value = categoryId,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun billAccountSelect(intent: BillCycleCrudIntent.BillAccountSelect) {
        if (bookIdStateOb.firstOrNull().orNull() == null) {
            tip(
                content = "请先选择账本".toStringItemDto(),
            )
        } else {
            ParameterSupport.getString(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .accountSelectBySuspend(
                        context = intent.context,
                        bookId = bookIdStateOb.firstOrNull(),
                    ),
                key = "data",
            ).orNull().let { accountId ->
                accountIdStateOb.emit(
                    value = accountId,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun billTransferFromAccountSelect(intent: BillCycleCrudIntent.BillTransferFromAccountSelect) {
        if (bookIdStateOb.firstOrNull().orNull() == null) {
            tip(
                content = "请先选择账本".toStringItemDto(),
            )
        } else {
            ParameterSupport.getString(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .accountSelectBySuspend(
                        context = intent.context,
                        bookId = bookIdStateOb.firstOrNull(),
                    ),
                key = "data",
            ).orNull().let { accountId ->
                transferFromAccountIdStateOb.emit(
                    value = accountId,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun billTransferToAccountSelect(intent: BillCycleCrudIntent.BillTransferToAccountSelect) {
        if (bookIdStateOb.firstOrNull().orNull() == null) {
            tip(
                content = "请先选择账本".toStringItemDto(),
            )
        } else {
            ParameterSupport.getString(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .accountSelectBySuspend(
                        context = intent.context,
                        bookId = bookIdStateOb.firstOrNull(),
                    ),
                key = "data",
            ).orNull().let { accountId ->
                transferToAccountIdStateOb.emit(
                    value = accountId,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun billNoteSelect(intent: BillCycleCrudIntent.BillNoteSelect) {
        ParameterSupport.getString(
            intent = AppRouterCoreApi::class
                .routeApi()
                .billCycleNoteBySuspend(
                    context = intent.context,
                    note = noteStateOb.firstOrNull(),
                ),
            key = "data",
            defaultValue = "",
        )?.let { value ->
            noteStateOb.emit(
                value = value
            )
        }
    }

    @IntentProcess
    private suspend fun delete(intent: BillCycleCrudIntent.Delete) {
        editIdInitData.awaitValue()?.let { editId ->
            confirmDialogOrError(
                content = "确定要删除这个任务吗?".toStringItemDto(),
            )
            withLoading {
                AppServices
                    .appNetworkSpi
                    .deleteBillCycleById(
                        id = editId,
                    )
            }
            tip(
                content = "删除成功".toStringItemDto(),
            )
            EventPublisher
                .eventObservable
                .add(
                    value = APP_EVENT_REFRESH_BILL_CYCLE,
                )
            postActivityFinishEvent()
        }
    }

    @IntentProcess
    @BusinessMVIUseCase.AutoLoading
    private suspend fun submit(intent: BillCycleCrudIntent.Submit) {

        val cycleType = cycleTypeStateOb.first()
        val cycleWeekType = cycleWeekTypeStateOb.first()
        val cycleMonthValue = cycleMonthValueStateOb.first()
        val cycleEndType = cycleEndTypeStateOb.first()
        val cycleCount = cycleCountStateOb.first()
        val hour = hourStateOb.first()
        val billAmount = billAmountStateOb.first()
        val billType = billTypeStateOb.first()
        val bookId = bookIdStateOb.firstOrNull().orNull()
        val categoryId = categoryIdStateOb.firstOrNull()
        val accountId = accountIdStateOb.firstOrNull()
        val transferFromAccountId = transferFromAccountIdStateOb.firstOrNull()
        val transferToAccountId = transferToAccountIdStateOb.firstOrNull()
        val note = noteStateOb.firstOrNull()

        if (bookId == null) {
            tip(
                content = "请选择账本".toStringItemDto(),
            )
            return
        }

        AppServices
            .appNetworkSpi
            .createOrUpdateBillCycle(
                id = editIdInitData.awaitValue(),
                bookId = bookId,
                cycleType = cycleType.str,
                loopCount = when (cycleEndType) {
                    BillCycleCrudEndType.COUNT -> cycleCount
                    BillCycleCrudEndType.NEVER -> -1
                },
                // 如果是 GMT+8, 那么这里就是 8
                timeZone = Calendar.getInstance().run {
                    this.get(Calendar.ZONE_OFFSET) / 1000 / 60 / 60
                },
                dayOfMonth = when (cycleType) {
                    BillCycleCrudCycleType.MONTHLY -> cycleMonthValue
                    else -> null
                },
                dayOfWeek = when (cycleType) {
                    BillCycleCrudCycleType.WEEKLY -> cycleWeekType.dayOfWeek
                    else -> null
                },
                hour = hour,
                billType = billType.value,
                categoryId = categoryId,
                accountId = when (billType) {
                    TallyBillDto.Type.NORMAL -> accountId
                    TallyBillDto.Type.TRANSFER -> transferFromAccountId
                    else -> null
                },
                transferTargetAccountId = when (billType) {
                    TallyBillDto.Type.NORMAL -> null
                    TallyBillDto.Type.TRANSFER -> transferToAccountId
                    else -> null
                },
                amount = billAmount.times(100f).roundToLong(),
                note = note.orEmpty(),
            )

        tip(
            content = "提交成功".toStringItemDto(),
        )

        EventPublisher
            .eventObservable
            .add(
                value = APP_EVENT_REFRESH_BILL_CYCLE,
            )

        // 消灭界面
        postActivityFinishEvent()

    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
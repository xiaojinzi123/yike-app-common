package com.xiaojinzi.tally.module.core.module.bill_crud.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.HotStateFlow
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.await
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.model.exception.NoBookSelectException
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.module.base.spi.CostUseCaseSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyDataSourceHelper
import com.xiaojinzi.tally.module.core.module.category_select.domain.CategorySelectIntent
import com.xiaojinzi.tally.module.core.module.category_select.domain.CategorySelectUseCase
import com.xiaojinzi.tally.module.core.module.category_select.domain.CategorySelectUseCaseImpl
import com.xiaojinzi.tally.module.core.spi.CostUseCaseSpiImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

enum class ResetType {
    All,
    ForBookSwitch,
}

enum class BillCrudTab(
    val amountConvert: Int,
) {
    Spending(
        amountConvert = -1,
    ),
    Income(
        amountConvert = 1,
    ),
    Transfer(
        amountConvert = -1,
    ),
}

sealed class BillCrudIntent {

    data class SubmitAndNewOne(
        val isForce: Boolean = false,
    ) : BillCrudIntent()

    data class Submit(
        @UiContext val context: Context,
        val isForce: Boolean = false,
    ) : BillCrudIntent()

    data class Delete(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class LabelDelete(
        val labelId: String,
    ) : BillCrudIntent()

    data class BookSwitch(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class LabelSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class DateTimeSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class AccountSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class TransferAccountSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class TransferTargetAccountSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data object TransferAccountSwitch : BillCrudIntent()

    data class ImageSelect(
        @UiContext val context: Context,
    ) : BillCrudIntent()

    data class ParameterInit(
        // 编辑的时候用到
        val billId: String?,
        val associatedRefundBillId: String?,
        val bookId: String?,
        val billType: TallyBillDto.Type = TallyBillDto.Type.NORMAL,
        val categoryId: String?,
        val accountId: String?,
        val transferAccountId: String?,
        val transferTargetAccountId: String?,
        val labelIdSet: Set<String> = emptySet(),
        val imageUrlList: List<String> = emptyList(),
        val time: Long?,
        val amount: Long?,
    ) : BillCrudIntent()

}

@ViewModelLayer
interface BillCrudUseCase : BusinessMVIUseCase {

    companion object {
        val TAG = BillCrudUseCase::class.java.simpleName
    }

    val costUseCase: CostUseCaseSpi

    /**
     * 分类选择的 UseCase
     */
    val categorySelectUseCase: CategorySelectUseCase

    /**
     * 是否显示不支持的账单的视图
     */
    val isShowNotSupportBillViewOb: HotStateFlow<Boolean>

    /**
     * 显示的 tab List
     */
    val tabListOb: HotStateFlow<List<BillCrudTab>>

    /**
     * 选中的 Tab
     */
    val tabStateOb: MutableSharedStateFlow<BillCrudTab>

    /**
     * 编辑用的 BillId
     */
    val billIdStateOb: HotStateFlow<String?>

    /**
     * 是否是编辑
     */
    val isEditStateOb: HotStateFlow<Boolean>

    /**
     * 账本信息
     */
    val bookInfoStateOb: HotStateFlow<TallyBookDto?>

    /**
     * 选中的标签列表
     */
    val labelIdListStateOb: HotStateFlow<Set<String>>

    /**
     * 选中的标签列表
     */
    val labelInfoListStateOb: HotStateFlow<List<TallyLabelDto>>

    /**
     * 创建退款账单的时候, 目标的账单信息
     * 只有创建退款单的时候会有值, 其他的时候都是 null
     * 这个有值就是表示是创建退款单, 不是编辑哦
     */
    val refundBillDetailStateOb: HotStateFlow<TallyBillDetailDto?>

    /**
     * 是否是为了退款来的
     * 1. 创建退款单
     * 2. 编辑退款单
     */
    val isForRefundState: HotStateFlow<Boolean>

    /**
     * 选择的账户
     */
    val accountIdStateOb: HotStateFlow<String?>

    /**
     *  转账选择的账户
     */
    val transferAccountIdStateOb: HotStateFlow<String?>

    /**
     *  转账选择的目标账户
     */
    val transferTargetAccountIdStateOb: HotStateFlow<String?>

    /**
     * 选择的账户
     */
    val accountStateOb: HotStateFlow<TallyAccountDto?>

    /**
     * 选择的转账账户
     */
    val transferAccountStateOb: HotStateFlow<TallyAccountDto?>

    /**
     * 选择的转账目标账户
     */
    val transferTargetAccountStateOb: HotStateFlow<TallyAccountDto?>

    /**
     * 备注
     */
    val noteStateOb: MutableSharedStateFlow<String>

    /**
     * 时间戳
     */
    val timeStampState: HotStateFlow<Long?>

    /**
     * 时间戳的字符串
     */
    val timeStampStrState: HotStateFlow<String?>

    /**
     * 图片地址列表
     */
    val imageUrlListState: HotStateFlow<List<String>>

    /**
     * 是否不参与计算
     */
    val isNotCalculateState: MutableSharedStateFlow<Boolean>

    /**
     * 是否可以切换账本
     */
    val canSwitchBookState: HotStateFlow<Boolean>

    /**
     * 是否显示不计入视图
     */
    val isShowNotCalculateViewState: HotStateFlow<Boolean>

}

@ViewModelLayer
class BillCrudUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val costUseCase: CostUseCaseSpi = CostUseCaseSpiImpl(),
    override val categorySelectUseCase: CategorySelectUseCase = CategorySelectUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillCrudUseCase {

    override val isShowNotSupportBillViewOb = MutableSharedStateFlow(
        initValue = false,
    )

    override val tabListOb = MutableSharedStateFlow<List<BillCrudTab>>(
        initValue = emptyList(),
    )

    override val tabStateOb = MutableSharedStateFlow(
        initValue = BillCrudTab.Spending,
    )

    override val billIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val isEditStateOb = billIdStateOb.map {
        it != null
    }

    override val bookInfoStateOb = categorySelectUseCase
        .bookIdStateOb
        .map { bookId ->
            bookId.orNull()?.let {
                AppServices
                    .tallyDataSourceSpi
                    .getBookById(
                        id = it,
                    )
            }
        }.sharedStateIn(
            scope = scope,
        )

    override val labelIdListStateOb = MutableSharedStateFlow(
        initValue = emptySet<String>(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val labelInfoListStateOb = AppServices
        .tallyDataSourceSpi
        .selectedBookStateOb
        .flatMapLatest { bookInfo ->
            bookInfo?.let {
                labelIdListStateOb.map { labelIdSet ->
                    labelIdSet.toList().mapNotNull { labelId ->
                        AppServices.tallyDataSourceSpi.getLabelUnderBook(
                            bookId = bookInfo.id,
                            id = labelId,
                        )
                    }
                }
            } ?: flowOf(value = emptyList())
        }.sharedStateIn(
            scope = scope,
        )

    override val refundBillDetailStateOb = MutableSharedStateFlow<TallyBillDetailDto?>(
        initValue = null,
    )

    override val isForRefundState = MutableSharedStateFlow(
        initValue = false,
    )

    override val accountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val transferAccountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val transferTargetAccountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val accountStateOb = combine(
        TallyDataSourceHelper.subscribeCurrentBookAccountList(),
        accountIdStateOb
    ) { allAccount, id ->
        allAccount.find { it.id == id }
    }.sharedStateIn(
        scope = scope,
    )

    override val transferAccountStateOb = combine(
        TallyDataSourceHelper.subscribeCurrentBookAccountList(),
        transferAccountIdStateOb
    ) { allAccount, id ->
        allAccount.find { it.id == id }
    }.sharedStateIn(
        scope = scope,
    )

    override val transferTargetAccountStateOb = combine(
        TallyDataSourceHelper.subscribeCurrentBookAccountList(),
        transferTargetAccountIdStateOb
    ) { allAccount, id ->
        allAccount.find { it.id == id }
    }.sharedStateIn(
        scope = scope,
    )

    override val noteStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    override val timeStampState = MutableSharedStateFlow<Long?>(
        initValue = null,
    )

    override val timeStampStrState = combine(
        AppServices
            .appConfigSpi
            .isShowHourAndMinuteWhenBillCrudStateOb,
        timeStampState,
    ) { isShowHourAndMinuteWhenBillCrud, timeStamp ->
        timeStamp?.let { timeStamp1 ->
            SimpleDateFormat(
                if (isShowHourAndMinuteWhenBillCrud) {
                    "yyyy-MM-dd HH:mm"
                } else {
                    "yyyy-MM-dd"
                },
                Locale.getDefault(),
            ).format(
                timeStamp1
            )
        }
    }.sharedStateIn(
        scope = scope,
    )

    override val imageUrlListState = MutableSharedStateFlow<List<String>>(
        initValue = emptyList(),
    )

    override val isNotCalculateState = MutableSharedStateFlow(
        initValue = false,
    )

    override val canSwitchBookState = combine(
        isEditStateOb, refundBillDetailStateOb,
    ) { isEdit, refundBill ->
        !isEdit && refundBill == null
    }

    override val isShowNotCalculateViewState = tabStateOb
        .map {
            it == BillCrudTab.Spending || it == BillCrudTab.Income
        }

    private suspend fun getDefAccount(
        bookId: String?,
    ): TallyAccountDto? {
        return bookId.orNull()?.let { bookId1 ->
            AppServices
                .tallyDataSourceSpi
                .getAccountByBookId(
                    bookId = bookId1,
                    isExcludeDeleted = true,
                ).firstOrNull { it.isDefault }
        }
    }

    private suspend fun reset(
        resetType: ResetType = ResetType.All,
    ) {
        categorySelectUseCase.intentForCategorySetNull(
            intent = CategorySelectIntent.CategorySetNull,
        )
        val currentBookId = categorySelectUseCase
            .bookIdStateOb
            .firstOrNull()
        accountIdStateOb.emit(
            value = getDefAccount(bookId = currentBookId)?.id,
        )
        transferAccountIdStateOb.emit(
            value = null,
        )
        transferTargetAccountIdStateOb.emit(
            value = null,
        )
        if (resetType == ResetType.All) {
            imageUrlListState.emit(
                value = emptyList(),
            )
            costUseCase.resetAction().await()
            timeStampState.emit(
                value = null,
            )
            noteStateOb.emit(
                value = "",
            )
            isNotCalculateState.emit(
                value = false,
            )
        }
    }

    private var parameterInitInfo: BillCrudIntent.ParameterInit? = null

    @IntentProcess
    private suspend fun parameterInit(
        intent: BillCrudIntent.ParameterInit,
    ) {
        if (intent.billId.isNullOrEmpty()) {
            val initBookId = intent.bookId.orNull() ?: AppServices
                .tallyDataSourceSpi
                .selectedBookStateOb
                .firstOrNull()
                ?.id
            // 到这里说明是新增, 账本 Id 就是默认选中的
            categorySelectUseCase
                .setBookId(
                    bookId = initBookId,
                )
            val categoryInfo =
                intent.categoryId.orNull()
                    ?.let { categoryId ->
                        initBookId?.let {
                            AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                                id = categoryId,
                                bookId = initBookId,
                            )?.let { categoryInfo ->
                                if (categoryInfo.parentId.orNull() == null) {
                                    categorySelectUseCase.intentForCategoryGroupSelect(
                                        intent = CategorySelectIntent.CategoryGroupSelect(
                                            id = categoryInfo.id,
                                        )
                                    )
                                } else {
                                    AppServices.tallyDataSourceSpi.getCategoryByIdAndBookId(
                                        id = categoryInfo.parentId.orEmpty(),
                                        bookId = initBookId,
                                    )?.let { parentCategoryInfo ->
                                        categorySelectUseCase.intentForCategoryGroupSelect(
                                            intent = CategorySelectIntent.CategoryGroupSelect(
                                                id = parentCategoryInfo.id,
                                            )
                                        )
                                        categorySelectUseCase.intentForCategoryItemSelect(
                                            intent = CategorySelectIntent.CategoryItemSelect(
                                                id = categoryInfo.id,
                                            )
                                        )
                                    }
                                }
                                categoryInfo
                            }
                        }
                    }
            // 如果是创建退款单
            val associatedRefundBillInfo = intent.associatedRefundBillId?.let { refundBillId ->
                AppServices
                    .tallyDataSourceSpi
                    .getBillDetailById(id = refundBillId)
            }
            // 如果 id 不为空, 但是查询失败则显示不支持的页面
            if (!intent.associatedRefundBillId.isNullOrEmpty()) {
                if (associatedRefundBillInfo == null) {
                    isShowNotSupportBillViewOb.emit(
                        value = true,
                    )
                } else {
                    refundBillDetailStateOb.emit(
                        value = associatedRefundBillInfo,
                    )
                    isForRefundState.emit(
                        value = true,
                    )
                }
            }

            accountIdStateOb.emit(
                value = associatedRefundBillInfo?.account?.id
                    ?: intent.accountId.orNull()
                    ?: getDefAccount(
                        initBookId,
                    )?.id,
            )
            transferAccountIdStateOb.emit(
                value = intent.transferAccountId,
            )
            transferTargetAccountIdStateOb.emit(
                value = intent.transferTargetAccountId,
            )
            labelIdListStateOb.emit(
                value = intent.labelIdSet,
            )
            imageUrlListState.emit(
                value = intent.imageUrlList,
            )
            timeStampState.emit(
                value = intent.time,
            )
            if (associatedRefundBillInfo == null) {
                intent.amount?.let { initAmount ->
                    costUseCase.costAppend(
                        target = MoneyFen(value = initAmount).toYuan().value.times(
                            other = when (intent.billType) {
                                TallyBillDto.Type.NORMAL -> {
                                    categoryInfo?.type?.moneyTransform ?: 1
                                }

                                TallyBillDto.Type.TRANSFER -> -1

                                else -> 1
                            },
                        ).run {
                            if (this.absoluteValue == 0f) {
                                0f
                            } else {
                                this
                            }
                        }.format2f(
                            isKeepZero = false,
                        )
                    )
                }
            } else {
                associatedRefundBillInfo.core.amount.value.let { initAmount ->
                    costUseCase.costAppend(
                        target = MoneyFen(value = initAmount).toYuan().value.run {
                            if (this.absoluteValue == 0f) {
                                0f
                            } else {
                                this.times(-1)
                            }
                        }.format2f(
                            isKeepZero = false,
                        )
                    )
                }
            }
            // 让页面不那么快切换 tab
            delay(200)
            tabListOb.emit(
                value = buildList {
                    this.add(element = BillCrudTab.Spending)
                    this.add(element = BillCrudTab.Income)
                    if (intent.associatedRefundBillId.isNullOrEmpty()) {
                        this.add(element = BillCrudTab.Transfer)
                    }
                }
            )
            tabStateOb.emit(
                value = if (associatedRefundBillInfo == null) {
                    when (intent.billType) {
                        TallyBillDto.Type.NORMAL -> when (
                            categoryInfo?.type
                        ) {
                            TallyCategoryDto.Companion.TallyCategoryType.SPENDING -> BillCrudTab.Spending
                            TallyCategoryDto.Companion.TallyCategoryType.INCOME -> BillCrudTab.Income
                            else -> BillCrudTab.Spending
                        }

                        TallyBillDto.Type.TRANSFER -> BillCrudTab.Transfer

                        else -> BillCrudTab.Spending
                    }
                } else {
                    when (
                        associatedRefundBillInfo.category?.type
                    ) {
                        TallyCategoryDto.Companion.TallyCategoryType.SPENDING -> BillCrudTab.Spending
                        TallyCategoryDto.Companion.TallyCategoryType.INCOME -> BillCrudTab.Income
                        // 其他情况则根据金额来判断, > 0 表示收入, < 0 表示支出
                        // 则退款的情况就正好反一下
                        else -> when (associatedRefundBillInfo.core.amount.value) {
                            in 0..Long.MAX_VALUE -> BillCrudTab.Spending
                            else -> BillCrudTab.Income
                        }
                    }
                },
            )
        } else // 占位
        {
            billIdStateOb.emit(
                value = intent.billId,
            )
            AppServices
                .tallyDataSourceSpi
                .getBillDetailById(
                    id = intent.billId,
                )?.let { targetBillDetail ->

                    // 如果是未知的类型, 那就不处理
                    if (targetBillDetail.core.type == TallyBillDto.Type.Unknown) {
                        isShowNotSupportBillViewOb.emit(value = true)
                        return
                    }

                    isForRefundState.emit(
                        value = targetBillDetail.core.type == TallyBillDto.Type.REFUND,
                    )

                    categorySelectUseCase.setBookId(
                        bookId = targetBillDetail.core.bookId,
                    )
                    val amount = targetBillDetail.core.amount.toYuan()
                    val categoryItem = targetBillDetail.category
                    val categoryGroup = categoryItem?.parentId.orNull()?.let { parentId ->
                        AppServices
                            .tallyDataSourceSpi
                            .getCategoryByIdAndBookId(
                                id = parentId,
                                bookId = targetBillDetail.core.bookId,
                            )
                    }
                    val billImageList = AppServices
                        .tallyDataSourceSpi
                        .getBillImageListByBillId(
                            bookId = targetBillDetail.core.bookId,
                            billId = targetBillDetail.core.id,
                        )
                        .filter {
                            !it.isDeleted
                        }
                    val targetCategory = categoryGroup ?: categoryItem
                    // 如果类别组是空的, 那就用类别 item 作为类别组
                    categorySelectUseCase.intentForCategoryGroupSelect(
                        intent = CategorySelectIntent.CategoryGroupSelect(
                            id = targetCategory?.id,
                        )
                    )
                    categorySelectUseCase.intentForCategoryItemSelect(
                        intent = CategorySelectIntent.CategoryItemSelect(
                            id = if (categoryGroup == null) {
                                null
                            } else {
                                categoryItem
                            }?.id,
                        )
                    )
                    when (targetBillDetail.core.type) {
                        TallyBillDto.Type.NORMAL,
                        TallyBillDto.Type.REFUND -> {
                            accountIdStateOb.emit(
                                value = targetBillDetail.core.accountId,
                            )
                        }

                        TallyBillDto.Type.TRANSFER -> {
                            transferAccountIdStateOb.emit(
                                value = targetBillDetail.core.accountId,
                            )
                            transferTargetAccountIdStateOb.emit(
                                value = targetBillDetail.core.transferTargetAccountId,
                            )
                        }

                        else -> {}
                    }
                    labelIdListStateOb.emit(
                        value = targetBillDetail.labelList.map { it.id }.toSet(),
                    )
                    timeStampState.emit(
                        value = targetBillDetail.core.time,
                    )
                    imageUrlListState.emit(
                        value = billImageList.map {
                            it.url.orEmpty()
                        }
                    )
                    noteStateOb.emit(
                        value = targetBillDetail.core.note.orEmpty(),
                    )
                    isNotCalculateState.emit(
                        value = targetBillDetail.core.isNotCalculate,
                    )
                    costUseCase.costAppend(
                        target = amount.value.run {
                            targetBillDetail.moneyTransform?.let { moneyTransform ->
                                this.times(
                                    other = moneyTransform,
                                )
                            } ?: this.absoluteValue
                        }.run {
                            if (this.absoluteValue == 0f) {
                                0f
                            } else {
                                this
                            }
                        }.format2f(
                            isKeepZero = false,
                        )
                    )
                    // 让页面不那么快切换 tab
                    delay(200)
                    tabListOb.emit(
                        value = buildList {
                            this.add(element = BillCrudTab.Spending)
                            this.add(element = BillCrudTab.Income)
                            if (targetBillDetail.core.type != TallyBillDto.Type.REFUND) {
                                this.add(element = BillCrudTab.Transfer)
                            }
                        }
                    )
                    tabStateOb.emit(
                        value = when (targetBillDetail.core.type) {
                            TallyBillDto.Type.NORMAL,
                            TallyBillDto.Type.REFUND -> when (categoryItem) {
                                null -> {
                                    if (amount.value > 0f) {
                                        BillCrudTab.Income
                                    } else {
                                        BillCrudTab.Spending
                                    }
                                }

                                else -> {
                                    when (categoryItem.type) {
                                        TallyCategoryDto.Companion.TallyCategoryType.SPENDING -> BillCrudTab.Spending
                                        TallyCategoryDto.Companion.TallyCategoryType.INCOME -> BillCrudTab.Income
                                        else -> BillCrudTab.Spending
                                    }
                                }
                            }

                            TallyBillDto.Type.TRANSFER -> BillCrudTab.Transfer
                            else -> BillCrudTab.Spending
                        },
                    )
                } ?: kotlin.run {
                isShowNotSupportBillViewOb.emit(
                    value = true,
                )
            }
        }
        parameterInitInfo = intent
    }

    /**
     * @return 是否完成
     */
    private suspend fun doSubmit(
        isForce: Boolean = false,
    ): Boolean {

        val userId = AppServices
            .userSpi
            .requiredLastUserId()

        val bookId = categorySelectUseCase.bookIdStateOb.firstOrNull().orNull()
            ?: throw NoBookSelectException()

        val currentTab = tabStateOb.first()

        val refundBillInfo = refundBillDetailStateOb.firstOrNull()
        val isForRefund = isForRefundState.first()

        val targetBillType = if (isForRefund) {
            TallyBillDto.Type.REFUND
        } else {
            when (currentTab) {
                BillCrudTab.Spending -> TallyBillDto.Type.NORMAL
                BillCrudTab.Income -> TallyBillDto.Type.NORMAL
                BillCrudTab.Transfer -> TallyBillDto.Type.TRANSFER
            }
        }

        val editBillId = billIdStateOb.firstOrNull().orNull()
        val editBillDetailInfo = if (editBillId == null) {
            null
        } else {
            AppServices
                .tallyDataSourceSpi
                .getBillDetailById(
                    id = editBillId,
                ).apply {
                    if (this == null) {
                        return false
                    }
                }
        }

        val categoryGroup = categorySelectUseCase.categoryGroupSelectedStateOb.firstOrNull()
        val categoryItem = categorySelectUseCase.categoryItemSelectedStateOb.first()
        // 目标的类别
        val targetCategory = categoryItem ?: categoryGroup

        val accountId = accountIdStateOb.firstOrNull()
        val transferAccountId = transferAccountIdStateOb.firstOrNull()
        val transferTargetAccountId = transferTargetAccountIdStateOb.firstOrNull()

        if (costUseCase.isEmptyOrZeroNumber()) {
            if (!isForce) {
                if (!AppServices.appConfigSpi.isAllowZeroAmountWhenBillCrudStateOb.first()) {
                    tip(content = "请填写金额".toStringItemDto())
                    return false
                }
            }
        }

        // 对类别为空时候的检测
        /*if (type == TallyBillDto.Type.NORMAL) {
            if (targetCategory == null) {
                tip(content = "请选择类别".toStringItemDto())
                return false
            }
        }*/

        if (currentTab == BillCrudTab.Transfer) {
            if (transferAccountId == null && transferTargetAccountId == null) {
                tip(content = "请选择转出账户或者转入账户".toStringItemDto())
                return false
            }
            if (transferAccountId == null) {
                if (!isForce) {
                    tip(content = "请选择转出账户".toStringItemDto())
                    return false
                }
            }
            if (transferTargetAccountId == null) {
                if (!isForce) {
                    tip(content = "请选择转入账户".toStringItemDto())
                    return false
                }
            }
        }

        val targetCategoryFinal = if (refundBillInfo == null) {
            if (editBillDetailInfo?.core?.type == TallyBillDto.Type.REFUND) {
                editBillDetailInfo.category
            } else {
                when (currentTab) {
                    BillCrudTab.Spending,
                    BillCrudTab.Income -> targetCategory

                    BillCrudTab.Transfer -> null
                }
            }
        } else {
            refundBillInfo.category
        }

        val accountIdFinal = if (refundBillInfo == null) {
            if (editBillDetailInfo?.core?.type == TallyBillDto.Type.REFUND) {
                accountId
            } else {
                when (currentTab) {
                    BillCrudTab.Spending, BillCrudTab.Income -> accountId
                    BillCrudTab.Transfer -> transferAccountId
                }
            }

        } else {
            accountId
        }

        // 转账的目标账户 Id
        val transferTargetAccountIdFinal = if (refundBillInfo == null) {
            when (currentTab) {
                BillCrudTab.Spending, BillCrudTab.Income -> null
                BillCrudTab.Transfer -> transferTargetAccountId
            }
        } else {
            null
        }

        val originBillIdFinal = refundBillInfo?.core?.id ?: editBillDetailInfo?.core?.originBillId

        val targetLabelIdList = labelInfoListStateOb.first().map { it.id }
        val targetImageUrlList = imageUrlListState.first()
        val targetTime = timeStampState.firstOrNull() ?: System.currentTimeMillis()
        val note = noteStateOb.first()

        // 计算金额
        val amount = costUseCase.calculateResult(
            target = costUseCase.costStrStateOb.first().strValue,
        )
        val amountFinal = MoneyFen(
            value = ((amount * 100).roundToLong()) * if (refundBillInfo == null) {
                if (editBillDetailInfo?.core?.type == TallyBillDto.Type.REFUND) {
                    1
                } else {
                    when (currentTab) {
                        BillCrudTab.Spending, BillCrudTab.Income -> targetCategoryFinal?.let {
                            when (targetCategoryFinal.type) {
                                TallyCategoryDto.Companion.TallyCategoryType.SPENDING -> -1
                                TallyCategoryDto.Companion.TallyCategoryType.INCOME -> 1
                                TallyCategoryDto.Companion.TallyCategoryType.UNKNOW -> -1
                            }
                        } ?: currentTab.amountConvert

                        BillCrudTab.Transfer -> currentTab.amountConvert
                    }
                }
            } else {
                1
            },
        )
        val isNotCalculateFinal = when (currentTab) {
            BillCrudTab.Spending, BillCrudTab.Income -> isNotCalculateState.first()
            BillCrudTab.Transfer -> false
        }
        if (editBillDetailInfo == null) {
            // 插入一个账单
            AppServices
                .tallyDataSourceSpi
                .insertBill(
                    target = TallyBillInsertDto(
                        userId = userId,
                        bookId = bookId,
                        type = targetBillType.value,
                        categoryId = targetCategoryFinal?.id,
                        accountId = accountIdFinal,
                        transferTargetAccountId = transferTargetAccountIdFinal,
                        originBillId = originBillIdFinal,
                        note = note,
                        time = targetTime,
                        amount = amountFinal,
                        isNotCalculate = isNotCalculateFinal,
                    ),
                    labelIdList = targetLabelIdList,
                    imageUrlList = targetImageUrlList,
                )
        } else // 占位
        {
            editBillDetailInfo.core
                .let { tallyBillDto ->
                    AppServices
                        .tallyDataSourceSpi
                        .updateBill(
                            target = tallyBillDto.copy(
                                userId = userId,
                                bookId = bookId,
                                type = targetBillType,
                                categoryId = targetCategoryFinal?.id,
                                accountId = accountIdFinal,
                                transferTargetAccountId = transferTargetAccountIdFinal,
                                originBillId = originBillIdFinal,
                                note = note,
                                time = targetTime,
                                amount = amountFinal,
                                isNotCalculate = isNotCalculateFinal,
                            ),
                            labelIdList = targetLabelIdList,
                            imageUrlList = targetImageUrlList,
                        )
                }
        }
        return true
    }

    @IntentProcess
    private suspend fun submitAndNewOne(
        intent: BillCrudIntent.SubmitAndNewOne,
    ) {
        if (doSubmit(
                isForce = intent.isForce,
            )
        ) {
            reset()
            parameterInitInfo?.run {
                parameterInit(
                    intent = this,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun submit(
        intent: BillCrudIntent.Submit,
    ) {
        if (doSubmit(
                isForce = intent.isForce,
            )
        ) {
            intent.context.tryFinishActivity()
        }
    }

    @IntentProcess
    private suspend fun deleteBill(
        intent: BillCrudIntent.Delete,
    ) {
        val userId = AppServices
            .userSpi
            .requiredLastUserId()
        val billId = billIdStateOb.firstOrNull() ?: return
        confirmDialogOrError(
            content = "确定删除这个账单吗？".toStringItemDto(),
        )
        val targetBill = AppServices
            .tallyDataSourceSpi
            .getBillDetailById(
                id = billId,
            )?.core
        if (targetBill?.userId == userId) {
            AppServices
                .tallyDataSourceSpi
                .updateBill(
                    target = targetBill.copy(
                        isDeleted = true,
                    )
                )
            commonUseCase.postActivityFinishEvent()
        } else {
            tip(content = "您没有权限删除这个账单".toStringItemDto())
        }
    }

    @IntentProcess
    private suspend fun labelDelete(
        intent: BillCrudIntent.LabelDelete,
    ) {
        labelIdListStateOb.emit(
            value = labelIdListStateOb.first().toMutableSet().apply {
                if (this.contains(element = intent.labelId)) {
                    this.remove(element = intent.labelId)
                } else {
                    this.add(
                        element = intent.labelId,
                    )
                }
            }
        )
    }

    @IntentProcess
    private suspend fun bookSwitch(
        intent: BillCrudIntent.BookSwitch,
    ) {
        val isEdit = isEditStateOb.first()
        if (!isEdit) {
            val result = categorySelectUseCase.intentForBookSwitch(
                intent = CategorySelectIntent.BookSwitch(
                    context = intent.context,
                )
            )
            if (result) {
                reset(
                    resetType = ResetType.ForBookSwitch,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun labelSelect(
        intent: BillCrudIntent.LabelSelect,
    ) {
        val bookId = categorySelectUseCase.bookIdStateOb.firstOrNull().orNull()
            ?: throw NoBookSelectException()
        val labelIdList = ParameterSupport.getStringArrayList(
            intent = AppRouterCoreApi::class
                .routeApi()
                .labelSelectBySuspend(
                    context = intent.context,
                    bookId = bookId,
                    idList = ArrayList(
                        labelInfoListStateOb.first().map { it.id }
                    ),
                ),
            key = "data",
        ) ?: emptyList()
        labelIdListStateOb.emit(
            value = labelIdList.toSet(),
        )
    }

    @IntentProcess
    private suspend fun dateTimeSelect(
        intent: BillCrudIntent.DateTimeSelect,
    ) {
        val isShowHourAndMinuteWhenBillCrud = AppServices
            .appConfigSpi
            .isShowHourAndMinuteWhenBillCrudStateOb
            .first()
        timeStampState.emit(
            value = ParameterSupport.getLong(
                intent = AppRouterBaseApi::class
                    .routeApi()
                    .dateTimeSelectBySuspend(
                        context = intent.context,
                        time = timeStampState.firstOrNull(),
                        type = if (isShowHourAndMinuteWhenBillCrud) {
                            DateTimeType.Time
                        } else {
                            DateTimeType.Day
                        }
                    ),
                key = "data",
            ),
        )
    }

    @IntentProcess
    private suspend fun accountSelect(
        intent: BillCrudIntent.AccountSelect,
    ) {
        val bookId = categorySelectUseCase.bookIdStateOb
            .firstOrNull() ?: throw NoBookSelectException()
        accountIdStateOb.emit(
            value = ParameterSupport.getString(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .accountSelectBySuspend(
                        context = intent.context,
                        bookId = bookId,
                    ),
                key = "data",
            ).orNull(),
        )
    }

    @IntentProcess
    private suspend fun transferAccountSelect(
        intent: BillCrudIntent.TransferAccountSelect,
    ) {
        val bookId =
            categorySelectUseCase.bookIdStateOb.firstOrNull() ?: throw NoBookSelectException()
        val accountId = AppRouterCoreApi::class
            .routeApi()
            .accountSelectBySuspend(
                context = intent.context,
                bookId = bookId,
            )
            .getStringExtra("data").orNull()
        transferAccountIdStateOb.emit(
            value = accountId,
        )
    }

    @IntentProcess
    private suspend fun transferTargetAccountSelect(
        intent: BillCrudIntent.TransferTargetAccountSelect,
    ) {
        val bookId =
            categorySelectUseCase.bookIdStateOb.firstOrNull() ?: throw NoBookSelectException()
        val accountId = AppRouterCoreApi::class
            .routeApi()
            .accountSelectBySuspend(
                context = intent.context,
                bookId = bookId,
            )
            .getStringExtra("data").orNull()
        transferTargetAccountIdStateOb.emit(
            value = accountId,
        )
    }

    @IntentProcess
    private suspend fun transferAccountSwitch(
        intent: BillCrudIntent.TransferAccountSwitch,
    ) {
        val transferAccountId = transferAccountIdStateOb.first()
        val transferTargetAccountId = transferTargetAccountIdStateOb.first()
        transferAccountIdStateOb.emit(
            value = transferTargetAccountId,
        )
        transferTargetAccountIdStateOb.emit(
            value = transferAccountId,
        )
    }

    @IntentProcess
    private suspend fun imageSelect(
        intent: BillCrudIntent.ImageSelect,
    ) {
        val urlList = ParameterSupport.getStringArrayList(
            intent = AppRouterCoreApi::class
                .routeApi()
                .billImageCrudBySuspend(
                    context = intent.context,
                    imageUrlList = ArrayList(
                        imageUrlListState.first(),
                    ),
                ),
            key = "data",
        ) ?: emptyList()
        LogSupport.d(
            tag = BillCrudUseCase.TAG,
            content = "urlList = ${urlList.joinToString()}",
        )
        imageUrlListState.emit(
            value = urlList,
        )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
        costUseCase.destroy()
        categorySelectUseCase.destroy()
    }

}
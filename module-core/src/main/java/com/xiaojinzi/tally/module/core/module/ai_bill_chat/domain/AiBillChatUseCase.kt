package com.xiaojinzi.tally.module.core.module.ai_bill_chat.domain

import android.content.Context
import androidx.annotation.UiContext
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.PublishHotObservable
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.DAY_MS
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.NormalMutableSharedFlow
import com.xiaojinzi.support.ktx.findException
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.network.AppNetworkException
import com.xiaojinzi.tally.lib.res.model.tally.BillChatDto
import com.xiaojinzi.tally.lib.res.model.tally.BillChatInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.MoneyFen
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDetailDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBillInsertDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.DevelopHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

data class AiBillChatItemUseCaseDto(
    val core: BillChatDto,
    val billDetail: TallyBillDetailDto?,
)

sealed class AiBillChatIntent {

    data class Submit(
        @UiContext val context: Context,
    ) : AiBillChatIntent()

    data class Retry(
        @UiContext val context: Context,
        val id: Long,
    ) : AiBillChatIntent()

    data class EditBill(
        @UiContext val context: Context,
        val id: Long,
    ) : AiBillChatIntent()

    data class DeleteBill(
        @UiContext val context: Context,
        val id: Long,
    ) : AiBillChatIntent()

    data class ToBillDetail(
        @UiContext val context: Context,
        val billId: String?,
    ) : AiBillChatIntent()

}

@ViewModelLayer
interface AiBillChatUseCase : BusinessMVIUseCase {

    companion object {
        const val TAG = "AiBillChatUseCase"
    }

    /**
     *  去第一条数据的事件
     */
    @PublishHotObservable
    val toFirstItemEvent: NormalMutableSharedFlow<Unit>

    /**
     * 隐藏键盘的事件
     */
    @PublishHotObservable
    val hideKeyboardEvent: Flow<Unit>

    /**
     * 第几页
     */
    @StateHotObservable
    val pageStateOb: Flow<Int>

    /**
     * 输入的内容
     */
    @StateHotObservable
    val contentStateOb: MutableSharedStateFlow<String>

    /**
     * 要显示的列表
     * 会根据上面的时间
     */
    @StateHotObservable
    val billChatListStateOb: Flow<List<AiBillChatItemUseCaseDto>>

}

@ViewModelLayer
class AiBillChatUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AiBillChatUseCase {

    override val toFirstItemEvent = NormalMutableSharedFlow<Unit>()

    override val hideKeyboardEvent = NormalMutableSharedFlow<Unit>()

    override val pageStateOb = MutableSharedStateFlow(
        initValue = 1,
    )

    override val contentStateOb = MutableSharedStateFlow(
        initValue = "",
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val billChatListStateOb = AppServices
        .tallyDataSourceSpi
        .selectedBookStateOb
        .flatMapLatest { currentBookInfo ->
            currentBookInfo?.let {
                pageStateOb
                    .distinctUntilChanged()
                    .flatMapLatest { page ->
                        LogSupport.d(
                            tag = AiBillChatUseCase.TAG,
                            content = "billListStateOb flatMapLatest page = $page",
                        )
                        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
                        tallyDataSourceSpi
                            .subscribeDataBaseTableChangedOb(
                                TallyTable.Bill,
                                TallyTable.BillLabel,
                                TallyTable.BillChat,
                                TallyTable.Account,
                                TallyTable.Category,
                                emitOneWhileSubscribe = true,
                            )
                            // 计算出起始时间
                            .map {
                                val dayTimeList = tallyDataSourceSpi
                                    .getBillChatDayTimeList(
                                        bookId = currentBookInfo.id,
                                        pageStartIndex = 0,
                                        pageSize = page * 10L,
                                    )
                                val startTime = dayTimeList.minOrNull()
                                if (startTime == null) {
                                    emptyList()
                                } else // 占位
                                {
                                    tallyDataSourceSpi
                                        .getBillChatList(
                                            bookId = currentBookInfo.id,
                                            afterTime = startTime,
                                        ).map { billChatItem ->
                                            AiBillChatItemUseCaseDto(
                                                core = billChatItem,
                                                billDetail = billChatItem.billId.orNull()
                                                    ?.let { billId ->
                                                        tallyDataSourceSpi.getBillDetailById(
                                                            id = billId,
                                                        )?.getAdapter
                                                    },
                                            )
                                        }
                                }
                            }
                            .onEach {
                                LogSupport.d(
                                    tag = AiBillChatUseCase.TAG,
                                    content = "billListStateOb resultList.size = ${it.size}",
                                )
                            }
                    }
            } ?: flowOf(
                value = emptyList(),
            )

        }
        .sharedStateIn(
            scope = scope,
            initValue = emptyList(),
        )

    private suspend fun doAiParse(
        context: Context,
        billChatId: Long,
        content: String,
        time: Long? = null,
    ) {
        val doAiParseResult = runCatching {
            val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
            val currentBookInfo = tallyDataSourceSpi.requiredSelectedBookInfo()
            val currentUserInfo = AppServices.userSpi.requiredUserInfo()
            val defAccount = tallyDataSourceSpi
                .getAccountByBookId(
                    bookId = currentBookInfo.id,
                    isExcludeDeleted = true,
                )
                .let { list ->
                    if (list.size == 1) {
                        list.first()
                    } else {
                        list.firstOrNull { it.isDefault }
                    }
                }
            val categoryList = tallyDataSourceSpi
                .getCategoryByBookId(bookId = currentBookInfo.id)
                .mapNotNull { it.getAdapter }
            val spendingCategoryList = categoryList.filter {
                it.type == TallyCategoryDto.Companion.TallyCategoryType.SPENDING
            }
            val incomeCategoryList = categoryList.filter {
                it.type == TallyCategoryDto.Companion.TallyCategoryType.INCOME
            }
            val amount = runCatching {
                content
                    // 过滤出数字和.
                    .filter {
                        it.isDigit() || it == '.'
                    }.toFloatOrNull()
            }.getOrNull()
            val analyzeRunCatchingResult = amount?.let {
                kotlin.runCatching {
                    AppServices
                        .appNetworkSpi
                        .aiBillAnalyze(
                            spendingCategoryNameList = spendingCategoryList.mapNotNull { it.name.orNull() },
                            incomeCategoryNameList = incomeCategoryList.mapNotNull { it.name.orNull() },
                            content = content,
                        )
                }
            }
            val analyzeResult = analyzeRunCatchingResult?.getOrNull()
            if (amount == null) {
                tip(content = "无法识别金额".toStringItemDto())
                tallyDataSourceSpi.getBillChatById(
                    id = billChatId,
                )?.let {
                    tallyDataSourceSpi.updateBillChat(
                        target = it.copy(
                            state = BillChatDto.STATE_FAIL,
                        )
                    )
                }
            } else if (analyzeResult == null) {

                val isVipException = analyzeRunCatchingResult?.exceptionOrNull()?.findException(
                    targetClass = AppNetworkException::class,
                )?.code == AppNetworkException.CODE_VIP_AUTH_FAIL

                tallyDataSourceSpi.getBillChatById(
                    id = billChatId,
                )?.let {
                    tallyDataSourceSpi.updateBillChat(
                        target = it.copy(
                            state = BillChatDto.STATE_FAIL,
                        )
                    )
                }
                if (isVipException) {
                    confirmDialogOrError(
                        content = "AI 试用次数已经用完, 继续使用请开通 Vip".toStringItemDto(),
                        negative = "朕再想想".toStringItemDto(),
                        positive = "开通 Vip".toStringItemDto(),
                    )
                    AppRouterUserApi::class
                        .routeApi()
                        .toVipBuyView(
                            context = context,
                        )
                } else {
                    tip(content = "AI 分析失败".toStringItemDto())
                }
            } else // 占位
            {
                if (amount > 9999999f) {
                    tip(content = "金额超出最大限制: 9999999".toStringItemDto())
                } else {
                    val isYesterday = content.contains(other = "昨天")
                    val targetCategoryItem = when (analyzeResult.isSpending) {
                        null -> {
                            spendingCategoryList
                                .find { it.name == analyzeResult.categoryName }
                                ?: incomeCategoryList
                                    .find { it.name == analyzeResult.categoryName }
                        }

                        false -> {
                            incomeCategoryList
                                .find { it.name == analyzeResult.categoryName }
                        }

                        else -> {
                            spendingCategoryList
                                .find { it.name == analyzeResult.categoryName }
                        }
                    }
                    val isSpending = when (targetCategoryItem?.type) {
                        TallyCategoryDto.Companion.TallyCategoryType.INCOME -> false
                        else -> {
                            analyzeResult.isSpending != false
                        }
                    }
                    val absAmount = amount.times(100f).roundToLong().absoluteValue
                    val targetTime = time ?: if (isYesterday) {
                        System.currentTimeMillis() - DAY_MS
                    } else {
                        System.currentTimeMillis()
                    }
                    val billId = tallyDataSourceSpi.insertBill(
                        target = TallyBillInsertDto(
                            userId = currentUserInfo.id,
                            bookId = currentBookInfo.id,
                            type = TallyBillDto.Type.NORMAL.value,
                            accountId = defAccount?.id,
                            time = targetTime,
                            categoryId = targetCategoryItem?.id,
                            amount = MoneyFen(
                                value = if (!isSpending) {
                                    absAmount
                                } else {
                                    -absAmount
                                },
                            ),
                            note = content.filter {
                                !it.isDigit() && it != '.'
                            }.trim(),
                        )
                    )
                    tallyDataSourceSpi.getBillChatById(
                        id = billChatId,
                    )?.let { billChatDto ->
                        tallyDataSourceSpi.updateBillChat(
                            target = billChatDto.copy(
                                state = BillChatDto.STATE_SUCCESS,
                                billId = billId,
                            )
                        )
                    }
                }
            }

        }
        LogSupport.d(
            tag = AiBillChatUseCase.TAG,
            content = "doAiParse result = $doAiParseResult",
        )
        if (DevelopHelper.isDevelop) {
            doAiParseResult.exceptionOrNull()?.printStackTrace()
        }
    }

    @IntentProcess
    private suspend fun toBillDetail(intent: AiBillChatIntent.ToBillDetail) {
        intent.billId.orNull()?.let { billId ->
            AppRouterCoreApi::class
                .routeApi()
                .toBillDetailView(
                    context = intent.context,
                    billId = billId,
                )
        }
    }

    @IntentProcess
    private suspend fun deleteBill(intent: AiBillChatIntent.DeleteBill) {
        confirmDialogOrError(
            content = "确定删除此账单吗?".toStringItemDto(),
        )
        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
        tallyDataSourceSpi.getBillChatById(id = intent.id)
            ?.let { billChatInfo ->
                // 如果账单存在就删除
                billChatInfo.billId.orNull()?.let { billId ->
                    tallyDataSourceSpi.getBillDetailById(
                        id = billId,
                    )?.let { billDetail ->
                        tallyDataSourceSpi.updateBill(
                            target = billDetail.core.copy(
                                isDeleted = false,
                            )
                        )
                    }
                }
                tallyDataSourceSpi.deleteBillChatById(
                    id = billChatInfo.id,
                )
            }
    }

    @IntentProcess
    private suspend fun editBill(intent: AiBillChatIntent.EditBill) {
        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
        tallyDataSourceSpi.getBillChatById(id = intent.id)
            ?.let { billChatInfo ->
                billChatInfo.billId.orNull()?.let { billId ->
                    AppRouterCoreApi::class
                        .routeApi()
                        .toBillCrudView(
                            context = intent.context,
                            billId = billId,
                        )
                }
            }
    }

    @IntentProcess
    private suspend fun retry(intent: AiBillChatIntent.Retry) {
        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
        tallyDataSourceSpi.getBillChatById(id = intent.id)
            ?.let { billChatInfo ->
                // 更新为初始化状态
                tallyDataSourceSpi.updateBillChat(
                    target = billChatInfo.copy(
                        state = BillChatDto.STATE_INIT,
                    )
                )
                val content = billChatInfo.content
                if (content.isNullOrEmpty()) {
                    tip(
                        content = "内容为空, 不能分析".toStringItemDto(),
                    )
                } else {
                    doAiParse(
                        context = intent.context,
                        billChatId = billChatInfo.id,
                        content = content,
                        time = billChatInfo.timeCreated,
                    )
                }
            }
    }

    @IntentProcess
    private suspend fun submit(intent: AiBillChatIntent.Submit) {
        val tallyDataSourceSpi = AppServices.tallyDataSourceSpi
        val currentBookInfo = tallyDataSourceSpi.requiredSelectedBookInfo()
        val content = contentStateOb.firstOrNull()?.trim()
        if (content.isNullOrEmpty()) {
            tip(
                content = "内容不能为空".toStringItemDto(),
            )
        } else {
            // 插入一个初始状态的账单聊天
            val billChatId = tallyDataSourceSpi
                .insertBillChat(
                    target = BillChatInsertDto(
                        state = BillChatDto.STATE_INIT,
                        content = content,
                        bookId = currentBookInfo.id,
                        billId = null,
                    )
                )
            contentStateOb.emit(
                value = ""
            )
            toFirstItemEvent.emit(
                value = Unit,
            )
            hideKeyboardEvent.emit(
                value = Unit,
            )
            doAiParse(
                context = intent.context,
                billChatId = billChatId,
                content = content,
            )
        }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

    init {
        scope.launchIgnoreError {
            LogSupport.d(
                tag = AiBillChatUseCase.TAG,
                content = "updateAllInitBillChatToFail start",
            )
            AppServices
                .tallyDataSourceSpi
                .updateAllInitBillChatToFail()
            LogSupport.d(
                tag = AiBillChatUseCase.TAG,
                content = "updateAllInitBillChatToFail end",
            )
        }
    }

}
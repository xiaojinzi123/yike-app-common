package com.xiaojinzi.tally.module.core.module.bill_search.domain

import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.ui.text.input.TextFieldValue
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.component.support.ParameterSupport
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.DAY_MS
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getYearByTimeStamp
import com.xiaojinzi.support.ktx.getYearInterval
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.lib.res.model.support.DateTimeModel
import com.xiaojinzi.tally.lib.res.model.support.DateTimeType
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyBookDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyCategoryDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyLabelDto
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCase
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCaseImpl
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppRouterBaseApi
import com.xiaojinzi.tally.module.base.support.AppRouterCoreApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

sealed class BillSearchIntent {

    data object Submit : BillSearchIntent()

    data class ParameterInit(
        @UiContext val context: Context,
        val accountIdSet: Set<String>,
        val labelIdSet: Set<String>,
    ) : BillSearchIntent()

    data object ResetSearch : BillSearchIntent()

    data class YearSelect(
        @UiContext val context: Context,
        val isSearchAfterSelect: Boolean = false,
    ) : BillSearchIntent()

    data class TimeStartSelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class TimeEndSelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class DoSearch(
        @UiContext val context: Context,
        val isVipTip: Boolean = false,
    ) : BillSearchIntent()

    data class BookSelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class BookDelete(
        val id: String,
    ) : BillSearchIntent()

    data class CategorySelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class CategoryDelete(
        val id: String,
    ) : BillSearchIntent()

    data class AccountSelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class AccountDelete(
        val id: String,
    ) : BillSearchIntent()

    data class LabelSelect(
        @UiContext val context: Context,
    ) : BillSearchIntent()

    data class LabelDelete(
        val id: String,
    ) : BillSearchIntent()

}

@ViewModelLayer
interface BillSearchUseCase : BusinessMVIUseCase {

    val billQueryConditionUseCase: CommonBillQueryConditionUseCase

    /**
     * 搜索的 key
     */
    @StateHotObservable
    val searchKeyStateOb: MutableStateFlow<TextFieldValue>

    /**
     * 账本 ID 的列表
     */
    @StateHotObservable
    val bookIdListStateOb: Flow<List<String>>

    /**
     * 选择了一个账本的时候, 这个才有值
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

    /**
     * 账本的列表
     */
    @StateHotObservable
    val bookInfoListStateOb: Flow<List<TallyBookDto>>

    /**
     * 类别 ID 的列表
     */
    @StateHotObservable
    val categoryIdListStateOb: Flow<List<String>>

    /**
     * 类别的列表
     */
    @StateHotObservable
    val categoryInfoListStateOb: Flow<List<TallyCategoryDto>>

    /**
     * 账户 ID 的列表
     */
    @StateHotObservable
    val accountIdListStateOb: Flow<List<String>>

    /**
     * 账户的列表
     */
    @StateHotObservable
    val accountInfoListStateOb: Flow<List<TallyAccountDto>>

    /**
     * 标签 ID 的列表
     */
    @StateHotObservable
    val labelIdListStateOb: Flow<List<String>>

    /**
     * 标签的列表
     */
    @StateHotObservable
    val labelInfoListStateOb: Flow<List<TallyLabelDto>>

    /**
     * 选择的年份的时间
     */
    @StateHotObservable
    val yearSelectTimeStateOb: Flow<Long>

    /**
     * 选择的年份的区间
     */
    @StateHotObservable
    val yearSelectRangeTimeStateOb: Flow<Pair<Long, Long>>

    /**
     * 选择的年份
     */
    @StateHotObservable
    val yearSelectStateOb: Flow<Int>

    /**
     * 起始时间
     */
    @StateHotObservable
    val timeStartStateOb: Flow<Long?>

    /**
     * 起始时间
     */
    @StateHotObservable
    val timeEndStateOb: Flow<Long?>

    /**
     * 最低金额
     */
    @StateHotObservable
    val amountMinStateOb: MutableStateFlow<String>

    /**
     * 最高金额
     */
    @StateHotObservable
    val amountMaxStateOb: MutableStateFlow<String>

    /**
     * 是否有图片
     */
    @StateHotObservable
    val hasImageStateOb: MutableStateFlow<Boolean>

    /**
     * 是否使用了高级搜索 o
     */
    @StateHotObservable
    val isUseAdvancedSearchStateOb: Flow<Boolean>

}

@OptIn(FlowPreview::class)
@ViewModelLayer
class BillSearchUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val billQueryConditionUseCase: CommonBillQueryConditionUseCase = CommonBillQueryConditionUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), BillSearchUseCase {

    override val searchKeyStateOb = MutableStateFlow(value = TextFieldValue(text = ""))

    override val bookIdListStateOb = MutableSharedStateFlow<List<String>>(
        initValue = emptyList(),
    )

    override val bookIdStateOb = bookIdListStateOb
        .map { bookIdList ->
            if (bookIdList.size == 1) {
                bookIdList.first()
            } else {
                null
            }
        }

    override val bookInfoListStateOb = bookIdListStateOb
        .map { list ->
            list.mapNotNull {
                AppServices
                    .tallyDataSourceSpi
                    .getBookById(
                        id = it,
                    )
            }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val categoryIdListStateOb = MutableSharedStateFlow<List<String>>(
        initValue = emptyList(),
    )

    override val categoryInfoListStateOb = combine(
        bookIdStateOb,
        categoryIdListStateOb
    ) { bookId, categoryIdList ->
        bookId?.let {
            categoryIdList.mapNotNull {
                AppServices
                    .tallyDataSourceSpi
                    .getCategoryByIdAndBookId(
                        id = it,
                        bookId = bookId,
                    )
            }
        } ?: emptyList()
    }.sharedStateIn(
        scope = scope,
    )

    override val accountIdListStateOb = MutableSharedStateFlow<List<String>>(
        initValue = emptyList(),
    )

    override val accountInfoListStateOb = combine(
        bookIdStateOb,
        accountIdListStateOb
    ) { bookId, accountIdList ->
        bookId?.let {
            accountIdList.mapNotNull {
                AppServices
                    .tallyDataSourceSpi
                    .getAccountByIdAndBookId(
                        id = it,
                        bookId = bookId,
                    )
            }
        } ?: emptyList()
    }.sharedStateIn(
        scope = scope,
    )

    override val labelIdListStateOb = MutableSharedStateFlow<List<String>>(
        initValue = emptyList(),
    )

    override val labelInfoListStateOb = combine(
        bookIdStateOb,
        labelIdListStateOb
    ) { bookId, labelIdList ->
        bookId?.let {
            labelIdList.mapNotNull {
                AppServices
                    .tallyDataSourceSpi
                    .getLabelUnderBook(
                        id = it,
                        bookId = bookId,
                    )
            }
        } ?: emptyList()
    }.sharedStateIn(
        scope = scope,
    )

    override val yearSelectTimeStateOb = MutableSharedStateFlow(
        initValue = System.currentTimeMillis(),
    )

    override val yearSelectRangeTimeStateOb = yearSelectTimeStateOb
        .map {
            getYearInterval(
                timeStamp = it,
            )
        }
        .sharedStateIn(
            scope = scope,
        )

    override val yearSelectStateOb = yearSelectTimeStateOb
        .map {
            getYearByTimeStamp(
                timeStamp = it,
            )
        }

    override val timeStartStateOb = MutableSharedStateFlow<Long?>(
        initValue = null,
    )

    override val timeEndStateOb = MutableSharedStateFlow<Long?>(
        initValue = null,
    )

    override val amountMinStateOb = MutableStateFlow(value = "")

    override val amountMaxStateOb = MutableStateFlow(value = "")

    override val hasImageStateOb = MutableStateFlow(value = false)

    override val isUseAdvancedSearchStateOb = com.xiaojinzi.support.ktx.combine(
        timeStartStateOb,
        timeEndStateOb,
        amountMinStateOb,
        amountMaxStateOb,
        categoryIdListStateOb,
        accountIdListStateOb,
        labelIdListStateOb,
        hasImageStateOb,
    ) { timeStart, timeEnd, amountMin, amountMax, categoryIdList, accountIdList, labelIdList, hasImage ->
        timeStart != null || timeEnd != null || amountMin.isNotEmpty() || amountMax.isNotEmpty() ||
                categoryIdList.isNotEmpty() || accountIdList.isNotEmpty() || labelIdList.isNotEmpty() || hasImage
    }.sharedStateIn(
        scope = scope,
    )

    @IntentProcess
    private suspend fun yearSelect(intent: BillSearchIntent.YearSelect) {
        val time = ParameterSupport.getLong(
            intent = AppRouterBaseApi::class
                .routeApi()
                .dateTimeSelectBySuspend(
                    context = intent.context,
                    time = yearSelectTimeStateOb.firstOrNull(),
                    type = DateTimeType.Year,
                ),
            key = "data",
        ) ?: System.currentTimeMillis()
        yearSelectTimeStateOb.emit(
            value = time,
        )
        timeStartStateOb.emit(
            value = null,
        )
        timeEndStateOb.emit(
            value = null,
        )
        // 时间太快的时候, 有 Bug
        delay(500)
        if (intent.isSearchAfterSelect) {
            this.addIntent(
                intent = BillSearchIntent.DoSearch(
                    context = intent.context,
                ),
            )
        }
    }

    @IntentProcess
@BusinessMVIUseCase.ErrorIgnore
    private suspend fun timeStartSelect(intent: BillSearchIntent.TimeStartSelect) {
        val (startTime, endTime) = yearSelectRangeTimeStateOb.first()
        val currentEndTime = timeEndStateOb.first()
        ParameterSupport.getLong(
            intent = AppRouterBaseApi::class
                .routeApi()
                .dateTimeSelectBySuspend(
                    context = intent.context,
                    time = timeStartStateOb.firstOrNull()
                        ?: yearSelectTimeStateOb.firstOrNull(),
                    model = DateTimeModel.DropLast,
                ),
            key = "data",
        )?.coerceIn(
            minimumValue = startTime,
            maximumValue = endTime,
        )?.let { time ->
            if (currentEndTime != null && time > currentEndTime) {
                tip(
                    content = "开始时间不能大于结束时间".toStringItemDto(),
                )
            } else {
                timeStartStateOb.emit(
                    value = time,
                )
            }
        }
    }

    @IntentProcess
@BusinessMVIUseCase.ErrorIgnore
    private suspend fun timeEndSelect(intent: BillSearchIntent.TimeEndSelect) {
        val (startTime, endTime) = yearSelectRangeTimeStateOb.first()
        val currentStartTime = timeStartStateOb.first()
        ParameterSupport.getLong(
            intent = AppRouterBaseApi::class
                .routeApi()
                .dateTimeSelectBySuspend(
                    context = intent.context,
                    time = timeEndStateOb.firstOrNull() ?: yearSelectTimeStateOb.firstOrNull(),
                    model = DateTimeModel.DropLast,
                ),
            key = "data",
        )?.plus(other = DAY_MS)?.minus(other = 1)?.coerceIn(
            minimumValue = startTime,
            maximumValue = endTime,
        )?.let { time ->
            if (currentStartTime != null && time < currentStartTime) {
                tip(
                    content = "结束时间不能小于开始时间".toStringItemDto(),
                )
            } else {
                timeEndStateOb.emit(
                    value = time,
                )
            }
        }
    }

    @IntentProcess
    private suspend fun bookSelect(intent: BillSearchIntent.BookSelect) {
        bookIdListStateOb.emit(
            value = ParameterSupport.getStringArrayList(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .bookSelect1SuspendForResult(
                        context = intent.context,
                        bookIdList = ArrayList(
                            bookIdListStateOb.first(),
                        ),
                    ),
                key = "data",
            ) ?: emptyList(),
        )
        categoryIdListStateOb.emit(
            value = emptyList(),
        )
        accountIdListStateOb.emit(
            value = emptyList(),
        )
    }

    @IntentProcess
    private suspend fun bookDelete(intent: BillSearchIntent.BookDelete) {
        bookIdListStateOb.emit(
            value = bookIdListStateOb
                .first()
                .filter {
                    it != intent.id
                }
        )
        categoryIdListStateOb.emit(
            value = emptyList(),
        )
        accountIdListStateOb.emit(
            value = emptyList(),
        )
    }

    @IntentProcess
    private suspend fun categorySelect(intent: BillSearchIntent.CategorySelect) {
        val bookInfoList = bookInfoListStateOb.first()
        if (bookInfoList.size != 1) {
            tip(
                content = "只能选择一个账本, 才能选择类别".toStringItemDto(),
            )
            return
        }
        val bookId = bookInfoList.firstOrNull()?.id ?: return
        categoryIdListStateOb.emit(
            value = ParameterSupport.getStringArrayList(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .categorySelect1BySuspend(
                        context = intent.context,
                        bookId = bookId,
                        categoryIdList = ArrayList(
                            categoryIdListStateOb.first(),
                        ),
                    ),
                key = "data",
            ) ?: emptyList(),
        )
    }

    @IntentProcess
    private suspend fun categoryDelete(intent: BillSearchIntent.CategoryDelete) {
        categoryIdListStateOb.emit(
            value = categoryIdListStateOb
                .first()
                .filter {
                    it != intent.id
                }
        )
    }

    @IntentProcess
    private suspend fun accountSelect(intent: BillSearchIntent.AccountSelect) {
        val bookInfoList = bookInfoListStateOb.first()
        if (bookInfoList.size != 1) {
            tip(
                content = "只能选择一个账本, 才能选择账户".toStringItemDto(),
            )
            return
        }
        val bookId = bookInfoList.firstOrNull()?.id ?: return
        accountIdListStateOb.emit(
            value = ParameterSupport.getStringArrayList(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .accountSelect1BySuspend(
                        context = intent.context,
                        bookId = bookId,
                        accountIdList = ArrayList(
                            accountIdListStateOb.first(),
                        ),
                    ),
                key = "data",
            ) ?: emptyList(),
        )
    }

    @IntentProcess
    private suspend fun labelSelect(intent: BillSearchIntent.LabelSelect) {
        val bookInfoList = bookInfoListStateOb.first()
        if (bookInfoList.size != 1) {
            tip(
                content = "只能选择一个账本, 才能选择标签".toStringItemDto(),
            )
            return
        }
        val bookId = bookInfoList.firstOrNull()?.id ?: return
        labelIdListStateOb.emit(
            value = ParameterSupport.getStringArrayList(
                intent = AppRouterCoreApi::class
                    .routeApi()
                    .labelSelectBySuspend(
                        context = intent.context,
                        bookId = bookId,
                        idList = ArrayList(
                            labelIdListStateOb.first(),
                        ),
                    ),
                key = "data",
            ) ?: emptyList(),
        )
    }

    @IntentProcess
    private suspend fun accountDelete(intent: BillSearchIntent.AccountDelete) {
        accountIdListStateOb.emit(
            value = accountIdListStateOb
                .first()
                .filter {
                    it != intent.id
                }
        )
    }

    @IntentProcess
    private suspend fun labelDelete(intent: BillSearchIntent.LabelDelete) {
        labelIdListStateOb.emit(
            value = labelIdListStateOb
                .first()
                .filter {
                    it != intent.id
                }
        )
    }

    @IntentProcess
    private suspend fun parameterInit(intent: BillSearchIntent.ParameterInit) {

        val accountInfoList = if (intent.accountIdSet.isNotEmpty()) {
            intent
                .accountIdSet
                .mapNotNull {
                    AppServices
                        .tallyDataSourceSpi
                        .getAccountById(
                            id = it,
                        )
                        ?.getAdapter
                }
        } else {
            emptyList()
        }

        val labelInfoList = if (intent.labelIdSet.isNotEmpty()) {
            intent
                .labelIdSet
                .mapNotNull {
                    AppServices
                        .tallyDataSourceSpi
                        .getLabel(
                            id = it,
                        )
                        ?.getAdapter
                }
        } else {
            emptyList()
        }

        val bookIdListMerge = accountInfoList
            .map { it.bookId } + labelInfoList.map { it.bookId }

        if (bookIdListMerge.size == 1) {
            bookIdListStateOb.emit(
                value = bookIdListMerge,
            )
            accountIdListStateOb.emit(
                value = accountInfoList.map { it.id },
            )
            labelIdListStateOb.emit(
                value = labelInfoList.map { it.id },
            )
            addIntent(
                intent = BillSearchIntent.DoSearch(
                    context = intent.context,
                    isVipTip = true,
                )
            )
        }

    }

    @IntentProcess
    private suspend fun resetSearch(intent: BillSearchIntent.ResetSearch) {
        yearSelectTimeStateOb.emit(
            value = System.currentTimeMillis(),
        )
        timeStartStateOb.emit(
            value = null,
        )
        timeEndStateOb.emit(
            value = null,
        )
        bookIdListStateOb.emit(
            value = emptyList(),
        )
        categoryIdListStateOb.emit(
            value = emptyList(),
        )
        accountIdListStateOb.emit(
            value = emptyList(),
        )
        amountMinStateOb.emit(
            value = "",
        )
        amountMaxStateOb.emit(
            value = "",
        )
        hasImageStateOb.emit(
            value = false,
        )
    }

    /**
     * 根据选择好的条件, 拼接出查询的条件
     * null 表示没有数据
     */
    @IntentProcess
@BusinessMVIUseCase.ErrorIgnore
    private suspend fun doSearch(intent: BillSearchIntent.DoSearch) {
        val isUseAdvancedSearch =  isUseAdvancedSearchStateOb.first()
        val isVip = AppServices.userSpi.isVipStateOb.first()
        if (isUseAdvancedSearch) {
            if (!isVip) {
                if (intent.isVipTip) {
                    confirmDialogOrError(
                        content = "高级搜索功能需要 VIP 才能使用\n是否开通 VIP?".toStringItemDto(),
                    )
                }
                AppRouterUserApi::class
                    .routeApi()
                    .toVipBuyView(
                        context = intent.context,
                    )
                return
            }
        }
        val timeRange = yearSelectRangeTimeStateOb.first()
        val startTime = if (isVip) {
            timeStartStateOb.first()
        } else {
            null
        }
        val endTime = if (isVip) {
            timeEndStateOb.first()
        } else {
            null
        }
        val searchKey = searchKeyStateOb.first().text
        // 账本不卡 vip
        val bookIdList = bookIdListStateOb.first()
        val singleBookId = bookIdList.firstOrNull()
        val categoryIdList = if (isVip) {
            categoryIdListStateOb.first()
        } else {
            emptyList()
        }
        val accountIdList = if (isVip) {
            accountIdListStateOb.first()
        } else {
            emptyList()
        }
        val labelIdList = if (isVip) {
            labelIdListStateOb.first()
        } else {
            emptyList()
        }
        val amountMin = if (isVip) {
            amountMinStateOb.first().toLongOrNull()?.times(100)
        } else {
            null
        }
        val amountMax = if (isVip) {
            amountMaxStateOb.first().toLongOrNull()?.times(100)
        } else {
            null
        }
        val hasImage = if (isVip) {
            hasImageStateOb.first()
        } else {
            false
        }
        if (startTime != null && endTime != null && startTime > endTime) {
            tip(
                content = "开始时间不能大于结束时间".toStringItemDto(),
            )
            return
        }

        // 搜索的条件
        val searchQueryInfo = if (searchKey.isEmpty()) {
            null
        } else // 占位
        {
            val categoryIdListForKeySearch = AppServices
                .tallyDataSourceSpi
                .searchCategory(
                    key = searchKey,
                )
                .filter { !it.isDeleted }
                .map { it.id }
            val accountIdListForKeySearch = AppServices
                .tallyDataSourceSpi
                .searchAccount(
                    key = searchKey,
                )
                .filter { !it.isDeleted }
                .map { it.id }
            val labelIdListForKeySearch = AppServices
                .tallyDataSourceSpi
                .searchLabel(
                    key = searchKey,
                )
                .filter { !it.isDeleted }
                .map { it.id }
            val billIdList = if (labelIdListForKeySearch.isEmpty()) {
                emptyList()
            } else {
                AppServices
                    .tallyDataSourceSpi
                    .getBillIdListByLabelIdList(
                        labelIdList = labelIdListForKeySearch,
                        isExcludeDeleted = true,
                    )
            }
            TallyDataSourceSpi.Companion.SearchQueryConditionDto(
                billIdList = billIdList.orNull(),
                categoryIdList = categoryIdListForKeySearch.orNull(),
                aboutAccountIdList = accountIdListForKeySearch.orNull(),
                noteKey = searchKey,
            )
        }
        // null 可以显示全部, 空数组表示没有数据
        // 当所有条件都是没选择的时候, 就不需要查询了, 直接 null 表示没有数据
        val queryConditionInfo =
            if (searchQueryInfo == null && bookIdList.isEmpty() &&
                categoryIdList.isEmpty() && accountIdList.isEmpty() &&
                labelIdList.isEmpty() &&
                amountMin == null && amountMax == null &&
                startTime == null && endTime == null &&
                !hasImage
            ) {
                null
            } else {
                val billIdListInLabel = if (labelIdList.isEmpty() || singleBookId.isNullOrEmpty()) {
                    emptyList()
                } else {
                    AppServices
                        .tallyDataSourceSpi
                        .getBillIdListByLabelIdList(
                            bookId = singleBookId,
                            labelIdList = labelIdList,
                        )
                }
                // 如果有图片条件成立
                val billIdListInImage = if (hasImage) {
                    AppServices
                        .tallyDataSourceSpi
                        .getBillIdListInImage(
                            isExcludeDeleted = true,
                        )
                } else {
                    emptyList()
                }
                val billIdList =
                    if (billIdListInLabel.isNotEmpty() && billIdListInImage.isNotEmpty()) {
                        // billIdListInLabel 和 billIdListInImage 交集
                        billIdListInLabel.intersect(other = billIdListInImage.toSet())
                    } else {
                        billIdListInLabel.union(other = billIdListInImage)
                    }.toList()
                if (labelIdList.isNotEmpty() && billIdListInLabel.isEmpty()) {
                    null
                } else if (hasImage && billIdListInImage.isEmpty()) {
                    null
                } else {
                    TallyDataSourceSpi.Companion.BillQueryConditionDto(
                        idList = billIdList,
                        categoryIdList = categoryIdList,
                        aboutAccountIdList = accountIdList,
                        searchQueryInfo = searchQueryInfo,
                        bookIdList = bookIdList,
                        amountMin = amountMin,
                        amountMax = amountMax,
                        startTimeInclude = startTime ?: timeRange.first,
                        endTimeInclude = endTime ?: timeRange.second,
                    )
                }
            }
        billQueryConditionUseCase.queryConditionStateOb.emit(
            value = queryConditionInfo,
        )
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: BillSearchIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
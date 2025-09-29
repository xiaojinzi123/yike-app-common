package com.xiaojinzi.tally.module.core.module.account_detail.domain

import androidx.annotation.UiContext
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.TallyAccountDto
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCase
import com.xiaojinzi.tally.module.base.module.common_bill_list.domain.CommonBillQueryConditionUseCaseImpl
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

sealed class AccountDetailIntent {

    data class ParameterDataInit(
        @UiContext val accountId: String?,
    ) : AccountDetailIntent()

    data object Submit : AccountDetailIntent()

}

@ViewModelLayer
interface AccountDetailUseCase : BusinessMVIUseCase {

    val commonBillQueryConditionUseCase: CommonBillQueryConditionUseCase

    /**
     *  账户 Id
     */
    @StateHotObservable
    val accountIdStateOb: Flow<String?>

    /**
     * 账户信息
     */
    @StateHotObservable
    val accountInfoStateOb: Flow<TallyAccountDto?>

}

@ViewModelLayer
class AccountDetailUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
    override val commonBillQueryConditionUseCase: CommonBillQueryConditionUseCase = CommonBillQueryConditionUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountDetailUseCase {

    override val accountIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val accountInfoStateOb = combine(
        AppServices
            .tallyDataSourceSpi
            .subscribeDataBaseTableChangedOb(
                TallyTable.Account, TallyTable.Bill,
                emitOneWhileSubscribe = true,
            ),
        accountIdStateOb,
    ) { _, accountId ->
        accountId?.let {
            AppServices
                .tallyDataSourceSpi
                .getAccountById(
                    id = accountId,
                )
        }
    }.sharedStateIn(
        scope = scope,
    )

    @IntentProcess
    private suspend fun parameterDataInit(
        intent: AccountDetailIntent.ParameterDataInit,
    ) {
        accountIdStateOb.emit(
            value = intent.accountId,
        )
        intent.accountId.orNull()?.let { accountId ->
            commonBillQueryConditionUseCase
                .queryConditionStateOb
                .emit(
                    value = TallyDataSourceSpi.Companion.BillQueryConditionDto(
                        searchQueryInfo = TallyDataSourceSpi.Companion.SearchQueryConditionDto(
                            aboutAccountIdList = listOf(
                                element = accountId,
                            ),
                        )
                    )
                )
        }

    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: AccountDetailIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
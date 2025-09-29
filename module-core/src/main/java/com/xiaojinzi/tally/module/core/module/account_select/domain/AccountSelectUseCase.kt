package com.xiaojinzi.tally.module.core.module.account_select.domain

import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.orNull
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

sealed class AccountSelectIntent {

    data class ParameterInit(
        val bookId: String? = null,
    ) : AccountSelectIntent()

    data object Submit : AccountSelectIntent()

}

@ViewModelLayer
interface AccountSelectUseCase : BusinessMVIUseCase {

    /**
     * 账本 Id
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

}

@ViewModelLayer
class AccountSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountSelectUseCase {

    override val bookIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    @IntentProcess
    private suspend fun parameterInit(intent: AccountSelectIntent.ParameterInit) {
        val targetBookId = intent.bookId.orNull()
            ?: AppServices.tallyDataSourceSpi.selectedBookStateOb.firstOrNull()?.id
        bookIdStateOb.emit(
            value = targetBookId,
        )
    }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: AccountSelectIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
package com.xiaojinzi.tally.module.core.module.account_select1.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import kotlinx.coroutines.flow.Flow

sealed class AccountSelect1Intent {

    data class Submit(
        @UiContext val context: Context,
    ) : AccountSelect1Intent()

    data class ParameterInit(
        val bookId: String,
        val accountIdSet: Set<String>,
    ) : AccountSelect1Intent()

    data class ItemSelect(
        val accountId: String,
    ) : AccountSelect1Intent()

}

@ViewModelLayer
interface AccountSelect1UseCase : BusinessMVIUseCase {

    /**
     * 哪个账本下的
     */
    @StateHotObservable
    val bookIdStateOb: Flow<String?>

    /**
     * 账户选择的 Id 的 Set
     */
    @StateHotObservable
    val accountIdSelectSetStateOb: Flow<Set<String>>

}

@ViewModelLayer
class AccountSelect1UseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountSelect1UseCase {

    override val bookIdStateOb = MutableSharedStateFlow<String?>(
        initValue = null,
    )

    override val accountIdSelectSetStateOb = MutableSharedStateFlow<Set<String>>(
        initValue = emptySet(),
    )

    @IntentProcess
    private suspend fun parameterInit(intent: AccountSelect1Intent.ParameterInit) {
        bookIdStateOb.emit(
            value = intent.bookId,
        )
        accountIdSelectSetStateOb.emit(
            value = intent.accountIdSet,
        )
    }

    @IntentProcess
    private suspend fun itemSelect(intent: AccountSelect1Intent.ItemSelect) {
        accountIdSelectSetStateOb.emit(
            value = accountIdSelectSetStateOb.value.toMutableSet().apply {
                if (contains(element = intent.accountId)) {
                    remove(element = intent.accountId)
                } else {
                    add(element = intent.accountId)
                }
            }
        )
    }

    @IntentProcess
    private suspend fun submit(intent: AccountSelect1Intent.Submit) {
        intent.context.getActivity()
            ?.run {
                this.setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        putStringArrayListExtra(
                            "data",
                            ArrayList(
                                accountIdSelectSetStateOb.value
                            )
                        )
                    }
                )
                this.finish()
            }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
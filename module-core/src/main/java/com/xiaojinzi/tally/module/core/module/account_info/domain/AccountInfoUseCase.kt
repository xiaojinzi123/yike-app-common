package com.xiaojinzi.tally.module.core.module.account_info.domain

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.tally.lib.res.model.tally.MoneyYuan
import com.xiaojinzi.tally.lib.res.model.user.UserInfoCacheDto
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.support.TallyDataSourceHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Keep
data class AccountInfoUseCaseDto(
    val id: String,
    @DrawableRes
    val iconRsd: Int?,
    val name: String?,
    val userCacheInfo: UserInfoCacheDto?,
    val balanceInit: MoneyYuan,
    val balanceCurrent: MoneyYuan,
    val isExcluded: Boolean,
    val isDefault: Boolean,
)

sealed class AccountInfoIntent {

    data object Submit : AccountInfoIntent()

}

@ViewModelLayer
interface AccountInfoUseCase : BusinessMVIUseCase {

    /**
     * 所有账户
     */
    @StateHotObservable
    val allAccountStateOb: Flow<List<AccountInfoUseCaseDto>>

    /**
     * 资产
     */
    @StateHotObservable
    val assetsStateOb: Flow<MoneyYuan>

    /**
     * 负债
     */
    @StateHotObservable
    val deptStateOb: Flow<MoneyYuan>

}

@ViewModelLayer
class AccountInfoUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountInfoUseCase {

    override val allAccountStateOb: Flow<List<AccountInfoUseCaseDto>> = TallyDataSourceHelper
        .subscribeCurrentBookAccountList()
        .map { list ->
            list.map { item ->
                AccountInfoUseCaseDto(
                    id = item.id,
                    iconRsd = AppServices.iconMappingSpi[item.iconName],
                    name = item.name,
                    userCacheInfo = AppServices
                        .tallyDataSourceSpi
                        .getCacheUserInfo(
                            userId = item.userId,
                        ),
                    balanceInit = item.balanceInit.toYuan(),
                    balanceCurrent = item.balanceCurrent.toYuan(),
                    isExcluded = item.isExcluded,
                    isDefault = item.isDefault,
                )
            }.sortedByDescending { it.balanceCurrent.value }
        }
        .sharedStateIn(
            scope = scope,
        )

    override val assetsStateOb = allAccountStateOb
        .map { list ->
            list
                .filter { !it.isExcluded }
                .map { it.balanceCurrent }
                .filter { it.value > 0.0 }
                .reduceOrNull { acc, fl -> acc + fl } ?: MoneyYuan()
        }

    override val deptStateOb = allAccountStateOb
        .map { list ->
            list
                .filter { !it.isExcluded }
                .map { it.balanceCurrent }
                .filter { it.value < 0.0 }
                .reduceOrNull { acc, fl -> acc + fl } ?: MoneyYuan()
        }

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun submit(intent: AccountInfoIntent.Submit) {
        // TODO
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
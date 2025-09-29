package com.xiaojinzi.tally.module.core.module.account_icon_select.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.annotation.UiContext
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.support.ktx.getActivity
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.spi.IconMappingSpi
import kotlinx.coroutines.flow.Flow

@Keep
data class AccountIconSelectItem(
    val iconInfo: IconMappingSpi.IconMappingDto,
    val name: StringItemDto,
)

@Keep
data class AccountIconSelectGroup(
    val title: StringItemDto,
    val items: List<AccountIconSelectItem>,
)

sealed class AccountIconSelectIntent {

    data object Submit : AccountIconSelectIntent()

    data class ItemClick(
        @UiContext val context: Context,
        val item: AccountIconSelectItem,
    ) : AccountIconSelectIntent()

}

@ViewModelLayer
interface AccountIconSelectUseCase : BusinessMVIUseCase {

    @StateHotObservable
    val dataListStateOb: Flow<List<AccountIconSelectGroup>>

}

@ViewModelLayer
class AccountIconSelectUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), AccountIconSelectUseCase {

    override val dataListStateOb = MutableSharedStateFlow(
        initValue = listOf(
            AccountIconSelectGroup(
                title = "普通账号".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Coupon1,
                        name = "现金".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.BankCard1,
                        name = "银行卡".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Alipay1,
                        name = "支付宝".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Wechat1,
                        name = "微信".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "信用账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.BankCard2,
                        name = "信用卡".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.HuaBei1,
                        name = "花呗".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.JD1,
                        name = "白条".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "专用账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Bank1,
                        name = "公积金".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.FirstAidKit1,
                        name = "医保".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "充值账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Bus1,
                        name = "公交卡".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.School1,
                        name = "校园卡".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Vip1,
                        name = "会员卡".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.ShoppingCart1,
                        name = "购物卡".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "投资账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.ChartStock1,
                        name = "股票".toStringItemDto(),
                    ),
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Funds1,
                        name = "基金".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "负债账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Income1,
                        name = "借入".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "债权账户".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.Expenses1,
                        name = "借出".toStringItemDto(),
                    ),
                ),
            ),
            AccountIconSelectGroup(
                title = "其他".toStringItemDto(),
                items = listOf(
                    AccountIconSelectItem(
                        iconInfo = IconMappingSpi.IconMapping.More1,
                        name = "其他".toStringItemDto(),
                    ),
                ),
            ),
        )
    )

    @BusinessMVIUseCase.AutoLoading
    @IntentProcess
    private suspend fun itemClick(intent: AccountIconSelectIntent.ItemClick) {
        intent
            .context
            .getActivity()
            ?.let { act ->
                act.setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        this.putExtra("iconName", intent.item.iconInfo.name)
                    },
                )
                act.finish()
            }
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

}
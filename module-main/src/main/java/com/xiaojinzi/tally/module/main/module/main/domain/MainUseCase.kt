package com.xiaojinzi.tally.module.main.module.main.domain

import androidx.annotation.DrawableRes
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

enum class MainTabDto(
    @DrawableRes
    val iconRsd: Int,
    val nameStringItem: StringItemDto,
) {

    Bill(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_book1,
        nameStringItem = "账单".toStringItemDto(),
    ),

    Assets(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_account1,
        nameStringItem = "资产".toStringItemDto(),
    ),

    AutoRecord(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_robot1,
        nameStringItem = "自动记账".toStringItemDto(),
    ),

    Calendar(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_calendar1,
        nameStringItem = "日历".toStringItemDto(),
    ),

    Statistics(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_analysis1,
        nameStringItem = "统计".toStringItemDto(),
    ),

    Tools(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_category_manage1,
        nameStringItem = "工具".toStringItemDto(),
    ),

}

sealed class MainIntent {

    data class TabChanged(
        val value: MainTabDto,
    ) : MainIntent()

}

@ViewModelLayer
interface MainUseCase : BusinessMVIUseCase {

    /**
     * tab list
     */
    @StateHotObservable
    val tabListStateOb: Flow<List<MainTabDto>>

    /**
     * 选中的 tab
     */
    @StateHotObservable
    val tabSelectedStateOb: Flow<MainTabDto>

}

@ViewModelLayer
class MainUseCaseImpl(
    private val commonUseCase: CommonUseCase = CommonUseCaseImpl(),
) : BusinessMVIUseCaseImpl(
    commonUseCase = commonUseCase,
), MainUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val tabListStateOb = AppServices
        .appConfigSpi
        .isShowAssetsTabStateOb
        .flatMapLatest { isShowAssetsTab ->
            flowOf(
                buildList {
                    add(MainTabDto.Bill)
                    if (isShowAssetsTab) {
                        add(MainTabDto.Assets)
                    }
                    add(MainTabDto.AutoRecord)
                    add(MainTabDto.Statistics)
                    add(MainTabDto.Tools)
                }
            )
        }.sharedStateIn(
            scope = scope,
        )

    override val tabSelectedStateOb = MutableStateFlow(
        value = MainTabDto.Bill,
    )

    @IntentProcess
    private suspend fun tabChanged(
        intent: MainIntent.TabChanged,
    ) {
        tabSelectedStateOb.emit(
            value = intent.value,
        )
    }

    override fun destroy() {
        super.destroy()
        commonUseCase.destroy()
    }

    init {
        AppServices.appWidgetSpi?.tryUpdateWidget()
    }

}

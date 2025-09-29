package com.xiaojinzi.tally.module.main.module.main.domain

import androidx.annotation.DrawableRes
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.anno.IntentProcess
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCase
import com.xiaojinzi.reactive.template.domain.BusinessMVIUseCaseImpl
import com.xiaojinzi.reactive.template.domain.CommonUseCase
import com.xiaojinzi.reactive.template.domain.CommonUseCaseImpl
import com.xiaojinzi.support.activity_stack.ActivityStack
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.annotation.ViewModelLayer
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.ktx.DAY_MS
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.launchIgnoreError
import com.xiaojinzi.support.ktx.sharedStateIn
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    Calendar(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_calendar1,
        nameStringItem = "日历".toStringItemDto(),
    ),

    Statistics(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_analysis1,
        nameStringItem = "统计".toStringItemDto(),
    ),

    My(
        iconRsd = com.xiaojinzi.tally.lib.res.R.drawable.res_people1,
        nameStringItem = "我的".toStringItemDto(),
    ),

}

sealed class MainIntent {

    data object Hello : MainIntent()

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
                    add(MainTabDto.Calendar)
                    if (isShowAssetsTab) {
                        add(MainTabDto.Assets)
                    }
                    add(MainTabDto.Statistics)
                    add(MainTabDto.My)
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
        // 尝试更新一下小部件
        AppServices
            .appWidgetSpi?.apply {
                this.tryUpdateWidget()
            }
        // 如果没有账本选择, 就选择系统创建的那个
        scope.launchIgnoreError {
            val userInfo =
                AppServices.userSpi.userInfoStateOb.firstOrNull() ?: return@launchIgnoreError
            val tallyDataSourceSpi = AppServices
                .tallyDataSourceSpi
            val selectBook = tallyDataSourceSpi
                .selectedBookStateOb
                .firstOrNull()
            if (selectBook == null) {
                tallyDataSourceSpi
                    .allBookStateOb
                    .first()
                    .find { it.userId == userInfo.id && it.isSystem }
                    ?.id?.let {
                        tallyDataSourceSpi
                            .switchBook(
                                bookId = it,
                            )
                    }
            }
        }
        // 用户信息有变更就刷新, 第一次进来也更新
        AppServices
            .userSpi
            .userInfoStateOb
            .filterNotNull()
            .onEach { _ ->
                // 更新会员信息
                AppServices
                    .userSpi
                    .updateVipInfoAction()
                    .awaitIgnoreException()
            }
            .launchIn(scope = scope)
        /**
         * 尝试做一些事
         * -. 更新 Token 信息
         * -. 更新会员信息
         * -. 更新账本信息(开启同步之后， 如果账本不存在, 这会被删除)
         * -. 开启同步
         */
        scope.launchIgnoreError {
            // 续费提醒
            val isTipRechargeVip = AppServices
                .appConfigSpi
                .isTipBeforeVipExpireStateOb
                .first()
            if (isTipRechargeVip) {
                AppServices
                    .userSpi
                    .vipInfoStateOb
                    .firstOrNull()
                    ?.let { vipInfo ->
                        val currentTime = System.currentTimeMillis()
                        if (vipInfo.expiredTime > currentTime && (vipInfo.expiredTime - currentTime) < 7 * DAY_MS) {
                            ActivityStack
                                .topAlive
                                ?.let { topAct ->
                                    AppRouterUserApi::class
                                        .routeApi()
                                        .toVipExpireRemindView(
                                            context = topAct,
                                        )
                                }
                        }
                    }
            }
            // 检查更新
            ActivityStack
                .topAlive
                ?.let { topAct ->
                    AppRouterMainApi::class
                        .routeApi()
                        .toAppUpdateView(
                            context = topAct,
                        )
                }
            // 刚启动, 做的事情可能有点多, 迟点开启同步
            delay(2000)
            // 开启同步
            AppServices
                .tallyDataSyncSpi
                .apply {
                    this?.setSyncSwitch(
                        enable = true,
                    )
                    // 这里不再触发,
                    // this.trySync()
                }
        }
    }

}
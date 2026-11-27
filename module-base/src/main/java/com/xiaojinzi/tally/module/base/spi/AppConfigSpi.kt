package com.xiaojinzi.tally.module.base.spi

import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.module.common.base.spi.spPersistence
import com.xiaojinzi.support.annotation.StateHotObservable
import com.xiaojinzi.support.ktx.AppScope
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import kotlinx.coroutines.flow.Flow

interface AppConfigSpi {

    /**
     *  是否 引导视图1 显示过了
     */
    @StateHotObservable
    val isShowedGuide1StateOb: Flow<Boolean>

    /**
     * 是否显示小时和分钟
     */
    @StateHotObservable
    val isShowHourAndMinuteWhenBillCrudStateOb: Flow<Boolean>

    /**
     * 是否支持记账的时候数值是 0
     */
    @StateHotObservable
    val isAllowZeroAmountWhenBillCrudStateOb: Flow<Boolean>

    /**
     * 是否显示资产 Tab
     */
    @StateHotObservable
    val isShowAssetsTabStateOb: Flow<Boolean>

    /**
     * 是否振动
     */
    @StateHotObservable
    val isVibrateStateOb: Flow<Boolean>

    /**
     * 是否资产统计数据可见
     */
    @StateHotObservable
    val isAssetsVisibleStateOb: Flow<Boolean>

    /**
     * 是否首页的账单统计数据可见
     */
    @StateHotObservable
    val isHomeBillStatVisibleStateOb: Flow<Boolean>

    /**
     * 切换状态
     */
    fun switchShowedGuide1(b: Boolean)

    /**
     * 切换状态
     */
    fun switchShowHourAndMinuteWhenBillCrud(b: Boolean)

    /**
     * 切换状态
     */
    fun switchAllowZeroAmountWhenBillCrudStateOb(b: Boolean)

    /**
     * 切换状态
     */
    fun switchShowAssetsStateOb(b: Boolean)

    /**
     * 切换振动状态
     */
    fun switchVibrate(isVibrate: Boolean)

    /**
     * 切换状态
     */
    fun switchAssetsVisible(b: Boolean)

    /**
     * 切换状态
     */
    fun switchHomeBillStatVisible(b: Boolean)

}

@ServiceAnno(AppConfigSpi::class)
class AppConfigSpiImpl : AppConfigSpi {

    override val isShowedGuide1StateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isShowedGuide1Config",
            def = false,
        )

    override val isShowHourAndMinuteWhenBillCrudStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isShowHourAndMinuteWhenBillCrudConfig",
            def = false,
        )

    override val isAllowZeroAmountWhenBillCrudStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isAllowZeroAmountWhenBillCrudConfig",
            def = false,
        )

    override val isShowAssetsTabStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isShowAssetsTabConfig",
            def = true,
        )

    override val isVibrateStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isVibrateConfig",
            def = true,
        )

    override val isAssetsVisibleStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isAssetsVisibleConfig",
            def = true,
        )

    override val isHomeBillStatVisibleStateOb = MutableSharedStateFlow<Boolean>()
        .spPersistence(
            scope = AppScope,
            key = "isHomeBillStatVisibleConfig",
            def = true,
        )

    override fun switchShowedGuide1(b: Boolean) {
        isShowedGuide1StateOb.value = b
    }

    override fun switchShowHourAndMinuteWhenBillCrud(b: Boolean) {
        isShowHourAndMinuteWhenBillCrudStateOb.value = b
    }

    override fun switchAllowZeroAmountWhenBillCrudStateOb(b: Boolean) {
        isAllowZeroAmountWhenBillCrudStateOb.value = b
    }

    override fun switchShowAssetsStateOb(b: Boolean) {
        isShowAssetsTabStateOb.value = b
    }

    override fun switchVibrate(isVibrate: Boolean) {
        isVibrateStateOb.value = isVibrate
    }

    override fun switchAssetsVisible(b: Boolean) {
        isAssetsVisibleStateOb.value = b
    }

    override fun switchHomeBillStatVisible(b: Boolean) {
        isHomeBillStatVisibleStateOb.value = b
    }

}
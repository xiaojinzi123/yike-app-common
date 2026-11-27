package com.xiaojinzi.tally.module.base.spi

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AppWidgetSpi {

    enum class WidgetType { DayStatistics, MonthStatistics, MonthBudget }

    enum class PinRequestResult { Unsupported, Accepted, Rejected, Failed }

    /** Accepted 仅表示桌面接受请求，是否完成添加由用户在系统提示中确认。 */
    suspend fun requestPinWidget(context: Context, type: WidgetType): PinRequestResult

    /** 使用当前账本和桌面同源的 Glance 布局，预览内不执行跳转。 */
    @Composable
    fun Preview(type: WidgetType, modifier: Modifier)

    /**
     * 启动 App 内小部件的更新服务
     * 如果小部件有被使用
     */
    suspend fun startWidgetServiceIfUsed()

    /**
     * 尝试更新一下小部件
     */
    fun tryUpdateWidget()

}
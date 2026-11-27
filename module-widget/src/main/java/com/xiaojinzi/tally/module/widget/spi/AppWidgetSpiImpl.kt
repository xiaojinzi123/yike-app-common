package com.xiaojinzi.tally.module.widget.spi

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.xiaojinzi.tally.module.widget.AppWidgetDataSource
import com.xiaojinzi.tally.module.widget.WidgetPreview
import com.xiaojinzi.tally.module.widget.composeWidgetPreview
import com.xiaojinzi.tally.module.widget.kind
import kotlinx.coroutines.CancellationException
import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi.PinRequestResult
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi.WidgetType
import com.xiaojinzi.tally.module.widget.AppDayStatisticsWidgetReceiver
import com.xiaojinzi.tally.module.widget.AppMonthStatisticsWidgetReceiver
import com.xiaojinzi.tally.module.widget.QianjiBudgetWidgetReceiver
import com.xiaojinzi.tally.module.widget.WidgetRefreshScheduler

@ServiceAnno(AppWidgetSpi::class)
class AppWidgetSpiImpl : AppWidgetSpi {
    @OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
    override suspend fun requestPinWidget(context: Context, type: WidgetType): PinRequestResult {
        return try {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) {
                return PinRequestResult.Unsupported
            }
            val receiver = when (type) {
                WidgetType.DayStatistics -> AppDayStatisticsWidgetReceiver::class.java
                WidgetType.MonthStatistics -> AppMonthStatisticsWidgetReceiver::class.java
                WidgetType.MonthBudget -> QianjiBudgetWidgetReceiver::class.java
            }
            val snapshot = AppWidgetDataSource.readSnapshot()
            val preview = composeWidgetPreview(context, type.kind(), snapshot, DpSize(300.dp, 160.dp))
            val extras = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
            if (manager.requestPinAppWidget(ComponentName(context, receiver), extras, null)) {
                PinRequestResult.Accepted
            } else {
                PinRequestResult.Rejected
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            PinRequestResult.Failed
        }
    }

    @Composable
    override fun Preview(type: WidgetType, modifier: Modifier) {
        WidgetPreview(type, modifier)
    }

    // Preserve the SPI contract; the historic name no longer means starting a service.
    override suspend fun startWidgetServiceIfUsed() {
        WidgetRefreshScheduler.requestRefresh(app)
    }

    override fun tryUpdateWidget() {
        WidgetRefreshScheduler.requestRefresh(app)
    }
}

package com.xiaojinzi.tally.module.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent

class AppMonthStatisticsWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_loading) {
    override val sizeMode = WidgetDesign.sizes

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) = showWidgetCompositionError(context, appWidgetId, throwable)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetRefreshScheduler.ensureScheduled(context)
        val initial = AppWidgetDataSource.readSnapshot()
        val snapshots = AppWidgetDataSource.observe()
        provideContent {
            val snapshot by snapshots.collectAsState(initial = initial)
            WidgetContent(snapshot, WidgetKind.Month)
        }
    }
}

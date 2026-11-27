package com.xiaojinzi.tally.module.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CancellationException
import java.time.YearMonth

/** Keep composition recovery independent of Glance composition and the local data source. */
internal fun showWidgetCompositionError(context: Context, appWidgetId: Int, throwable: Throwable) {
    if (throwable is CancellationException) throw throwable
    Log.e("WidgetRecovery", "Widget composition failed", throwable)
    val month = YearMonth.now().toString()
    val intent = Intent(context, WidgetActionActivity::class.java).apply {
        // Match WidgetActionActivity.action without requiring a successfully loaded snapshot.
        data = Uri.Builder().scheme("qianji-widget").authority("local")
            .appendPath(WidgetActionActivity.OPEN_APP)
            .appendQueryParameter("book", "")
            .appendQueryParameter("month", month)
            .build()
        putExtra("widgetDestination", WidgetActionActivity.OPEN_APP)
        putExtra("bookId", null as String?)
        putExtra("month", month)
    }
    val openApp = PendingIntent.getActivity(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val views = RemoteViews(context.packageName, R.layout.widget_loading).apply {
        setOnClickPendingIntent(R.id.widget_error_root, openApp)
    }
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
}

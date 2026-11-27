package com.xiaojinzi.tally.module.widget

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi.WidgetType
import kotlinx.coroutines.CancellationException

internal fun WidgetType.kind(): WidgetKind = when (this) {
    WidgetType.DayStatistics -> WidgetKind.Day
    WidgetType.MonthStatistics -> WidgetKind.Month
    WidgetType.MonthBudget -> WidgetKind.Budget
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
internal suspend fun composeWidgetPreview(
    context: Context,
    kind: WidgetKind,
    snapshot: WidgetSnapshot,
    size: DpSize,
    composer: GlanceRemoteViews = GlanceRemoteViews(),
): RemoteViews = composer.compose(context = context, size = size) {
    WidgetContent(snapshot, kind, preview = true)
}.remoteViews

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun WidgetPreview(type: WidgetType, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val snapshots = remember { AppWidgetDataSource.observe() }
    val snapshot by snapshots.collectAsState(initial = WidgetSnapshot())
    val composer = remember { GlanceRemoteViews() }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val size = DpSize(maxWidth, 160.dp)
        var views by remember(snapshot, type, size, configuration) { mutableStateOf<RemoteViews?>(null) }
        var failed by remember(snapshot, type, size, configuration) { mutableStateOf(false) }
        LaunchedEffect(snapshot, type, size, configuration) {
            // Clear the old preview before rendering a new privacy/book revision.
            views = null
            failed = false
            try {
                views = composeWidgetPreview(context, type.kind(), snapshot, size, composer)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.w("WidgetPreview", "Unable to compose widget preview", failure)
                failed = true
            }
        }
        val current = views
        if (current == null) {
            Box(Modifier.fillMaxWidth().height(size.height), contentAlignment = Alignment.Center) {
                Text(
                    if (failed) "预览暂不可用，请重新选择样式" else "正在读取小部件预览…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(size.height),
                factory = { PreviewHost(it.applicationContext) },
                update = { host ->
                    if (host.tag !== current) {
                        host.removeAllViews()
                        try {
                            // Activity inflaters replace platform widgets with AppCompat subclasses.
                            host.addView(current.apply(host.context, host), FrameLayout.LayoutParams(-1, -1))
                            host.tag = current
                        } catch (failure: Exception) {
                            android.util.Log.w("WidgetPreview", "Unable to inflate widget preview", failure)
                            failed = true
                            views = null
                        }
                    }
                },
            )
        }
    }
}

/** A preview must not launch the actions embedded in a widget. */
private class PreviewHost(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = true
}

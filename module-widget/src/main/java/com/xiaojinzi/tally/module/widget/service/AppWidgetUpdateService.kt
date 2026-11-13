package com.xiaojinzi.tally.module.widget.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.xiaojinzi.support.ktx.LogSupport
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.awaitIgnoreException
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.tally.lib.res.model.tally.TallyTable
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.widget.AppDayStatisticsWidget
import com.xiaojinzi.tally.module.widget.AppMonthStatisticsWidget
import com.xiaojinzi.tally.module.widget.QianjiBudgetWidget
import com.xiaojinzi.tally.module.widget.ktx.updateAllWidgetAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


/**
 * App 中的小部件的更新 Service
 */
class AppWidgetUpdateService : Service() {

    companion object {

        const val TAG = "AppWidgetUpdateService"

        fun startWidgetServiceIfUsedAction(): SuspendAction0 {
            return suspendAction0 {
                val manager = GlanceAppWidgetManager(app)
                val widgetClassList = listOf(
                    AppDayStatisticsWidget::class.java,
                    AppMonthStatisticsWidget::class.java,
                    QianjiBudgetWidget::class.java,
                )
                val allUsedWidgetIds = widgetClassList.flatMap {
                    manager.getGlanceIds(it)
                }
                LogSupport.d(
                    tag = TAG,
                    content = "allUsedWidgetIds.size = ${allUsedWidgetIds.size}",
                )
                if (allUsedWidgetIds.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        app.startForegroundService(
                            Intent(
                                app,
                                AppWidgetUpdateService::class.java
                            )
                        )
                    } else {
                        app.startService(
                            Intent(
                                app,
                                AppWidgetUpdateService::class.java
                            )
                        )
                    }
                }
            }
        }

    }

    private val scope = MainScope()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // 8.0 以上需要特殊处理
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("app.tally.widget", "yikeForegroundService")
        } else {
            ""
        }

        val notification = NotificationCompat
            .Builder(this, channelId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(
            1, notification,
        )

        LogSupport.d(
            tag = TAG,
            content = "onCreate",
        )

        // 如果账单发生变化, 就更新 widget
        AppServices
            .tallyDataSourceInitSpi
            .isInitStateOb
            .flatMapLatest { isDatasourceInit ->
                LogSupport.d(
                    tag = TAG,
                    content = "isDatasourceInit = $isDatasourceInit",
                )
                if (isDatasourceInit) {
                    AppServices
                        .tallyDataSourceSpi
                        .subscribeDataBaseTableChangedOb(
                            TallyTable.Bill, emitOneWhileSubscribe = true,
                        )
                } else {
                    flowOf(value = null)
                }
            }
            .filterNotNull()
            .flowOn(context = Dispatchers.IO)
            .onEach {
                LogSupport.d(
                    tag = TAG,
                    content = "更新 widget 啦",
                )
                updateAllWidgetAction()
                    .awaitIgnoreException()
            }
            .flowOn(context = Dispatchers.Main)
            .launchIn(scope = scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        LogSupport.d(
            tag = TAG,
            content = "onDestroy",
        )
        scope.cancel()
    }

}
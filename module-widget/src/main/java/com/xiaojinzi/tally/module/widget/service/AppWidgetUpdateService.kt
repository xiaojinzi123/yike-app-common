package com.xiaojinzi.tally.module.widget.service

import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.tally.module.widget.WidgetRefreshScheduler

/** Compatibility facade only. No Android Service is registered or started. */
@Deprecated("Widget updates now use bounded WorkManager requests and Room flows")
object AppWidgetUpdateService {
    fun startWidgetServiceIfUsedAction(): SuspendAction0 = suspendAction0 {
        WidgetRefreshScheduler.requestRefresh(app)
    }
}

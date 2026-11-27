package com.xiaojinzi.tally.module.widget.ktx

import androidx.annotation.RestrictTo
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.tally.module.widget.WidgetRefreshScheduler

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun updateAllWidgetAction(): SuspendAction0 = suspendAction0 {
    WidgetRefreshScheduler.requestRefresh(app)
}

package com.xiaojinzi.tally.module.widget.ktx

import android.content.res.Configuration
import androidx.annotation.RestrictTo
import androidx.glance.appwidget.updateAll
import com.xiaojinzi.support.ktx.SuspendAction0
import com.xiaojinzi.support.ktx.app
import com.xiaojinzi.support.ktx.suspendAction0
import com.xiaojinzi.tally.module.widget.AppDayStatisticsWidget
import com.xiaojinzi.tally.module.widget.AppMonthStatisticsWidget
import com.xiaojinzi.tally.module.widget.QianjiBudgetWidget

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal val isSystemInDarkTheme: Boolean
    get() {
        val uiMode = app.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun updateAllWidgetAction(): SuspendAction0 {
    return suspendAction0 {
        AppDayStatisticsWidget().updateAll(
            context = app,
        )
        AppMonthStatisticsWidget().updateAll(
            context = app,
        )
        QianjiBudgetWidget().updateAll(
            context = app,
        )
    }
}
package com.xiaojinzi.tally.module.widget

import androidx.glance.appwidget.GlanceAppWidget

class QianjiBudgetWidgetReceiver : BaseAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = QianjiBudgetWidget()

}

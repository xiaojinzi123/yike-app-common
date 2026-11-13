package com.xiaojinzi.tally.module.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import com.xiaojinzi.support.ktx.format2f
import com.xiaojinzi.support.ktx.nothing
import com.xiaojinzi.tally.module.widget.ktx.isSystemInDarkTheme
import kotlin.math.absoluteValue

/**
 * 钱迹式预算小部件：本月支出 / 预算 / 剩余（超支红色）。
 *
 * 数据全部来自本机 Room：月支出走 TallyStatisticsHelper，预算走钱迹 budget 表。
 */
class QianjiBudgetWidget : GlanceAppWidget() {

    companion object {
        const val TAG = "QianjiBudgetWidget"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val monthSpending by AppWidgetDataSource.currentMonthSpendingStateOb.collectAsState(
                initial = null
            )
            val monthBudget by AppWidgetDataSource.currentMonthBudgetStateOb.collectAsState(
                initial = null
            )
            val spendingFen = monthSpending?.value?.absoluteValue ?: 0L
            val budgetFen = monthBudget?.value ?: 0L
            val remainFen = budgetFen - spendingFen
            val over = budgetFen > 0L && remainFen < 0L

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(
                            imageProvider = ImageProvider(
                                resId = if (isSystemInDarkTheme) {
                                    R.drawable.widget_bg_dark1
                                } else {
                                    R.drawable.widget_bg_light1
                                },
                            ),
                        )
                        .clickable {
                            // 打开 App 启动页
                            context.startActivity(
                                Intent().apply {
                                    this.`package` = context.packageName
                                    this.action = "app_launch_20240723"
                                    this.addCategory(Intent.CATEGORY_DEFAULT)
                                    this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .nothing(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        modifier = GlanceModifier.nothing(),
                        text = "本月支出",
                        style = TextDefaults.defaultTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.outline,
                        ),
                    )
                    Text(
                        modifier = GlanceModifier.nothing(),
                        text = "¥" + (spendingFen / 100f).format2f(),
                        style = TextDefaults.defaultTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    )
                    Spacer(
                        modifier = GlanceModifier
                            .height(height = 8.dp)
                            .nothing()
                    )
                    Text(
                        modifier = GlanceModifier.nothing(),
                        text = if (budgetFen > 0L) {
                            "预算 ¥" + (budgetFen / 100f).format2f()
                        } else {
                            "未设预算"
                        },
                        style = TextDefaults.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.outline,
                        ),
                    )
                    Text(
                        modifier = GlanceModifier.nothing(),
                        text = when {
                            budgetFen <= 0L -> "去设置"
                            over -> "超支 ¥" + (-remainFen / 100f).format2f()
                            else -> "剩余 ¥" + (remainFen / 100f).format2f()
                        },
                        style = TextDefaults.defaultTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (over) {
                                GlanceTheme.colors.error
                            } else {
                                GlanceTheme.colors.onSurface
                            },
                        ),
                    )
                }
            }
        }
    }

}

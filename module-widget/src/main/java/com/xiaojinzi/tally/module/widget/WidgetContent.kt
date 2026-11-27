package com.xiaojinzi.tally.module.widget

import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** RemoteViews use the same solid surfaces and 20dp corners as the in-app NexioUI cards. */
internal object WidgetDesign {
    val sizes = SizeMode.Responsive(
        setOf(DpSize(110.dp, 56.dp), DpSize(140.dp, 160.dp), DpSize(300.dp, 160.dp), DpSize(300.dp, 250.dp)),
    )
    val text = ColorProvider(R.color.widget_text)
    val secondary = ColorProvider(R.color.widget_secondary)
    val accent = ColorProvider(R.color.widget_accent)
    val onAccent = ColorProvider(R.color.widget_on_accent)
    val progress = ColorProvider(R.color.widget_progress)
    val track = ColorProvider(R.color.widget_track)
    val error = ColorProvider(R.color.widget_error)
}

internal enum class WidgetKind { Budget, Day, Month }

@Composable
internal fun WidgetContent(snapshot: WidgetSnapshot, kind: WidgetKind, preview: Boolean = false) {
    val context = LocalContext.current
    val size = LocalSize.current
    val layout = WidgetLayout.select(size.width.value, size.height.value, context.resources.configuration.fontScale)
    val short = layout.compact
    val wide = layout.wide
    val tall = layout.tall
    val largeText = layout.largeText
    val contentWidth = size.width.value - if (short) 16f else 24f
    val budgetAction = WidgetActionActivity.action(context, WidgetActionActivity.BUDGET, snapshot)
    val addAction = WidgetActionActivity.action(context, WidgetActionActivity.ADD_BILL, snapshot)
    val appAction = WidgetActionActivity.action(context, WidgetActionActivity.OPEN_APP, snapshot)
    val mainAction = if (kind == WidgetKind.Budget) budgetAction else appAction
    val title = when (kind) {
        WidgetKind.Budget -> "${snapshot.monthLabel}预算"
        WidgetKind.Day -> "今日收支"
        WidgetKind.Month -> "${snapshot.monthLabel}收支"
    }
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg1))
            .then(if (preview) GlanceModifier else GlanceModifier.clickable(if (snapshot.status == WidgetStatus.Ready) mainAction else appAction))
            .padding(if (short) 8.dp else 12.dp),
    ) {
        if (snapshot.status != WidgetStatus.Ready) {
            WidgetText(
                title, if (short) 11 else 17, bold = true,
                modifier = GlanceModifier.fillMaxWidth(),
                availableWidth = contentWidth, maxHeight = if (short) 16f else 24f,
            )
            Spacer(GlanceModifier.defaultWeight())
            WidgetText(
                snapshot.statusText, if (short) 11 else 13,
                modifier = GlanceModifier.fillMaxWidth(), color = WidgetDesign.secondary,
                maxLines = if (short || largeText) 1 else 2,
                maxHeight = if (short) 16f else 20f,
            )
            if (!short) {
                Spacer(GlanceModifier.defaultWeight())
                WidgetButton(
                    when (snapshot.status) {
                        WidgetStatus.NoBook -> "选择账本"
                        WidgetStatus.Error -> "打开重试"
                        else -> "打开应用"
                    },
                    appAction,
                    GlanceModifier.fillMaxWidth(),
                    availableWidth = contentWidth,
                    preview = preview,
                )
            }
        } else if (short) {
            // Small legacy IDs keep one clear action; a second button would crowd out the amount.
            WidgetText(
                title + if (kind == WidgetKind.Budget) " · ${snapshot.budgetLabel}" else " · 支出",
                11, modifier = GlanceModifier.fillMaxWidth(), color = WidgetDesign.secondary,
                availableWidth = contentWidth, maxHeight = 16f,
            )
            WidgetAmountText(
                if (kind == WidgetKind.Budget) snapshot.budgetAmount(maxCharacters = 9)
                else snapshot.amount(spending(snapshot, kind), maxCharacters = 9),
                17, availableWidth = contentWidth, maxHeight = 23f,
                color = if (kind == WidgetKind.Budget && snapshot.overBudget) WidgetDesign.error else WidgetDesign.text,
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetText(
                    title, 17, modifier = GlanceModifier.defaultWeight(), bold = true,
                    availableWidth = if (wide) (contentWidth - 8f) / 2f else contentWidth,
                    maxHeight = 24f,
                )
                if (wide) {
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetText(
                        snapshot.bookName, 13, modifier = GlanceModifier.defaultWeight(),
                        color = WidgetDesign.secondary, textAlign = TextAlign.End, maxHeight = 20f,
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                if (kind == WidgetKind.Budget) {
                    BudgetBody(snapshot, wide, tall, largeText)
                } else {
                    MainAmount(
                        if (kind == WidgetKind.Day) "今日支出" else "本月支出", snapshot.amount(spending(snapshot, kind)),
                        wide = wide, tall = tall, largeText = largeText,
                    )
                }
                if (wide && (kind != WidgetKind.Budget || tall) && (!largeText || tall)) {
                    Spacer(GlanceModifier.defaultWeight())
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Metric("收入", snapshot.amount(income(snapshot, kind), maxCharacters = 9), GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(12.dp))
                        Metric(
                            if (kind == WidgetKind.Budget) "支出" else "结余",
                            snapshot.amount(if (kind == WidgetKind.Budget) snapshot.monthSpending else balance(snapshot, kind), maxCharacters = 9),
                            GlanceModifier.defaultWeight(),
                        )
                    }
                }
                if (tall && !largeText) {
                    Spacer(GlanceModifier.height(12.dp))
                    WidgetText(
                        if (kind == WidgetKind.Day) snapshot.day else snapshot.month,
                        13, color = WidgetDesign.secondary, maxHeight = 20f,
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val buttonWidth = if (wide) (contentWidth - 8f) / 2f else contentWidth
                if (wide) {
                    WidgetButton(
                        if (kind == WidgetKind.Budget) "预算管理" else "查看账单",
                        mainAction, GlanceModifier.defaultWeight(), availableWidth = buttonWidth, primary = false, preview = preview,
                    )
                    Spacer(GlanceModifier.width(8.dp))
                }
                WidgetButton("记一笔", addAction, GlanceModifier.defaultWeight(), availableWidth = buttonWidth, preview = preview)
            }
        }
    }
}

@Composable
private fun MainAmount(
    label: String,
    value: String,
    wide: Boolean,
    tall: Boolean,
    largeText: Boolean,
    color: ColorProvider = WidgetDesign.text,
) {
    val contentWidth = LocalSize.current.width.value - 24f
    if (wide && !tall && !largeText) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetText(label, 13, modifier = GlanceModifier.width(64.dp), color = WidgetDesign.secondary, maxHeight = 20f)
            Spacer(GlanceModifier.width(8.dp))
            WidgetAmountText(
                value, 24, availableWidth = contentWidth - 72f,
                modifier = GlanceModifier.defaultWeight(), color = color,
                textAlign = TextAlign.End, maxHeight = 32f,
            )
        }
    } else {
        WidgetText(label, 13, color = WidgetDesign.secondary, maxHeight = if (tall) 20f else 18f)
        WidgetAmountText(
            value, if (tall) 28 else 24, availableWidth = contentWidth,
            color = color, maxHeight = if (tall) 38f else 30f,
        )
    }
}

@Composable
private fun BudgetBody(snapshot: WidgetSnapshot, wide: Boolean, tall: Boolean, largeText: Boolean) {
    val noBudget = snapshot.amountsVisible && (snapshot.budget ?: 0L) <= 0L
    MainAmount(
        snapshot.budgetLabel,
        snapshot.budgetAmount(),
        wide = wide, tall = tall, largeText = largeText,
        color = if (snapshot.overBudget) WidgetDesign.error else WidgetDesign.text,
    )
    if (!snapshot.amountsVisible) {
        if ((wide && !largeText) || tall) {
            WidgetText(
                "金额与进度已隐藏", 13, modifier = GlanceModifier.fillMaxWidth(),
                color = WidgetDesign.secondary, maxHeight = 20f,
            )
        }
    } else if (!noBudget) {
        if (tall) WidgetText("总预算 ${snapshot.amount(snapshot.budget)}", 13, color = WidgetDesign.secondary, maxHeight = 20f)
        if (wide || tall || !largeText) {
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = snapshot.progress ?: 0f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = if (snapshot.overBudget) WidgetDesign.error else WidgetDesign.progress,
                backgroundColor = WidgetDesign.track,
            )
        }
    }
}

private fun spending(snapshot: WidgetSnapshot, kind: WidgetKind) =
    if (kind == WidgetKind.Day) snapshot.daySpending else snapshot.monthSpending

private fun income(snapshot: WidgetSnapshot, kind: WidgetKind) =
    if (kind == WidgetKind.Day) snapshot.dayIncome else snapshot.monthIncome

private fun balance(snapshot: WidgetSnapshot, kind: WidgetKind): Long? {
    val income = income(snapshot, kind) ?: return null
    val spending = spending(snapshot, kind) ?: return null
    return income - spending
}

@Composable
private fun Metric(label: String, value: String, modifier: GlanceModifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        WidgetText(label, 13, modifier = GlanceModifier.width(32.dp), color = WidgetDesign.secondary, availableWidth = 32f, maxHeight = 20f)
        Spacer(GlanceModifier.width(6.dp))
        WidgetAmountText(
            value, 14,
            availableWidth = (LocalSize.current.width.value - 36f) / 2f - 38f,
            modifier = GlanceModifier.defaultWeight(), textAlign = TextAlign.End, maxHeight = 22f,
        )
    }
}

@Composable
private fun WidgetAmountText(
    value: String,
    preferredSize: Int,
    availableWidth: Float,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
    color: ColorProvider = WidgetDesign.text,
    textAlign: TextAlign = TextAlign.Start,
    maxHeight: Float = 32f,
) {
    WidgetText(
        value, preferredSize, modifier = modifier, color = color, bold = true,
        availableWidth = availableWidth, maxHeight = maxHeight, textAlign = textAlign,
    )
}

@Composable
private fun WidgetButton(
    label: String,
    action: Action,
    modifier: GlanceModifier,
    availableWidth: Float,
    primary: Boolean = true,
    preview: Boolean = false,
) {
    Box(
        modifier = modifier.height(36.dp)
            .background(ImageProvider(if (primary) R.drawable.widget_action_primary else R.drawable.widget_action_secondary))
            .then(if (preview) GlanceModifier else GlanceModifier.clickable(action)),
        contentAlignment = Alignment.Center,
    ) {
        WidgetText(
            label, 14, bold = true, color = if (primary) WidgetDesign.onAccent else WidgetDesign.text,
            availableWidth = availableWidth - 16f, maxHeight = 28f,
        )
    }
}

@Composable
private fun WidgetText(
    text: String,
    size: Int,
    modifier: GlanceModifier = GlanceModifier,
    color: ColorProvider = WidgetDesign.text,
    bold: Boolean = false,
    maxLines: Int = 1,
    availableWidth: Float? = null,
    maxHeight: Float? = null,
    textAlign: TextAlign = TextAlign.Start,
) {
    val metrics = LocalContext.current.resources.displayMetrics
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT
    }
    // Measure real glyphs, including compact amount units, at the launcher's current font scale.
    val fittedSize = fitWidgetTextSize(size) { candidate ->
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, candidate.toFloat(), metrics)
        val tooWide = availableWidth != null && paint.measureText(text) > (availableWidth - 4f).coerceAtLeast(1f) * metrics.density
        val tooHigh = maxHeight != null && paint.fontMetrics.let { it.bottom - it.top } > maxHeight * metrics.density
        !tooWide && !tooHigh
    }
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        style = TextStyle(
            color = color, fontSize = fittedSize.sp, textAlign = textAlign,
            fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
        ),
    )
}

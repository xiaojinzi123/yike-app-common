package com.xiaojinzi.tally.module.base.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.haooz.nexioui.button.FlatTextButton
import com.haooz.nexioui.switchpreference.SwitchPreference
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi.PinRequestResult
import com.xiaojinzi.tally.module.base.spi.AppWidgetSpi.WidgetType
import com.xiaojinzi.tally.module.base.support.AppServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PortWidgetPicker(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val widgets = AppServices.appWidgetSpi
    var selected by rememberSaveable { mutableStateOf(WidgetType.MonthBudget) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var requesting by remember { mutableStateOf(false) }
    val amountsVisible by AppServices.appConfigSpi.isHomeBillStatVisibleStateOb.collectAsState(initial = false)
    val options = remember {
        listOf(
            WidgetType.MonthBudget to "月度预算",
            WidgetType.DayStatistics to "今日收支",
            WidgetType.MonthStatistics to "月度收支",
        )
    }
    val label = options.first { it.first == selected }.second
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        PortTheme {
            PortPage(
                title = "桌面小部件",
                subtitle = "把常用的收支与预算留在桌面",
                onBack = onDismiss,
            ) {
                PortSectionTitle("选择样式")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (type, title) ->
                        FlatTextButton(
                            text = title,
                            modifier = Modifier.weight(1f),
                            enabled = !requesting,
                            minWidth = 0.dp,
                            minHeight = PortDesign.TouchTarget,
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            textColor = if (selected == type) NexioTheme.colorScheme.primary else NexioTheme.colorScheme.onSurface,
                            onClick = { selected = type; message = null },
                        )
                    }
                }
                PortSectionTitle("当前账本预览", subtitle = "与桌面使用相同布局和数据，预览内不执行跳转")
                if (widgets != null) {
                    widgets.Preview(selected, Modifier.fillMaxWidth())
                } else {
                    PortCard { Text("当前版本暂不可用", style = MaterialTheme.typography.bodyMedium) }
                }
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    SwitchPreference(
                        title = "显示金额",
                        summary = "与首页同步；关闭后，小部件同时隐藏金额和预算进度",
                        checked = amountsVisible,
                        enabled = !requesting,
                        onCheckedChange = { AppServices.appConfigSpi.switchHomeBillStatVisible(b = it) },
                    )
                    PortDivider()
                    PortActionRow(
                        title = "尺寸随桌面调整",
                        subtitle = "添加后长按小部件调整大小，窄版展示重点金额，宽版展示更多收支。",
                    )
                }
                PortPrimaryButton(
                    text = if (requesting) "正在准备预览…" else "添加「$label」到桌面",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = widgets != null && !requesting,
                    onClick = {
                        if (!requesting) {
                            requesting = true
                            message = null
                            val requested = selected
                            scope.launch {
                                try {
                                    val result = widgets?.requestPinWidget(context, requested) ?: PinRequestResult.Failed
                                    message = when (result) {
                                        PinRequestResult.Accepted -> "添加请求已发送，请在桌面系统提示中确认。"
                                        PinRequestResult.Unsupported -> "当前桌面不支持一键添加。请返回桌面并长按空白处，在小部件列表中选择「$label」。"
                                        PinRequestResult.Rejected -> "桌面未接受添加请求。可以重试，或长按桌面空白处手动添加「$label」。"
                                        PinRequestResult.Failed -> "暂时无法请求添加。请重试，或长按桌面空白处手动添加「$label」。"
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    message = "暂时无法请求添加，请重试。"
                                } finally {
                                    requesting = false
                                }
                            }
                        }
                    },
                )
                message?.let {
                    Text(it, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "记账或调整预算后会自动刷新；系统可能因省电策略延后后台更新。",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package com.xiaojinzi.tally.module.qianji

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.theme.AppTheme
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortCardExpand
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortPrimaryButton
import com.xiaojinzi.tally.module.base.theme.PortSectionTitle
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.qianji.authorization.AccessibilityAuthorizationController
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordPreferences
import com.xiaojinzi.tally.module.qianji.autorecord.AutoRecordState
import com.xiaojinzi.tally.module.qianji.autorecord.PaymentNotifications
import com.xiaojinzi.tally.module.qianji.autorecord.PendingPayments

@RouterAnno(hostAndPath = AppRouterConfig.QIANJI_AUTO_RECORD)
class QianjiAutoRecordActivity : ComponentActivity() {
    private var revision by mutableIntStateOf(0)
    private lateinit var authorization: AccessibilityAuthorizationController
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authorization = AccessibilityAuthorizationController(this)
        setContent { AppTheme { PortTheme { Content() } } }
    }

    override fun onResume() {
        super.onResume()
        PendingPayments.load(this)
        authorization.refresh()
        revision++
    }

    @Composable
    private fun Content() {
        val status by AutoRecordState.status.collectAsState()
        val pending by PendingPayments.items.collectAsState()
        val grant by authorization.state.collectAsState()
        var enabled by remember { mutableStateOf(AutoRecordPreferences.enabled(this)) }
        var overlay by remember { mutableStateOf(AutoRecordPreferences.overlay(this)) }
        val notifications = remember(revision) { PaymentNotifications.availability(this) }
        val notificationsReady = notifications == PaymentNotifications.Availability.READY ||
            notifications == PaymentNotifications.Availability.CHANNEL_SILENT
        PortPage(
            title = "小亚自动记账",
            subtitle = "识别支付结果，由你核对并确认",
            onBack = { finish() },
        ) {
            PortCard(contentPadding = PaddingValues(0.dp)) {
                Column(
                    modifier = Modifier.padding(PortDesign.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                ) {
                    PortStatusPill(
                        text = when {
                            !enabled -> "支付识别已关闭"
                            !grant.systemServiceEnabled -> "等待开启无障碍服务"
                            !grant.serviceConnected -> "等待服务连接"
                            else -> "已准备好识别"
                        },
                        emphasized = enabled && grant.systemServiceEnabled && grant.serviceConnected,
                    )
                    Text(
                        "仅处理支持的支付页面。金额有歧义时不记账，不自动操作支付应用。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PortDivider()
                PortActionRow(
                    title = "支付识别",
                    subtitle = if (enabled) "支付宝、微信、云闪付、数字人民币" else "默认关闭，开启后仍需系统无障碍授权",
                    trailing = {
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            AutoRecordPreferences.setEnabled(this@QianjiAutoRecordActivity, it)
                        })
                    },
                )
            }
            PortSectionTitle("权限与提醒")
            PortCard(contentPadding = PaddingValues(0.dp)) {
                PortActionRow(
                    title = "系统无障碍开关",
                    subtitle = if (grant.systemServiceEnabled) "本应用服务已在系统中开启" else "尚未开启 · 可前往系统设置",
                    onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    trailing = {
                        PortStatusPill(
                            if (grant.systemServiceEnabled) "已开启" else "未开启",
                            emphasized = grant.systemServiceEnabled,
                        )
                    },
                )
                PortDivider()
                PortActionRow(
                    title = "服务连接",
                    subtitle = if (grant.serviceConnected) "无障碍服务已连接" else "开启系统开关后重新检测",
                    trailing = {
                        PortStatusPill(
                            if (grant.serviceConnected) "已连接" else "未连接",
                            emphasized = grant.serviceConnected,
                        )
                    },
                )
                PortDivider()
                PortActionRow(
                    title = "通知提醒",
                    subtitle = when (notifications) {
                        PaymentNotifications.Availability.READY -> "已就绪 · 点击通知核对账单"
                        PaymentNotifications.Availability.CHANNEL_SILENT -> "已允许 · 提醒频道为静默"
                        PaymentNotifications.Availability.PERMISSION_REQUIRED -> "需要通知权限 · 待确认记录仍保留在本页"
                        PaymentNotifications.Availability.APP_DISABLED -> "应用通知已关闭 · 待确认记录仍保留在本页"
                        PaymentNotifications.Availability.CHANNEL_DISABLED -> "提醒频道已关闭 · 待确认记录仍保留在本页"
                        else -> "通知状态暂不可用 · 待确认记录仍保留在本页"
                    },
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        }
                    },
                    trailing = {
                        PortStatusPill(
                            when (notifications) {
                                PaymentNotifications.Availability.READY -> "已就绪"
                                PaymentNotifications.Availability.CHANNEL_SILENT -> "静默"
                                PaymentNotifications.Availability.PERMISSION_REQUIRED -> "需授权"
                                PaymentNotifications.Availability.APP_DISABLED,
                                PaymentNotifications.Availability.CHANNEL_DISABLED -> "已关闭"
                                else -> "不可用"
                            },
                            emphasized = notificationsReady,
                            isError = !notificationsReady,
                        )
                    },
                )
                PortDivider()
                PortActionRow(
                    title = "悬浮确认卡",
                    subtitle = "由已连接的无障碍服务显示，15 秒后收起；记录保留在待确认列表",
                    trailing = {
                        Switch(checked = overlay, onCheckedChange = {
                            overlay = it
                            AutoRecordPreferences.setOverlay(this@QianjiAutoRecordActivity, it)
                        })
                    },
                )
            }
            PortSectionTitle("辅助授权")
            PortCard(contentPadding = PaddingValues(0.dp)) {
                Text(
                    "可用 Root 或 Shizuku 辅助开启本应用的无障碍服务。只在点击时执行，保留其他无障碍服务设置。",
                    modifier = Modifier.padding(PortDesign.CardPadding),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PortDivider()
                PortActionRow(
                    title = "电脑 ADB 授权",
                    subtitle = if (grant.secureSettingsGranted) "已获得安全设置权限" else "复制命令到已连接的电脑执行，再返回本页",
                    enabled = !grant.busy,
                    onClick = {
                        val command = authorization.adbGrantCommand()
                        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                            ClipData.newPlainText("ADB 授权命令", command),
                        )
                        android.widget.Toast.makeText(this@QianjiAutoRecordActivity, "已复制 ADB 授权命令", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    trailing = {
                        PortStatusPill(
                            if (grant.secureSettingsGranted) "已授权" else "复制命令",
                            emphasized = grant.secureSettingsGranted,
                        )
                    },
                )
                PortDivider()
                PortActionRow(
                    title = "一键开启无障碍",
                    subtitle = if (grant.secureSettingsGranted) "使用已授予的权限开启本应用服务" else "需先执行电脑 ADB 命令，或使用 Root / Shizuku",
                    enabled = !grant.busy,
                    onClick = { authorization.requestWithSecureSettings() },
                )
                PortDivider()
                PortActionRow(
                    title = "Root 一键开启",
                    subtitle = "已 Root 设备可使用，请在 Root 管理器中允许",
                    enabled = !grant.busy,
                    onClick = { authorization.requestRoot() },
                )
                PortDivider()
                PortActionRow(
                    title = "Shizuku（ADB）一键开启",
                    subtitle = grant.shizukuStatus,
                    enabled = !grant.busy,
                    onClick = { authorization.requestShizuku() },
                )
                Column(
                    modifier = Modifier.padding(PortDesign.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PortDesign.SmallGap, androidx.compose.ui.Alignment.End),
                    ) {
                        PortSecondaryButton(text = "打开 Shizuku", onClick = {
                            val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (launch != null) startActivity(launch)
                            else android.widget.Toast.makeText(this@QianjiAutoRecordActivity, "请先安装并启动 Shizuku", android.widget.Toast.LENGTH_LONG).show()
                        }, enabled = !grant.busy)
                        PortSecondaryButton(text = "刷新状态", onClick = { authorization.recheck(); revision++ }, enabled = !grant.busy)
                    }
                    Column {
                        PortCardExpand(visible = grant.busy || grant.message != null) {
                            Column(
                                modifier = Modifier.padding(bottom = PortDesign.ItemGap),
                                verticalArrangement = Arrangement.spacedBy(PortDesign.SmallGap),
                            ) {
                                if (grant.busy) {
                                    PortStatusPill("正在等待授权或验证服务连接…")
                                }
                                grant.message?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (grant.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Text(
                            "Shizuku 可通过无线调试或电脑 ADB 启动，也支持 Root。辅助授权后仍需开启上方的支付识别开关。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            PortSectionTitle("待确认", subtitle = "最多保留 32 条，超过 24 小时自动过期")
            if (pending.isEmpty()) {
                PortCard {
                    Text("还没有待确认的支付记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            pending.asReversed().forEach { item ->
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    PortActionRow(
                        title = "${if (item.payment.income) "收入" else "支出"} ¥${PaymentNotifications.money(item.payment.amountFen)}",
                        subtitle = (if (item.synthetic) "自检 · " else "") + item.payment.merchant,
                        onClick = { startActivity(PaymentNotifications.intent(this@QianjiAutoRecordActivity, item.id)) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PortSecondaryButton(text = "忽略", onClick = {
                            PendingPayments.remove(this@QianjiAutoRecordActivity, item.id)
                            PaymentNotifications.cancel(this@QianjiAutoRecordActivity, item.id)
                        })
                    }
                }
            }
            PortSectionTitle("识别自检")
            PortCard {
                Column(verticalArrangement = Arrangement.spacedBy(PortDesign.ItemGap)) {
                    Text(
                        "自检会展示合成支付页面，由真实无障碍事件读取节点、识别、去重，再弹出悬浮卡和通知。它不代表真实支付 App 的适配率。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PortPrimaryButton(
                        text = "打开识别自检",
                        onClick = { startActivity(Intent(this@QianjiAutoRecordActivity, QianjiRecognitionLabActivity::class.java)) },
                    )
                    Text(
                        "${status.sampled} 次采集 · ${status.matched} 次识别 · ${status.duplicate} 次重复",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(status.lastResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::authorization.isInitialized) authorization.close()
        super.onDestroy()
    }
}

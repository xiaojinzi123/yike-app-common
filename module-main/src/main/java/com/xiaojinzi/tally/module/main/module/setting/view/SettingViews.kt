package com.xiaojinzi.tally.module.main.module.setting.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.smalltitle.SmallTitle
import com.haooz.nexioui.switchpreference.SwitchPreference
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.component.impl.Router
import com.xiaojinzi.component.impl.routeApi
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.bean.StringItemDto
import com.xiaojinzi.support.compose.util.contentWithComposable
import com.xiaojinzi.support.ktx.shake
import com.xiaojinzi.support.ktx.toStringItemDto
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.lib.res.model.support.LocalImageItemDto
import com.xiaojinzi.tally.lib.res.model.support.rememberPainter
import com.xiaojinzi.tally.lib.res.model.support.toLocalImageItemDto
import com.xiaojinzi.tally.module.base.support.AppRouterConfig
import com.xiaojinzi.tally.module.base.support.AppRouterMainApi
import com.xiaojinzi.tally.module.base.support.AppRouterUserApi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.PortWidgetPicker
import com.xiaojinzi.tally.module.base.theme.WallpaperSettingsRow
import com.xiaojinzi.tally.module.main.module.setting.domain.AppIconSwitcher
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingSwitchView1(
    modifier: Modifier = Modifier,
    image: LocalImageItemDto,
    title: StringItemDto,
    value: Boolean,
    subtitle: String? = null,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    SwitchPreference(
        modifier = modifier.fillMaxWidth(),
        title = title.contentWithComposable(),
        summary = subtitle,
        checked = value,
        enabled = onCheckedChange != null,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onCheckedChange = { onCheckedChange?.invoke(it) },
        startAction = {
            Box(
                modifier = Modifier
                    .size(PortDesign.IconContainer)
                    .clip(PortDesign.SmallShape)
                    .background(NexioTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = image.rememberPainter(),
                    contentDescription = null,
                    modifier = Modifier.size(PortDesign.IconSize),
                    tint = NexioTheme.colorScheme.primary.copy(
                        alpha = if (onCheckedChange != null) 1f else 0.38f,
                    ),
                )
            }
        },
    )
}

@Composable
fun SettingActionView1(
    modifier: Modifier = Modifier,
    image: LocalImageItemDto,
    title: StringItemDto,
    onclick: () -> Unit = {},
) {
    PortActionRow(
        modifier = modifier,
        title = title.contentWithComposable(),
        icon = image.rememberPainter(),
        onClick = onclick,
    )
}

@Composable
internal fun WidgetSettingsRow() {
    var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
    PortActionRow(
        title = "桌面小部件",
        subtitle = "预览收支与预算，选择样式并添加到桌面",
        icon = rememberVectorPainter(Icons.Rounded.Widgets),
        onClick = { showWidgetPicker = true },
        trailing = {
            PortSecondaryButton(
                text = "预览",
                onClick = { showWidgetPicker = true },
            )
        },
    )
    if (showWidgetPicker) {
        PortWidgetPicker(onDismiss = { showWidgetPicker = false })
    }
}

@Composable
private fun AppIconRow(
    current: AppIconSwitcher.IconOption,
    message: String?,
    onClick: () -> Unit,
) {
    PortActionRow(
        title = "应用图标",
        subtitle = message ?: "当前：${current.label} · 可随时切回原图标",
        icon = rememberVectorPainter(Icons.Rounded.Palette),
        onClick = onClick,
        trailing = { PortSecondaryButton(text = "更换", onClick = onClick) },
    )
}

@Composable
private fun AppIconPickerDialog(
    current: AppIconSwitcher.IconOption,
    onDismiss: () -> Unit,
    onSelected: (AppIconSwitcher.IconOption) -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexioTheme.colorScheme.surfaceContainer,
        title = { Text("应用图标", color = NexioTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                AppIconSwitcher.options.forEachIndexed { index, option ->
                    if (index > 0) PortDivider()
                    val selected = option.id == current.id
                    val bitmap = remember(option.id) {
                        AppIconSwitcher.iconBitmap(
                            context = context,
                            option = option,
                            sizePx = (44 * context.resources.displayMetrics.density).toInt(),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = PortDesign.ActionRowHeight)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(PortDesign.SmallShape),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                option.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = NexioTheme.colorScheme.onSurface,
                            )
                            Text(
                                option.description,
                                fontSize = 14.sp,
                                color = NexioTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        RadioButton(selected = selected, onClick = null)
                    }
                }
            }
        },
        confirmButton = { PortSecondaryButton(text = "取消", onClick = onDismiss) },
    )
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun SettingView(
    needInit: Boolean? = false,
) {
    val context = LocalContext.current
    val showTime by AppServices.appConfigSpi.isShowHourAndMinuteWhenBillCrudStateOb.collectAsState(
        initial = false,
    )
    val allowZero by AppServices.appConfigSpi.isAllowZeroAmountWhenBillCrudStateOb.collectAsState(
        initial = false,
    )
    val showAssets by AppServices.appConfigSpi.isShowAssetsTabStateOb.collectAsState(initial = false)
    val showAmounts by AppServices.appConfigSpi.isHomeBillStatVisibleStateOb.collectAsState(initial = false)
    val bookSelected by AppServices.tallyDataSourceSpi.selectedBookStateOb.collectAsState(initial = null)
    val themeIndex by AppServices.appInfoSpi.themeIndexStateOb.collectAsState(initial = 0)
    var showAppearancePicker by rememberSaveable { mutableStateOf(false) }
    var showIconPicker by rememberSaveable { mutableStateOf(false) }
    var iconOption by remember { mutableStateOf(AppIconSwitcher.current(context)) }
    var iconMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val vibrate by AppServices.appConfigSpi.isVibrateStateOb.collectAsState(initial = true)
    BusinessContentView<SettingViewModel>(needInit = needInit) {
        PortPage(
            title = "设置",
            subtitle = "让记账适合你的习惯",
            onBack = { context.tryFinishActivity() },
            actions = { PortStatusPill(text = "仅本机", emphasized = true) },
        ) {
            Column {
                SmallTitle(
                    text = "记账工具",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    PortActionRow(
                        title = "自动记账",
                        subtitle = "识别方式、权限与待确认账单",
                        icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_robot1),
                        onClick = {
                            Router.with(context).hostAndPath(AppRouterConfig.QIANJI_AUTO_RECORD).forward()
                        },
                    )
                    PortDivider()
                    PortActionRow(
                        title = "导入账单",
                        subtitle = "把已有记录带入当前记账流程",
                        icon = rememberVectorPainter(Icons.Rounded.FileDownload),
                        onClick = {
                            Router.with(context).hostAndPath(AppRouterConfig.QIANJI_IMPORT).forward()
                        },
                    )
                    PortDivider()
                    PortActionRow(
                        title = "月度预算",
                        subtitle = "当前账本的本月总预算与分类预算",
                        icon = rememberVectorPainter(Icons.Rounded.AccountBalanceWallet),
                        enabled = bookSelected != null,
                        onClick = {
                            if (bookSelected != null) {
                                val calendar = Calendar.getInstance()
                                Router.with(context = context)
                                    .hostAndPath(hostAndPath = AppRouterConfig.QIANJI_BUDGET)
                                    .putString("bookId", bookSelected?.id)
                                    .putString(
                                        "month",
                                        String.format(
                                            Locale.CHINA,
                                            "%04d-%02d",
                                            calendar[Calendar.YEAR],
                                            calendar[Calendar.MONTH] + 1,
                                        ),
                                    )
                                    .forward()
                            }
                        },
                    )
                }
            }

            Column {
                SmallTitle(
                    text = "记账偏好",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    SettingSwitchView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_shake1.toLocalImageItemDto(),
                        title = "数字键盘振动反馈".toStringItemDto(),
                        value = vibrate,
                    ) {
                        if (it) shake()
                        AppServices.appConfigSpi.switchVibrate(isVibrate = it)
                    }
                    PortDivider()
                    SettingSwitchView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_clock1.toLocalImageItemDto(),
                        title = "记账精确到时分".toStringItemDto(),
                        value = showTime,
                    ) {
                        AppServices.appConfigSpi.switchShowHourAndMinuteWhenBillCrud(b = it)
                    }
                    PortDivider()
                    SettingSwitchView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_zero1.toLocalImageItemDto(),
                        title = "允许零金额账单".toStringItemDto(),
                        value = allowZero,
                    ) {
                        AppServices.appConfigSpi.switchAllowZeroAmountWhenBillCrudStateOb(b = it)
                    }
                }
            }

            Column {
                SmallTitle(
                    text = "显示与外观",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    SettingSwitchView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_account1.toLocalImageItemDto(),
                        title = "显示资产导航".toStringItemDto(),
                        subtitle = "在底栏查看现金、银行卡等资产账户",
                        value = showAssets,
                    ) {
                        AppServices.appConfigSpi.switchShowAssetsStateOb(b = it)
                    }
                    PortDivider()
                    SettingSwitchView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_eye1.toLocalImageItemDto(),
                        title = "显示首页金额".toStringItemDto(),
                        subtitle = "同时控制首页与桌面小部件的金额、预算进度",
                        value = showAmounts,
                    ) {
                        AppServices.appConfigSpi.switchHomeBillStatVisible(b = it)
                    }
                    PortDivider()
                    PortActionRow(
                        title = "外观",
                        subtitle = when (themeIndex) {
                            1 -> "浅色"
                            2 -> "深色"
                            else -> "跟随系统"
                        },
                        icon = painterResource(com.xiaojinzi.tally.lib.res.R.drawable.res_theme1),
                        onClick = { showAppearancePicker = true },
                        trailing = {
                            PortSecondaryButton(
                                text = "切换",
                                onClick = { showAppearancePicker = true },
                            )
                        },
                    )
                    PortDivider()
                    WallpaperSettingsRow()
                    PortDivider()
                    SettingActionView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_theme1.toLocalImageItemDto(),
                        title = "主题配色".toStringItemDto(),
                    ) {
                        AppRouterMainApi::class.routeApi().toThemeSelectView(context = context)
                    }
                }
            }

            Column {
                SmallTitle(
                    text = "应用与桌面",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    WidgetSettingsRow()
                    PortDivider()
                    AppIconRow(
                        current = iconOption,
                        message = iconMessage,
                        onClick = {
                            iconMessage = null
                            showIconPicker = true
                        },
                    )
                    PortDivider()
                    SettingActionView1(
                        image = com.xiaojinzi.tally.lib.res.R.drawable.res_warn1.toLocalImageItemDto(),
                        title = "关于与开源许可".toStringItemDto(),
                    ) {
                        AppRouterUserApi::class.routeApi().toAboutUsView(context = context)
                    }
                }
            }
        }
        if (showAppearancePicker) {
            AlertDialog(
                onDismissRequest = { showAppearancePicker = false },
                containerColor = NexioTheme.colorScheme.surfaceContainer,
                title = { Text("外观", color = NexioTheme.colorScheme.onSurface) },
                text = {
                    Column(modifier = Modifier.selectableGroup()) {
                        listOf(0 to "跟随系统", 1 to "浅色", 2 to "深色").forEach { (index, label) ->
                            if (index > 0) PortDivider()
                            val selected = if (index == 0) themeIndex !in 1..2 else themeIndex == index
                            PortActionRow(
                                modifier = Modifier.selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        AppServices.appInfoSpi.switchTheme(themeIndex = index)
                                        showAppearancePicker = false
                                    },
                                ),
                                title = label,
                                trailing = { RadioButton(selected = selected, onClick = null) },
                            )
                        }
                    }
                },
                confirmButton = {
                    PortSecondaryButton(
                        text = "完成",
                        onClick = { showAppearancePicker = false },
                    )
                },
            )
        }
        if (showIconPicker) {
            AppIconPickerDialog(
                current = iconOption,
                onDismiss = { showIconPicker = false },
                onSelected = { option ->
                    val switched = AppIconSwitcher.switchTo(context, option)
                    iconOption = AppIconSwitcher.current(context)
                    iconMessage = if (switched) {
                        "已切换到「${option.label}」图标，桌面可能需要几秒刷新"
                    } else {
                        "图标切换失败，请稍后重试"
                    }
                    showIconPicker = false
                },
            )
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun SettingViewWrap() {
    PortTheme { SettingView() }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun SettingViewPreview() {
    PortTheme { SettingView(needInit = false) }
}

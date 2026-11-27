package com.xiaojinzi.tally.module.main.module.theme_select.view

import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.nexioui.basiccomponent.BasicComponent
import com.haooz.nexioui.smalltitle.SmallTitle
import com.haooz.nexioui.theme.NexioTheme
import com.xiaojinzi.reactive.template.view.BusinessContentView
import com.xiaojinzi.support.ktx.tryFinishActivity
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.base.theme.PortActionRow
import com.xiaojinzi.tally.module.base.theme.PortCard
import com.xiaojinzi.tally.module.base.theme.PortDesign
import com.xiaojinzi.tally.module.base.theme.PortDivider
import com.xiaojinzi.tally.module.base.theme.PortPage
import com.xiaojinzi.tally.module.base.theme.PortSecondaryButton
import com.xiaojinzi.tally.module.base.theme.PortStatusPill
import com.xiaojinzi.tally.module.base.theme.PortTheme
import com.xiaojinzi.tally.module.base.theme.WallpaperSettingsRow
import com.xiaojinzi.tally.module.main.module.theme_select.domain.ThemeSelectIntent
import kotlinx.coroutines.InternalCoroutinesApi

@Composable
private fun ThemePalettePreview(
    item: ThemeSelectItemVo,
    isDark: Boolean,
    dynamicColors: ColorScheme?,
) {
    val fallback = dynamicColors ?: MaterialTheme.colorScheme
    val colors = when (item) {
        is ThemeNormalSelectItemVo -> if (isDark) {
            listOf(
                item.darkPrimary, item.darkSecondary, item.darkTertiary,
                item.darkPrimaryContainer, item.darkSecondaryContainer, item.darkTertiaryContainer,
            )
        } else {
            listOf(
                item.lightPrimary, item.lightSecondary, item.lightTertiary,
                item.lightPrimaryContainer, item.lightSecondaryContainer, item.lightTertiaryContainer,
            )
        }
        is ThemeSystemSelectItemVo -> listOf(
            fallback.primary, fallback.secondary, fallback.tertiary,
            fallback.primaryContainer, fallback.secondaryContainer, fallback.tertiaryContainer,
        )
    }
    Column(
        modifier = Modifier
            .size(PortDesign.IconContainer)
            .clip(PortDesign.SmallShape),
    ) {
        colors.chunked(3).forEach { rowColors ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(PortDesign.IconContainer / 2)
                            .background(color),
                    )
                }
            }
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun ThemeSelectView(
    needInit: Boolean? = null,
) {
    val context = LocalContext.current
    BusinessContentView<ThemeSelectViewModel>(needInit = needInit) { vm ->
        val isUseDark by AppServices.appInfoSpi.isDarkThemeStateOb.collectAsState(initial = false)
        val themeIndex by AppServices.appInfoSpi.themeIndexStateOb.collectAsState(initial = 0)
        val themeList by vm.themeListStateObVo.collectAsState(initial = emptyList())
        val selectedTheme = themeList.firstOrNull { it.isSelected }
        val dynamicColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isUseDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else null
        val openAppearance: () -> Unit = {
            vm.addIntent(ThemeSelectIntent.OnMoreClick(context = context))
        }

        PortPage(
            title = "主题配色",
            subtitle = "选择喜欢的颜色，外观随你的习惯调整",
            onBack = { context.tryFinishActivity() },
        ) {
            Column {
                SmallTitle(
                    text = "当前外观",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    PortActionRow(
                        title = "外观",
                        subtitle = when (themeIndex) {
                            1 -> "浅色"
                            2 -> "深色"
                            else -> "跟随系统"
                        },
                        onClick = openAppearance,
                        trailing = {
                            PortSecondaryButton(text = "切换", onClick = openAppearance)
                        },
                    )
                    PortDivider()
                    PortActionRow(
                        title = "当前配色",
                        subtitle = selectedTheme?.name ?: "正在读取配色",
                        trailing = {
                            PortStatusPill(
                                text = if (selectedTheme != null) "已应用" else "读取中",
                                emphasized = selectedTheme != null,
                            )
                        },
                    )
                }
            }

            Column {
                SmallTitle(
                    text = "选择配色",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(
                    modifier = Modifier.selectableGroup(),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    themeList.forEachIndexed { index, item ->
                        if (index > 0) PortDivider()
                        BasicComponent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = item.isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        vm.addIntent(
                                            ThemeSelectIntent.ThemeColorSet(themeName = item.themeName),
                                        )
                                    },
                                ),
                            title = item.name,
                            summary = if (item is ThemeSystemSelectItemVo) {
                                "跟随设备壁纸自动调整"
                            } else {
                                "同时支持浅色与深色外观"
                            },
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            startAction = {
                                ThemePalettePreview(
                                    item = item,
                                    isDark = isUseDark,
                                    dynamicColors = dynamicColors,
                                )
                            },
                            endActions = {
                                RadioButton(selected = item.isSelected, onClick = null)
                            },
                        )
                    }
                }
            }

            Column {
                SmallTitle(
                    text = "应用背景",
                    insideMargin = PaddingValues(start = 16.dp, bottom = 8.dp),
                )
                PortCard(contentPadding = PaddingValues(0.dp)) {
                    WallpaperSettingsRow()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    text = "系统配色会读取设备的动态颜色；更换壁纸后，应用配色也会随之调整。",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 13.sp,
                    color = NexioTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun ThemeSelectViewWrap() {
    PortTheme { ThemeSelectView() }
}

@InternalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun ThemeSelectViewPreview() {
    PortTheme { ThemeSelectView(needInit = false) }
}
